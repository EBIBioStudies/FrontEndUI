package uk.ac.ebi.biostudies.test.integration;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import uk.ac.ebi.biostudies.BSInterfaceTestApplication;

import java.util.List;
import java.util.Scanner;

import static junit.framework.TestCase.assertEquals;

@Category(IntegrationTest.class)
public class DetailTest {

    protected static WebDriver driver;
    protected static String baseUrl;

    @BeforeClass
    public static void setUpBeforeClass() {
        driver = new HtmlUnitDriver( BrowserVersion.FIREFOX_38 , true);
        baseUrl = new BSInterfaceTestApplication().getPreferences().getString("bs.test.integration.server.url");
    }

    @Before
    public void setUp() {
    }

    @Test
    public void testFileCount() {
        // store file and link count on the search page
        driver.get(baseUrl + "/studies/search.html?sortby=files&sortorder=descending");
        String fileCountText = driver.findElement(By.cssSelector(".browse-study-release-files")).getText();
        int fileCount = Integer.parseInt(fileCountText.substring(0, fileCountText.indexOf(" ")));
        driver.findElements(By.cssSelector(".browse-study-title a")).get(0).click();
        WebDriverWait wait = new WebDriverWait(driver, 5);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#file-list_info")));
        String filesCountOnDetails = driver.findElement(By.cssSelector("#file-list_info")).getText();
        assertEquals( "Showing 1 to "+(fileCount<5?fileCount:5)+" of "+(fileCount)+" entries", filesCountOnDetails.replaceAll(",",""));
    }


    @Test
    public void testLinkCount() {
        // store file and link count on the search page
        driver.get(baseUrl + "/studies/search.html?sortby=links&sortorder=descending");
        String linkCountText = driver.findElement(By.cssSelector(".browse-study-release-links")).getText();
        int expectedLinkCount = Integer.parseInt(linkCountText.substring(0, linkCountText.indexOf(" ")));
        driver.findElements(By.cssSelector(".browse-study-title a")).get(0).click();
        WebDriverWait wait = new WebDriverWait(driver, 5);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".dataTables_info")));
        List<WebElement> elements = driver.findElements(By.cssSelector(".dataTables_info"));
        int actualLinkCount =0;

        for(WebElement we: elements) {
            if(we.getAttribute("id").startsWith("link-list")) {
                Scanner scanner = new Scanner(we.getText());
                int links = 0;
                while (scanner.hasNext()) {
                    if(scanner.hasNextInt()) {
                        links = scanner.nextInt();
                    } else {
                        scanner.next();
                    }
                }
                actualLinkCount+= links;
            }
        }
        assertEquals(expectedLinkCount, actualLinkCount);
    }

    @Test
    public void testTitle() {
        driver.get(baseUrl + "/studies/search.html?query=cancer");
        WebElement secondLink = driver.findElements(By.cssSelector(".browse-study-title a")).get(1);
        String expectedTitle = secondLink.getText();
        secondLink.click();
        WebDriverWait wait = new WebDriverWait(driver, 5);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#ae-detail-title")));
        assertEquals(expectedTitle, driver.findElement(By.cssSelector("#ae-detail-title")).getText());
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

    @Test
    public void testDownloadSelection() {
        driver.get(baseUrl + "/studies/search.html?sortby=files&sortorder=descending");
        String fileCountText = driver.findElements(By.cssSelector(".browse-study-release-files")).get(1).getText();
        int fileCount = Integer.parseInt(fileCountText.substring(0, fileCountText.indexOf(" ")));
        List<WebElement> studies = driver.findElements(By.cssSelector(".browse-study-title a"));
        studies.get(1).click();
        WebDriverWait wait = new WebDriverWait(driver, 5);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#select-all-files")));
        driver.findElement(By.cssSelector("#select-all-files")).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#download-selected-files")));
        assertEquals("Download all "+fileCount, driver.findElement(By.cssSelector("#download-selected-files")).getText());
    }

}
