package uk.ac.ebi.biostudies.test.integration;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.support.events.EventFiringWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import uk.ac.ebi.biostudies.BSInterfaceTestApplication;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertTrue;

@Category(IntegrationTest.class)
public class SearchTest {

    protected static WebDriver driver;
    protected static String baseUrl;

    @BeforeClass
    public static void setUpBeforeClass() {
        driver = new HtmlUnitDriver();
        ((HtmlUnitDriver)driver).setJavascriptEnabled(true);
        baseUrl = new BSInterfaceTestApplication().getPreferences().getString("bs.test.integration.server.url");
    }

    @Before
    public void setUp() {
        driver.get(baseUrl+"/studies/");
    }

    @Test
    public void testPageStats() {
        driver.get(baseUrl + "/studies/search.html?query=cancer");
        String pages = driver.findElement(By.cssSelector(".ae-stats")).getText();
        assertTrue(pages.startsWith("Showing 1"));
    }

    @Test
    public void testAutoComplete() throws Exception{
        driver.get(baseUrl + "/studies/search.html?query=cancer");
        driver.findElement(By.id("local-searchbox")).clear();
        driver.findElement(By.id("local-searchbox")).sendKeys("dna");
        WebDriverWait wait = new WebDriverWait(driver, 3);
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".ac_inner")));
        //checking only the first suggestion with more than two words
        assertEquals("DNA assay", driver.findElements(By.cssSelector(".ac_inner li")).get(3).getText());

    }


}
