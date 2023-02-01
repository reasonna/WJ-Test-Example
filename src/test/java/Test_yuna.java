import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import utils.AndroidManager;
import utils.Constants;

public class Test_yuna {
    public AppiumDriver driver;
    public WebDriverWait wait;



    @BeforeMethod
    public void setup() throws MalformedURLException {
        DesiredCapabilities caps = new DesiredCapabilities();
        caps.setCapability("deviceName", "x500");
        caps.setCapability("udid", "R9TT502BVQT"); //DeviceId from "adb devices" command
        caps.setCapability("platformName", "Android");
        caps.setCapability("platformVersion", "10");
        driver = new AppiumDriver(new URL("http://127.0.0.1:4723/wd/hub"), caps);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }
//    @Test
//    public void basicTest() throws InterruptedException {
//        //Click and pass Splash
//        wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(Constants.lib_btn_xpath))).click();
//        //Click I am searching a job
//        AndroidManager.getElementById(Constants.search_input_id).click();
//        AndroidManager.getElementById(Constants.search_input_id).sendKeys();
//        AndroidManager.getElementById(Constants.search_icon_id).click();
//        wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(By.id(Constants.resultBook_id))).get(1).click();
//        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id(Constants.bookTitle_id))).getText();
//        System.out.println(wait.until(ExpectedConditions.visibilityOfElementLocated(By.id(Constants.bookTitle_id))).getText());
//        Assert.assertEquals(wait.until(ExpectedConditions.visibilityOfElementLocated(By.id(Constants.bookTitle_id))).getText(),
//                "","실제 책 이름: "
//                        + wait.until(ExpectedConditions.visibilityOfElementLocated(By.id(Constants.bookTitle_id))).getText()+" 이 예상과 다름");
////        System.out.println("실제 책 이름" + wait.until(ExpectedConditions.visibilityOfElementLocated(By.id(bookTitle))).getText()+"이 예상"+A+"과 다름");
//
//
//    }


}
