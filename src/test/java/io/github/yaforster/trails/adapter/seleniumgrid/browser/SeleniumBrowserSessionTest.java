package io.github.yaforster.trails.adapter.seleniumgrid.browser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.withSettings;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.inOrder;

import io.github.yaforster.trails.core.test.action.browser.ViewportCoordinates;
import io.github.yaforster.trails.core.test.action.element.Locator;
import io.github.yaforster.trails.core.test.action.element.LocatorType;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.openqa.selenium.HasDownloads;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.StaleElementReferenceException;
import org.mockito.InOrder;
import org.openqa.selenium.interactions.Interactive;
import org.openqa.selenium.interactions.Sequence;

@SuppressWarnings("unchecked")
class SeleniumBrowserSessionTest {

    @Test
    void shouldFindReplacementElement_whenPageReplacesOriginalBeforeClick() {
        WebDriver driver = mock(WebDriver.class, withSettings().extraInterfaces(HasDownloads.class));
        WebElement replacement = mock();
        when(driver.findElement(By.cssSelector("#save")))
                                                         .thenThrow(
                                                                 new StaleElementReferenceException("Page refreshed"))
                                                         .thenReturn(replacement);
        when(replacement.isDisplayed()).thenReturn(true);
        when(replacement.isEnabled()).thenReturn(true);
        when(replacement.getTagName()).thenReturn("button");

        new SeleniumBrowserSession(driver, 100).click(new Locator(LocatorType.CSS, "#save"));

        verify(replacement).click();
    }

    @Test
    void shouldClearInputBeforeTyping_whenReplacementIsRequested() {
        WebDriver driver = mock(WebDriver.class, withSettings().extraInterfaces(HasDownloads.class));
        WebElement input = mock();
        when(driver.findElement(By.cssSelector("#name"))).thenReturn(input);
        when(input.isDisplayed()).thenReturn(true);
        when(input.isEnabled()).thenReturn(true);
        when(input.getTagName()).thenReturn("input");

        new SeleniumBrowserSession(driver, 100).type(new Locator(LocatorType.CSS, "#name"), "Ada", true, 0);

        InOrder inputActions = inOrder(input);
        inputActions.verify(input)
                    .clear();
        inputActions.verify(input)
                    .sendKeys("Ada");
    }

    @Test
    void shouldPerformOneAbsoluteViewportPointerSequenceForCoordinateClick() {
        WebDriver driver = mock(WebDriver.class, withSettings().extraInterfaces(HasDownloads.class, Interactive.class));

        new SeleniumBrowserSession(driver, 100).click(new ViewportCoordinates(12, 34));

        ArgumentCaptor<Collection<Sequence>> sequences = ArgumentCaptor.forClass(Collection.class);
        verify((Interactive) driver).perform(sequences.capture());
        Map<String, Object> encoded = sequences.getValue()
                                               .iterator()
                                               .next()
                                               .encode();
        List<Map<String, Object>> actions = (List<Map<String, Object>>) encoded.get("actions");
        assertThat(actions).extracting(action -> action.get("type"))
                           .containsExactly("pointerMove", "pointerDown", "pointerUp");
        assertThat(actions.getFirst()).containsEntry("origin", "viewport")
                                      .containsEntry("x", 12)
                                      .containsEntry("y", 34);
    }

}
