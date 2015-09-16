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

import static com.thoughtworks.selenium.SeleneseTestNgHelper.assertEquals;
import static org.junit.Assert.assertTrue;

@Category(IntegrationTest.class)
public class BrowseTest {

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
    public void testIndex() {
        assertTrue(driver.getTitle().equals("Studies < BioStudies < EMBL-EBI"));
    }

    @Test
    public void testLoginAndLogout() {
        driver.findElement(By.cssSelector(".login")).click();
        WebDriverWait wait = new WebDriverWait(driver, 5);
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#ae-login")));
        driver.findElement(By.cssSelector("#ae-user-field")).sendKeys("mike");
        driver.findElement(By.cssSelector("#ae-pass-field")).sendKeys("mike");
        driver.findElement(By.cssSelector("#ae-login-form > input[type='submit']")).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".login")));
        assertEquals("Logout [mike]", driver.findElement(By.cssSelector(".login")).getText());
        driver.findElement(By.cssSelector(".login")).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".login")));
        assertEquals("Login", driver.findElement(By.cssSelector(".login")).getText());
    }

}
