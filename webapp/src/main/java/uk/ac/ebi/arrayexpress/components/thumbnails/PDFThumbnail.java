package uk.ac.ebi.arrayexpress.components.thumbnails;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.util.ImageIOUtil;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Created by awais on 01/09/2015.
 */
public class PDFThumbnail implements IThumbnail{

    private static String [] supportedTypes= {"pdf"};

    @Override
    public String[] getSupportedTypes() {
        return supportedTypes;
    }
    @Override
    public void generateThumbnail(String sourceFilePath, File thumbnailFile) throws IOException{
        PDPage page = (PDPage) PDDocument.load(sourceFilePath).getDocumentCatalog().getAllPages().get(0);
        BufferedImage image = page.convertToImage(BufferedImage.TYPE_INT_RGB, 96);
        ImageIOUtil.writeImage(image, thumbnailFile.getAbsolutePath(), 96);
    }
}
