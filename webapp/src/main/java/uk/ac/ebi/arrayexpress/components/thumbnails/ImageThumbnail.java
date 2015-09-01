package uk.ac.ebi.arrayexpress.components.thumbnails;

import javax.imageio.ImageIO;
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
        net.coobird.thumbnailator.Thumbnails.of(sourceFilePath)
                .size(200, 200)
                .outputFormat("png")
                .toFile(thumbnailFile);
    }
}
