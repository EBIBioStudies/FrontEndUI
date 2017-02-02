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

import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.trans.XPathException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.biostudies.app.ApplicationComponent;
import uk.ac.ebi.biostudies.utils.saxon.Document;
import uk.ac.ebi.biostudies.utils.saxon.SaxonException;
import uk.ac.ebi.biostudies.utils.saxon.StoredDocument;
import uk.ac.ebi.biostudies.utils.saxon.search.FacetManager;
import uk.ac.ebi.biostudies.utils.saxon.search.Indexer;
import uk.ac.ebi.biostudies.utils.saxon.search.IndexerException;
import uk.ac.ebi.biostudies.utils.saxon.search.Querier;
import uk.ac.ebi.microarray.biostudies.shared.auth.User;

import javax.xml.stream.*;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang.StringUtils.isNotBlank;

public class Studies extends ApplicationComponent  {
    private final Logger logger = LoggerFactory.getLogger(getClass());

//    public final static String MAP_STUDIES_VIEWS = "studies-views";
//    public final static String MAP_STUDIES = "studies-downloads";
//    public final static String MAP_STUDIES_COMPLETE_DOWNLOADS = "studies-complete-downloads";
//    public final static String MAP_STUDIES = "studies";
//    public final static String MAP_STUDIES_FOR_USER = "studies-for-user";

//    private Document document;
//    private FilePersistence<PersistableString> species;
//    private FilePersistence<PersistableString> arrays;

//    private MapEngine maps;
    private SaxonEngine saxon;
    private SearchEngine search;
//    private Users users;
//    private Events events;
    private Autocompletion autocompletion;
    public final String INDEX_ID = "studies";
    private final int SUBMISSIONS_PER_BATCH = 500;

    @Override
    public void initialize() throws Exception {
//        this.maps = getComponent(MapEngine.class);
        this.saxon = getComponent(SaxonEngine.class);
        this.search = getComponent(SearchEngine.class);
//        this.users = (Users) getComponent("Users");
//        this.events = (Events) getComponent("Events");
        this.autocompletion = getComponent(Autocompletion.class);

//        this.document = new StoredDocument(
//                new File(getPreferences().getString("bs.studies.persistence-location")),
//                "studies");

//        this.species = new FilePersistence<>(
//                new PersistableString()
//                , new File(getPreferences().getString("bs.species.dropdown-html-location"))
//
//        );
//
//        this.arrays = new FilePersistence<>(
//                new PersistableString()
//                , new File(getPreferences().getString("bs.arrays.dropdown-html-location"))
//        );

//        maps.registerMap(new MapEngine.SimpleValueMap(MAP_EXPERIMENTS_IN_ATLAS));
//        maps.registerMap(new MapEngine.SimpleValueMap(MAP_STUDIES_VIEWS));
//        maps.registerMap(new MapEngine.SimpleValueMap(MAP_STUDIES));
//        maps.registerMap(new MapEngine.SimpleValueMap(MAP_STUDIES_COMPLETE_DOWNLOADS));
//        maps.registerMap(new MapEngine.SimpleValueMap(MAP_VISIBLE_EXPERIMENTS));
//        maps.registerMap(new MapEngine.SimpleValueMap(MAP_EXPERIMENTS_FOR_PROTOCOL));
//        maps.registerMap(new MapEngine.SimpleValueMap(MAP_EXPERIMENTS_FOR_ARRAY));
//        maps.registerMap(new MapEngine.SimpleValueMap(MAP_EXPERIMENTS_FOR_USER));
//        users.registerUserMap(new MapEngine.SimpleValueMap(INDEX_ID));

        update("<studies/>"); // Initialise index with empty body if it doesn't exist
//        updateMaps();
       // this.saxon.registerDocumentSource(this);
    }

    @Override
    public void terminate() throws Exception {
    }

    /*
    @Override
    public String getURI() {
        return "studies.xml";
    }

    @Override
    public synchronized NodeInfo getRootNode() throws IOException {
        return document.getRootNode();
    }

    @Override
    public synchronized void setRootNode(NodeInfo rootNode) throws IOException, SaxonException {
        if (null != rootNode) {
            document = new StoredDocument(rootNode,
                    new File(getPreferences().getString("bs.studies.persistence-location")));
            updateIndex();
//            updateMaps();
        } else {
            this.logger.error("Studies NOT updated, NULL document passed");
        }
    }
    */
//    public String getSpecies() throws IOException {
//        return this.species.getObject().get();
//    }
//
//    public String getArrays() throws IOException {
//        return this.arrays.getObject().get();
//    }

    // Creates a new document from a string, saves it, and indexes it.
    public synchronized void update(String xmlString) throws IOException, InterruptedException {
        try {
            NodeInfo updateXml = this.saxon.transform(
                    xmlString
                    , "preprocess-studies-xml.xsl"
                    , null
            );
            if (null != updateXml) {
                Document document = new StoredDocument(updateXml.getDocumentRoot(),
                        new File(getPreferences().getString("bs.studies.persistence-location")));
                updateIndex(document);
            }
        } catch (SaxonException x) {
            throw new RuntimeException(x);
        }
    }


