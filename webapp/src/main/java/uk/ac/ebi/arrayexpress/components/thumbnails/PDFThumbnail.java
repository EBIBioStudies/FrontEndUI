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

package uk.ac.ebi.arrayexpress.components.thumbnails;

import org.apache.pdfbox.io.RandomAccess;
import org.apache.pdfbox.io.RandomAccessFile;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.util.ImageIOUtil;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class PDFThumbnail implements IThumbnail {

    private static String[] supportedTypes = {"pdf"};

    @Override
    public String[] getSupportedTypes() {
        return supportedTypes;
    }

    @Override
    public void generateThumbnail(String sourceFilePath, File thumbnailFile) throws IOException {
        PDDocument pdf = null;
        try {
            pdf = PDDocument.load(sourceFilePath);
            PDPage page = (PDPage) pdf.getDocumentCatalog().getAllPages().get(0);
            BufferedImage image = page.convertToImage(BufferedImage.TYPE_INT_RGB, 96);
            ImageIOUtil.writeImage(image, thumbnailFile.getAbsolutePath(), 96);
        } finally {
            if(pdf!=null) {
                pdf.close();
            }
        }
    }
}
