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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.biostudies.app.ApplicationComponent;
import uk.ac.ebi.biostudies.utils.saxon.DocumentUpdater;
import uk.ac.ebi.biostudies.utils.saxon.SaxonException;
import uk.ac.ebi.biostudies.utils.saxon.StoredDocument;
import uk.ac.ebi.biostudies.utils.saxon.XMLDocumentSource;

import java.io.File;
import java.io.IOException;

public class Events extends ApplicationComponent implements XMLDocumentSource {
    // logging machinery
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private StoredDocument document;
    private SearchEngine search;

    public final String INDEX_ID = "events";

    public static interface IEventInformation {
        public abstract NodeInfo getEventXML();
    }

    @Override
    public void initialize() throws Exception {
        SaxonEngine saxon = getComponent(SaxonEngine.class);
        this.search = getComponent(SearchEngine.class);

        this.document = new StoredDocument(
                new File(getPreferences().getString("bs.events.persistence-location")),
                "events"
        );

        updateIndex();
        saxon.registerDocumentSource(this);
    }

    @Override
    public void terminate() throws Exception {
    }

    @Override
    public String getURI() {
        return "events.xml";
    }

    @Override
    public synchronized NodeInfo getRootNode() throws IOException {
        return document.getRootNode();
    }

    @Override
    public synchronized void setRootNode(NodeInfo rootNode) throws IOException, SaxonException {
        if (null != rootNode) {
            document = new StoredDocument(rootNode,
                    new File(getPreferences().getString("bs.events.persistence-location")));
            updateIndex();
        } else {
            this.logger.error("Events NOT updated, NULL document passed");
        }
    }

    public void addEvent(IEventInformation event) throws IOException, InterruptedException {
        try {
            NodeInfo events = event.getEventXML();
            if (null != events) {
                new DocumentUpdater(this, events).update();
            }
        } catch (SaxonException x) {
            throw new RuntimeException(x);
        }
    }

    private void updateIndex() throws IOException {
        try {
            this.search.getController().index(INDEX_ID, document);
        } catch (Exception x) {
            throw new RuntimeException(x);
        }
    }
}