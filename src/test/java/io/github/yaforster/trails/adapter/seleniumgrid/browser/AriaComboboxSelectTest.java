package io.github.yaforster.trails.adapter.seleniumgrid.browser;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

class AriaComboboxSelectTest {

    @Test
    void shouldSelectVisibleMatchingOption_whenHiddenDuplicateExists() {
        WebDriver driver = mock();
        WebElement combobox = mock();
        WebElement hiddenOption = mock();
        WebElement visibleOption = mock();
        when(combobox.getDomAttribute("role")).thenReturn("combobox");
        when(driver.findElements(By.cssSelector("[role='option']"))).thenReturn(List.of(hiddenOption, visibleOption));
        when(visibleOption.isDisplayed()).thenReturn(true);
        when(visibleOption.getText()).thenReturn("Germany");

        new AriaComboboxSelect(driver).select(combobox, "Germany");

        verify(visibleOption).click();
    }

    @Test
    void shouldRejectUnsupportedControl_whenElementIsNotACombobox() {
        WebDriver driver = mock();
        WebElement element = mock();
        when(element.getTagName()).thenReturn("div");

        assertThatThrownBy(
                () -> new AriaComboboxSelect(driver).select(element, "Germany"))
                                                                                .isInstanceOf(
                                                                                        NoSuchElementException.class)
                                                                                .hasMessageContaining(
                                                                                        "No supported select control");
    }
}
