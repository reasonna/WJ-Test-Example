package utils;

import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.List;

public class AndroidManager {

    public static AndroidDriver driver;
    private static WebDriverWait wait;

    private AndroidManager(){}

    public static AndroidDriver getDriver() {
        if(driver == null){
            DesiredCapabilities caps = new DesiredCapabilities();
            try {
                URL url = new URL("http://127.0.0.1:4723/wd/hub");
                caps.setCapability("deviceName", "M2 Pad");
                caps.setCapability("udid", "WJD11AFN02513"); //DeviceId from "adb devices" command
                caps.setCapability("platformName", "Android");
                caps.setCapability("platformVersion", "10");
                driver = new AndroidDriver(url, caps);
                // 로그 레벨 설정
//                driver.manage().logs().getAvailableLogTypes();
                // 앱 로그 확인
//                System.out.println(driver.manage().logs().get("logcat").getAll());

            }catch(MalformedURLException e){
                throw new RuntimeException(e);
            }
        }
        return driver;
    }

    public static WebDriverWait getWait(){
        return getWait(10);
    }

    public static WebDriverWait getWait(int duration) {
        if(wait ==null){
            if(driver == null){
                driver = getDriver();
            }
            wait = new WebDriverWait(driver, Duration.ofSeconds(duration));
        }
        return wait;
    }

    public static WebElement getElementById(String id){
       return getWait().until(ExpectedConditions.visibilityOfElementLocated(By.id(id)));
    }

    public static WebElement getElementByXpath(String xPath){
        return getWait().until(ExpectedConditions.visibilityOfElementLocated(By.xpath(xPath)));
    }

    public static List<WebElement> getAllElementById(String id){
        return  getWait().until(ExpectedConditions.visibilityOfAllElementsLocatedBy(By.id(id)));
    }

    public static List<WebElement> getAllElementByXpath(String xPath){
        return  getWait().until(ExpectedConditions.visibilityOfAllElementsLocatedBy(By.xpath(xPath)));
    }


}