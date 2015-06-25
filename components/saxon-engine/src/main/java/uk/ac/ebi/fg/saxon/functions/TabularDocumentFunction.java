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

package uk.ac.ebi.fg.saxon.functions;

import net.sf.saxon.expr.XPathContext;
import net.sf.saxon.lib.ExtensionFunctionCall;
import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.om.Sequence;
import net.sf.saxon.om.StructuredQName;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.EmptySequence;
import net.sf.saxon.value.SequenceType;

public class TabularDocumentFunction extends ExtensionFunctionDefinition {
    private static final StructuredQName qName =
            new StructuredQName("", NamespaceConstant.AE_EXT, "tabularDocument");

    public StructuredQName getFunctionQName() {
        return qName;
    }

    public int getMinimumNumberOfArguments() {
        return 2;
    }

    public int getMaximumNumberOfArguments() {
        return 3;
    }

    public SequenceType[] getArgumentTypes() {
        return new SequenceType[]{
                SequenceType.SINGLE_STRING
                , SequenceType.SINGLE_STRING
                , SequenceType.OPTIONAL_STRING
        };
    }

    public SequenceType getResultType(SequenceType[] suppliedArgumentTypes) {
        return SequenceType.OPTIONAL_NODE;
    }

    public ExtensionFunctionCall makeCallExpression() {
        return new TabularDocumentCall();
    }

    private static class TabularDocumentCall extends ExtensionFunctionCall {
//        private final Logger logger = LoggerFactory.getLogger(getClass());

//        private Files files;

        public TabularDocumentCall() {
//            files = (Files) Application.getAppComponent("Files");
        }

        public Sequence call(XPathContext context, Sequence[] arguments) throws XPathException {
//            try {
//                Controller controller = context.getController();
//                String baseURI = ((NodeInfo) context.getContextItem()).getBaseURI();
//
//                String accession = SequenceTool.getStringValue(arguments[0]);
//                String name = SequenceTool.getStringValue(arguments[1]);
//                String options = (arguments.length > 2) ? SequenceTool.getStringValue(arguments[2]) : null;
//
//                String location = files.getLocation(accession, null, name);
//                if (null != location) {
//                    File flatFile = new File(files.getRootFolder(), location);
//
//                    if (flatFile.exists()) {
//                        try (InputStream in = new FileInputStream(flatFile)) {
//                            InputSource is = new InputSource(
//                                    new FilteringIllegalHTMLCharactersReader(
//                                            new UnescapingXMLNumericReferencesReader(
//                                                    new InputStreamReader(
//                                                            in
//                                                            , new SmartUTF8CharsetDecoder()
//                                                    )
//                                            )
//                                    )
//                            );
//                            is.setSystemId(baseURI);
//
//                            Source source = new SAXSource(
//                                    new FlatFileXMLReader(options)
//                                    , is
//                            );
//                            source.setSystemId(baseURI);
//
//                            Builder b = controller.makeBuilder();
//                            Receiver s = b;
//
//                            source = AugmentedSource.makeAugmentedSource(source);
//                            ((AugmentedSource) source).setStripSpace(Whitespace.XSLT);
//
//                            if (controller.getExecutable().stripsInputTypeAnnotations()) {
//                                s = controller.getConfiguration().getAnnotationStripper(s);
//                            }
//
//                            Sender.send(source, s, null);
//
//                            NodeInfo node = b.getCurrentRoot();
//                            b.reset();
//
//                            return node;
//                        }
//                    } else {
//                        logger.error("Unable to open document [{}]", location);
//                    }
//                } else {
//                    logger.error(
//                            "Unable to locate document [{}], accesion [{}]"
//                            , name
//                            , accession)
//                    ;
//                }
//            } catch (IOException x) {
//                throw new XPathException(x);
//            }
            return EmptySequence.getInstance();
        }
    }
}
