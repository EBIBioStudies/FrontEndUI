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

package uk.ac.ebi.biostudies.jobs;

import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.biostudies.app.ApplicationJob;
import uk.ac.ebi.biostudies.components.Studies;

import java.io.File;
import java.io.IOException;

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
                logger.info("Reload of study data from [{}] requested", sourceLocation);
                updateStudies(new File(sourceLocation, "studies.xml"));
                logger.info("Reload of study data from [{}] completed", sourceLocation);
            }
        } catch (Exception x) {
            throw new RuntimeException(x);
        }
    }

    private void updateStudies(File file) throws IOException {
        try {
            getComponent(Studies.class).updateFromXMLFile(file, false);
            logger.info("Study information reload completed");
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

}