package uk.ac.ebi.biostudies.test.integration;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import uk.ac.ebi.biostudies.BSInterfaceTestApplication;

import static junit.framework.TestCase.assertEquals;

@Category(IntegrationTest.class)
public class DetailTest {

    protected static WebDriver driver;
    protected static String baseUrl;

    @BeforeClass
    public static void setUpBeforeClass() {
        driver = new HtmlUnitDriver();
        ((HtmlUnitDriver) driver).setJavascriptEnabled(true);
        baseUrl = new BSInterfaceTestApplication().getPreferences().getString("bs.test.integration.server.url");
    }

    @Before
    public void setUp() {
    }

    @Test
    public void testFileCount() {
        // store file and link count on the search page
        driver.get(baseUrl + "/studies/search.html?query=S-EPMC3315455");
        String fileCountText = driver.findElement(By.cssSelector(".browse-study-release-files")).getText();
        int fileCount = Integer.parseInt(fileCountText.substring(0, fileCountText.indexOf(" ")));
        driver.findElement(By.cssSelector(".browse-study-title a")).click();
        WebDriverWait wait = new WebDriverWait(driver, 5);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#file-list_info")));
        String filesCountOnDetails = driver.findElement(By.cssSelector("#file-list_info")).getText();
        assertEquals(filesCountOnDetails, "Showing 1 to 5 of "+fileCount+" entries");
    }


    @Test
    public void testLinkCount() {
        // store file and link count on the search page
        driver.get(baseUrl + "/studies/search.html?query=S-EPMC2685405");
        String linkCountText = driver.findElement(By.cssSelector(".browse-study-release-links")).getText();
        int linkCount = Integer.parseInt(linkCountText.substring(0, linkCountText.indexOf(" ")));
        driver.findElement(By.cssSelector(".browse-study-title a")).click();
        WebDriverWait wait = new WebDriverWait(driver, 5);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".links")));
        int linkCountOnDetails = driver.findElements(By.cssSelector(".links a")).size() - 2; // removing shoe more/less links
        assertEquals(linkCountOnDetails, linkCount);
    }

    /* Not working with HtmlUnitDriver
    @Test
    public void testFileFilter() {
        driver.get(baseUrl + "/studies/");
        new Select(driver.findElement(By.id("studies-browse-sorter"))).selectByVisibleText("Files");
        driver.findElement(By.cssSelector(".browse-study-title a")).click();
        WebDriverWait wait = new WebDriverWait(driver, 5);
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("#file-list_filter input")));
        driver.findElement(By.cssSelector("#file-list_filter input")).click();
        driver.findElement(By.cssSelector("#file-list_filter input")).sendKeys("pdf");
        assertEquals("Showing 1 to 4 of 4 entries (filtered from 35 total entries)",
                driver.findElement(By.cssSelector("#file-list_info")).getText());
    }
    */

}
