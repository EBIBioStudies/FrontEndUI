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
import com.google.common.io.Files;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.arrayexpress.app.ApplicationJob;
import uk.ac.ebi.arrayexpress.components.Ontologies;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

public class ReloadOntologyJob extends ApplicationJob {
    // logging machinery
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void doExecute(JobExecutionContext jec) throws Exception {
        // check if efo.owl is in temp folder; if it's not there, copy from source
        String efoLocation = getPreferences().getString("bs.efo.location");
        File efoFile = new File(efoLocation);
        if (!efoFile.exists()) {
            String efoBuiltinSource = getPreferences().getString("bs.efo.source");
            try (InputStream is = getApplication().getResource(efoBuiltinSource).openStream()) {
                Files.write(CharStreams.toString(new InputStreamReader(is, "UTF-8")),
                        efoFile, Charset.forName("UTF-8"));
            }
        }

        logger.info("Loading EFO ontology from [{}]", efoFile.getPath());

        try (InputStream is = new FileInputStream(efoFile)) {
            getComponent(Ontologies.class).update(is);
            logger.info("EFO loading completed");
        }
    }
}