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
        AndroidManager.getElementByXpath(Constants.book_view_xpath).click();
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


}

