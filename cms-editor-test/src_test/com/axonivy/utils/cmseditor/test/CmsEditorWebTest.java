package com.axonivy.utils.cmseditor.test;

import static com.codeborne.selenide.CollectionCondition.allMatch;
import static com.codeborne.selenide.CollectionCondition.sizeGreaterThanOrEqual;
import static com.codeborne.selenide.Condition.enabled;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selenide.open;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import com.axonivy.ivy.webtest.IvyWebTest;
import com.axonivy.ivy.webtest.engine.EngineUrl;
import com.codeborne.selenide.Selenide;

@IvyWebTest(headless = true)
public class CmsEditorWebTest {

  private String testCmsUri = "/TestContent";
  private String testCmsValue = "Test Content";

  @BeforeEach
  void startProcess() {
    open(EngineUrl.createProcessUrl("/cms-editor/18DE86A37D77D574/start.ivp"));
  }

  @Test
  public void testDownloadAndCancelButtonShouldBeVisible() {
    $(By.id("content-form:downloadButton")).shouldBe(visible);
    $(By.id("content-form:cancel-button")).shouldBe(visible);
  }

  @Test
  public void testFilterByCmsUriShouldDisplayTwoRows() {
    sendKeysToSearchInput(testCmsUri);
    assertCmsTableRowCountGte(2);
  }

  @Test
  public void testFilterByCmsValueShouldDisplayTwoRows() {
    sendKeysToSearchInput(testCmsValue);
    assertCmsTableRowCountGte(2);
  }

  private void assertCmsTableRowCountGte(int size) {
    $$("[id^='content-form:table-cms-keys:'][id$=':cms-uri']").shouldHave(sizeGreaterThanOrEqual(size));
  }

  private void sendKeysToSearchInput(String keysToSend) {
    $(By.id("content-form:search-input")).sendKeys(keysToSend);
  }

  private void clickOptionShowOnlyTodo() {
    $(By.id("content-form:option-button_button")).click();
    $(By.id("content-form:show-todo-checkbox")).$(".ui-chkbox-box").click();
  }

  @Test
  public void testCheckShowTodoOptionShouldDisplayTwoRows() {
    clickOptionShowOnlyTodo();
    assertCmsTableRowCountGte(1);
  }

  @Test
  public void testEditedButNotSaveShouldShowError() {
    var cmsList = $$("[id^='content-form:table-cms-keys:'][id$=':cms-uri']");
    var selectedCms = cmsList.get(0);
    var otherCms = cmsList.get(1);
    selectedCms.click();

    $$("[id^='content-form:cms-values:'][id$=':cms-values-tab']").shouldHave(sizeGreaterThanOrEqual(1));
    // assert all content items is preview mode
    var displayItems = $$("[id^='content-form:cms-values:'][id$='_display']");
    displayItems.shouldBe(allMatch("All elements should be visible", element -> element.isDisplayed()));
    var displayItem = displayItems.first();
    // click one content
    displayItem.click();
    var contentItem = $(By.id(displayItem.getAttribute("id").replaceAll("_display", "_content")));
    displayItem.shouldBe(hidden);
    contentItem.shouldBe(visible);

    contentItem.$(By.className("sun-editor-editable")).setValue("Content is updated at " + System.currentTimeMillis());
    contentItem.$(".se-btn.se-resizing-enabled.se-tooltip").should(enabled);
    Selenide.sleep(1000);
    otherCms.click();

    // assert check save before change to other item
    var errorDialog = $(By.id("primefacesmessagedlg"));
    errorDialog.should(visible);
    errorDialog.$(".ui-dialog-titlebar-close").click();
    Selenide.sleep(1000);

    // assert check save before search items
    sendKeysToSearchInput("Lorem ifsum");
    errorDialog.should(visible);
    errorDialog.$(".ui-dialog-titlebar-close").click();
    Selenide.sleep(1000);

    // assert check save before change option
    clickOptionShowOnlyTodo();
    errorDialog.should(visible);
    errorDialog.$(".ui-dialog-titlebar-close").click();
    Selenide.sleep(1000);

  }

  @Test
  public void testEditedAndSavedShouldNotShowError() {
    var cmsList = $$("[id^='content-form:table-cms-keys:'][id$=':cms-uri']");
    var selectedCms = cmsList.get(0);
    var otherCms = cmsList.get(1);
    selectedCms.click();
    var displayItem = $$("[id^='content-form:cms-values:'][id$='_display']").first();
    displayItem.click();
    var contentItem = $(By.id(displayItem.getAttribute("id").replaceAll("_display", "_content")));
    contentItem.$(By.className("sun-editor-editable")).setValue("Content is updated");
    contentItem.$(By.cssSelector("button[data-command='save']")).click();
    Selenide.sleep(1000);
    otherCms.click();
    $(By.id("primefacesmessagedlg")).should(hidden);
  }
}