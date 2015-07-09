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
    protected static String baseUrl;

    @BeforeClass
    public static void setUpBeforeClass() {
        driver = new HtmlUnitDriver();
        baseUrl = new BSInterfaceTestApplication().getPreferences().getString("bs.test.integration.server.url");
    }

    @Before
    public void setUp() {
        driver.get(baseUrl+"/studies/");
    }

    @Test
    public void testIndex() {
        assertTrue(driver.getTitle().equals("Studies < BioStudies < EMBL-EBI"));
    }

}
