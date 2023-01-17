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

    @After  // 시나리오 끝나고 실행, step은 모두 시나리오 안에서 실행중
    public void after(Scenario scenario){
        // 시나리오가 실패할때만 스크린샷 필요
        if(scenario.isFailed()) {
            // 구글: appium screenshot 검색, Driver >> AndroidManager.getDriver로 바꿔서 넣rh BASE64로 받기
            String file = ((TakesScreenshot)AndroidManager.getDriver()).getScreenshotAs(OutputType.BASE64);
            String filename = scenario.getName().trim().replaceAll(" ", "_");   // 공백을 언더바로 바꾸기
            byte[] decodedBase64 = Base64.decodeBase64(file);   // Base64 디코더

            String scrDir = "defect_screenshots";
            new File(scrDir).mkdirs();  // 폴더만들기
            Path path = Paths.get("").toAbsolutePath(); 
            String currentPath = path.toString();
            System.out.println("currentPath: " + currentPath);  // 확인

            String dest =  filename + ".png";   // .png파일로 만들기
            try{
                OutputStream stream = new FileOutputStream(scrDir + "/" + dest);
                stream.write(decodedBase64);
                System.out.println("screenshot name = " + dest);
                stream.close();     // stream 닫기
            } catch (IOException e){
                e.printStackTrace();
            }

        }
    }

}
