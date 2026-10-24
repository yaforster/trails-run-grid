package io.github.yaforster.trails.adapter.seleniumgrid.browser;

import io.github.yaforster.trails.core.test.action.browser.viewport.ViewportDimensions;
import io.github.yaforster.trails.core.test.action.browser.viewport.ViewportMove;
import io.github.yaforster.trails.core.test.action.browser.viewport.ViewportMoveDirection;
import io.github.yaforster.trails.core.test.action.browser.viewport.ViewportMoveUnit;
import io.github.yaforster.trails.core.test.action.browser.viewport.ViewportResizeOutcome;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Translates Trails viewport movements into pixel scrolling or keyboard page navigation. Selenium has no direct
 * equivalent for Trails' viewport-relative distance units, which must be resolved against the current page height.
 */
class SeleniumViewportOperations {

    private final WebDriver driver;
    private final SeleniumViewportResizer resizer;

    SeleniumViewportOperations(WebDriver driver) {
        this.driver = driver;
        this.resizer = new SeleniumViewportResizer(driver);
    }

    ViewportResizeOutcome resize(ViewportDimensions dimensions) {
        return resizer.resize(dimensions);
    }

    void move(ViewportMove movement, ViewportMoveDirection direction, double amount, ViewportMoveUnit unit) {
        switch (movement) {
            case SCROLL_BY -> scrollBy(direction, amount, unit);
            case PAGE_FLIP -> pageFlip(direction, amount);
            case SCROLL_TO_TOP -> javascript().executeScript("window.scrollTo(0, 0);");
            case SCROLL_TO_BOTTOM -> javascript().executeScript(
                    "window.scrollTo(0, document.body.scrollHeight || document.documentElement.scrollHeight);");
        }
    }

    private void scrollBy(ViewportMoveDirection direction, double amount, ViewportMoveUnit unit) {
        double pixels = scrollDistanceInPixels(amount, unit);
        double signedDistance = direction == ViewportMoveDirection.UP ? -pixels : pixels;
        javascript().executeScript("window.scrollBy(0, arguments[0]);", signedDistance);
    }

    private double scrollDistanceInPixels(double amount, ViewportMoveUnit unit) {
        if (unit != ViewportMoveUnit.VIEWPORTS) {
            return amount;
        }
        Object height = javascript().executeScript(
                "return window.innerHeight || document.documentElement.clientHeight || 0;");
        return height instanceof Number number ? amount * number.doubleValue() : amount;
    }

    private void pageFlip(ViewportMoveDirection direction, double amount) {
        CharSequence key = direction == ViewportMoveDirection.UP ? Keys.PAGE_UP : Keys.PAGE_DOWN;
        WebElement activeElement = driver.switchTo()
                                         .activeElement();
        int roundedPageCount = (int) Math.round(amount);
        int pageCount = Math.max(0, roundedPageCount);
        for (int page = 0; page < pageCount; page++) {
            activeElement.sendKeys(key);
        }
    }

    private JavascriptExecutor javascript() {
        return (JavascriptExecutor) driver;
    }

}
