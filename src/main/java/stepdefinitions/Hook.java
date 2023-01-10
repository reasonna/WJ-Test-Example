package stepdefinitions;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.nativekey.AndroidKey;
import io.appium.java_client.android.nativekey.KeyEvent;
import io.cucumber.java.Before;
import utils.AndroidManager;


public class Hook {

    @Before // 시나리오
    public void doHomeButtonBefore(){
        AndroidManager.getDriver().pressKey(new KeyEvent(AndroidKey.HOME));
    }
}
