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

package uk.ac.ebi.biostudies.components;

import net.sf.saxon.Configuration;
import net.sf.saxon.TransformerFactoryImpl;
import net.sf.saxon.event.SequenceWriter;
import net.sf.saxon.expr.instruct.TerminationException;
import net.sf.saxon.jaxp.TransformerImpl;
import net.sf.saxon.lib.ExtensionFunctionDefinition;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.sxpath.IndependentContext;
import net.sf.saxon.sxpath.XPathEvaluator;
import net.sf.saxon.sxpath.XPathExpression;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.tree.tiny.TinyBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.biostudies.app.Application;
import uk.ac.ebi.biostudies.app.ApplicationComponent;
import uk.ac.ebi.biostudies.utils.LRUMap;
import uk.ac.ebi.biostudies.utils.saxon.Document;
import uk.ac.ebi.biostudies.utils.saxon.StoredDocument;
import uk.ac.ebi.biostudies.utils.saxon.XMLDocumentSource;
import uk.ac.ebi.biostudies.utils.saxon.SaxonException;
import uk.ac.ebi.biostudies.utils.saxon.search.FacetManager;
import uk.ac.ebi.fg.saxon.functions.*;

import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.net.URL;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

public class SaxonEngine extends ApplicationComponent implements URIResolver, ErrorListener {
    // logging machinery
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private TransformerFactoryImpl trFactory;

    public XPathEvaluator getxPathEvaluator() {
        return xPathEvaluator;
    }

    private XPathEvaluator xPathEvaluator;
    private Map<String, Templates> templatesCache = new Hashtable<>();
    private Map<String, XMLDocumentSource> documentSources = new Hashtable<>();
    private Map<String, XPathExpression> xPathExpMap = Collections.synchronizedMap(new LRUMap<String, XPathExpression>(100));

    private Document appDocument;

    public static final String XML_STRING_ENCODING = "UTF-8";

    public SaxonEngine() {
    }

    @Override
    public void initialize() throws Exception {
        // This is so we make sure we use Saxon and not anything else
        trFactory = (TransformerFactoryImpl) TransformerFactoryImpl.newInstance();
        trFactory.setErrorListener(this);
        trFactory.setURIResolver(this);

        // TODO: study the impact of this change later
        //trFactory.getConfiguration().setTreeModel(Builder.TINY_TREE_CONDENSED);

        // create application document
        appDocument = new StoredDocument(
                "<?xml version=\"1.0\" encoding=\"" + XML_STRING_ENCODING +"\"?><application name=\""
                        + getApplication().getName()
                        + "\"/>",
                null
        );

        MapEngine mapEngine = getComponent(MapEngine.class);

        registerExtensionFunction(new SerializeXMLFunction());
        registerExtensionFunction(new TabularDocumentFunction());
        registerExtensionFunction(new GetMappedValueFunction(mapEngine));
        registerExtensionFunction(new FormatFileSizeFunction());
        registerExtensionFunction(new TrimTrailingDotFunction());
        registerExtensionFunction(new HTMLDocumentFunction());
        registerExtensionFunction(new HTTPStatusFunction());

        xPathEvaluator = new XPathEvaluator(trFactory.getConfiguration());
        IndependentContext namespaces = new IndependentContext(trFactory.getConfiguration());
        namespaces.declareNamespace("ae", NamespaceConstant.AE_EXT);
        xPathEvaluator.setStaticContext(namespaces);
    }

    @Override
    public void terminate() throws Exception {
    }

    public void registerDocumentSource(XMLDocumentSource documentSource) {
        logger.debug("Registering source [{}]", documentSource.getURI());
        this.documentSources.put(documentSource.getURI(), documentSource);
    }

    public void unregisterDocumentSource(XMLDocumentSource documentSource) {
        logger.debug("Removing source [{}]", documentSource.getURI());
        this.documentSources.remove(documentSource.getURI());
    }

