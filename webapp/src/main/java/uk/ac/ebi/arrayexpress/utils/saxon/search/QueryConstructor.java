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

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.WildcardQuery;

import java.util.Map;

public class QueryConstructor implements IQueryConstructor {
    @Override
    public Query construct(IndexEnvironment env, Map<String, String[]> querySource) throws ParseException {
        BooleanQuery result = new BooleanQuery();
        for (Map.Entry<String, String[]> queryItem : querySource.entrySet()) {
            if (queryItem.getKey().equalsIgnoreCase("project")) continue;
            if (env.fields.containsKey(queryItem.getKey()) && queryItem.getValue()!=null && queryItem.getValue().length > 0) {
                QueryParser parser = new EnhancedQueryParser(env, queryItem.getKey(), env.indexAnalyzer);
                parser.setDefaultOperator(QueryParser.Operator.OR);
                for (String value : queryItem.getValue()) {
                    if (!"".equals(value)) {
                        if (env.fields.get(queryItem.getKey()).shouldEscape) {
                            value = value.replaceAll("([+\"!()\\[\\]{}^~*?:\\\\-]|&&|\\|\\|)", "\\\\$1");
                        }
                        Query q = parser.parse(value);
                        result.add(q, BooleanClause.Occur.SHOULD);
                    }
                }
            }
        }
        return result;
    }

    @Override
    public Query construct(IndexEnvironment env, String queryString) throws ParseException {
        QueryParser parser = new EnhancedQueryParser(env, env.defaultField, env.indexAnalyzer);
        parser.setDefaultOperator(QueryParser.Operator.AND);
        return parser.parse(queryString);
    }

    @Override
    public Query getAccessControlledQuery(Query query, IndexEnvironment env, Map<String, String[]> querySource) throws ParseException {
        // empty query returns everything
        if (query instanceof BooleanQuery && ((BooleanQuery) query).clauses().isEmpty()) {
            Term term = new Term("id", "*");
            query = new WildcardQuery(term);
            querySource.put("queryIsEmpty",new String[]{"true"});
        }

        BooleanQuery queryWithAccessControl = new BooleanQuery();
        queryWithAccessControl.add(query, BooleanClause.Occur.MUST);

        if (querySource.containsKey("allow") && querySource.get("allow").length>0) {
            QueryParser parser = new EnhancedQueryParser(env, "access", env.indexAnalyzer);
            parser.setDefaultOperator(QueryParser.Operator.OR);
            String access = StringUtils.join(querySource.get("allow"), " ");
            Query q = parser.parse(access);
            queryWithAccessControl.add(q, BooleanClause.Occur.MUST);
        }

        if (querySource.containsKey("deny") && querySource.get("deny").length>0) {
            QueryParser parser = new EnhancedQueryParser(env, "access", env.indexAnalyzer);
            parser.setDefaultOperator(QueryParser.Operator.AND);
            String access = StringUtils.join(querySource.get("deny"), " ");
            Query q = parser.parse(access);
            queryWithAccessControl.add(q, BooleanClause.Occur.MUST_NOT);
        }

        if (querySource.containsKey("project")) {
            QueryParser parser = new EnhancedQueryParser(env, "project", env.indexAnalyzer);
            parser.setDefaultOperator(QueryParser.Operator.AND);
            Query q = parser.parse(querySource.get("project")[0]);
            queryWithAccessControl.add(q, BooleanClause.Occur.MUST);
        }


        return queryWithAccessControl;
    }
}
