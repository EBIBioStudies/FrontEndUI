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

import com.google.common.io.CharStreams;
import net.sf.saxon.om.NodeInfo;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import uk.ac.ebi.arrayexpress.app.ApplicationJob;
import uk.ac.ebi.arrayexpress.components.Files;
import uk.ac.ebi.arrayexpress.components.SaxonEngine;
import uk.ac.ebi.arrayexpress.utils.io.FilteringIllegalHTMLCharactersReader;
import uk.ac.ebi.arrayexpress.utils.io.RemovingMultipleSpacesReader;
import uk.ac.ebi.arrayexpress.utils.saxon.FlatFileXMLReader;

import javax.xml.transform.sax.SAXSource;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RescanFilesJob extends ApplicationJob {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void doExecute(JobExecutionContext jec) throws Exception {
        Files files = getComponent(Files.class);
        SaxonEngine saxonEngine = getComponent(SaxonEngine.class);

        String rootFolder = files.getRootFolder();
        if (null != rootFolder) {
            String listAllFilesCommand = getPreferences().getString("bs.files.list-all-command");
            this.logger.info("Rescan of downloadable files from [{}] requested", rootFolder);

            List<String> commandParams = new ArrayList<>();
            commandParams.add("/bin/sh");
            commandParams.add("-c");
            commandParams.add(listAllFilesCommand);
            this.logger.debug("Executing [{}]", listAllFilesCommand);
            ProcessBuilder pb = new ProcessBuilder(commandParams);
            Map<String, String> env = pb.environment();
            env.put("LC_ALL", "en_US.UTF-8");
            env.put("LANG", "en_US.UTF-8");
            env.put("LANGUAGE", "en_US.UTF-8");
            Process process = pb.start();

            InputStream stdOut = process.getInputStream();
            InputStream stdErr = process.getErrorStream();

            SAXSource source = new SAXSource();
            source.setInputSource(
                    new InputSource(
                            new FilteringIllegalHTMLCharactersReader(
                                    new RemovingMultipleSpacesReader(
                                            new InputStreamReader(
                                                    stdOut
                                            )
                                    )
                            )
                    )
            );

            source.setXMLReader(new FlatFileXMLReader(' ', '\"'));

            Map<String, String[]> transformParams = new HashMap<>();
            transformParams.put("rootFolder", new String[]{rootFolder});
            /**
            StringTools.stringToFile(
                    saxonEngine.serializeDocument(source)
                    , new File(
                        System.getProperty("java.io.tmpdir")
             , "bs-files-src.xml"
                    )
                    , "UTF-8"
            );
             **/
            NodeInfo result = saxonEngine.transform(
                    source
                    , "preprocess-files-xml.xsl"
                    , transformParams
            );

            String errorString = CharStreams.toString(new InputStreamReader(stdErr, "UTF-8"));
            int returnCode = process.waitFor();

            if (0 == returnCode) {
                getComponent(Files.class).reload(result, errorString);
                this.logger.info("Rescan of downloadable files completed");
            } else {
                this.logger.error("Rescan returned exit code [{}], update not performed", returnCode);
                if (errorString.length() > 0) {
                    throw new RuntimeException(errorString);
                }
            }

        } else {
            this.logger.error("Rescan problem: root folder has not been set");
        }
    }
}