    public NodeInfo getRegisteredDocument(String documentURI) throws IOException {
        if (this.documentSources.containsKey(documentURI)) {
            return this.documentSources.get(documentURI).getRootNode();
        } else {
            return null;
        }
    }

    // implements URIResolver.resolve
    @Override
    public Source resolve(String href, String base) throws TransformerException {
        Source src;
        try {
            // try document sources first
            if (documentSources.containsKey(href)) {
                return documentSources.get(href).getRootNode();
            } else {
                if (null != href && !href.startsWith("/")) {
                    href = "/WEB-INF/server-assets/stylesheets/" + href;
                }
                URL resource = Application.getInstance().getResource(href);
                if (null == resource) {
                    throw new TransformerException("Unable to locate resource [" + href + "]");
                }
                InputStream input = resource.openStream();
                if (null == input) {
                    throw new TransformerException("Unable to open stream for resource [" + resource.toString() + "]");
                }
                src = new StreamSource(input);
            }
        } catch (TransformerException x) {
            throw x;
        } catch (Exception x) {
            logger.error("Caught an exception:", x);
            throw new TransformerException(x.getMessage());
        }

        return src;
    }

    // implements ErrorListener.error
    @Override
    public void error(TransformerException x) throws TransformerException {
        logger.error("Caught XSLT transformation error:", x);
        getApplication().handleException("[PROBLEM] XSLT transformation error occurred", x);
        throw x;
    }

    // implements ErrorListener.fatalError
    @Override
    public void fatalError(TransformerException x) throws TransformerException {
        if (!(x instanceof HTTPStatusException || x.getMessage().contains("Illegal HTML"))) {
            logger.error("Caught XSLT fatal transformation error:", x);
            getApplication().handleException("[SEVERE] XSLT fatal transformation error occurred", x);
        }
        throw x;
    }

    // implements ErrorListener.warning
    @Override
    public void warning(TransformerException x) {
        logger.warn(x.getLocalizedMessage());
    }

    public Document getAppDocument() {
        return appDocument;
    }

    public void registerExtensionFunction(ExtensionFunctionDefinition f) {
        trFactory.getConfiguration().registerExtensionFunction(f);
    }

    public String serializeDocument(Source source) throws SaxonException {
        return serializeDocument(source, false);
    }

