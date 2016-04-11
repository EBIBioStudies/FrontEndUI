/*
 * Copyright 2009-2016 European Molecular Biology Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package uk.ac.ebi.biostudies.utils.saxon.search;

import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.trans.XPathException;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.queries.mlt.MoreLikeThis;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.*;
import org.apache.lucene.util.BytesRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.biostudies.app.Application;
import uk.ac.ebi.biostudies.components.SaxonEngine;
import uk.ac.ebi.biostudies.utils.StringTools;
import uk.ac.ebi.biostudies.utils.saxon.SaxonException;
import uk.ac.ebi.biostudies.utils.search.EFOExpandedHighlighter;
import uk.ac.ebi.microarray.biostudies.shared.auth.User;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Querier {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private IndexEnvironment env;
    public Querier(IndexEnvironment env) {
        this.env = env;
    }

    public List<String> getTerms(String fieldName, int minFreq) throws IOException {
        List<String> termsList = new ArrayList<>();

        try (IndexReader reader = DirectoryReader.open(this.env.indexDirectory)) {
            Terms terms = MultiFields.getTerms(reader, fieldName);
            if (null != terms) {
                TermsEnum iterator = terms.iterator();
                BytesRef byteRef;
                while((byteRef = iterator.next()) != null) {
                    if (iterator.docFreq() >= minFreq) {
                        termsList.add(byteRef.utf8ToString());
                    }
                }
            }
        }
        return termsList;
    }

    public void dumpTerms(String fieldName) throws IOException {
        try (IndexReader reader = DirectoryReader.open(this.env.indexDirectory)) {
            Terms terms = MultiFields.getTerms(reader, fieldName);
            if (null != terms) {
                File f = new File(System.getProperty("java.io.tmpdir"), fieldName + "_terms.txt");
                try (BufferedWriter w = new BufferedWriter(new FileWriter(f))) {
                    StringBuilder sb = new StringBuilder();
                    TermsEnum iterator = terms.iterator();
                    BytesRef byteRef;
                    while ((byteRef = iterator.next()) != null) {
                        sb.append(iterator.docFreq()).append('\t').append(byteRef.utf8ToString()).append(StringTools.EOL);
                    }
                    w.write(sb.toString());
                }
            }
        }
    }

    public Integer getDocCount(Query query) throws IOException {
        try (IndexReader reader = DirectoryReader.open(this.env.indexDirectory)) {
            IndexSearcher searcher = new IndexSearcher(reader);

            // +1 is a trick to prevent from having an exception thrown if documentNodes.size() value is 0
            TopDocs hits = searcher.search(query, Integer.MAX_VALUE);


            return hits.totalHits;
        }
    }

    public List<NodeInfo> query(QueryInfo queryInfo) throws ParseException, IOException, SaxonException {
        Query query = queryInfo.getQuery();
        Map<String, String[]> params = queryInfo.getParams();
        boolean queryIsEmpty = params.containsKey("queryIsEmpty");
        boolean isDetailPage = params.get("path-info")[0].contains("detail");
        String sortBy =  (params.containsKey("sortby")) ? params.get("sortby")[0] : null;

        if (sortBy==null ) {
            sortBy = queryIsEmpty ? "release_date" : "relevance";
            params.put("sortby", new String[]{sortBy});
        }

        SortField.Type sortFieldType = (sortBy != null && !"relevance".equalsIgnoreCase(sortBy)) ?
                (env.fields.containsKey(sortBy) && "string".equalsIgnoreCase(env.fields.get(sortBy).type) ?
                        SortField.Type.STRING
                        : SortField.Type.LONG)
                : SortField.Type.SCORE;

        // set default sorting order if none is specified. Changes should be reflected in jquery.bs.studies-browse-*.js too.
        if (!params.containsKey("sortorder")) {
            if ("accession".equalsIgnoreCase(sortBy) || "title".equalsIgnoreCase(sortBy) || "authors".equalsIgnoreCase(sortBy))
                params.put("sortorder", new String[]{"ascending"});
            else
                params.put("sortorder", new String[]{"descending"});
        }
        boolean shouldReverse =  ("descending".equalsIgnoreCase(params.get("sortorder")[0]) ? true : false);

        // relevance should by default be descending
        if (sortBy ==null || "relevance".equalsIgnoreCase(sortBy) ) {
            shouldReverse = !shouldReverse;
        }
        SortField sortField = new SortField(sortBy, sortFieldType, shouldReverse);
        Sort sort = new Sort( sortField );

        try (IndexReader reader = DirectoryReader.open(this.env.indexDirectory)) {
            LeafReader leafReader = SlowCompositeReaderWrapper.wrap(reader);
            IndexSearcher searcher = new IndexSearcher(reader);
            logger.info("Search of index [{}] with query [{}] started sorted by {} reversed={}", env.indexId,
                    query.toString(), sort, shouldReverse);
            TopDocs hits = searcher.search(
                    query,
                    isDetailPage ? 1 : Integer.MAX_VALUE,
                    sort
            );
            logger.info("Search reported [{}] matches", hits.totalHits);

            if (hits.totalHits==0 && params.containsKey("keywords")) {
                String q = params.get("keywords")[0];
                String[] suggestions = this.env.spellChecker.suggestSimilar(q,5);
                if (suggestions.length>0) {
                    params.put("suggestions", suggestions);
                }
            }

            final List<NodeInfo> matchingNodes = new ArrayList<>();

            // if page is from search results, get the first hit only since it should be the one with the accession match
            if (isDetailPage && hits.totalHits>0) {
                NodeInfo nodeInfo = Application.getAppComponent(SaxonEngine.class).buildDocument(
                                leafReader.document(hits.scoreDocs[0].doc).get("xml"));
                if(nodeInfo!=null) {
                    matchingNodes.add(nodeInfo);
                    getSimilarStudies(params,hits.scoreDocs[0],leafReader,searcher);
                }
            } else {
                int page = params.containsKey("page") ? Integer.parseInt(params.get("page")[0].toString()) : 1;
                int pageSize = params.containsKey("pagesize") ? Integer.parseInt(params.get("pagesize")[0].toString()) : 25;
                int from = 1+ ( page - 1 ) * pageSize <  hits.totalHits ? 1+ ( page - 1 ) * pageSize : 1;
                int to =  ( from + pageSize - 1 ) > hits.totalHits ? hits.totalHits : from + pageSize - 1;
                params.put("total", new String[]{hits.totalHits+""});
                params.put("from", new String[]{from+""});
                params.put("to", new String[]{to + ""});
                ScoreDoc [] scoreDocs = hits.scoreDocs;
                for (int i = from - 1; i < to; i++) {
                    try {
                        NodeInfo nodeInfo = getNodeInfo(leafReader.document(scoreDocs[i].doc), params);
                        if(nodeInfo!=null) matchingNodes.add(nodeInfo);
                    } catch (SaxonException e) {
                        e.printStackTrace();
                    }
                }
                addHighlights(queryInfo, params, leafReader, from, to, scoreDocs);
            }

            logger.info("Search completed {}", matchingNodes.size());

            addProjectParameters(params, reader, searcher);

            return matchingNodes;
        }
    }

    private NodeInfo getNodeInfo(Document doc, Map<String, String[]> params) throws SaxonException {
        NodeInfo nodeInfo = null;
        if (!params.containsKey("fo")) {
            nodeInfo= Application.getAppComponent(SaxonEngine.class).buildDocument(doc.get("xml"));
        } else {
            StringBuffer sb = new StringBuffer("<study>");
            for (IndexableField field : doc.getFields()) {
                String fieldName = field.name();
                if (fieldName.equalsIgnoreCase("xml") || fieldName.equalsIgnoreCase("keywords")) continue;
                sb.append("<").append(fieldName).append(">");
                sb.append(StringEscapeUtils.escapeXml11(field.stringValue()));
                sb.append("</").append(fieldName).append(">");
            }
            sb.append("</study>");
            nodeInfo = Application.getAppComponent(SaxonEngine.class).buildDocument(sb.toString());
        }
        return nodeInfo;
    }

    private void addProjectParameters(Map<String, String[]> params, IndexReader reader, IndexSearcher searcher) throws ParseException, IOException, SaxonException {
        if (params.containsKey("project")) {
            Map<String, String[]> querySource = new HashMap<>();
            querySource.put("accession", params.get("project"));
            querySource.put("allow", params.get("allow"));
            querySource.put("deny", params.get("deny"));
            QueryConstructor qc = new QueryConstructor();
            Query projectQuery = qc.construct(this.env, querySource);
            projectQuery = qc.getAccessControlledQuery(projectQuery, this.env, querySource);
            TopDocs results = searcher.search(projectQuery, 1);
            if (results != null && results.totalHits == 1) {
                SaxonEngine saxon =  Application.getAppComponent(SaxonEngine.class);
                NodeInfo projectXML = saxon.buildDocument(reader.document(results.scoreDocs[0].doc).get("xml"));
                try {
                    params.put("project-title", new String[] {
                            saxon.evaluateXPathSingleAsString(projectXML, "study/title")});
                    params.put("project-description", new String[] {
                            saxon.evaluateXPathSingleAsString(projectXML,
                                    "study/attribute[lower-case(@name)='description']/value")});
                    params.put("project-url", new String[] {
                            saxon.evaluateXPathSingleAsString(projectXML,
                                    "study/attribute[lower-case(@name)='url']/value")});
                } catch (XPathException e) {
                    throw new SaxonException(e);
                }
            } else {
                params.remove("project" );
            }
        }
    }

    private void addHighlights(QueryInfo queryInfo, Map<String, String[]> params, LeafReader leafReader, int from, int to, ScoreDoc[] scoreDocs) throws IOException {
        //do highlighting for fields shown on search page
        ArrayList<String> accessions = new ArrayList<>();
        ArrayList<String> titles = new ArrayList<>();
        ArrayList<String> authors = new ArrayList<>();
        ArrayList<String> snippets = new ArrayList<>();
        EFOExpandedHighlighter highlighter = new EFOExpandedHighlighter();
        highlighter.setEnvironment(this.env);
        for (int i = from - 1; i < to; i++) {
            String accession = leafReader.document(scoreDocs[i].doc).get("id");
            accessions.add(highlighter.highlightFragment(queryInfo, "id", accession));
            String title = leafReader.document(scoreDocs[i].doc).get("title");
            titles.add(highlighter.highlightFragment(queryInfo, "title", title));
            String author = leafReader.document(scoreDocs[i].doc).get("authors");
            authors.add(highlighter.highlightFragment(queryInfo, "authors", author));
            String snippet = leafReader.document(scoreDocs[i].doc).get("keywords");
            String highlightedSnippet = highlighter.highlightFragment(queryInfo, "keywords", snippet);
            snippets.add( snippet.length() == highlightedSnippet.length() ? "" : highlightedSnippet );

        }
        params.put("accessions", accessions.toArray(new String[accessions.size()]));
        params.put("titles", titles.toArray(new String[titles.size()]));
        params.put("authors", authors.toArray(new String[authors.size()]));
        params.put("fragments", snippets.toArray(new String[snippets.size()]));
    }

    private void getSimilarStudies(Map<String, String[]> params, ScoreDoc scoreDoc, IndexReader leafReader, IndexSearcher searcher) throws ParseException, IOException {
        if (!params.containsKey("accessionNumber")) return;
        int maxHits = 3;
        MoreLikeThis mlt = new MoreLikeThis(leafReader);
        mlt.setFieldNames(new String[]{"keywords"});
        mlt.setAnalyzer(this.env.indexAnalyzer);
        QueryConstructor qc = new QueryConstructor();
        String likeQuery = mlt.like( scoreDoc.doc).toString().replaceAll(":-",":").replaceAll("keywords: "," ");
        if (likeQuery.endsWith("keywords:")) likeQuery = likeQuery.substring(0,likeQuery.length()-9);
        Query mltQuery = qc.construct(this.env,likeQuery
                + " NOT type:project NOT accession:"
                + params.get("accessionNumber")[0]);// remove projects and self
        TopDocs mltDocs = searcher.search( qc.getAccessControlledQuery(mltQuery, this.env, params) , maxHits);
        String[] titles = new String[mltDocs.scoreDocs.length];
        String[] accessions = new String[mltDocs.scoreDocs.length];
        for (int i = 0; i < mltDocs.scoreDocs.length; i++) {
            accessions[i]= leafReader.document(mltDocs.scoreDocs[i].doc).get("accession");
            titles[i] = leafReader.document(mltDocs.scoreDocs[i].doc).get("title");
        }
        if (mltDocs.totalHits>0) {
            params.put("similarTitles", titles);
            params.put("similarAccessions", accessions);
        }
    }

    public String getDocumentXml(String accession, User user) {
        try {
            Map<String, String[]> querySource = new HashMap<>();
            querySource.put("accession", new String[]{accession});
            if (user!=null) {
                querySource.put("allow", user.getAllow());
                querySource.put("deny", user.getDeny());
            } else {
                querySource.put("allow", new String[]{"public"});
            }
            QueryConstructor qc = new QueryConstructor();
            Query query = qc.construct(this.env, querySource);
            query = qc.getAccessControlledQuery(query, this.env, querySource);
            try (IndexReader reader = DirectoryReader.open(this.env.indexDirectory)) {
                IndexSearcher searcher = new IndexSearcher(reader);
                TopDocs hits = searcher.search(query, 1);
                if (hits != null && hits.totalHits == 1) {
                    return reader.document(hits.scoreDocs[0].doc).get("xml");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}