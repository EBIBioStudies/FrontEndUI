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
import net.sf.saxon.s9api.Axis;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.*;
import org.apache.lucene.util.BytesRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.arrayexpress.app.Application;
import uk.ac.ebi.arrayexpress.components.SaxonEngine;
import uk.ac.ebi.arrayexpress.utils.StringTools;
import uk.ac.ebi.arrayexpress.utils.saxon.SaxonException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
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
            TopDocs hits = searcher.search(query, this.env.documentNodes.size() + 1);


            return hits.totalHits;
        }
    }

    public List<NodeInfo> query(QueryInfo queryInfo) throws ParseException, IOException, SaxonException {
        Query query = queryInfo.getQuery();
        Map<String, String[]> params = queryInfo.getParams();
        String sortBy =  (params.containsKey("sortby")) ? params.get("sortby")[0] : null;

        // empty query returns everything
        if (query instanceof BooleanQuery && ((BooleanQuery) query).clauses().isEmpty()) {
            if (sortBy==null) {
                sortBy = "release_date";
                params.put("sortby", new String[]{"release_date"});
            }
            logger.info("Empty search, returning all [{}] documents", this.env.documentNodes.size());
            Term term = new Term("title", "*");
            query = new WildcardQuery(term);
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
        boolean reverse =  ("descending".equalsIgnoreCase(params.get("sortorder")[0]) ? true : false);

        // relevance should by default be descending
        if (sortBy ==null || "relevance".equalsIgnoreCase(sortBy) ) {
            reverse = !reverse;
        }
        SortField sortField = new SortField(sortBy, sortFieldType  , reverse);
        Sort sort = new Sort( sortField );

        try (IndexReader reader = DirectoryReader.open(this.env.indexDirectory)) {
            LeafReader leafReader = SlowCompositeReaderWrapper.wrap(reader);
            IndexSearcher searcher = new IndexSearcher(reader);

            logger.info("Search of index [{}] with query [{}] started sorted by {} reversed={}", env.indexId,
                    query.toString(), sort, reverse);


            TopDocs hits = searcher.search(
                    query,
                    this.env.documentNodes.size() + 1,
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
            if (params.containsKey("n")) {
               matchingNodes.add(getSingleDocument(params, hits, leafReader));
            } else {
                ScoreDoc [] scoreDocs = hits.scoreDocs;
                for (int i = from - 1; i < to; i++) {
                    try {
                        matchingNodes.add(
                                Application.getAppComponent(SaxonEngine.class)
                                        .buildDocument(leafReader.document(scoreDocs[i].doc).get("xml"))
                        );
                    } catch (SaxonException e) {
                        e.printStackTrace();
                    }
                }
            }

            logger.info("Search completed {}", matchingNodes.size());

            return matchingNodes;
        }
    }

    private NodeInfo getSingleDocument(Map<String, String[]> params, TopDocs hits, IndexReader leafReader) throws IOException, SaxonException {
        int position = Integer.parseInt(params.get("n")[0]) - 1;
        ScoreDoc[] scoreDocs = hits.scoreDocs;

        NodeInfo ni = Application.getAppComponent(SaxonEngine.class).buildDocument(leafReader.document(scoreDocs[position].doc).get("xml"));
        params.put("accessionNumber", new String[]{ni.iterateAxis(Axis.CHILD.getAxisNumber()).next().getStringValue()});
        params.put("accessionIndex", new String[]{"" + position});

        if (position > 0) {
            NodeInfo prev = Application.getAppComponent(SaxonEngine.class).buildDocument(leafReader.document(scoreDocs[position - 1].doc).get("xml"));
            params.put("previousAccession", new String[]{prev.iterateAxis(Axis.CHILD.getAxisNumber()).next().getStringValue()});
        }
        if (position < hits.totalHits - 1) {
            NodeInfo next = Application.getAppComponent(SaxonEngine.class).buildDocument(leafReader.document(scoreDocs[position + 1].doc).get("xml"));
            params.put("nextAccession", new String[]{next.iterateAxis(Axis.CHILD.getAxisNumber()).next().getStringValue()});
        }
        return ni;
    }
}
