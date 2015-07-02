package uk.ac.ebi.biostudies.test.integration;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import uk.ac.ebi.biostudies.BSInterfaceTestApplication;

import static org.junit.Assert.assertTrue;

@Category(IntegrationTest.class)
public class IndexTest {

    protected static WebDriver driver;
    protected static String serverURL;


    @BeforeClass
    public static void setUpBeforeClass() throws Exception{
        driver = new HtmlUnitDriver();
        serverURL = new BSInterfaceTestApplication().getPreferences().getString("bs.test.integration.server.url");
    }

    @Before
    public void setUp(){
        driver.get(serverURL);
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
}
