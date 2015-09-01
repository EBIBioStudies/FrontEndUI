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

package uk.ac.ebi.arrayexpress.jobs;

import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.arrayexpress.app.ApplicationJob;
import uk.ac.ebi.arrayexpress.components.*;
import uk.ac.ebi.arrayexpress.utils.FileTools;
import uk.ac.ebi.arrayexpress.utils.StringTools;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import static org.apache.commons.lang.StringUtils.isNotBlank;

public class ReloadStudiesJob extends ApplicationJob {
    // logging machinery
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void doExecute(JobExecutionContext jec) throws Exception {
        try {
            // check preferences and if source location is defined, use that
            String sourceLocation = getPreferences().getString("bs.studies.source-location");
            if (isNotBlank(sourceLocation)) {
                logger.info("Reload of experiment data from [{}] requested", sourceLocation);
                updateStudies(new File(sourceLocation, "studies.xml"));
//                updateNews(new File(sourceLocation, "news.xml"));
//                updateUsers(new File(sourceLocation, "users.xml"));
//                updateArrayDesigns(new File(sourceLocation, "arrays.xml"));
//                updateProtocols(new File(sourceLocation, "protocols.xml"));
                logger.info("Reload of experiment data from [{}] completed", sourceLocation);
            }
        } catch (Exception x) {
            throw new RuntimeException(x);
        }
    }



    private void loadMapFromFile(String mapName, File mapFile) throws IOException {
        if (null != mapFile && mapFile.exists()) {
            getComponent(MapEngine.class).loadMap(mapName, mapFile);
        }
    }

    private void clearMap(String mapName) {
        getComponent(MapEngine.class).clearMap(mapName);
    }

//    private void updateNews(File xmlFile) throws IOException, InterruptedException {
//        if (null != xmlFile && xmlFile.exists()) {
//            String xmlString = getXmlFromFile(xmlFile);
//            if (isNotBlank(xmlString)) {
//                ((News) getComponent("News")).update(xmlString);
//                logger.info("News reload completed");
//            }
//        }
//    }
//
//    private void updateUsers(File xmlFile) throws IOException, InterruptedException {
//        if (null != xmlFile && xmlFile.exists()) {
//            String xmlString = getXmlFromFile(xmlFile);
//            if (isNotBlank(xmlString)) {
//                ((Users) getComponent("Users")).update(xmlString, Users.UserSource.AE2);
//                logger.info("User information reload completed");
//            } else {
//                throw new IOException("[" + xmlFile.getPath() + "] is null or empty");
//            }
//        } else {
//            throw new IOException("Unable to locate [" + (null != xmlFile ? xmlFile.getPath() : "null") + "]");
//        }
//    }
//
    private void updateStudies(File file) throws IOException, InterruptedException {
        if (null != file && file.exists()) {
            String xml = FileTools.readXMLStringFromFile(file);

            if (isNotBlank(xml)) {
                // export to temp directory anyway (only if debug is enabled)
                if (logger.isDebugEnabled()) {
                    com.google.common.io.Files.write(
                            xml
                            , new File(
                                    System.getProperty("java.io.tmpdir")
                                    , "src-studies.xml"
                            )
                            , Charset.forName("UTF-8")
                    );
                }
                getComponent(Studies.class).update(xml);


//                UpdateSourceInformation sourceInformation = new UpdateSourceInformation(
//                        Studies.StudySource.AE2
//                        , file
//                );
//
//                loadMapFromFile(
//                        Studies.MAP_EXPERIMENTS_IN_ATLAS
//                        , new File(file.getParentFile(), "atlas-experiments.txt")
//                );
//
//                loadMapFromFile(
//                        Studies.MAP_STUDIES_VIEWS
//                        , new File(file.getParentFile(), "experiments-views.txt")
//                );
//
//                loadMapFromFile(
//                        Studies.MAP_STUDIES
//                        , new File(file.getParentFile(), "experiments-downloads.txt")
//                );
//
//                loadMapFromFile(
//                        Studies.MAP_STUDIES_COMPLETE_DOWNLOADS
//                        , new File(file.getParentFile(), "experiments-complete-downloads.txt")
//                );
//                clearMap(Studies.MAP_EXPERIMENTS_IN_ATLAS);
//                clearMap(Studies.MAP_STUDIES_VIEWS);
//                clearMap(Studies.MAP_STUDIES);
//                clearMap(Studies.MAP_STUDIES_COMPLETE_DOWNLOADS);

                logger.info("Study information reload completed");
            } else {
                throw new IOException("[" + file.getPath() + "] is null or empty");
            }
        } else {
            throw new IOException("Unable to locate [" + (null != file ? file.getPath() : "null") + "]");
        }
    }

//    private void updateArrayDesigns(File xmlFile) throws IOException, InterruptedException {
//        if (null != xmlFile && xmlFile.exists()) {
//            String xmlString = getXmlFromFile(xmlFile);
//            if (isNotBlank(xmlString)) {
//                ((ArrayDesigns) getComponent("ArrayDesigns")).update(xmlString, ArrayDesigns.ArrayDesignSource.AE2);
//                logger.info("Array design information reload completed");
//            } else {
//                throw new IOException("[" + xmlFile.getPath() + "] is null or empty");
//            }
//        } else {
//            throw new IOException("Unable to locate [" + (null != xmlFile ? xmlFile.getPath() : "null") + "]");
//        }
//    }
//
//    private void updateProtocols(File xmlFile) throws IOException, InterruptedException {
//        if (null != xmlFile && xmlFile.exists()) {
//            String xmlString = getXmlFromFile(xmlFile);
//            if (isNotBlank(xmlString)) {
//                ((Protocols) getComponent("Protocols")).update(xmlString, Protocols.ProtocolsSource.AE2);
//                logger.info("Protocols information reload completed");
//            } else {
//                throw new IOException("[" + xmlFile.getPath() + "] is null or empty");
//            }
//        } else {
//            throw new IOException("Unable to locate [" + (null != xmlFile ? xmlFile.getPath() : "null") + "]");
//        }
//    }
}