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

import org.apache.lucene.queryparser.xml.builders.BooleanQueryBuilder;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import uk.ac.ebi.biostudies.utils.saxon.search.QueryInfo;

public class EFOExpandableQueryInfo extends QueryInfo {
    private Query originalQuery;
    private BooleanQuery.Builder synonymPartQueryBuilder = new BooleanQuery.Builder();
    private BooleanQuery.Builder efoExpansionPartQueryBuilder = new BooleanQuery.Builder();

    public Query getOriginalQuery() {
        return originalQuery;
    }

    public void setOriginalQuery(Query originalQuery) {
        this.originalQuery = originalQuery;
    }

    public Query getSynonymPartQuery() {
        return synonymPartQueryBuilder.build();
    }

    public void addToSynonymPartQuery(Query part) {
        synonymPartQueryBuilder.add(part, BooleanClause.Occur.SHOULD);
    }

    public Query getEfoExpansionPartQuery() {
        return efoExpansionPartQueryBuilder.build();
    }

    public void addToEfoExpansionPartQuery(Query part) {
        efoExpansionPartQueryBuilder.add(part, BooleanClause.Occur.SHOULD);
    }
}
