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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.AgeFileFilter;
import org.apache.commons.lang.time.DateUtils;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.arrayexpress.app.ApplicationJob;
import uk.ac.ebi.arrayexpress.components.Files;
import uk.ac.ebi.arrayexpress.components.Studies;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Iterator;

import static org.apache.commons.lang.StringUtils.isNotBlank;

public class DeleteZipFTPFilesJob extends ApplicationJob {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Override
    public void doExecute(JobExecutionContext jec) throws Exception {
        try {
            logger.debug("Looking for expired Zipped FTP files");
            Files files = getComponent(Files.class);
            Date oldestFileDate = DateUtils.addDays(new Date(), -1);

            Iterator<File> filesToDelete = FileUtils.iterateFiles(
                    new File(files.getFtpFolder()), new AgeFileFilter(oldestFileDate), null);
            while (filesToDelete.hasNext()) {
                File file = filesToDelete.next();
                file.delete();
                logger.debug("Deleted "+ file.getAbsolutePath());
            }
        } catch (Exception x) {
            throw new RuntimeException(x);
        }
    }


}