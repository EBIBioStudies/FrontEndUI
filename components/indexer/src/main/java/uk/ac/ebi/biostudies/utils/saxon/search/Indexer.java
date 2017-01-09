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

import net.sf.saxon.*;
import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.sxpath.XPathEvaluator;
import net.sf.saxon.sxpath.XPathExpression;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.BooleanValue;
import net.sf.saxon.value.Int64Value;
import net.sf.saxon.value.NumericValue;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.facet.FacetField;
import org.apache.lucene.index.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.BytesRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class Indexer {
    protected final static String DOCID_FIELD = "docId";
    public static final String XML_STRING_ENCODING = "UTF-8";
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private XPathEvaluator xPathEvaluator;
    private TransformerFactoryImpl trFactory;

    private final IndexEnvironment env;
    private static Map<String,IndexWriter> indexWriters  = new HashMap<>();


    public Indexer(String indexId, XPathEvaluator xPathEvaluator) throws IOException {
        this.env = new IndexEnvironment(indexId);
        this.xPathEvaluator = xPathEvaluator;
        if (!indexWriters.containsKey(env.indexId)) {
            indexWriters.put(env.indexId, createOrAppendIndex(this.env.indexDirectory, this.env.indexAnalyzer));
        }
        trFactory = (TransformerFactoryImpl) TransformerFactoryImpl.newInstance();

    }

    public List<NodeInfo> index (String xml) throws IndexerException, InterruptedException {
        try {
            StringReader reader = new StringReader(xml);
            net.sf.saxon.Configuration config = trFactory.getConfiguration();
            List<NodeInfo> nodes = index(config.buildDocument(new StreamSource(reader)));
            reader.close();
            return nodes;
        } catch (XPathException x) {
            throw new IndexerException(x);
        }
    }

    public List<NodeInfo> index(NodeInfo documentNode) throws IndexerException, InterruptedException {
        return  index(documentNode,true);
    }

    public List<NodeInfo> index(NodeInfo documentNode, boolean commit) throws IndexerException, InterruptedException {
        try {
            //logger.debug( serializeDocument(documentNode));
            List<Item> documentNodes = evaluateXPath(documentNode, this.env.indexDocumentPath);
            List<NodeInfo> indexedNodes = new ArrayList<>(documentNodes.size());
            IndexWriter w = indexWriters.get(env.indexId);
            for (Item node : documentNodes) {
                Document d = new Document();

                String idValue = ""; // value of id field (i.e. accession in case of studies)

                for (IndexEnvironment.FieldInfo field : this.env.fields.values()) {
                    try {
                        List<Item> values = evaluateXPath((NodeInfo) node, field.path);
                        for (Item v : values) {
                            if ("long".equals(field.type)) {
                                addLongField(d, field.name, v, field.shouldStore);
                                if (!"none".equalsIgnoreCase(field.docValueType)) {
                                    d.add( new SortedNumericDocValuesField (field.name, Long.parseLong(v.getStringValue())) );
                                    d.add( new StoredField (field.name, Long.parseLong(v.getStringValue())) );
                                }

                            } else if ("date".equals(field.type)) {
                                // todo: addDateIndexField(d, field.name, v);
                                logger.error("Date fields are not supported yet, field [{}] will not be created", field.name);
                            } else if ("boolean".equals(field.type)) {
                                addBooleanIndexField(d, field.name, v);
                            } else if(!v.getStringValue().isEmpty() && "facet".equalsIgnoreCase(field.type)) {
                                addFacetField(d, v, field.name);
                            }
                            else {
                                addStringField(d, field.name, v, field.shouldAnalyze, field.shouldStore, field.boost);
                                if (!"none".equalsIgnoreCase(field.docValueType)) {
                                    String value = v.getStringValue().toLowerCase();
                                    d.add( new SortedDocValuesField(field.name, new BytesRef(value.length()<256 ? value:value.substring(0,256))));
                                }
                            }
                            if ( field.name.equalsIgnoreCase(env.idField)) {
                                idValue = v.getStringValue();
                            }
                        }
                    } catch (XPathException x) {
                        String expression = ((NodeInfo) node).getStringValue();
                        logger.error("Caught an exception while indexing expression [" + field.path + "] for document [" + expression.substring(0, expression.length() > 20 ? 20 : expression.length()) + "...]", x);
                        throw x;
                    }
                }
                addXMLField(d, node);
                addDocIdField(d, idValue);
                //logger.debug("Indexing document {} = {}", env.idField, idValue);
                try {
                    Document facetedDocument = FacetManager.FACET_CONFIG.build(FacetManager.getTaxonomyWriter() ,d);
                    w.addDocument(facetedDocument);
//                    w.updateDocument(new Term("id", idValue), facetedDocument);
                } catch (Exception e) {
                    logger.error(" Error indexing " +d);
                    logger.error(e.getMessage());
                }
                indexedNodes.add((NodeInfo) node);
            }

            if(commit) {
                w.commit();
            }

            return indexedNodes;
        } catch (IOException | XPathException x) {
            throw new IndexerException(x);
        }
    }

    private void addFacetField(Document d, Item value, String name){
        d.add(new FacetField(name, value.getStringValue().trim().toLowerCase()));
    }

    private void addXMLField(Document d, Item node) throws IndexerException {
        FieldType fieldType = new FieldType();
        fieldType.setOmitNorms(true);
        fieldType.setIndexOptions(IndexOptions.NONE);
        fieldType.setStored(true);
        fieldType.setTokenized(false);
        fieldType.freeze();
        Field field =new Field("xml", serializeDocument((NodeInfo)node) , fieldType );
        d.add(field);
    }


    private IndexWriter createOrAppendIndex(Directory indexDirectory, Analyzer analyzer) throws IOException {
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        return new IndexWriter(indexDirectory, config);
    }

    public void clearIndex(boolean commit) throws IOException {
        IndexWriter w = indexWriters.get(env.indexId);
        w.deleteAll();
        if (commit) {
            w.forceMergeDeletes();
            w.commit();
        }
    }

    public void delete(String accession) throws IOException {
        IndexWriter w = indexWriters.get(env.indexId);
        w.deleteDocuments( new Term("id",accession));
        w.forceMergeDeletes();
        w.commit();
    }

    private void addStringField(Document document, String name, Item value, boolean shouldAnalyze, boolean shouldStore, float boost) {
        String stringValue = value.getStringValue();
        FieldType fieldType = new FieldType();
        fieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);
        fieldType.setTokenized(shouldAnalyze);
        fieldType.setStored(shouldStore);
        fieldType.setStoreTermVectors(shouldStore);
        Field field =new Field(name, stringValue, fieldType);
        field.setBoost(boost);
        document.add(field);
    }

    private void addBooleanIndexField(Document document, String name, Item value) {
        Boolean boolValue;
        if (value instanceof BooleanValue) {
            boolValue = ((BooleanValue) value).getBooleanValue();
        } else {
            String stringValue = value.getStringValue();
            boolValue = stringValue.equalsIgnoreCase("true") ? true : false;
        }

        document.add(new StringField(name, null == boolValue ? "" : boolValue.toString(), Field.Store.NO));
    }

    private void addLongField(Document document, String name, Item value, boolean store) {
        Long longValue;
        try {
            if (value instanceof Int64Value) {
                longValue = ((Int64Value) value).asBigInteger().longValue();
            } else if (value instanceof NumericValue) {
                longValue = ((NumericValue) value).longValue();
            } else {
                longValue = Long.parseLong(value.getStringValue());
            }
            LongPoint longPoint = new LongPoint(name, longValue);
            document.add(longPoint);
        } catch (XPathException x) {
            logger.error("Unable to convert value [" + value.getStringValue() + "]", x);
        }
    }

    private void addDocIdField(Document document, String value) {
        FieldType fieldType = new FieldType();
        fieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);
        fieldType.setTokenized(false);
        fieldType.setStored(true);
        Field field =new Field("id", value, fieldType);
        document.add(field);
    }

    public List<Item> evaluateXPath(NodeInfo node, String xpath) throws XPathException {
        XPathExpression xpe = xPathEvaluator.createExpression(xpath);
        return xpe.evaluate(xpe.createDynamicContext(node));
    }

    public String serializeDocument(Source source) throws IndexerException {
        try (ByteArrayOutputStream outStream = new ByteArrayOutputStream()) {
            Transformer transformer = trFactory.newTransformer();

            transformer.setOutputProperty(OutputKeys.METHOD, "xml");
            transformer.setOutputProperty(OutputKeys.INDENT, "no");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(OutputKeys.ENCODING, XML_STRING_ENCODING);
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");

            transformer.transform(source, new StreamResult(outStream));
            return outStream.toString(XML_STRING_ENCODING);
        } catch (TransformerException | IOException x) {
            throw new IndexerException(x);
        }
    }
}