    public String serializeDocument(Source source, boolean omitXmlDeclaration) throws SaxonException {
        try (ByteArrayOutputStream outStream = new ByteArrayOutputStream()) {
            Transformer transformer = trFactory.newTransformer();

            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.INDENT, "no");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.ENCODING, XML_STRING_ENCODING);
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, omitXmlDeclaration ? "yes" : "no");

            transformer.transform(source, new StreamResult(outStream));
            return outStream.toString(XML_STRING_ENCODING);
        } catch (TransformerException | IOException x) {
            throw new SaxonException(x);
        }
    }

    public NodeInfo buildDocument(String xml) throws SaxonException {
        try {
            StringReader reader = new StringReader(xml);
            Configuration config = trFactory.getConfiguration();
            return config.buildDocument(new StreamSource(reader));
        } catch (XPathException x) {
            logger.error("SaxonEngine has problem in parsing following xml {}", xml, x);
            throw new SaxonException(x);
        }
    }

    public NodeInfo buildDocument(File file) throws SaxonException {
        try {
            Configuration config = trFactory.getConfiguration();
            return config.buildDocument(new StreamSource(file));
        } catch (XPathException x) {
            throw new SaxonException(x);
        }
    }

    private XPathExpression getXPathExpression(String xpath) throws XPathException {
        if (xPathExpMap.containsKey(xpath)) {
            return xPathExpMap.get(xpath);
        } else {
            XPathExpression xpe = xPathEvaluator.createExpression(xpath);
            xPathExpMap.put(xpath, xpe);
            return xpe;
        }
    }

    public List<Item> evaluateXPath(NodeInfo node, String xpath) throws XPathException {
        XPathExpression xpe = getXPathExpression(xpath);
        return xpe.evaluate(xpe.createDynamicContext(node));
    }

    public Item evaluateXPathSingle(NodeInfo node, String xpath) throws XPathException {
        XPathExpression xpe = getXPathExpression(xpath);
        return xpe.evaluateSingle(xpe.createDynamicContext(node));
    }

    public String evaluateXPathSingleAsString(NodeInfo node, String xpath) throws XPathException {
        Item e = evaluateXPathSingle(node, xpath);
        if (null == e) {
            return null;
        } else {
            return e.getStringValue();
        }
    }

    public String transformToString(URL sourceUrl, String stylesheet, Map<String, String[]> params)
            throws SaxonException, IOException {
        try (InputStream inStream = sourceUrl.openStream(); ByteArrayOutputStream outStream = new ByteArrayOutputStream()) {
            if (transform(new StreamSource(inStream), stylesheet, params, new StreamResult(outStream))) {
                return outStream.toString(XML_STRING_ENCODING);
            } else {
                return null;
            }
        }
    }

    public String transformToString(Source source, String stylesheet, Map<String, String[]> params)
            throws SaxonException, IOException {
        try (ByteArrayOutputStream outStream = new ByteArrayOutputStream()) {
            if (transform(source, stylesheet, params, new StreamResult(outStream))) {
                return outStream.toString(XML_STRING_ENCODING);
            } else {
                return null;
            }
        }
    }

    public NodeInfo transform(String sourceString, String stylesheet, Map<String, String[]> params)
            throws SaxonException {
        return transform(new StreamSource(new StringReader(sourceString)), stylesheet, params);
    }

    public NodeInfo transform(Source source, String stylesheet, Map<String, String[]> params)
            throws SaxonException {
        TinyBuilder dstDocument = new TinyBuilder(trFactory.getConfiguration().makePipelineConfiguration());
        if (transform(source, stylesheet, params, dstDocument)) {
            return dstDocument.getCurrentRoot();
        }
        return null;
    }

    public boolean transform(Source src, String stylesheet, Map<String, String[]> params, Result dst)
            throws SaxonException {
        boolean result = false;
        try {
            Templates templates;
            if (!templatesCache.containsKey(stylesheet)) {
//                logger.debug("Caching prepared stylesheet [{}]", stylesheet);
                // Open the stylesheet
                Source xslSource = resolve(stylesheet, null);

                templates = trFactory.newTemplates(xslSource);
                templatesCache.put(stylesheet, templates);
            } else {
//                logger.debug("Getting prepared stylesheet [{}] from cache", stylesheet);
                templates = templatesCache.get(stylesheet);
            }
            Transformer xslt = templates.newTransformer();

            ((TransformerImpl) xslt).getUnderlyingController()
                    .setMessageEmitter(new LoggerWriter(logger));

            if(params!=null && params.containsKey("chkfacets")){
                xslt.setParameter("vFacetData", buildDocument(FacetManager.getFacetResults()));
                params.remove("chkfacets");
            }

            // assign the parameters (if not null)
            if (null != params) {
                for (Map.Entry<String, String[]> param : params.entrySet()) {
                    if (null != param.getValue()) {
                        xslt.setParameter(param.getKey()
                                , 1 == param.getValue().length ? param.getValue()[0] : param.getValue()
                        );
                    }
                }
            }

            // Perform the transformation, sending the output to the response.
//            logger.debug("Performing transformation, stylesheet [{}]", stylesheet);
            xslt.transform(src, dst);
//            logger.debug("Transformation completed");

            result = true;
        } catch (TerminationException x) {
            logger.error("Transformation has been terminated by XSL instruction, please inspect log for details");
        } catch (TransformerException x) {
            throw new SaxonException(x);
        }
        return result;
    }

    class LoggerWriter extends SequenceWriter {
        private Logger logger;

        protected LoggerWriter(Logger logger) {
            super(null);
            this.logger = logger;
        }

        public void write(Item item) {
            logger.debug("[xsl:message] {}", item.getStringValue());
        }
    }
}
