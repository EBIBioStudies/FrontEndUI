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

/**
 * Created by awais on 01/09/2015.
 */
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
