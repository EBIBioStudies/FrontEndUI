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

package uk.ac.ebi.fg.saxon.functions.search;

import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.lib.ExtensionFunctionCall;
import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.om.*;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.ListIterator;
import net.sf.saxon.value.EmptySequence;
import net.sf.saxon.value.SequenceType;

import java.util.List;

public class QueryIndexFunction extends ExtensionFunctionDefinition {

    private static final StructuredQName qName =
            new StructuredQName("", NamespaceConstant.AE_SEARCH_EXT, "queryIndex");

    private final IQuerier querier;

    @SuppressWarnings("unused")
    public QueryIndexFunction() {
        this.querier = new DummyQuerier();
    }

    public QueryIndexFunction(IQuerier querier) {
        this.querier = querier;
    }

    public StructuredQName getFunctionQName() {
        return qName;
    }

    public int getMinimumNumberOfArguments() {
        return 1;
    }

    public int getMaximumNumberOfArguments() {
        return 2;
    }

    public SequenceType[] getArgumentTypes() {
        return new SequenceType[]{SequenceType.SINGLE_STRING, SequenceType.SINGLE_STRING};
    }

    public SequenceType getResultType(SequenceType[] suppliedArgumentTypes) {
        return SequenceType.NODE_SEQUENCE;
    }

    public ExtensionFunctionCall makeCallExpression() {
        return new QueryIndexCall(querier);
    }

    private static class QueryIndexCall extends ExtensionFunctionCall {

        private final IQuerier querier;

        public QueryIndexCall(IQuerier querier) {
            this.querier = querier;
        }

        public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException {
            String first = SequenceTool.getStringValue(arguments[0]);
            String second = arguments.length > 1 ? SequenceTool.getStringValue(arguments[1]) : null;

            List<NodeInfo> nodes;
            try {
                if (null == second) {
                    Integer intQueryId;
                    try {
                        intQueryId = Integer.decode(first);
                    } catch (NumberFormatException x) {
                        throw new XPathException("queryId [" + first + "] must be integer");
                    }

                    nodes = querier.queryIndex(intQueryId);
                } else {
                    nodes = querier.queryIndex(first, second);
                }
            } catch (Exception x) {
                throw new XPathException("Caught exception while querying index", x);
            }
            return null != nodes
                    ? SequenceTool.toLazySequence(new ListIterator(nodes))
                    : EmptySequence.getInstance();
        }
    }

    private static class DummyQuerier implements IQuerier {
        public List<NodeInfo> queryIndex(Integer queryId) {
            return null;
        }

        public List<NodeInfo> queryIndex(String indexId, String queryString) {
            return null;
        }

    }
}

