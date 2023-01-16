package stepdefinitions;

import io.appium.java_client.android.nativekey.AndroidKey;
import io.appium.java_client.android.nativekey.KeyEvent;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import utils.AndroidManager;
import org.apache.commons.codec.binary.Base64;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;


public class Hook {

    @Before // 시나리오 시작하기 전에 실행 하는 
    public void doHomeButtonBefore(){
        // 홈으로 돌아간 다음 시작
        AndroidManager.getDriver().pressKey(new KeyEvent(AndroidKey.HOME)); 
    }

    @After
    public void after(Scenario scenario){
        if(scenario.isFailed()) {
            String file = ((TakesScreenshot)AndroidManager.getDriver()).getScreenshotAs(OutputType.BASE64);
            String filename = scenario.getName().trim().replaceAll(" ", "_");
            byte[] decodedBase64 = Base64.decodeBase64(file);

            String scrDir = "defect_screenshots";
            new File(scrDir).mkdirs();
            Path path = Paths.get("").toAbsolutePath();
            String currentPath = path.toString();
            System.out.println("currentPath: " + currentPath);

            String dest =  filename + ".png";
            try{
                OutputStream stream = new FileOutputStream(scrDir + "/" + dest);
                stream.write(decodedBase64);
                System.out.println("screenshot name = " + dest);
                stream.close();
            } catch (IOException e){
                e.printStackTrace();
            }

        }
    }

}
