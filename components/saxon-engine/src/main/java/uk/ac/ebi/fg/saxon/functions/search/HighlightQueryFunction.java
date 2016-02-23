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
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.SequenceTool;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.SequenceType;
import net.sf.saxon.value.StringValue;

public class HighlightQueryFunction extends ExtensionFunctionDefinition {
    private static final StructuredQName qName =
            new StructuredQName("", NamespaceConstant.AE_SEARCH_EXT, "highlightQuery");

    private final IHighlighter highlighter;

    @SuppressWarnings("unused")
    public HighlightQueryFunction() {
        this.highlighter = new DummyHighlighter();
    }

    public HighlightQueryFunction(IHighlighter controller) {
        this.highlighter = controller;
    }

    public StructuredQName getFunctionQName() {
        return qName;
    }

    public int getMinimumNumberOfArguments() {
        return 3;
    }

    public int getMaximumNumberOfArguments() {
        return 3;
    }

    public SequenceType[] getArgumentTypes() {
        return new SequenceType[]{SequenceType.SINGLE_STRING, SequenceType.SINGLE_STRING, SequenceType.SINGLE_STRING};
    }

    public SequenceType getResultType(SequenceType[] suppliedArgumentTypes) {
        return SequenceType.SINGLE_STRING;
    }

    public ExtensionFunctionCall makeCallExpression() {
        return new HighlightQueryCall(highlighter);
    }

    private static class HighlightQueryCall extends ExtensionFunctionCall {
        private final IHighlighter highlighter;

        public HighlightQueryCall(IHighlighter highlighter) {
            this.highlighter = highlighter;
        }

        public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException {
            String queryId = SequenceTool.getStringValue(arguments[0]);
            String fieldName = SequenceTool.getStringValue(arguments[1]);
            String text = SequenceTool.getStringValue(arguments[2]);

            Integer intQueryId;
            try {
                intQueryId = Integer.decode(queryId);
            } catch (NumberFormatException x) {
                throw new XPathException("queryId [" + queryId + "] must be integer");
            }

            String result = highlighter.highlightQuery(
                    intQueryId,
                    Strings.nullToEmpty(fieldName),
                    Strings.nullToEmpty(text)
            );

            return StringValue.makeStringValue(result);
        }
    }

    private static class DummyHighlighter implements IHighlighter {
        public String highlightQuery(Integer queryId, String fieldName, String text) {
            return text;
        }
    }
}