    public synchronized void updateFromXMLFile(String xmlFileName, boolean deleteFileAfterProcessing) throws IOException, InterruptedException, TransformerException, IndexerException, SaxonException, XMLStreamException {
        updateFromXMLFile(xmlFileName, deleteFileAfterProcessing, true);
    }

    public synchronized void updateFromXMLFile(String xmlFileName, boolean deleteFileAfterProcessing, boolean makeCopy) throws IOException, InterruptedException, TransformerException, IndexerException, SaxonException, XMLStreamException {
        if (xmlFileName == null) {
            xmlFileName = "studies.xml";
        }
        String sourceLocation = getPreferences().getString("bs.studies.source-location");
        if (isNotBlank(sourceLocation)) {
            File xmlFile = new File(sourceLocation, xmlFileName);
            Indexer indexer = new Indexer(INDEX_ID, saxon.getxPathEvaluator());
            MultithreadStudyParser.updateFromXMLFile(xmlFile, deleteFileAfterProcessing, makeCopy, sourceLocation, indexer, saxon, autocompletion);
        }
    }

    public synchronized void updateFromXMLFile(File originalXMLFile, boolean deleteFileAfterProcessing) throws IOException, InterruptedException, SaxonException, TransformerException, IndexerException, XMLStreamException {
        String sourceLocation = getPreferences().getString("bs.studies.source-location");
        Indexer indexer = new Indexer(INDEX_ID, saxon.getxPathEvaluator());
        MultithreadStudyParser.updateFromXMLFile(originalXMLFile, deleteFileAfterProcessing, true, sourceLocation, indexer, saxon, autocompletion);
    }

    // Updates the index one study at a time
    public synchronized void updateFromXMLFile(File originalXMLFile, boolean deleteFileAfterProcessing, boolean makeCopy) throws IOException, InterruptedException, SaxonException, TransformerException, IndexerException, XMLStreamException {
        String sourceLocation = getPreferences().getString("bs.studies.source-location");
        Indexer indexer = new Indexer(INDEX_ID, saxon.getxPathEvaluator());
        MultithreadStudyParser.updateFromXMLFile(originalXMLFile, deleteFileAfterProcessing, makeCopy, sourceLocation, indexer, saxon, autocompletion);
    }

    private void processSubmissionQueue(Indexer indexer, List<Source> submissions, boolean commit) throws XPathException, IndexerException, InterruptedException, IOException, SaxonException {
        int count = 0;
        StringBuilder sb = new StringBuilder("<pmdocument><submissions>");
        String deleteAccession;
        for (Source node:submissions) {
            deleteAccession =  (this.saxon.evaluateXPath((NodeInfo) node, "submission/@delete").size() > 0)
                ? this.saxon.evaluateXPath((NodeInfo) node, "submission/@acc").get(0).getStringValue()
                : null;
            if (deleteAccession==null) {
                count++;
                sb.append(saxon.serializeDocument(node, true));
            }
            if (count % SUBMISSIONS_PER_BATCH ==0
                    || deleteAccession!=null
                    || sb.length() >= 50 * Files.MB
                    ) {
                sb.append("</submissions></pmdocument>");
                NodeInfo submissionDocument = saxon.buildDocument(sb.toString());
                NodeInfo updateXml = this.saxon.transform(
                        submissionDocument
                        , "preprocess-studies-xml.xsl"
                        , null
                );
                indexer.index(updateXml, commit);
                sb = new StringBuilder("<pmdocument><submissions>");
                logger.info("Processed {}/{} submissions", count , submissions.size());
            }

            if (deleteAccession!=null) {
                this.search.getController().delete(INDEX_ID, deleteAccession);
                logger.info("Delete submission {} ", deleteAccession);
            }
        }

        // index unprocessed list
        sb.append("</submissions></pmdocument>");
        NodeInfo submissionDocument = saxon.buildDocument(sb.toString());
        NodeInfo updateXml = this.saxon.transform(
                submissionDocument
                , "preprocess-studies-xml.xsl"
                , null
        );
        indexer.index(updateXml, commit);
        logger.info("Processed {}/{} submissions", count, submissions.size());

    }

    private void updateIndex(Document document) throws IOException {
        try {
            this.search.getController().index(INDEX_ID, document);
            this.autocompletion.rebuild();
        } catch (Exception x) {
            throw new RuntimeException(x);
        }
    }

    public void delete(String accession) throws IOException {
        try {
            this.search.getController().delete(INDEX_ID, accession);
            this.autocompletion.rebuild();
        } catch (Exception x) {
            throw new RuntimeException(x);
        }
    }

    public void clearIndex() throws IOException {
        try {
            this.search.getController().clearIndex(INDEX_ID);
            this.autocompletion.rebuild();
        } catch (Exception x) {
            throw new RuntimeException(x);
        }
    }

    // returns null is document is not accessible
    public String getRelativePath(String accession, User authenticatedUser) throws SaxonException, XPathException {
        Querier querier = new Querier(this.search.getController().getEnvironment(INDEX_ID));
        String xml = querier.getDocumentXml(accession, authenticatedUser);
        if (xml==null) return null;
        NodeInfo documentNode = saxon.buildDocument(xml);
        return saxon.evaluateXPathSingleAsString(documentNode, "/study/@relPath");
    }




}
