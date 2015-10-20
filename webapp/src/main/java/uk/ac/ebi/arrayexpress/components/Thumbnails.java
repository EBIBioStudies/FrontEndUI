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
import org.apache.pdfbox.util.ImageIOUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.arrayexpress.app.ApplicationComponent;
import uk.ac.ebi.arrayexpress.components.thumbnails.*;

import javax.servlet.http.HttpServletResponse;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextLayout;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.AttributedCharacterIterator;
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

    public void sendThumbnail(HttpServletResponse response, String relativePath, String name) throws IOException {
        Files files = getComponent(Files.class);
        File thumbnail = new File(getThumbnailsFolder()+"/"+relativePath+"/"+name+".thumbnail.png");

        if (!thumbnail.exists()) {
            createThumbnail(files.getRootFolder() + "/"+ relativePath+"/Files/"+name, files, thumbnail);
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

    private void createPlaceholderThumbnail(String sourceFilePath, Files files, File thumbnailFile) throws IOException {
        synchronized (sourceFilePath) {
            thumbnailFile.getParentFile().mkdirs();
            //Using extension to decide on the class as mime-types are different across *nix/Windows
            String fileType = FilenameUtils.getExtension(sourceFilePath).toLowerCase();
            logger.debug("Creating placeholder thumbnail [{}] for file type {}", thumbnailFile.getAbsolutePath(), fileType);
            int imageWidth=50, imageHeight = 65;
            BufferedImage image = new BufferedImage(imageWidth,imageHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, imageWidth, imageHeight);
            g.setColor(Color.BLACK);
            g.setFont(new Font("sans-serif", Font.PLAIN, 12));
            int stringLen = (int) g.getFontMetrics().getStringBounds(fileType, g).getWidth();
            int start = imageWidth/2 - stringLen/2;
            g.drawString(fileType, start, imageHeight/2);
            ImageIOUtil.writeImage(image, thumbnailFile.getAbsolutePath(), 96);
        }
    }
    private void createThumbnail(String sourceFilePath, Files files, File thumbnailFile) throws IOException {
        synchronized (sourceFilePath) {
            thumbnailFile.getParentFile().mkdirs();
            //Using extension to decide on the class as mime-types are different across *nix/Windows
            String fileType = FilenameUtils.getExtension(sourceFilePath).toLowerCase();
            logger.debug("Creating thumbnail [{}] for file type {}", thumbnailFile.getAbsolutePath(), fileType);
            if (thumbnailGenerators.containsKey(fileType)) {
                try {
                    thumbnailGenerators.get(fileType).generateThumbnail(sourceFilePath, thumbnailFile);
                } catch (Throwable err) {
                    logger.debug("Error creating thumbnail: ", err.getMessage());
                    logger.debug("Will try to create placeholder now");
                    createPlaceholderThumbnail(sourceFilePath, files, thumbnailFile);
                }
            } else {
                logger.debug("Invalid file type for creating thumbnail: {}", fileType);
                throw new IOException("Invalid file type for creating thumbnail");
            }
        }
    }

    }
