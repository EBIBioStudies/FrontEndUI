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

package uk.ac.ebi.arrayexpress.components;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.arrayexpress.app.ApplicationComponent;
import uk.ac.ebi.arrayexpress.utils.persistence.FilePersistence;
import uk.ac.ebi.arrayexpress.utils.saxon.*;
import uk.ac.ebi.arrayexpress.utils.saxon.search.IndexerException;

import java.io.File;
import java.io.IOException;

public class Studies extends ApplicationComponent implements IDocumentSource {
    private final Logger logger = LoggerFactory.getLogger(getClass());

//    public final static String MAP_STUDIES_VIEWS = "studies-views";
//    public final static String MAP_STUDIES = "studies-downloads";
//    public final static String MAP_STUDIES_COMPLETE_DOWNLOADS = "studies-complete-downloads";
//    public final static String MAP_STUDIES = "studies";
//    public final static String MAP_STUDIES_FOR_USER = "studies-for-user";

    private FilePersistence<PersistableDocumentContainer> document;
//    private FilePersistence<PersistableString> species;
//    private FilePersistence<PersistableString> arrays;

    private MapEngine maps;
    private SaxonEngine saxon;
    private SearchEngine search;
//    private Users users;
//    private Events events;
    private Autocompletion autocompletion;

    public final String INDEX_ID = "studies";

    public Studies() {
    }

    @Override
    public void initialize() throws Exception {
        this.maps = (MapEngine) getComponent("MapEngine");
        this.saxon = (SaxonEngine) getComponent("SaxonEngine");
        this.search = (SearchEngine) getComponent("SearchEngine");
//        this.users = (Users) getComponent("Users");
//        this.events = (Events) getComponent("Events");
        this.autocompletion = (Autocompletion) getComponent("Autocompletion");

        this.document = new FilePersistence<>(
                new PersistableDocumentContainer("studies")
                , new File(getPreferences().getString("bs.studies.persistence-location"))
        );

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

        updateIndex();
//        updateMaps();
        this.saxon.registerDocumentSource(this);
    }

    @Override
    public void terminate() throws Exception {
    }

    // implementation of IDocumentSource.getDocumentURI()
    @Override
    public String getDocumentURI() {
        return "studies.xml";
    }

    // implementation of IDocumentSource.getDocument()
    @Override
    public synchronized Document getDocument() throws IOException {
        return this.document.getObject().getDocument();
    }

    // implementation of IDocumentSource.setDocument(Document)
    @Override
    public synchronized void setDocument(Document doc) throws IOException, InterruptedException {
        if (null != doc) {
            this.document.setObject(new PersistableDocumentContainer("studies", doc));
            updateIndex();
//            updateMaps();
        } else {
            this.logger.error("Studies NOT updated, NULL document passed");
        }
    }

//    public String getSpecies() throws IOException {
//        return this.species.getObject().get();
//    }
//
//    public String getArrays() throws IOException {
//        return this.arrays.getObject().get();
//    }

    public void update(String xmlString) throws IOException, InterruptedException {
//        boolean success = false;
        try {
            Document updateDoc = this.saxon.transform(
                    xmlString
                    , "preprocess-studies-xml.xsl"
                    , null
            );
            if (null != updateDoc) {
                new DocumentUpdater(this, updateDoc).update();
//                buildSpeciesArrays();
//                success = true;
            }
        } catch (SaxonException x) {
            throw new RuntimeException(x);
//        } finally {
//            sourceInformation.setOutcome(success);
//            events.addEvent(sourceInformation);
        }
    }

