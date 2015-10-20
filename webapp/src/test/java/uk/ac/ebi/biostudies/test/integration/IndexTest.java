package uk.ac.ebi.biostudies.test.integration;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import uk.ac.ebi.biostudies.BSInterfaceTestApplication;
import uk.ac.ebi.biostudies.utils.TestUtils;

import java.io.File;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertTrue;

@Category(IntegrationTest.class)
public class IndexTest {

    protected static WebDriver driver;
    protected static String baseUrl;


    @BeforeClass
    public static void setUpBeforeClass() throws Exception{
        driver = new HtmlUnitDriver();
        baseUrl = new BSInterfaceTestApplication().getPreferences().getString("bs.test.integration.server.url");
    }

    @Before
    public void setUp(){
        driver.get(baseUrl);
    }

    @Test
    public void testIndex() {
        assertTrue(driver.getTitle().equals("BioStudies < EMBL-EBI"));
    }

    @Test
    public void testBrowseLink() {
        driver.findElement(By.linkText("Browse BioStudies")).click();
        assertTrue(driver.getTitle().equals("Studies < BioStudies < EMBL-EBI"));
    }


    @Test
    public void testAddAndUpdateIndex() throws Exception{
        driver.get(baseUrl + "/admin/clear-index");

        // add a document
        StringBuffer sb = new StringBuffer("<pmdocument><submissions>");
        sb.append(TestUtils.getTestSubmission(0));
        sb.append("</submissions></pmdocument>");
        String sourceLocation = BSInterfaceTestApplication.getInstance().getPreferences().getString("bs.studies.source-location");
        File file = new File(sourceLocation, "temp-test-study.xml");
        FileUtils.writeStringToFile(file, sb.toString());
        driver.get(baseUrl + "/admin/reload-xml/temp-test-study.xml");
        file.delete();

        driver.get(baseUrl + "/");
        assertEquals("1 study", driver.findElement(By.cssSelector("#content > aside > ul > li > a")).getText());

        file = new File(sourceLocation, "temp-test-study.xml");
        FileUtils.writeStringToFile(file, StringUtils.replaceOnce(sb.toString(), "<value>Test Document 0</value>", "<value>Updated Test Document 0</value>"));
        driver.get(baseUrl + "/admin/reload-xml/temp-test-study.xml");
        file.delete();
        driver.get(baseUrl + "/studies/TEST-0");
        assertEquals("Updated Test Document 0",driver.findElement(By.cssSelector("#ae-detail-title")).getText());
        driver.get(baseUrl + "/admin/clear-index");
        driver.get(baseUrl + "/admin/reload-xml/test.xml");
    }


    @Test
    public void testDeleteDocument() throws Exception{
        driver.get(baseUrl + "/admin/clear-index");

        // add a document
        StringBuffer sb = new StringBuffer("<pmdocument><submissions>");
        sb.append(TestUtils.getTestSubmission(0));
        sb.append("</submissions></pmdocument>");
        String sourceLocation = BSInterfaceTestApplication.getInstance().getPreferences().getString("bs.studies.source-location");
        File file = new File(sourceLocation, "temp-test-study.xml");
        FileUtils.writeStringToFile(file, sb.toString());
        driver.get(baseUrl + "/reload-xml/temp-test-study.xml");
        file.delete();

        driver.get(baseUrl + "/admin/delete/TEST-0");
        driver.get(baseUrl + "/admin/studies");
        assertEquals("We’re sorry that we couldn’t find any matching studies", driver.findElement(By.cssSelector("h2.alert")).getText());

        driver.get(baseUrl + "/admin/clear-index");
        driver.get(baseUrl + "/admin/reload-xml/test.xml");
    }

}
