package stepdefinitions;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.nativekey.AndroidKey;
import io.appium.java_client.android.nativekey.KeyEvent;
import io.cucumber.java.Before;
import utils.AndroidManager;


public class Hook {

    @Before // 시나리오 시작하기 전에 실행 하는 
    public void doHomeButtonBefore(){
        // 홈으로 돌아간 다음 시작
        AndroidManager.getDriver().pressKey(new KeyEvent(AndroidKey.HOME)); 
    }
}
