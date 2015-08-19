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

import net.sf.saxon.om.Item;
import net.sf.saxon.om.NodeInfo;
import net.sf.saxon.trans.XPathException;
import net.sf.saxon.value.BooleanValue;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.arrayexpress.app.ApplicationComponent;
import uk.ac.ebi.arrayexpress.utils.saxon.SaxonException;
import uk.ac.ebi.arrayexpress.utils.saxon.StoredDocument;
import uk.ac.ebi.arrayexpress.utils.saxon.XMLDocumentSource;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class Files extends ApplicationComponent implements XMLDocumentSource {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final String MAP_FOLDER = "accession-folder";

    private String rootFolder;
    private String thumbnailsFolder;
    private StoredDocument document;
    private String lastReloadMessage = "";

    private MapEngine maps;
    private SaxonEngine saxon;
    private SearchEngine search;

    public final String INDEX_ID = "files";

    public Files() {
    }

    @Override
    public void initialize() throws Exception {
        this.maps = getComponent(MapEngine.class);
        this.saxon = getComponent(SaxonEngine.class);
        this.search = getComponent(SearchEngine.class);

        this.document = new StoredDocument(
                new File(getPreferences().getString("bs.files.persistence-location")),
                "files"
        );

        maps.registerMap(new MapEngine.SimpleValueMap(MAP_FOLDER));

        updateIndex();
        updateAccelerators();
        this.saxon.registerDocumentSource(this);
    }

    @Override
    public void terminate() throws Exception {
    }

    @Override
    public String getURI() {
        return "files.xml";
    }

    @Override
    public synchronized NodeInfo getRootNode() throws IOException {
        return document.getRootNode();
    }

    @Override
    public synchronized void setRootNode(NodeInfo rootNode) throws IOException, SaxonException {
        if (null != rootNode) {
            document = new StoredDocument(rootNode,
                    new File(getPreferences().getString("bs.files.persistence-location")));
            updateIndex();
            updateAccelerators();
        } else {
            this.logger.error("Files NOT updated, NULL document passed");
        }
    }

    public void reload(NodeInfo xml, String message) throws IOException, SaxonException {
        setRootNode(xml);
        this.lastReloadMessage = message;
    }

    private void updateIndex() throws IOException {
        try {
            this.search.getController().index(INDEX_ID, document);
        } catch (Exception x) {
            throw new RuntimeException(x);
        }
    }

    @SuppressWarnings("unchecked")
    private void updateAccelerators() throws IOException {
        this.logger.debug("Updating maps for files");

        maps.clearMap(MAP_FOLDER);

        try {
            List<Item> documentNodes = saxon.evaluateXPath(getRootNode(), "/files/folder");
            for (Item node : documentNodes) {
                // get all the expressions taken care of
                String accession = saxon.evaluateXPathSingleAsString((NodeInfo) node, "@accession");
                maps.setMappedValue(MAP_FOLDER, accession, node);
            }
            this.logger.debug("Maps updated");
        } catch (XPathException x) {
            throw new RuntimeException(x);
        }
    }

    public synchronized void setRootFolder(String folder) {
        if (null != folder && 0 < folder.length()) {
            if (folder.endsWith(File.separator)) {
                this.rootFolder = folder;
            } else {
                this.rootFolder = folder + File.separator;
            }
        } else {
            this.logger.error("setRootFolder called with null or empty parameter, expect problems down the road");
        }
    }

    public synchronized String getRootFolder() {
        if (null == this.rootFolder) {
            this.rootFolder = getPreferences().getString("bs.studies.files-location");
        }
        return this.rootFolder;
    }

    public synchronized String getThumbnailsFolder() {
        if (null == this.thumbnailsFolder) {
            this.thumbnailsFolder = getPreferences().getString("bs.studies.thumbnails-location");
        }
        return this.thumbnailsFolder;
    }

    public synchronized void clearThumbnails() throws IOException {
        FileUtils.deleteDirectory(new File(getThumbnailsFolder()));
    }

    public String getLastReloadMessage() {
        return this.lastReloadMessage;
    }

    private String getFileLocatingXPQuery(String accession, String name) {
        accession = StringEscapeUtils.escapeXml(accession);
        name = StringEscapeUtils.escapeXml(name);
        return "/files/folder" +
                (StringUtils.isNotBlank(accession) ? "[@accession = '" + accession + "']" : "") +
                "/file[@name = '" + name + "']";
    }

    // returns true is file is registered in the registry
    public boolean doesExist(String accession, String name) throws IOException {
        boolean result = false;

        if (StringUtils.isNotBlank(name)) {

            try {
                result = ((BooleanValue) this.saxon.evaluateXPathSingle(
                        getRootNode()
                        , "exists(" + getFileLocatingXPQuery(accession, name) + ")"
                )).effectiveBooleanValue();
            } catch (XPathException x) {
                logger.error("Caught an exception:", x);
            }
        }
        return result;
    }

    // returns absolute file location (if file exists, null otherwise) in local filesystem
    public String getLocation(String accession, String name) throws IOException {
        String location = null;

        if (StringUtils.isNotBlank(name)) {
            try {
                String fileXPQuery = getFileLocatingXPQuery(accession, name);
                String xPathQuery = "concat(" + fileXPQuery + "/../@location, '/', " + fileXPQuery + "/@location)";
                location = this.saxon.evaluateXPathSingleAsString(
                        getRootNode()
                        , xPathQuery
                );
            } catch (XPathException x) {
                logger.error("Caught an exception:", x);
            }
        }

        return location;
    }
}
