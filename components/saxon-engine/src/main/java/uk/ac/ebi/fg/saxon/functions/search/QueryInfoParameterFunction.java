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

package uk.ac.ebi.fg.saxon.functions.search;

import com.google.common.base.Strings;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.lib.ExtensionFunctionCall;
import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.SequenceTool;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.iter.ArrayIterator;
import net.sf.saxon.value.EmptySequence;
import net.sf.saxon.value.SequenceType;
import net.sf.saxon.value.StringValue;

public class QueryInfoParameterFunction extends ExtensionFunctionDefinition {
    private static final StructuredQName qName =
            new StructuredQName("", NamespaceConstant.AE_SEARCH_EXT, "getQueryInfoParameter");

    private final IQueryInfoAccessor queryInfoParameter;

    @SuppressWarnings("unused")
    public QueryInfoParameterFunction() {
        this.queryInfoParameter = new DummyQueryInfoAccessorFunction();
    }

    public QueryInfoParameterFunction(IQueryInfoAccessor controller) {
        this.queryInfoParameter = controller;
    }

    public StructuredQName getFunctionQName() {
        return qName;
    }

    public int getMinimumNumberOfArguments() {
        return 2;
    }

    public int getMaximumNumberOfArguments() {
        return 2;
    }

    public SequenceType[] getArgumentTypes() {
        return new SequenceType[]{SequenceType.SINGLE_STRING, SequenceType.SINGLE_STRING};
    }

    public SequenceType getResultType(SequenceType[] suppliedArgumentTypes) {
        return SequenceType.STRING_SEQUENCE;
    }

    public ExtensionFunctionCall makeCallExpression() {
        return new QueryInfoParameterCall(queryInfoParameter);
    }

    private static class QueryInfoParameterCall extends ExtensionFunctionCall {
        private final IQueryInfoAccessor queryInfoParameter;

        public QueryInfoParameterCall(IQueryInfoAccessor queryInfoParameter) {
            this.queryInfoParameter = queryInfoParameter;
        }

        public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException {
            String queryId = SequenceTool.getStringValue(arguments[0]);
            String key = SequenceTool.getStringValue(arguments[1]);

            Integer intQueryId;
            try {
                intQueryId = Integer.decode(queryId);
            } catch (NumberFormatException x) {
                throw new XPathException("queryId [" + queryId + "] must be integer");
            }

            String[] result = queryInfoParameter.getQueryInfoParameter(
                    intQueryId,
                    Strings.nullToEmpty(key)
            );

            if (result==null) return EmptySequence.getInstance();

            Item [] items = new Item[result.length];
            for (int i = 0; i < result.length ; i++) {
                items[i] = new StringValue(result[i]);
            }
            return null != result
                    ? SequenceTool.toLazySequence(new ArrayIterator(items))
                    : EmptySequence.getInstance();
        }
    }

    private static class DummyQueryInfoAccessorFunction implements IQueryInfoAccessor {
        @Override
        public String[] getQueryInfoParameter(Integer queryId, String key) {
            return null;
        }
    }
}