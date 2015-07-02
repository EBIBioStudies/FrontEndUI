package uk.ac.ebi.biostudies.test.integration;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import uk.ac.ebi.biostudies.BSInterfaceTestApplication;

import static org.junit.Assert.assertTrue;

@Category(IntegrationTest.class)
public class BrowseTest {

    protected static WebDriver driver;
    protected static String serverURL;

    @BeforeClass
    public static void setUpBeforeClass() {
        driver = new HtmlUnitDriver();
        serverURL = new BSInterfaceTestApplication().getPreferences().getString("bs.test.integration.server.url");
    }

    @Before
    public void setUp() {
        driver.get(serverURL+"/studies/"); // TODO: Shift the url to configuration
    }

    @Test
    public void testIndex() {
        assertTrue(driver.getTitle().equals("Studies < BioStudies < EMBL-EBI"));
    }

}
