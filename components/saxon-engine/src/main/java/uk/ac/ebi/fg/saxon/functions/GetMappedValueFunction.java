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

package uk.ac.ebi.fg.saxon.functions;

import net.sf.saxon.expr.JPConverter;
import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.lib.ExtensionFunctionCall;
import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.SequenceTool;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.EmptySequence;
import net.sf.saxon.value.SequenceType;

public class GetMappedValueFunction extends ExtensionFunctionDefinition {

    private static final StructuredQName qName =
            new StructuredQName("", NamespaceConstant.AE_EXT, "getMappedValue");

    private final IMapper mapper;

    @SuppressWarnings("unused")
    public GetMappedValueFunction() {
        mapper = new DummyMapper();
    }

    public GetMappedValueFunction(IMapper mapper) {
        this.mapper = mapper;
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
        return SequenceType.ANY_SEQUENCE;
    }

    public ExtensionFunctionCall makeCallExpression() {
        return new GetMappedValueCall(mapper);
    }

    private static class GetMappedValueCall extends ExtensionFunctionCall {

        private final IMapper mapper;

        public GetMappedValueCall(IMapper mapper) {
            this.mapper = mapper;
        }
        public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException {
            String mapName = SequenceTool.getStringValue(arguments[0]);
            String mapKey = SequenceTool.getStringValue(arguments[1]);
            Object value = mapper.getMappedValue(mapName, mapKey);

            if (null == value) {
                return EmptySequence.getInstance();
            } else {
                JPConverter converter = JPConverter.allocate(value.getClass(), null, context.getConfiguration());
                return converter.convert(value, context);
            }
        }
    }

    private static class DummyMapper implements IMapper {
        public Object getMappedValue(String mapName, String mapKey) {
            return null;
        }
    }
}
