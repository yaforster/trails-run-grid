package io.github.yaforster.trails.adapter.seleniumgrid.browser;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.Optional;

/**
 * Supplies click-and-wait selection for custom comboboxes, which Selenium's native HTML select support cannot handle.
 * This strategy assumes clicking exposes visible role-option elements; it matches text across the page, not within an
 * associated popup, and is not a general implementation of all ARIA combobox interaction patterns.
 */
final class AriaComboboxSelect {

    private static final By OPTION = By.cssSelector("[role='option']");
    private final WebDriver driver;

    AriaComboboxSelect(WebDriver driver) {
        this.driver = driver;
    }

    String select(WebElement element, String label) {
        requireCombobox(element);
        element.click();
        selectVisibleOption(label);
        return element.getTagName();
    }

    private void requireCombobox(WebElement element) {
        String role = element.getDomAttribute("role");
        if (!"combobox".equalsIgnoreCase(role)) {
            String tagName = element.getTagName();
            throw new NoSuchElementException("No supported select control found for element: " + tagName);
        }
    }

    private void selectVisibleOption(String label) {
        try {
            Duration timeout = Duration.ofSeconds(5);
            WebDriverWait wait = new WebDriverWait(driver, timeout);
            wait.until(_ -> selectOptionIfVisible(label));
        } catch (TimeoutException exception) {
            throw new NoSuchElementException("No option with label '" + label + "' was found.", exception);
        }
    }

    private boolean selectOptionIfVisible(String label) {
        Optional<WebElement> matchingOption = driver.findElements(OPTION)
                                                    .stream()
                                                    .filter(WebElement::isDisplayed)
                                                    .filter(option -> {
                                                        String optionText = option.getText();
                                                        return label.equals(optionText);
                                                    })
                                                    .findFirst();
        if (matchingOption.isEmpty()) {
            return false;
        }
        WebElement option = matchingOption.get();
        option.click();
        return true;
    }

}
