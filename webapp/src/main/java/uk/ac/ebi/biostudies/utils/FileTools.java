package uk.ac.ebi.biostudies.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

/**
 * Created by awais on 07/08/2015.
 */
public class FileTools {
    // logging machinery
    private static final Logger logger = LoggerFactory.getLogger(FileTools.class);

    private FileTools() {
    }

    public static String readXMLStringFromFile(File xmlFile) throws IOException {
        logger.info("Getting XML from file [{}]", xmlFile);
        String xml = com.google.common.io.Files.toString(
                xmlFile
                , Charset.forName("UTF-8")
        );
        xml = xml.replaceAll("&amp;#(\\d+);", "&#$1;");
        xml = StringTools.unescapeXMLDecimalEntities(xml);
        xml = StringTools.detectDecodeUTF8Sequences(xml);
        xml = StringTools.replaceIllegalHTMLCharacters(xml);
        return xml;
    }
}
