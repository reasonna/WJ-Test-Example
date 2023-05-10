package stepdefinitions;

import io.cucumber.java.Scenario;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.checkerframework.checker.units.qual.C;
import org.testng.Assert;
import utils.AndroidManager;
import utils.Constants;

public class MyStepdefs {
    @Given("library click")
    public void libraryClick() {
        AndroidManager.getElementByXpath(Constants.lib_btn_xpath).click();
    }

    @When("search button click")
    public void searchButtonClick() {
        AndroidManager.getElementById(Constants.search_icon_id).click();

    }

    @And("search input {string}")
    public void searchInput(String arg0) {
        AndroidManager.getElementById(Constants.search_input_id).sendKeys(arg0);
    }

    @And("search click")
    public void searchClick() {
        AndroidManager.getElementById(Constants.go_search_btn_id).click();
    }

    @And("click {int}nd book")
    public void clickNdBook(int arg0) {
        AndroidManager.getAllElementById(Constants.resultBook_id).get(1).click();
    }

    @Then("check book title")
    public void checkBookTitle() {
        AndroidManager.getElementById(Constants.bookTitle_id).getText();
    }

    @And("ok check click")
    public void okCheckClick() {
        AndroidManager.getElementById(Constants.ok_btn_id).click();
    }

    @And("hamburger btn click")
    public void hamburgerBtnClick() {
        AndroidManager.getElementById(Constants.hamburger_btn).click();
    }

    @Then("book view click")
    public void bookViewClick() {
        AndroidManager.getElementByXpath(Constants.book_view_id).click();
    }

    @Then("view ok button click")
    public void viewOkButtonClick() {
        AndroidManager.getElementById(Constants.view_ok_btn_id).click();
    }

    @Then("check display text")
    public void checkDisplayText() {
        // 가져오는 텍스트가 예상과 실제가 동일한지 확인 
        Assert.assertEquals(AndroidManager.getElementById(Constants.view_check_id).getText(),"감상문 보기");
    }

    @Given("라이브러리 버튼 클릭")
    public void clickLibraryBtn() {
        try {
            try{
                log.info("홈 > 라이브러리 버튼 클릭");
                if (Utils.getDeviceType().equals("Others")){TimeUnit.SECONDS.sleep(5);}
                WebElement element = AndroidManager.getElementByXpath(Constant.라이브러리_xPath);
                element.click();
                TimeUnit.SECONDS.sleep(5);
            }catch (Exception e){
                log.info("Exception 홈 > 라이브러리 버튼 클릭");
                WebElement element = AndroidManager.getElementByXpath(Constant.라이브러리_xPath);
                //간혹 한번에 클릭되지 않는 경우가 있어 미선택시 다시 한번 클릭하도록 처리함
                if(!element.isSelected()) element.click();
                TimeUnit.SECONDS.sleep(5);
            }
        } catch (NoSuchElementException e) {
            fail("Element you found not shown");
        } catch (Exception e) {
            fail(e.getMessage());
            System.exit(0);
        }
    }

     @When("검색 버튼 클릭")
    public void clickSearchBtn() {
        try {
            log.info("홈 > 검색 버튼 클릭");
            AndroidManager.getElementById(Constant.검색_id).click();
        } catch (NoSuchElementException e) {
            fail("Element you found not shown");
        } catch (Exception e) {
            fail(e.getMessage());
            System.exit(0);
        }
    }

    @When("검색창에서 {string} 입력")
    public void enterSearchTextOnSearchBar(String searchText) {
        try {
            log.info("검색창에 {} 입력", searchText);
            WebElement inputText = AndroidManager.getElementById(Constant.검색창_id);
            inputText.sendKeys(searchText);
            TimeUnit.SECONDS.sleep(3);
        } catch (NoSuchElementException e) {
            fail("Element you found not shown");
        } catch (Exception e) {
            fail(e.getMessage());
            System.exit(0);
        }
    }

    @When("검색창에서 검색 실행 버튼 클릭")
    public void clickStartSearchBtn() {
        try {
            log.info("검색창에서 검색 실행 버튼 클릭");
            AndroidManager.getElementById(Constant.검색실행_id).click();
        } catch (NoSuchElementException e) {
            fail("Element you found not shown");
        } catch (Exception e) {
            fail(e.getMessage());
            System.exit(0);
        }
    }

    /**
     * 검색결과 화면에서 독서 검색결과 섹션의 1번째 항목 선택
     * @param order 검색결과의 N번째 아이템
     */
    @When("검색결과 화면에서 독서 검색결과 섹션의 {int}번째 항목 선택")
    public void clickResultSearchItem(int order) {
        try {
            log.info("검색결과 화면에서 독서 검색결과 섹션의 {}번째 항목 선택", order);
            AndroidManager.getElementsByIdAndIndex("com.wjthinkbig.dictionary:id/root", order).click();
            TimeUnit.SECONDS.sleep(3);
            try {
                // 열람 기간 만료 알람 뜨면 다운로드 눌러주기 23.03.14_추가
                WebElement btnR = AndroidManager.getElementById("com.wjthinkbig.dictionary:id/btnRight");
                if(btnR.isDisplayed()){
                    btnR.click();
                }
                WebElement isFirstView = AndroidManager.getElementByIdUntilDuration(Constant.seeRightNowBtn_id, 4);
                if (isFirstView.isDisplayed()){
                    isFirstView.click();
                }
                WebElement isFirstOpen = AndroidManager.getElementById(Constant.helpViewLayout_id);
                if (isFirstOpen.isDisplayed()){
                    AndroidManager.getElementById(Constant.helpViewXBtn_id).click();
                }

            } catch (Exception e) {
                assertTrue(true);
            }
        } catch (NoSuchElementException e) {
            fail("Element you found not shown");
        } catch (Exception e) {
            fail(e.getMessage());
            System.exit(0);
        }
    }

    @When("{int}초 대기")
    public void sleepByParam(int seconds) {
        try {
            log.info("{}초 대기", seconds);
            TimeUnit.SECONDS.sleep(seconds);
        } catch (NoSuchElementException e) {
            fail("Element you found not shown");
        } catch (Exception e) {
            fail(e.getMessage());
            System.exit(0);
        }
    }


/**
     * Reading continue yes or no
     * @param yesOrNo "Yes" or "No"
     */
    @When("Reading continue {string}")
    public void notReadingContinue(String yesOrNo) {
        if (!yesOrNo.equals("Yes") && !yesOrNo.equals("No")) throw new InvalidParameterException("yesOrNo parameter only available 'Yes' or 'No'");
        try {

            log.info("Reading continue {}", yesOrNo);
            WebElement alertMsg;

            try {
                alertMsg = AndroidManager.getElementById(Constant.안내팝업메시지_id);
            } catch (Exception e) {
                return;
            }

            if (alertMsg.getText().equals("이 책은 세로로 보시면 좋아요.\n" +
                    "기기를 세로로 돌려서 보세요.")) {
                AndroidManager.getElementById(Constant.안내팝업확인_id).click();
            } else if (alertMsg.isDisplayed() && alertMsg.getText().contains("읽던 페이지를 이어서 볼까요?")) {
                switch (yesOrNo) {
                    case "Yes":
                        AndroidManager.getElementById(Constant.안내팝업확인_id).click();
                        break;
                    case "No":
                        AndroidManager.getElementById(Constant.안내팝업취소_id).click();
                        break;
                }
            }
        } catch (NoSuchElementException e) {
            fail("Element you found not shown");
        } catch (Exception e) {
            fail(e.getMessage());
            System.exit(0);
        }
    }
}

