package io.github.yaforster.trails.adapter.seleniumgrid.browser;

import io.github.yaforster.trails.core.test.BrowserElementText;
import io.github.yaforster.trails.core.test.action.element.ElementValueSource;
import io.github.yaforster.trails.core.test.action.element.Locator;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import io.micrometer.common.util.StringUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Adds Trails' locator-based interaction rules to Selenium: reacquire elements after stale references and resolve
 * usable child controls for click and typing actions. Keeping these rules together avoids divergent lookup and retry
 * behavior across browser actions.
 */
final class SeleniumElementInteractions {

    private static final Duration LOOKUP_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration LOOKUP_INTERVAL = Duration.ofMillis(100);
    private static final Set<String> NATIVE_CONTROL_TAGS = Set.of("button", "a", "input", "textarea", "select");
    private static final By CHILD_CONTROLS = By.cssSelector("button, a, input, textarea, select, [role='button']");
    private final WebDriver driver;

    SeleniumElementInteractions(WebDriver driver) {
        this.driver = driver;
    }

    boolean exists(Locator locator) {
        try {
            WebDriverWait wait = elementWait();
            return wait.until(_ -> {
                Optional<WebElement> element = findIfPresent(locator);
                return element.isPresent();
            });
        } catch (TimeoutException _) {
            return false;
        }
    }

    String click(Locator locator) {
        return withCurrentElement(locator, element -> {
            WebElement control = resolveUsableControl(element);
            return clickControl(control);
        });
    }

    String type(Locator locator, String value, boolean clearBeforeTyping, long delayAfterClearMillis) {
        return withCurrentElement(locator, element -> {
            WebElement control = resolveUsableControl(element);
            prepareInput(control, clearBeforeTyping, delayAfterClearMillis);
            return enterText(control, value);
        });
    }

    BrowserElementText readText(Locator locator) {
        return withCurrentElement(locator, element -> {
            String description = describeElement(element);
            String normalizedText = element.getText()
                                           .toLowerCase();
            return new BrowserElementText(description, normalizedText);
        });
    }

    String readValue(Locator locator, ElementValueSource source, String name) {
        return withCurrentElement(locator, element -> switch (source) {
            case ATTRIBUTE -> element.getAttribute(name);
            case CSS_VALUE -> element.getCssValue(name);
            case PROPERTY -> element.getDomProperty(name);
        });
    }

    String select(Locator locator, String label) {
        return withCurrentElement(locator, element -> selectVisibleOption(element, label));
    }

    Optional<WebElement> findIfPresent(Locator locator) {
        try {
            By selector = seleniumLocator(locator);
            WebElement element = driver.findElement(selector);
            return Optional.of(element);
        } catch (NoSuchElementException | StaleElementReferenceException _) {
            return Optional.empty();
        }
    }

    private String clickControl(WebElement control) {
        String description = describeElement(control);
        control.click();
        return description;
    }

    private void prepareInput(WebElement control, boolean clearBeforeTyping, long delayAfterClearMillis) {
        if (clearBeforeTyping) {
            control.clear();
            // Some pages react to clearing by updating the input. Respect the action's
            // configured pause before sending the replacement text.
            waitForInputToSettle(delayAfterClearMillis);
        }
    }

    private String enterText(WebElement control, String value) {
        String description = describeElement(control);
        control.sendKeys(value);
        return description;
    }

    private String selectVisibleOption(WebElement element, String label) {
        String tagName = element.getTagName();
        if ("select".equalsIgnoreCase(tagName)) {
            Select select = new Select(element);
            select.selectByVisibleText(label);
            return describeElement(element);
        }
        AriaComboboxSelect combobox = new AriaComboboxSelect(driver);
        return combobox.select(element, label);
    }

    private <T> T withCurrentElement(Locator locator, Function<WebElement, T> action) {
        WebDriverWait wait = elementWait();
        return wait.until(_ -> {
            try {
                By selector = seleniumLocator(locator);
                WebElement element = driver.findElement(selector);
                return action.apply(element);
            } catch (StaleElementReferenceException _) {
                // Pages can replace controls after we find them. In Selenium's wait callback,
                // null means "try again", so the next attempt finds the replacement control.
                return null;
            }
        });
    }

    private WebDriverWait elementWait() {
        WebDriverWait wait = new WebDriverWait(driver, LOOKUP_TIMEOUT);
        wait.pollingEvery(LOOKUP_INTERVAL);
        wait.ignoring(StaleElementReferenceException.class);
        return wait;
    }

    private By seleniumLocator(Locator locator) {
        String expression = locator.locatorString();
        return switch (locator.type()) {
            case CSS -> By.cssSelector(expression);
            case XPATH -> By.xpath(expression);
        };
    }

    private WebElement resolveUsableControl(WebElement element) {
        if (isUsable(element) && isRecognizedControl(element)) {
            return element;
        }
        return element.findElements(CHILD_CONTROLS)
                      .stream()
                      .filter(this::isUsable)
                      .findFirst()
                      .orElse(element);
    }

    private boolean isRecognizedControl(WebElement element) {
        String tagName = element.getTagName();
        String normalizedTagName = tagName.toLowerCase(Locale.ROOT);
        if (NATIVE_CONTROL_TAGS.contains(normalizedTagName)) {
            return true;
        }
        String role = element.getDomAttribute("role");
        return "button".equalsIgnoreCase(role);
    }

    private boolean isUsable(WebElement element) {
        try {
            return element.isDisplayed() && element.isEnabled();
        } catch (StaleElementReferenceException _) {
            return false;
        }
    }

    private String describeElement(WebElement element) {
        String tagName = element.getTagName();
        return StringUtils.isBlank(tagName) ? "Element" : tagName;
    }

    private void waitForInputToSettle(long delayMillis) {
        if (delayMillis <= 0) {
            return;
        }
        try {
            Thread.sleep(delayMillis);
        } catch (InterruptedException exception) {
            Thread.currentThread()
                  .interrupt();
            throw new IllegalStateException("Typing action was interrupted while waiting after clearing the input.",
                    exception);
        }
    }
}
