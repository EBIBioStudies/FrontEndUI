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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.biostudies.app.ApplicationComponent;

public class Files extends ApplicationComponent {
    public static final int KB = 1024;
    public static final int MB = KB * KB;
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private String rootFolder;
    private String tempZipFolder;
    private String ftpURL;

    public Files() {
    }

    public synchronized String getRootFolder() {
        return this.rootFolder;
    }

    public synchronized String getTempZipFolder() {
        return this.tempZipFolder;
    }

    public synchronized String getFtpURL() {
        return this.ftpURL;
    }

    @Override
    public void initialize() throws Exception {
        this.rootFolder = getPreferences().getString("bs.studies.files-location");
        this.tempZipFolder = getPreferences().getString("bs.files.temp-zip.location");
        this.ftpURL = getPreferences().getString("bs.files.ftp.url");
        logger.debug("bs.studies.files-location = {}", this.rootFolder);
        logger.debug("bs.files.temp-zip.location = {}", this.tempZipFolder);
        logger.debug("bs.files.ftp.url = {}", this.ftpURL);
    }

    @Override
    public void terminate() throws Exception {

    }
}
