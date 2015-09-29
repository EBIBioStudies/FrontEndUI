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
        BufferedImage input = ImageIO.read(sourceFile);
        float height = (float) input.getHeight(), width = (float) input.getWidth();
        if (width > 200 || height>200) {
            float inverseAspectRatio = height / width;
            BufferedImageOp resampler = new ResampleOp(200, Math.round(inverseAspectRatio * 200), ResampleOp.FILTER_LANCZOS);
            BufferedImage output = resampler.filter(input, null);
            if (!ImageIO.write(output, "png", thumbnailFile)) {
                throw new IOException("Cannot write thumbnail");
            }
        } else {
            ImageIO.write(input, "png", thumbnailFile);
        }
    }
}
