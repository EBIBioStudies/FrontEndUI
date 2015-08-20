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

import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.wmf.tosvg.WMFTranscoder;
import org.apache.batik.transcoder.wmf.tosvg.WMFUtilities;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.util.ImageIOUtil;
import org.apache.pdfbox.util.PDFImageWriter;
import org.apache.poi.hpsf.SummaryInformation;
import org.apache.poi.hpsf.Thumbnail;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hwpf.converter.AbstractWordUtils;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xssf.eventusermodel.XSSFReader;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.arrayexpress.app.ApplicationComponent;

import javax.activation.MimetypesFileTypeMap;
import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URLConnection;
import java.nio.file.Paths;
import java.util.Arrays;

public class Thumbnails extends ApplicationComponent {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private String thumbnailsFolder;

    public Thumbnails() {
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
        FileUtils.deleteDirectory(new File(getThumbnailsFolder()));
    }

    public void sendThumbnail(HttpServletResponse response, String location) throws IOException {
        Files files = getComponent(Files.class);
        File thumbnail = new File(files.getThumbnailsFolder()+location+".thumbnail.png");
        if (!thumbnail.exists()) {
            createThumbnail(files.getRootFolder() + location, files, thumbnail);
        }
        if (thumbnail.exists()) {
            FileInputStream in = new FileInputStream(thumbnail);
            IOUtils.copy(in, response.getOutputStream());
            in.close();
        }
        response.getOutputStream().flush();
        response.getOutputStream().close();
    }

    private void createThumbnail(String filepath, Files files, File thumbnailFile)  {
        try {
            thumbnailFile.getParentFile().mkdirs();
            String mimeType = java.nio.file.Files.probeContentType(Paths.get(filepath));
            logger.debug("Creating thumbnail [{}] for mime-type {}", thumbnailFile.getAbsolutePath(), mimeType);
            if (Arrays.asList(ImageIO.getReaderMIMETypes()).contains(mimeType)) {
                net.coobird.thumbnailator.Thumbnails.of(filepath)
                        .size(200, 200)
                        .outputFormat("png")
                        .toFile(thumbnailFile);
            } else if ("application/pdf".equalsIgnoreCase(mimeType)) {
                PDDocument document = PDDocument.load(filepath);
                PDPage page = (PDPage) PDDocument.load(filepath).getDocumentCatalog().getAllPages().get(0);
                BufferedImage image = page.convertToImage(BufferedImage.TYPE_INT_RGB,96);
                ImageIOUtil.writeImage(image, thumbnailFile.getAbsolutePath(), 96);
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

}
