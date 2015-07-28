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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueryInfoParameterAccessor implements IQueryInfoParameterAccessor {
    // logging machinery
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private IndexEnvironment env;

    public IQueryInfoParameterAccessor setEnvironment(IndexEnvironment env) {
        this.env = env;
        return this;
    }

    public String[] getQueryInfoParameter(QueryInfo queryInfo, String key) {
        try {
            return queryInfo.getParams().get(key);
        } catch (Exception x) {
            logger.error("Caught an exception:", x);
        }
        return null;
    }
}
