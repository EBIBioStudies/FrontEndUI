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

package uk.ac.ebi.biostudies.utils.search;

import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.xml.builders.BooleanQueryBuilder;
import org.apache.lucene.search.*;
import uk.ac.ebi.biostudies.utils.StringTools;
import uk.ac.ebi.biostudies.utils.saxon.search.IndexEnvironment;

import java.util.Map;

public class BatchQueryConstructor extends BackwardsCompatibleQueryConstructor {

    private final static String RE_MATCHES_BATCH_OF_ACCESSIONS = "^\\s*(([ae]-\\w{4}-\\d+)[\\s,;]+)+$";
    private final static String RE_SPLIT_BATCH_OF_ACCESSIONS = "[\\s,;]+";

    @Override
    public Query construct(IndexEnvironment env, Map<String, String[]> querySource) throws ParseException {

        Query query = super.construct(env, querySource);


        if (querySource.containsKey(FIELD_KEYWORDS)) {
            String keywords = StringTools.arrayToString(querySource.get(FIELD_KEYWORDS), " ").toLowerCase() + " ";
            if (keywords.matches(RE_MATCHES_BATCH_OF_ACCESSIONS)) {
                String[] accessions = keywords.split(RE_SPLIT_BATCH_OF_ACCESSIONS);

                query = removeTermQueriesForField(query, FIELD_KEYWORDS);

                BooleanQuery.Builder accQueryBuilder = new BooleanQuery.Builder();
                for (String acc : accessions) {
                    accQueryBuilder.add(new TermQuery(new Term(FIELD_ACCESSION, acc)), BooleanClause.Occur.SHOULD);
                }

                BooleanQuery.Builder topQueryBuilder = new BooleanQuery.Builder();
                if (query instanceof BooleanQuery) {
                    for(BooleanClause clause:  ((BooleanQuery) query).clauses()) {
                        topQueryBuilder.add(clause);
                    }
                } else {
                    topQueryBuilder.add(query, BooleanClause.Occur.SHOULD);
                }

                topQueryBuilder.add(accQueryBuilder.build(), BooleanClause.Occur.SHOULD);
                query = topQueryBuilder.build();
            }
        }



        return query;
    }

    @Override
    public Query construct(IndexEnvironment env, String queryString) throws ParseException {
        return super.construct(env, queryString);
    }

    private Query removeTermQueriesForField(Query query, String fieldName) {
        Query q = removeTermQueryForField(query, fieldName);
        if (null == q) {
            q = new BooleanQuery.Builder().build();
        }

        return q;
    }


    private Query removeTermQueryForField(Query query, String fieldName) {
        if (query instanceof BooleanQuery) {
            BooleanQuery.Builder booleanQueryBuilder = new BooleanQuery.Builder();
            for (BooleanClause clause : ((BooleanQuery) query).clauses()) {
                Query q = removeTermQueryForField(clause.getQuery(), fieldName);
                if (null != q) {
                    booleanQueryBuilder.add(q, clause.getOccur());
                }
                if (0 != booleanQueryBuilder.build().clauses().size()) {
                    query = booleanQueryBuilder.build();
                } else {
                    query = null;
                }
            }
        } else if (query instanceof TermQuery) {
            Term term = ((TermQuery)query).getTerm();
            if (fieldName.equals(term.field())) {
                return null;
            }
        } else if (query instanceof PhraseQuery) {
            Term[] terms = ((PhraseQuery)query).getTerms();
            for (Term term : terms) {
                if (fieldName.equals(term.field())) {
                    return null;
                }
            }
        }
        return query;
    }
}
