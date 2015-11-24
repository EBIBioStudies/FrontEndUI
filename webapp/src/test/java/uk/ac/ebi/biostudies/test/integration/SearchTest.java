package uk.ac.ebi.biostudies.test.integration;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlSearchInput;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import uk.ac.ebi.biostudies.BSInterfaceTestApplication;
import uk.ac.ebi.biostudies.utils.TestUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

@Category(IntegrationTest.class)
public class SearchTest {

    protected static WebDriver driver;
    protected static String baseUrl;

    @BeforeClass
    public static void setUpBeforeClass() {
        driver = new HtmlUnitDriver( BrowserVersion.FIREFOX_38 , true);
        baseUrl = new BSInterfaceTestApplication().getPreferences().getString("bs.test.integration.server.url");
        //driver.get(baseUrl + "/admin/reload-xml");
    }

    @Before
    public void setUp() {
        driver.get(baseUrl + "/studies/");
    }

    @Test
    public void testPageStats() {
        driver.get(baseUrl + "/studies");
        String pages = driver.findElement(By.cssSelector(".ae-stats")).getText();
        assertTrue(pages.startsWith("Showing 1"));
    }

    /* Does not work with <input type="search"...
    @Test
    public void testAutoComplete() throws Exception{
        driver.get(baseUrl + "/studies/");
        WebElement searchBox = driver.findElement (By.id("local-searchbox"));
        searchBox.click();
        searchBox.sendKeys("dna");
        WebDriverWait wait = new WebDriverWait(driver, 5);
        wait.until(ExpectedConditions.visibilityOfElementLocated (By.cssSelector(".ac_inner")));
        List<WebElement> we = driver.findElements(By.cssSelector(".ac_inner li"));
        assertTrue(we.get(3).getText().startsWith("DNA"));

    }*/

    @Test
    public void testAccessionAscendingSort() throws Exception{
        driver.get(baseUrl + "/studies/search.html?query=cancer");
        new Select(driver.findElement(By.id("studies-browse-sorter"))).selectByVisibleText("Accession");
        testSort(".browse-study-accession");
    }

    @Test
    public void testAccessionDescendingSort() throws Exception{
        driver.get(baseUrl + "/studies/search.html?query=cancer");
        new Select(driver.findElement(By.id("studies-browse-sorter"))).selectByVisibleText("Accession");
        driver.findElement(By.cssSelector(".studies-browse-sort-order-left")).click();
        testSort(".browse-study-accession", true);
    }

    private void testSort(String cssSelector) {
        testSort(cssSelector, false);
    }

    private void testSort(String cssSelector, boolean isDescending) {
        List<WebElement> list = driver.findElements(By.cssSelector(cssSelector));
        String [] values = new String[list.size()];
        for(int i=0; i < values.length; i++) {
            values[i] = list.get(i).getText().toLowerCase().trim();
        }
        String [] unsortedValues = values.clone();
        if (isDescending) {
            Arrays.sort(values, Collections.reverseOrder());
        } else {
            Arrays.sort(values);
        }
        assertArrayEquals(values, unsortedValues);
    }

    @Test
    public void testTitleAscendingSort() throws Exception{
        driver.get(baseUrl + "/studies/search.html?query=cancer");
        new Select(driver.findElement(By.id("studies-browse-sorter"))).selectByVisibleText("Title");
        testSort(".browse-study-title a");
    }

    @Test
    public void testTitleDescendingSort() throws Exception{
        driver.get(baseUrl + "/studies/search.html?query=cancer");
        new Select(driver.findElement(By.id("studies-browse-sorter"))).selectByVisibleText("Title");
        driver.findElement(By.cssSelector(".studies-browse-sort-order-left")).click();
        testSort(".browse-study-title a", true);
    }

    @Test
    public void testAuthorsAscendingSort() throws Exception{
        driver.get(baseUrl + "/studies/search.html?query=cancer");
        new Select(driver.findElement(By.id("studies-browse-sorter"))).selectByVisibleText("Authors");
        testSort(".browse-study-title + div.search-authors");
    }

    @Test
    public void testAuthorsDescendingSort() throws Exception{
        driver.get(baseUrl + "/studies/search.html?query=cancer");
        new Select(driver.findElement(By.id("studies-browse-sorter"))).selectByVisibleText("Authors");
        driver.findElement(By.cssSelector(".studies-browse-sort-order-left")).click();
        testSort(".browse-study-title + div.search-authors", true);
    }


    @Test
    public void testFilesDescendingSort() throws Exception{
        driver.get(baseUrl + "/studies/search.html?query=cancer");
        new Select(driver.findElement(By.id("studies-browse-sorter"))).selectByVisibleText("Files");
        List<WebElement> list = driver.findElements(By.cssSelector(".browse-study-release-files"));
        Integer [] values = new Integer[list.size()];
        for(int i=0; i < values.length; i++) {
            values[i] = Integer.parseInt(list.get(i).getText().toLowerCase().trim().replaceAll(" data files",""));
        }
        Integer [] unsortedValues = values.clone();
        Arrays.sort(values, Collections.reverseOrder());
        assertArrayEquals(values, unsortedValues);
    }

