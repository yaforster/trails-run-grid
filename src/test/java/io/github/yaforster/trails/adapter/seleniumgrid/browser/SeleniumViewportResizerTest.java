package io.github.yaforster.trails.adapter.seleniumgrid.browser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static org.mockito.Mockito.doThrow;

import io.github.yaforster.trails.core.test.action.browser.viewport.ViewportDimensions;
import io.github.yaforster.trails.core.test.action.browser.viewport.ViewportResizeOutcome;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

class SeleniumViewportResizerTest {

    @Test
    void shouldRetainInitialMeasurement_whenResizeAndDiagnosticMeasurementFail() {
        WebDriver driver = mock(WebDriver.class, withSettings().extraInterfaces(JavascriptExecutor.class));
        WebDriver.Options options = mock();
        WebDriver.Window window = mock();
        when(driver.manage()).thenReturn(options);
        when(options.window()).thenReturn(window);
        when(window.getSize()).thenReturn(new Dimension(820, 680));
        when(((JavascriptExecutor) driver).executeScript(
                "return [window.innerWidth, window.innerHeight];")).thenReturn(List.of(800, 600))
                                                                   .thenThrow(new IllegalStateException(
                                                                           "Measurement disconnected"));
        doThrow(new IllegalStateException("Resize rejected")).when(window)
                                                             .setSize(new Dimension(1020, 780));
        ViewportDimensions requested = new ViewportDimensions(1000, 700);

        ViewportResizeOutcome result = new SeleniumViewportResizer(driver).resize(requested);

        assertThat(result).isEqualTo(new ViewportResizeOutcome.Failure(requested,
                Optional.of(new ViewportDimensions(800, 600)),
                "Could not set outer window size: Resize rejected Diagnostic viewport measurement unavailable: Measurement disconnected"));
    }

    @Test
    void shouldCompensateForWindowFrame_whenResizingTheVisiblePage() {
        WebDriver driver = mock(WebDriver.class, withSettings().extraInterfaces(JavascriptExecutor.class));
        WebDriver.Options options = mock();
        WebDriver.Window window = mock();
        when(driver.manage()).thenReturn(options);
        when(options.window()).thenReturn(window);
        when(window.getSize()).thenReturn(new Dimension(820, 680));
        when(((JavascriptExecutor) driver).executeScript("return [window.innerWidth, window.innerHeight];")).thenReturn(
                List.of(800, 600), List.of(1000, 700));

        new SeleniumViewportResizer(driver).resize(new ViewportDimensions(1000, 700));

        verify(window).setSize(new Dimension(1020, 780));
    }

    @Test
    void shouldReportMeasuredSize_whenBrowserDoesNotReachRequestedSize() {
        WebDriver driver = mock(WebDriver.class, withSettings().extraInterfaces(JavascriptExecutor.class));
        WebDriver.Options options = mock();
        WebDriver.Window window = mock();
        when(driver.manage()).thenReturn(options);
        when(options.window()).thenReturn(window);
        when(window.getSize()).thenReturn(new Dimension(820, 680));
        when(((JavascriptExecutor) driver).executeScript("return [window.innerWidth, window.innerHeight];")).thenReturn(
                List.of(800, 600), List.of(900, 650));
        ViewportDimensions requested = new ViewportDimensions(1000, 700);

        ViewportResizeOutcome result = new SeleniumViewportResizer(driver).resize(requested);

        assertThat(result).isEqualTo(
                new ViewportResizeOutcome.Failure(requested, Optional.of(new ViewportDimensions(900, 650)),
                        "Browser did not reach requested viewport dimensions after one outer-window resize attempt."));
    }
}
