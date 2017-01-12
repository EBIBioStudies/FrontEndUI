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

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.WildcardQuery;
import uk.ac.ebi.biostudies.utils.AccessType;

import java.util.Map;

public class QueryConstructor implements IQueryConstructor {
    protected final static String FIELD_KEYWORDS = "keywords";
    protected final static String FIELD_ACCESSION = "accession";
    protected final static String FIELD_TITLE = "title";
    protected final static String FIELD_AUTHORS = "authors";
    protected final static String FIELD_ACCESS = "access";
    protected final static String FIELD_PROJECT = "project";
    protected final static String FIELD_TYPE = "type";

    @Override
    public Query construct(IndexEnvironment env, Map<String, String[]> querySource) throws ParseException {


        //expand query to other fields if not a detail page
        //TODO: Refactor/Modify this to avoid redundant fields
        if (!querySource.containsKey(FIELD_ACCESSION)) {
            if (querySource.containsKey(FIELD_KEYWORDS) && !querySource.containsKey(FIELD_AUTHORS)) {
                querySource.put(FIELD_AUTHORS, querySource.get(FIELD_KEYWORDS));
            }

            if (querySource.containsKey(FIELD_KEYWORDS) && !querySource.containsKey(FIELD_TITLE)) {
                querySource.put(FIELD_TITLE, querySource.get(FIELD_KEYWORDS));
            }

            if (querySource.containsKey(FIELD_KEYWORDS) && !querySource.containsKey(FIELD_ACCESSION)) {
                querySource.put(FIELD_ACCESSION, querySource.get(FIELD_KEYWORDS));
            }

            if (querySource.containsKey(FIELD_KEYWORDS) && !querySource.containsKey(FIELD_TYPE)) {
                querySource.put(FIELD_TYPE, querySource.get(FIELD_KEYWORDS));
            }
        } else {
            querySource.put("isDetailPage",new String[]{"true"});
        }

        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        for (Map.Entry<String, String[]> queryItem : querySource.entrySet()) {
            if (queryItem.getKey().equalsIgnoreCase(FIELD_PROJECT)) continue;
            if (env.fields.containsKey(queryItem.getKey()) && queryItem.getValue()!=null && queryItem.getValue().length > 0) {
                QueryParser parser = new EnhancedQueryParser(env, queryItem.getKey(), env.indexAnalyzer);
                parser.setDefaultOperator(QueryParser.Operator.OR);
                for (String value : queryItem.getValue()) {
                    if (!"".equals(value)) {
                        if (env.fields.get(queryItem.getKey()).shouldEscape) {
                            value = value.replaceAll("([+\"!()\\[\\]{}^~*?:\\\\-]|&&|\\|\\|)", "\\\\$1");
                        }
                        Query q = parser.parse(value);
                        builder.add(q, querySource.containsKey("isDetailPage") && queryItem.getKey().equalsIgnoreCase(FIELD_ACCESSION)
                                ? BooleanClause.Occur.MUST : BooleanClause.Occur.SHOULD);
                    }
                }
            }
        }
        return builder.build();
    }

    @Override
    public Query construct(IndexEnvironment env, String queryString) throws ParseException {
        QueryParser parser = new EnhancedQueryParser(env, env.defaultField, env.indexAnalyzer);
        parser.setDefaultOperator(QueryParser.Operator.OR);
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

        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        builder.add(query, BooleanClause.Occur.MUST);

        // add allow rules
        if (querySource.containsKey(AccessType.ALLOW) && querySource.get(AccessType.ALLOW)!=null && querySource.get(AccessType.ALLOW).length>0) {
            QueryParser parser = new EnhancedQueryParser(env, FIELD_ACCESS, env.indexAnalyzer);
            parser.setDefaultOperator(QueryParser.Operator.OR);
            String access = StringUtils.join(querySource.get(AccessType.ALLOW), " ");
            Query q = parser.parse(access);
            builder.add(q, BooleanClause.Occur.MUST);
        }

        // add deny rules
        if (querySource.containsKey(AccessType.DENY) && querySource.get(AccessType.DENY)!=null && querySource.get(AccessType.DENY).length>0) {
            QueryParser parser = new EnhancedQueryParser(env, FIELD_ACCESS, env.indexAnalyzer);
            parser.setDefaultOperator(QueryParser.Operator.AND);
            String access = StringUtils.join(querySource.get(AccessType.DENY), " ");
            Query q = parser.parse(access);
            builder.add(q, BooleanClause.Occur.MUST_NOT);
        }

        // filter on project
        if (querySource.containsKey(FIELD_PROJECT)) {
            QueryParser parser = new EnhancedQueryParser(env, FIELD_PROJECT, env.indexAnalyzer);
            parser.setDefaultOperator(QueryParser.Operator.AND);
            Query q = parser.parse(querySource.get(FIELD_PROJECT)[0]);
            builder.add(q, BooleanClause.Occur.MUST);
        }

        // remove compounds from list pages
        if (!querySource.containsKey("isDetailPage")) {
            QueryParser parser = new EnhancedQueryParser(env, FIELD_TYPE, env.indexAnalyzer);
            parser.setDefaultOperator(QueryParser.Operator.AND);
            Query q = parser.parse("compound");
            builder.add(q, BooleanClause.Occur.MUST_NOT);
        }

        return builder.build();
    }
}
