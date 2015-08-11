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
import org.apache.commons.io.IOUtils;
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
import java.io.*;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.*;

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

            SAXSource source = new SAXSource();

            String errorString = "";
            int returnCode = 0;

            if (System.getProperty("os.name").contains("Windows")) {
                getWindowsSource(rootFolder, source);
            } else {
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
                errorString = CharStreams.toString(new InputStreamReader(stdErr, "UTF-8"));
                returnCode = process.waitFor();

            }
            source.setXMLReader(new FlatFileXMLReader(' ', '\"'));

            Map<String, String[]> transformParams = new HashMap<>();
            transformParams.put("rootFolder", new String[]{rootFolder});
            NodeInfo result = saxonEngine.transform(
                    source
                    , "preprocess-files-xml.xsl"
                    , transformParams
            );

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

    // This methods is only for debugging on a windows machine
    private void getWindowsSource(String rootFolder, SAXSource source) throws IOException {
        final Path start = Paths.get(rootFolder);
        final StringBuffer sb = new StringBuffer();
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

        java.nio.file.Files.walkFileTree (start, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                if (dir.toString().equalsIgnoreCase(start.toString())) return FileVisitResult.CONTINUE;
                sb.append("drwxrwxr-x 2 ma-svc microarray ");
                sb.append(attrs.size());
                sb.append(" ");
                sb.append(dateFormat.format(new Date(attrs.creationTime().toMillis())));
                sb.append(" ");
                sb.append("\"");
                sb.append(dir.toString().replaceAll("\\\\", "/"));
                sb.append("\"\n");
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path filePath, BasicFileAttributes attrs)
                    throws IOException {
                sb.append("-rw-r--r-- 1 ma-svc microarray ");
                sb.append(attrs.size());
                sb.append(" ");
                sb.append(dateFormat.format(new Date(attrs.creationTime().toMillis())));
                sb.append(" ");
                sb.append("\"");
                sb.append(filePath.toString().replaceAll("\\\\", "/"));
                sb.append("\"\n");
                return FileVisitResult.CONTINUE;
            }
        });
        source.setInputSource(
                new InputSource(
                        new FilteringIllegalHTMLCharactersReader(
                                new RemovingMultipleSpacesReader(
                                        new InputStreamReader(
                                                IOUtils.toInputStream(sb.toString())
                                        )
                                )
                        )
                )
        );
    }
}
