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

import org.apache.commons.io.FileDeleteStrategy;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.arrayexpress.app.ApplicationComponent;
import uk.ac.ebi.arrayexpress.components.thumbnails.*;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Thumbnails extends ApplicationComponent {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private String thumbnailsFolder;
    private Map<String, IThumbnail> thumbnailGenerators = new HashMap<>();

    public Thumbnails() {
        //register thumbnail generators
        //TODO: use ServiceLoader or annotations instead
        registerThumbnailHandler(new ImageThumbnail());
        registerThumbnailHandler(new PDFThumbnail());
        registerThumbnailHandler(new DOCXThumbnail());
        registerThumbnailHandler(new TXTThumbnail());

    }

    private void registerThumbnailHandler(IThumbnail thumbnailHandler) {
        for (String mimeType: thumbnailHandler.getSupportedTypes()) {
            thumbnailGenerators.put(mimeType, thumbnailHandler);
        }
    }

    @Override
    public void initialize() throws Exception {
    }

    @Override
    public void terminate() throws Exception {
    }

    public synchronized String getThumbnailsFolder() {
        if (null == this.thumbnailsFolder) {
            this.thumbnailsFolder = getPreferences().getString("bs.studies.thumbnails-location");
        }
        return this.thumbnailsFolder;
    }

    public synchronized void clearThumbnails() throws IOException {
        FileDeleteStrategy.FORCE.delete(new File(getThumbnailsFolder()));
    }

    public void sendThumbnail(HttpServletResponse response, String location) throws IOException {
        Files files = getComponent(Files.class);
        File thumbnail = new File(getThumbnailsFolder()+location+".thumbnail.png");

        if (!thumbnail.exists()) {
            createThumbnail(files.getRootFolder() + location, files, thumbnail);
        }
        FileInputStream in = new FileInputStream(thumbnail);
        try {
            IOUtils.copy(in, response.getOutputStream());
        } finally {
            in.close();
        }
        response.getOutputStream().flush();
        response.getOutputStream().close();
    }

    private void createThumbnail(String sourceFilePath, Files files, File thumbnailFile) throws IOException {
        synchronized (sourceFilePath) {
            thumbnailFile.getParentFile().mkdirs();
            //Using extension to decide on the class as mime-types are different across *nix/Windows
            String fileType = FilenameUtils.getExtension(sourceFilePath).toLowerCase();
            logger.debug("Creating thumbnail [{}] for file type {}", thumbnailFile.getAbsolutePath(), fileType);
            if (thumbnailGenerators.containsKey(fileType)) {
                thumbnailGenerators.get(fileType).generateThumbnail(sourceFilePath, thumbnailFile);
            } else {
                logger.debug("Invalid file type for creating thumbnail: {}", fileType);
            }
        }
    }

}
