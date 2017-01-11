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
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.biostudies.components.SaxonEngine;
import uk.ac.ebi.biostudies.utils.saxon.Document;
import uk.ac.ebi.biostudies.utils.saxon.SaxonException;
import uk.ac.ebi.fg.saxon.functions.search.IHighlighter;
import uk.ac.ebi.fg.saxon.functions.search.IQueryInfoAccessor;

import javax.xml.transform.Source;
import java.io.IOException;
import java.net.URL;
import java.util.*;

public class Controller implements IHighlighter, IQueryInfoAccessor {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private Configuration config;
    private QueryPool queryPool;
    private QueryConstructor queryConstructor;
    private IQueryExpander queryExpander;
    private IQueryHighlighter queryHighlighter;
    private IQueryInfoParameterAccessor queryInfoParameterGetter;
    private SaxonEngine saxon;

    private Map<String, IndexEnvironment> environment = new HashMap<>();

    @SuppressWarnings("unused")
    public Controller(URL configFile) {
        this.config = new Configuration(configFile);
        this.queryPool = new QueryPool();
        BooleanQuery.setMaxClauseCount(Integer.MAX_VALUE);
    }

    public Controller(HierarchicalConfiguration config) {
        this.config = new Configuration(config);
        this.queryPool = new QueryPool();
        BooleanQuery.setMaxClauseCount(Integer.MAX_VALUE);
    }

    public void setQueryConstructor(QueryConstructor queryConstructor) {
        this.queryConstructor = queryConstructor;
    }

    public void setQueryExpander(IQueryExpander queryExpander) {
        this.queryExpander = queryExpander;
    }

    public void setQueryHighlighter(IQueryHighlighter queryHighlighter) {
        this.queryHighlighter = queryHighlighter;
    }

    public void setXPathEngine(SaxonEngine saxon) {
        this.saxon = saxon;
    }

    public boolean hasIndexDefined(String indexId) {
        return this.environment.containsKey(indexId);
    }

    public IndexEnvironment getEnvironment(String indexId) {
        if (!this.environment.containsKey(indexId)) {
            this.environment.put(indexId, new IndexEnvironment(indexId));
        }

        return this.environment.get(indexId);
    }

    public void index(String indexId, Document document) throws IndexerException, InterruptedException, IOException {
        this.logger.info("Started indexing for index id [{}]", indexId);
        getEnvironment(indexId).setDocumentInfo(
                document.getHash()
                , new Indexer(indexId, saxon.getxPathEvaluator()).index(document.getRootNode())
        );
        this.logger.info("Indexing for index id [{}] completed", indexId);
    }

    public void clearIndex(String indexId) throws IndexerException, InterruptedException, IOException {
        this.logger.info("Clearing index for index id [{}]", indexId);
        new Indexer(indexId, saxon.getxPathEvaluator()).clearIndex(true);
        this.logger.info("Indexfor index id [{}] cleared", indexId);
    }

    public void delete(String indexId, String accession) throws IndexerException, InterruptedException, IOException {
        this.logger.info("Deleting {} from index id [{}]", accession, indexId);
        new Indexer(indexId, saxon.getxPathEvaluator()).delete(accession);
        this.logger.info("Document {} deleted", accession);
    }

    public List<String> getTerms(String indexId, String fieldName, int minFreq) throws IOException {
        IndexEnvironment env = getEnvironment(indexId);
        if (!env.doesFieldExist(fieldName)) {
            this.logger.error("Field [{}] for index id [{}] does not exist, returning empty list");
            return new ArrayList<>();
        } else {
            return new Querier(env).getTerms(fieldName, minFreq);
        }
    }

    @SuppressWarnings("unused")
    public Integer getDocCount(String indexId, Map<String, String[]> queryParams) throws IOException, ParseException {
        IndexEnvironment env = getEnvironment(indexId);

        Query query = queryConstructor.construct(env, queryParams);
        return new Querier(env).getDocCount(query);

    }

    @SuppressWarnings("unused")
    public void dumpTerms(String indexId, String fieldName) throws IOException {
        IndexEnvironment env = getEnvironment(indexId);
        if (env.doesFieldExist(fieldName)) {
            new Querier(env).dumpTerms(fieldName);
        }
    }

    public Set<String> getFieldNames(String indexId) {
        IndexEnvironment env = getEnvironment(indexId);
        return (null != env ? env.fields.keySet() : null);
    }

    public String getFieldTitle(String indexId, String fieldName) {
        IndexEnvironment env = getEnvironment(indexId);
        return (null != env && env.doesFieldExist(fieldName) ? env.fields.get(fieldName).title : null);
    }

    public String getFieldType(String indexId, String fieldName) {
        IndexEnvironment env = getEnvironment(indexId);
        return (null != env && env.doesFieldExist(fieldName) ? env.fields.get(fieldName).type : null);
    }

    public IQueryInfoParameterAccessor getQueryInfoParameterGetter() {
        return queryInfoParameterGetter;
    }

    public void setQueryInfoParameterGetter(IQueryInfoParameterAccessor queryInfoParameterGetter) {
        this.queryInfoParameterGetter = queryInfoParameterGetter;
    }

    public Integer addQuery(String indexId, Map<String, String[]> queryParams)
            throws ParseException, IOException {
        if (null == this.queryConstructor) {
            // sort of lazy init if we forgot to specify more advanced highlighter
            this.setQueryConstructor(new QueryConstructor());
        }

        return this.queryPool.addQuery(
                getEnvironment(indexId)
                , this.queryConstructor
                , queryParams
                , this.queryExpander
        );
    }

    public Source search(Integer queryId) throws ParseException, IOException, SaxonException {
        QueryInfo queryInfo = this.queryPool.getQueryInfo(queryId);
        Querier querier = new Querier( getEnvironment(queryInfo.getIndexId()));
        List<NodeInfo> results = querier.query(queryInfo);
        long total = queryInfo.getParams().containsKey("total") ?
                    Long.parseLong(queryInfo.getParams().get("total")[0])
                    : 0;
        StringBuilder sb = new StringBuilder( String.format("<studies total=\"%d\" >",total));
        for (NodeInfo node : results) {
            sb.append(saxon.serializeDocument(node,true));
        }
        sb.append("</studies>");
        //logger.debug(sb.toString());
        return saxon.buildDocument(sb.toString());
    }

    @Override
    public String highlightQuery(Integer queryId, String fieldName, String text) {
        if (null == this.queryHighlighter) {
            // sort of lazy init if we forgot to specify more advanced highlighter
            this.setQueryHighlighter(new QueryHighlighter());
        }
        QueryInfo queryInfo = this.queryPool.getQueryInfo(queryId);
        if (null != queryInfo) {
            return queryHighlighter.setEnvironment(getEnvironment(queryInfo.getIndexId()))
                    .highlightQuery(queryInfo, fieldName, text);
        } else {
            this.logger.error("Unable to find query info for query with id [{}]", queryId);
            return text;
        }
    }

    @Override
    public String[] getQueryInfoParameter(Integer queryId, String key) {
        if (null == this.queryInfoParameterGetter) {
            this.setQueryInfoParameterGetter(new QueryInfoParameterAccessor());
        }
        QueryInfo queryInfo = this.queryPool.getQueryInfo(queryId);
        if (null != queryInfo) {
            return queryInfoParameterGetter.setEnvironment(getEnvironment(queryInfo.getIndexId()))
                    .getQueryInfoParameter(queryInfo, key);
        } else {
            this.logger.error("Unable to find query info for query with id [{}]", queryId);
            return null;
        }
    }
}
