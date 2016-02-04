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

package uk.ac.ebi.arrayexpress.utils.saxon.search;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.WildcardQuery;
import uk.ac.ebi.arrayexpress.utils.LRUMap;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class QueryPool {
    //private final Logger logger = LoggerFactory.getLogger(getClass());

    private final static int MAX_NUMBER_OF_QUERIES = 1000;

    private AtomicInteger queryId;

    private Map<Integer, QueryInfo> queries = Collections.synchronizedMap(new LRUMap<Integer, QueryInfo>(MAX_NUMBER_OF_QUERIES));

    public QueryPool() {
        this.queryId = new AtomicInteger(0);
    }

    public Integer addQuery(
            IndexEnvironment env
            , IQueryConstructor queryConstructor
            , Map<String, String[]> queryParams
            , IQueryExpander queryExpander)
            throws ParseException, IOException {
        QueryInfo info;

        if (null != queryExpander) {
            info = queryExpander.newQueryInfo();
        } else {
            info = new QueryInfo();
        }

        info.setIndexId(env.indexId);
        info.setParams(queryParams);
        info.setQuery(queryConstructor.construct(env, queryParams));
        if (null != queryExpander) {
            info.setQuery(queryExpander.expandQuery(env, info));
        }
        info.setQuery(queryConstructor.getAccessControlledQuery(info.getQuery(), env, queryParams));

        Integer id;
        this.queries.put(id = this.queryId.addAndGet(1), info);

        return id;
    }

    public QueryInfo getQueryInfo(Integer queryId) {
        QueryInfo info = null;

        if (queries.containsKey(queryId)) {
            info = queries.get(queryId);
        }

        return info;
    }
}
