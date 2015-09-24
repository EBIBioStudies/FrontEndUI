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

import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.BooleanValue;
import net.sf.saxon.value.Int64Value;
import net.sf.saxon.value.NumericValue;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.util.BytesRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.arrayexpress.components.SaxonEngine;
import uk.ac.ebi.arrayexpress.utils.StringTools;
import uk.ac.ebi.arrayexpress.utils.saxon.SaxonException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class Indexer {
    protected final static String DOCID_FIELD = "docId";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final IndexEnvironment env;
    private final SaxonEngine saxon;

    public Indexer(IndexEnvironment env, SaxonEngine saxon) {
        this.env = env;
        this.saxon = saxon;
    }

    public List<NodeInfo> index(NodeInfo documentNode) throws IndexerException, InterruptedException {
        try {
            try (IndexWriter w = createIndex(this.env.indexDirectory, this.env.indexAnalyzer)) {
                //setDocumentHash(document.getHash());

                List<Item> documentNodes = saxon.evaluateXPath(documentNode, this.env.indexDocumentPath);
                List<NodeInfo> indexedNodes = new ArrayList<>(documentNodes.size());

                for (Item node : documentNodes) {
                    Document d = new Document();

                    String idValue = ""; // value of id field (i.e. accession in case of studies)

                    for (IndexEnvironment.FieldInfo field : this.env.fields.values()) {
                        try {
                            List<Item> values = saxon.evaluateXPath((NodeInfo) node, field.path);
                            for (Item v : values) {
                                if ("long".equals(field.type)) {
                                    addLongField(d, field.name, v, field.shouldStore);
                                    if (!"none".equalsIgnoreCase(field.docValueType)) {
                                        d.add( new NumericDocValuesField (field.name, Long.parseLong(v.getStringValue())) );
                                    }
                                } else if ("date".equals(field.type)) {
                                    // todo: addDateIndexField(d, field.name, v);
                                    logger.error("Date fields are not supported yet, field [{}] will not be created", field.name);
                                } else if ("boolean".equals(field.type)) {
                                    addBooleanIndexField(d, field.name, v);
                                } else {
                                    addStringField(d, field.name, v, field.shouldAnalyze, field.shouldStore, field.boost);
                                    if (!"none".equalsIgnoreCase(field.docValueType)) {
                                        d.add( new SortedDocValuesField(field.name, new BytesRef(v.getStringValue().toLowerCase())));
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
                    w.updateDocument(new Term("id", idValue), d);
                    indexedNodes.add((NodeInfo) node);
                }

                w.commit();

                return indexedNodes;
            }
        } catch (IOException | XPathException | SaxonException x) {
            throw new IndexerException(x);
        }
    }



    public List<NodeInfo> index(uk.ac.ebi.arrayexpress.utils.saxon.Document document) throws IndexerException, InterruptedException {
        return index(document.getRootNode());
    }

    private void addXMLField(Document d, Item node) throws SaxonException {
        FieldType fieldType = new FieldType();
        fieldType.setOmitNorms(true);
        fieldType.setIndexOptions(IndexOptions.NONE);
        fieldType.setStored(true);
        fieldType.setTokenized(false);
        fieldType.freeze();
        Field field =new Field("xml", saxon.serializeDocument((NodeInfo)node) , fieldType );
        d.add(field);
    }


    private IndexWriter createIndex(Directory indexDirectory, Analyzer analyzer) throws IOException {
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        return new IndexWriter(indexDirectory, config);
    }

    public void clearIndex() throws IOException {
        IndexWriterConfig config = new IndexWriterConfig(this.env.indexAnalyzer);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND );
        try (IndexWriter w = new IndexWriter(this.env.indexDirectory, config) ){
            w.deleteAll();
            w.forceMergeDeletes();
            w.commit();
        }

    }

    public void delete(String accession) throws IOException {
        IndexWriterConfig config = new IndexWriterConfig(this.env.indexAnalyzer);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND );
        try (IndexWriter w = new IndexWriter(this.env.indexDirectory, config) ){
            w.deleteDocuments( new Term("id",accession));
            w.forceMergeDeletes();
            w.commit();
        }

    }

    private void addStringField(Document document, String name, Item value, boolean shouldAnalyze, boolean shouldStore, float boost) {
        String stringValue = value.getStringValue();
        FieldType fieldType = new FieldType();
        fieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);
        fieldType.setTokenized(shouldAnalyze);
        fieldType.setStored(shouldStore);
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
            boolValue = StringTools.stringToBoolean(stringValue);
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
            document.add(new LongField(name, longValue, store ? Field.Store.YES : Field.Store.NO));
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

}
