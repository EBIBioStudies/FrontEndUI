/*
 * Copyright 2009-2015 European Molecular Biology Laboratory
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

package uk.ac.ebi.arrayexpress.utils.saxon.search;

import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.pattern.NameTest;
import net.sf.saxon.s9api.Axis;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.type.Type;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.*;
import org.apache.lucene.search.highlight.*;
import org.apache.lucene.util.BytesRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.arrayexpress.app.Application;
import uk.ac.ebi.arrayexpress.components.SaxonEngine;
import uk.ac.ebi.arrayexpress.utils.StringTools;
import uk.ac.ebi.arrayexpress.utils.saxon.SaxonException;
import uk.ac.ebi.arrayexpress.utils.search.EFOExpandedHighlighter;
import uk.ac.ebi.microarray.arrayexpress.shared.auth.User;

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
                    Integer.MAX_VALUE,
                    sort
            );

            logger.info("Search reported [{}] matches", hits.totalHits);
            final List<NodeInfo> matchingNodes = new ArrayList<>();
            final NumericDocValues ids = leafReader.getNumericDocValues(Indexer.DOCID_FIELD);

            int page = params.containsKey("page") ? Integer.parseInt(params.get("page")[0].toString()) : 1;
            int pageSize = params.containsKey("pagesize") ? Integer.parseInt(params.get("pagesize")[0].toString()) : 25;
            int from = 1+ ( page - 1 ) * pageSize <  hits.totalHits ? 1+ ( page - 1 ) * pageSize : 1;
            int to =  ( from + pageSize - 1 ) > hits.totalHits ? hits.totalHits : from + pageSize - 1;
            params.put("total", new String[]{hits.totalHits+""});
            params.put("from", new String[]{from+""});
            params.put("to", new String[]{to + ""});


            // if page is from search results, get the document at nth position in the search results
            // and store the previous and next result as well. Otherwise, return the whole result set
            if (params.get("path-info")[0].contains("detail")) {
                NodeInfo nodeInfo = getSingleDocument(params, hits, leafReader);
                if(nodeInfo!=null) matchingNodes.add(nodeInfo);
            } else {
                ScoreDoc [] scoreDocs = hits.scoreDocs;
                for (int i = from - 1; i < to; i++) {
                    try {
                        NodeInfo nodeInfo = Application.getAppComponent(SaxonEngine.class)
                                .buildDocument(leafReader.document(scoreDocs[i].doc).get("xml"));
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

    private NodeInfo getSingleDocument(Map<String, String[]> params, TopDocs hits, IndexReader leafReader) throws IOException, SaxonException {
        ScoreDoc[] scoreDocs = hits.scoreDocs;
        int position = -1;
        for (int i = 0; i < scoreDocs.length; i++) {
            if (leafReader.document(scoreDocs[i].doc).get("accession").equalsIgnoreCase(params.get("accessionNumber")[0])) {
                position=i;
                break;
            }
        }


        try {
            NodeInfo ni = Application.getAppComponent(SaxonEngine.class).buildDocument(leafReader.document(scoreDocs[position].doc).get("xml"));
            params.put("accessionNumber", new String[]{ni.iterateAxis(Axis.DESCENDANT.getAxisNumber(), new NameTest(Type.ELEMENT, "", "accession", ni.getNamePool())).next().getStringValue()});
            params.put("accessionIndex", new String[]{"" + position});

            if (position > 0) {
                NodeInfo prev = Application.getAppComponent(SaxonEngine.class).buildDocument(leafReader.document(scoreDocs[position - 1].doc).get("xml"));
                params.put("previousAccession", new String[]{prev.iterateAxis(Axis.DESCENDANT.getAxisNumber(), new NameTest(Type.ELEMENT, "", "accession", ni.getNamePool())).next().getStringValue()});
            }
            if (position < hits.totalHits - 1) {
                NodeInfo next = Application.getAppComponent(SaxonEngine.class).buildDocument(leafReader.document(scoreDocs[position + 1].doc).get("xml"));
                params.put("nextAccession", new String[]{next.iterateAxis(Axis.DESCENDANT.getAxisNumber(), new NameTest(Type.ELEMENT, "", "accession", ni.getNamePool())).next().getStringValue()});
            }
            return ni;

        } catch (ArrayIndexOutOfBoundsException ex) {
            logger.warn("Trying to load an inaccessible document");
            return null;
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