    private void updateIndex() throws IOException, InterruptedException {
        Thread.sleep(0);
        try {
            this.search.getController().index(INDEX_ID, this.getDocument());
            this.autocompletion.rebuild();
        } catch (IndexerException x) {
            throw new RuntimeException(x);
        }
    }

//    private void updateMaps() throws IOException {
//        this.logger.debug("Updating maps for studies");
//
//        maps.clearMap(MAP_VISIBLE_EXPERIMENTS);
//        maps.clearMap(MAP_EXPERIMENTS_FOR_PROTOCOL);
//        maps.clearMap(MAP_EXPERIMENTS_FOR_ARRAY);
//        users.clearUserMap(INDEX_ID);
//
//        try {
//            List<Object> documentNodes = saxon.evaluateXPath(getDocument(), "/experiments/experiment[source/@visible = 'true']");
//
//            for (Object node : documentNodes) {
//                try {
//                    NodeInfo exp = (NodeInfo) node;
//
//                    String accession = saxon.evaluateXPathSingleAsString(exp, "accession");
//                    maps.setMappedValue(MAP_VISIBLE_EXPERIMENTS, accession, exp);
//                    List<Object> userIds = saxon.evaluateXPath(exp, "user/@id");
//                    if (null != userIds && userIds.size() > 0) {
//                        Set<String> usersForExperiment = new HashSet<>(userIds.size());
//                        for (Object userId : userIds) {
//                            String id = ((Item) userId).getStringValue();
//
//                            @SuppressWarnings("unchecked")
//                            Set<String> experimentsForUser = (Set<String>) maps.getMappedValue(MAP_EXPERIMENTS_FOR_USER, id);
//                            if (null == experimentsForUser) {
//                                experimentsForUser = new HashSet<>();
//                                maps.setMappedValue(MAP_EXPERIMENTS_FOR_USER, id, experimentsForUser);
//                            }
//                            experimentsForUser.add(accession);
//                            usersForExperiment.add(id);
//                        }
//                        users.setUserMapping(INDEX_ID, accession, usersForExperiment);
//                    }
//
//                    List<Object> protocolIds = saxon.evaluateXPath(exp, "protocol/id");
//                    if (null != protocolIds) {
//                        for (Object protocolId : protocolIds) {
//                            String id = ((Item) protocolId).getStringValue();
//                            @SuppressWarnings("unchecked")
//                            Set<String> experimentsForProtocol = (Set<String>) maps.getMappedValue(MAP_EXPERIMENTS_FOR_PROTOCOL, id);
//                            if (null == experimentsForProtocol) {
//                                experimentsForProtocol = new HashSet<>();
//                                maps.setMappedValue(MAP_EXPERIMENTS_FOR_PROTOCOL, id, experimentsForProtocol);
//                            }
//                            experimentsForProtocol.add(accession);
//                        }
//                    }
//                    List<Object> arrayAccessions = saxon.evaluateXPath(exp, "arraydesign/accession");
//                    if (null != arrayAccessions) {
//                        for (Object arrayAccession : arrayAccessions) {
//                            String arrayAcc = ((Item) arrayAccession).getStringValue();
//                            @SuppressWarnings("unchecked")
//                            Set<String> experimentsForArray = (Set<String>) maps.getMappedValue(MAP_EXPERIMENTS_FOR_ARRAY, arrayAcc);
//                            if (null == experimentsForArray) {
//                                experimentsForArray = new HashSet<>();
//                                maps.setMappedValue(MAP_EXPERIMENTS_FOR_ARRAY, arrayAcc, experimentsForArray);
//                            }
//                            experimentsForArray.add(accession);
//                        }
//                    }
//                } catch (XPathException x) {
//                    this.logger.error("Caught an exception:", x);
//                }
//            }
//
//            this.logger.debug("Maps updated");
//        } catch (Exception x) {
//            this.logger.error("Caught an exception:", x);
//        }
//    }
//
//    private void buildSpeciesArrays() throws IOException {
//        // todo: move this to a separate component (autocompletion?)
//        try {
//            String speciesString = saxon.transformToString(this.getDocument(), "build-species-list-html.xsl", null);
//            this.species.setObject(new PersistableString(speciesString));
//
//            String arraysString = saxon.transformToString(this.getDocument(), "build-arrays-list-html.xsl", null);
//            this.arrays.setObject(new PersistableString(arraysString));
//        } catch (SaxonException x) {
//            throw new RuntimeException(x);
//        }
//
//    }
}
