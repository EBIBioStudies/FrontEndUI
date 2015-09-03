package uk.ac.ebi.arrayexpress.components.thumbnails;

import com.twelvemonkeys.image.ResampleOp;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.io.File;
import java.io.IOException;

/**
 * Created by awais on 01/09/2015.
 */
public class ImageThumbnail implements IThumbnail{

    String[] suppportedType = {"bmp","jpg","wbmp","jpeg","png","gif","tif","tiff","pdf","docx","txt","csv"};
    @Override
    public String[] getSupportedTypes() {
        return suppportedType;
    }

    @Override
    public void generateThumbnail(String sourceFilePath, File thumbnailFile) throws IOException{
        File sourceFile = new File(sourceFilePath);
        BufferedImage input = ImageIO.read(sourceFile); // Image to resample
        BufferedImageOp resampler = new ResampleOp(200,200, ResampleOp.FILTER_LANCZOS); // A good default filter, see class documentation for more info
        BufferedImage output = resampler.filter(input, null);
        if (!ImageIO.write(output, "png", thumbnailFile)) {
            throw new IOException("Cannot write thumbnail");
        }
    }
}
