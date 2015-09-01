package uk.ac.ebi.arrayexpress.components.thumbnails;

import java.io.File;
import java.io.IOException;

/**
 * Created by awais on 01/09/2015.
 */
public interface IThumbnail {
    void generateThumbnail(String sourceFilePath, File thumbnailFile) throws IOException;
    String[] getSupportedTypes();
}
