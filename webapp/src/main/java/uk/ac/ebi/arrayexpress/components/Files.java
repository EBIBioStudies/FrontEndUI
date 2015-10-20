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

public class Files extends ApplicationComponent {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private String rootFolder;
    public Files() {
    }
    public synchronized String getRootFolder() {
        return this.rootFolder;
    }

    @Override
    public void initialize() throws Exception {
        this.rootFolder = getPreferences().getString("bs.studies.files-location");
    }

    @Override
    public void terminate() throws Exception {

    }
}
