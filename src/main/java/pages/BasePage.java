package pages;

// Base Page — Selenium & Appium Automation Framework
// Author: Muhammad Ammar Ahmed

import org.openqa.selenium.*;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import io.appium.java_client.AppiumDriver;
import io.appium.java_client.MobileElement;
import java.time.Duration;
import java.util.List;
import java.util.logging.Logger;

public class BasePage {

    protected WebDriver driver;
    protected WebDriverWait wait;
    private static final Logger logger = Logger.getLogger(BasePage.class.getName());
    private static final int DEFAULT_TIMEOUT = 15;

    public BasePage(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(DEFAULT_TIMEOUT));
        PageFactory.initElements(driver, this);
    }

    // ── Element Interactions ──────────────────────────────────────────────────

    protected void click(By locator) {
        logger.info("Clicking element: " + locator);
        waitForClickable(locator).click();
    }

    protected void click(WebElement element) {
        waitForClickable(element).click();
    }

    protected void type(By locator, String text) {
        logger.info("Typing '" + text + "' into: " + locator);
        WebElement el = waitForVisible(locator);
        el.clear();
        el.sendKeys(text);
    }

    protected String getText(By locator) {
        return waitForVisible(locator).getText().trim();
    }

    protected String getAttributeValue(By locator, String attribute) {
        return waitForVisible(locator).getAttribute(attribute);
    }

    protected boolean isDisplayed(By locator) {
        try {
            return driver.findElement(locator).isDisplayed();
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    protected void selectByVisibleText(By locator, String text) {
        Select select = new Select(waitForVisible(locator));
        select.selectByVisibleText(text);
    }

    protected void selectByValue(By locator, String value) {
        Select select = new Select(waitForVisible(locator));
        select.selectByValue(value);
    }

    protected void scrollToElement(WebElement element) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);
    }

    protected void scrollToTop() {
        ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, 0);");
    }

    protected void jsClick(By locator) {
        WebElement element = driver.findElement(locator);
        ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
    }

    protected void hover(By locator) {
        WebElement element = waitForVisible(locator);
        new org.openqa.selenium.interactions.Actions(driver)
            .moveToElement(element).perform();
    }

    protected void dragAndDrop(By source, By target) {
        WebElement src = driver.findElement(source);
        WebElement tgt = driver.findElement(target);
        new org.openqa.selenium.interactions.Actions(driver)
            .dragAndDrop(src, tgt).perform();
    }

    // ── Wait Helpers ──────────────────────────────────────────────────────────

    protected WebElement waitForVisible(By locator) {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    protected WebElement waitForClickable(By locator) {
        return wait.until(ExpectedConditions.elementToBeClickable(locator));
    }

    protected WebElement waitForClickable(WebElement element) {
        return wait.until(ExpectedConditions.elementToBeClickable(element));
    }

    protected void waitForInvisible(By locator) {
        wait.until(ExpectedConditions.invisibilityOfElementLocated(locator));
    }

    protected void waitForText(By locator, String text) {
        wait.until(ExpectedConditions.textToBePresentInElementLocated(locator, text));
    }

    protected void waitForUrl(String urlFragment) {
        wait.until(ExpectedConditions.urlContains(urlFragment));
    }

    protected List<WebElement> waitForAll(By locator) {
        return wait.until(ExpectedConditions.visibilityOfAllElementsLocatedBy(locator));
    }

    // ── Mobile Helpers (Appium) ───────────────────────────────────────────────

    protected void mobileSwipeUp() {
        if (driver instanceof AppiumDriver) {
            AppiumDriver<MobileElement> appiumDriver = (AppiumDriver<MobileElement>) driver;
            Dimension size = appiumDriver.manage().window().getSize();
            int startX = size.width / 2;
            int startY = (int) (size.height * 0.8);
            int endY   = (int) (size.height * 0.2);
            new io.appium.java_client.TouchAction<>(appiumDriver)
                .press(io.appium.java_client.touch.offset.PointOption.point(startX, startY))
                .waitAction(io.appium.java_client.touch.WaitOptions.waitOptions(Duration.ofMillis(500)))
                .moveTo(io.appium.java_client.touch.offset.PointOption.point(startX, endY))
                .release().perform();
        }
    }

    protected void mobileClick(String accessibilityId) {
        if (driver instanceof AppiumDriver) {
            AppiumDriver<MobileElement> appiumDriver = (AppiumDriver<MobileElement>) driver;
            appiumDriver.findElementByAccessibilityId(accessibilityId).click();
        }
    }

    // ── Browser Helpers ───────────────────────────────────────────────────────

    protected void navigateTo(String url) {
        logger.info("Navigating to: " + url);
        driver.get(url);
    }

    protected String getPageTitle() {
        return driver.getTitle();
    }

    protected String getCurrentUrl() {
        return driver.getCurrentUrl();
    }

    protected void switchToFrame(By frameLocator) {
        wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(frameLocator));
    }

    protected void switchToDefaultContent() {
        driver.switchTo().defaultContent();
    }

    protected void acceptAlert() {
        wait.until(ExpectedConditions.alertIsPresent()).accept();
    }

    protected String getAlertText() {
        return wait.until(ExpectedConditions.alertIsPresent()).getText();
    }

    protected void takeScreenshot(String fileName) {
        TakesScreenshot ts = (TakesScreenshot) driver;
        byte[] screenshot = ts.getScreenshotAs(OutputType.BYTES);
        try {
            java.nio.file.Files.write(
                java.nio.file.Paths.get("screenshots/" + fileName + ".png"), screenshot);
        } catch (Exception e) {
            logger.warning("Failed to save screenshot: " + e.getMessage());
        }
    }
}