    @Test
    public void testFilesAscendingSort() throws Exception{
        driver.get(baseUrl + "/studies/search.html?query=cancer");
        new Select(driver.findElement(By.id("studies-browse-sorter"))).selectByVisibleText("Files");
        driver.findElement(By.cssSelector(".studies-browse-sort-order-right")).click();
        List<WebElement> list = driver.findElements(By.cssSelector(".browse-study-release-files"));
        Integer [] values = new Integer[list.size()];
        for(int i=0; i < values.length; i++) {
            values[i] = Integer.parseInt(list.get(i).getText().toLowerCase().trim().replaceAll(" data files",""));
        }
        Integer [] unsortedValues = values.clone();
        assertArrayEquals(values, unsortedValues);
    }

    @Test
    public void testLinksDescendingSort() throws Exception{
        driver.get(baseUrl + "/studies/search.html?query=cancer");
        new Select(driver.findElement(By.id("studies-browse-sorter"))).selectByVisibleText("Links");
        List<WebElement> list = driver.findElements(By.cssSelector(".browse-study-release-links"));
        Integer [] values = new Integer[list.size()];
        for(int i=0; i < values.length; i++) {
            values[i] = Integer.parseInt(list.get(i).getText().toLowerCase().trim().replaceAll(" links",""));
        }
        Integer [] unsortedValues = values.clone();
        Arrays.sort(values, Collections.reverseOrder());
        assertArrayEquals(values, unsortedValues);
    }

    @Test
    public void testLinksAscendingSort() throws Exception{
        driver.get(baseUrl + "/studies/search.html?query=cancer");
        new Select(driver.findElement(By.id("studies-browse-sorter"))).selectByVisibleText("Links");
        driver.findElement(By.cssSelector(".studies-browse-sort-order-right")).click();
        List<WebElement> list = driver.findElements(By.cssSelector(".browse-study-release-links"));
        Integer [] values = new Integer[list.size()];
        for(int i=0; i < values.length; i++) {
            values[i] = Integer.parseInt(list.get(i).getText().toLowerCase().trim().replaceAll(" links", ""));
        }
        Integer [] unsortedValues = values.clone();
        assertArrayEquals(values, unsortedValues);
    }

    @Test
    public void testReleasedDescendingSort() throws Exception{
        driver.get(baseUrl + "/studies/search.html?query=cancer");
        new Select(driver.findElement(By.id("studies-browse-sorter"))).selectByVisibleText("Released");
        List<WebElement> list = driver.findElements(By.cssSelector(".browse-study-release-date"));
        Date [] values = new Date[list.size()];
        SimpleDateFormat formatter = new SimpleDateFormat("d MMM yyyy");
        for(int i=0; i < values.length; i++) {
            values[i] = formatter.parse(list.get(i).getText().trim());
        }
        Date [] unsortedValues = values.clone();
        Arrays.sort(values, Collections.reverseOrder());
        assertArrayEquals(values, unsortedValues);
    }


    @Test
    public void testReleasedAscendingSort() throws Exception{
        driver.get(baseUrl + "/studies/search.html?query=cancer");
        new Select(driver.findElement(By.id("studies-browse-sorter"))).selectByVisibleText("Released");
        List<WebElement> list = driver.findElements(By.cssSelector(".browse-study-release-date"));
        Date [] values = new Date[list.size()];
        SimpleDateFormat formatter = new SimpleDateFormat("d MMM yyyy");
        for(int i=0; i < values.length; i++) {
            values[i] = formatter.parse(list.get(i).getText().trim());
        }
        Date [] unsortedValues = values.clone();
        assertArrayEquals(values, unsortedValues);
    }


    @Test
    public void testPaging() throws Exception{
        driver.get(baseUrl + "/studies");
        driver.findElement(By.linkText("2")).click();
        String pages = driver.findElement(By.cssSelector(".ae-stats")).getText();
        assertTrue(pages.startsWith("Showing 26"));
        String accession  = driver.findElement(By.cssSelector(".browse-study-accession")).getText();
        driver.findElement(By.cssSelector(".browse-study-title a")).click();
        WebDriverWait wait = new WebDriverWait(driver, 5);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".accessionNumber")));
        assertEquals(accession, driver.findElement(By.cssSelector(".accessionNumber")).getText());
    }

    @Test
    public void testClearIndex() throws Exception {
        driver.get(baseUrl + "/admin/clear-index");
        driver.get(baseUrl + "/studies/");
        WebDriverWait wait = new WebDriverWait(driver, 5);
        wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("h2.alert")));
        driver.get(baseUrl + "/admin/reload-xml/");
        assertTrue(true);
    }

    @Test
    public void testLargeIndex() throws Exception{
        int totalDocs = 100;
        driver.get(baseUrl + "/admin/clear-index");
        StringBuffer sb = new StringBuffer("<pmdocument><submissions>");
        for (int doc = 1; doc <= totalDocs; doc++) {
            sb.append(TestUtils.getTestSubmission(doc));
        }
        sb.append("</submissions></pmdocument>");
        String sourceLocation = BSInterfaceTestApplication.getInstance().getPreferences().getString("bs.studies.source-location");
        File file = new File(sourceLocation, "temp-test-study.xml");
        FileUtils.writeStringToFile(file, sb.toString());
        driver.get(baseUrl + "/admin/reload-xml/temp-test-study.xml");
        file.delete();
        driver.get(baseUrl + "/studies/");
        String pages = driver.findElement(By.cssSelector(".ae-stats")).getText();
        assertTrue(pages.endsWith("Showing 1 - 25 of " + (totalDocs) +" results"));
        driver.get(baseUrl + "/admin/clear-index");
        driver.get(baseUrl + "/admin/reload-xml");
    }

}
