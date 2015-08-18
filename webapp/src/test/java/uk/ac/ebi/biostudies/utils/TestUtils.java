package uk.ac.ebi.biostudies.utils;

import org.apache.commons.lang3.RandomStringUtils;

/**
 * Created by awais on 18/08/2015.
 */
public class TestUtils {

    public static String getTestSubmission(int doc) {
        StringBuffer sb = new StringBuffer();
        String docId = ""+doc;
        sb.append(String.format("<submission acc=\"TEST-%s\" id=\"%s\" access=\"Public\">", docId, docId));
        sb.append(String.format("<section id=\"%s\" type=\"Study\">", docId));
        sb.append(String.format("<attributes><attribute><name>Title</name><value>Test Document %s</value></attribute>", docId));
        sb.append(String.format("<attribute><name>Description</name><value>Description for Test Document %s</value></attribute>", docId));
        for (int i = 0; i < 10; i++) {
            sb.append(
                    String.format("<attribute><name>Attribute %s</name><value>%s</value></attribute>",
                            RandomStringUtils.randomAlphabetic(10), RandomStringUtils.randomAlphabetic(15)
                    )
            );
        }
        sb.append(String.format("</attributes><links>", docId));
        for (int i = 0; i < 10; i++) {
            sb.append(
                    String.format("<link><url>http://%s</url><attributes><attribute><name>Description</name><value>Link %s</value></attribute></attributes></link>",
                            RandomStringUtils.randomAlphabetic(10), RandomStringUtils.randomAlphabetic(15)
                    )
            );
        }
        sb.append(String.format("</links></section></submission>", docId));
        return sb.toString();
    }

}
