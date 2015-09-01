package uk.ac.ebi.arrayexpress.components.thumbnails;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.util.ImageIOUtil;
import org.apache.poi.xwpf.converter.pdf.PdfConverter;
import org.apache.poi.xwpf.converter.pdf.PdfOptions;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by awais on 01/09/2015.
 */
public class DOCXThumbnail implements IThumbnail{

    private static String [] supportedTypes= {"docx"};

    @Override
    public String[] getSupportedTypes() {
        return supportedTypes;
    }

    @Override
    public void generateThumbnail(String sourceFilePath, File thumbnailFile) throws IOException{
        //TODO: Confirm licence
        //convert word to pdf
        String tempPDFFilePath = thumbnailFile.getAbsolutePath() + ".pdf";
        FileInputStream in = new FileInputStream(sourceFilePath);
        FileOutputStream out = new FileOutputStream(tempPDFFilePath);
        XWPFDocument wordDoc = new XWPFDocument(in);
        PdfConverter.getInstance().convert(wordDoc, out, PdfOptions.create());
        in.close();
        out.close();
        //convert pdf to image
        PDPage page = (PDPage) PDDocument.load(tempPDFFilePath).getDocumentCatalog().getAllPages().get(0);
        BufferedImage image = page.convertToImage(BufferedImage.TYPE_INT_RGB, 96);
        ImageIOUtil.writeImage(image, thumbnailFile.getAbsolutePath(), 96);
        new File(tempPDFFilePath).delete();
    }


}
