package io.github.yaforster.trails.adapter.seleniumgrid.browser;

import io.github.yaforster.trails.core.test.action.browser.viewport.ViewportDimensions;
import io.github.yaforster.trails.core.test.action.browser.viewport.ViewportResizeOutcome;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import java.util.List;
import java.util.Optional;

/**
 * Trails requests visible-page dimensions, whereas Selenium resizes the outer browser window. Compensates for window
 * decorations and verifies the viewport after one resize attempt, retaining measured dimensions in failure diagnostics.
 */
class SeleniumViewportResizer {

    private final WebDriver driver;

    SeleniumViewportResizer(WebDriver driver) {
        this.driver = driver;
    }

    ViewportResizeOutcome resize(ViewportDimensions requested) {
        ViewportDimensions initial;
        try {
            initial = measureViewport();
        } catch (RuntimeException exception) {
            Optional<ViewportDimensions> unavailableMeasurement = Optional.empty();
            return failure(requested, unavailableMeasurement, "Could not measure current viewport", exception);
        }
        return resizeFromMeasuredViewport(requested, initial);
    }

    private ViewportResizeOutcome resizeFromMeasuredViewport(ViewportDimensions requested, ViewportDimensions initial) {
        Optional<ViewportDimensions> initialMeasurement = Optional.of(initial);
        Dimension outerWindow;
        try {
            outerWindow = driver.manage()
                                .window()
                                .getSize();
        } catch (RuntimeException exception) {
            return failure(requested, initialMeasurement, "Could not read current outer window size", exception);
        }

        Dimension targetOuterWindow;
        try {
            targetOuterWindow = compensatedOuterWindow(requested, initial, outerWindow);
        } catch (RuntimeException exception) {
            return failure(requested, initialMeasurement, "Could not calculate compensated outer window size",
                    exception);
        }
        return resizeWindowAndVerifyViewport(requested, initial, targetOuterWindow);
    }

    private ViewportResizeOutcome resizeWindowAndVerifyViewport(ViewportDimensions requested,
            ViewportDimensions initial, Dimension targetOuterWindow) {
        try {
            driver.manage()
                  .window()
                  .setSize(targetOuterWindow);
        } catch (RuntimeException exception) {
            return reportResizeFailureWithLatestMeasurement(requested, initial, exception);
        }
        return verifyViewportReachedRequestedSize(requested, initial);
    }

    private ViewportResizeOutcome verifyViewportReachedRequestedSize(ViewportDimensions requested,
            ViewportDimensions initial) {
        try {
            ViewportDimensions measured = measureViewport();
            if (requested.equals(measured)) {
                return new ViewportResizeOutcome.Success(requested, measured);
            }
            Optional<ViewportDimensions> latestMeasurement = Optional.of(measured);
            return new ViewportResizeOutcome.Failure(requested, latestMeasurement,
                    "Browser did not reach requested viewport dimensions after one outer-window resize attempt.");
        } catch (RuntimeException exception) {
            Optional<ViewportDimensions> initialMeasurement = Optional.of(initial);
            return failure(requested, initialMeasurement, "Could not measure viewport after resize", exception);
        }
    }

    private ViewportResizeOutcome.Failure reportResizeFailureWithLatestMeasurement(ViewportDimensions requested,
            ViewportDimensions initial, RuntimeException setSizeFailure) {
        try {
            ViewportDimensions measured = measureViewport();
            Optional<ViewportDimensions> latestMeasurement = Optional.of(measured);
            return failure(requested, latestMeasurement, "Could not set outer window size", setSizeFailure);
        } catch (RuntimeException measurementFailure) {
            Optional<ViewportDimensions> initialMeasurement = Optional.of(initial);
            String resizeCause = cause("Could not set outer window size", setSizeFailure);
            String measurementCause = cause(measurementFailure);
            String diagnostic = resizeCause + " Diagnostic viewport measurement unavailable: " + measurementCause;
            return new ViewportResizeOutcome.Failure(requested, initialMeasurement, diagnostic);
        }
    }

    private Dimension compensatedOuterWindow(ViewportDimensions requested, ViewportDimensions initial,
            Dimension outerWindow) {
        // Selenium resizes the whole window, including its frame and toolbar. Add that
        // surrounding space so the visible page, rather than the window, reaches the target.
        long outerWidth = outerWindow.getWidth();
        long outerHeight = outerWindow.getHeight();
        int initialWidth = initial.width();
        int initialHeight = initial.height();
        int requestedWidth = requested.width();
        int requestedHeight = requested.height();
        long horizontalDecoration = Math.subtractExact(outerWidth, initialWidth);
        long verticalDecoration = Math.subtractExact(outerHeight, initialHeight);
        long targetWidth = Math.addExact(requestedWidth, horizontalDecoration);
        long targetHeight = Math.addExact(requestedHeight, verticalDecoration);
        if (targetWidth < 1 || targetWidth > Integer.MAX_VALUE || targetHeight < 1
                || targetHeight > Integer.MAX_VALUE) {
            throw new ArithmeticException("Compensated outer window dimensions are outside Selenium Dimension range.");
        }
        int width = Math.toIntExact(targetWidth);
        int height = Math.toIntExact(targetHeight);
        return new Dimension(width, height);
    }

    private ViewportDimensions measureViewport() {
        Object measurement = javascript().executeScript("return [window.innerWidth, window.innerHeight];");
        if (!(measurement instanceof List<?> values) || values.size() != 2) {
            throw new IllegalStateException("Browser did not return two viewport dimensions.");
        }
        Object rawWidth = values.get(0);
        Object rawHeight = values.get(1);
        int width = measuredDimension(rawWidth);
        int height = measuredDimension(rawHeight);
        return new ViewportDimensions(width, height);
    }

    private int measuredDimension(Object value) {
        if (!(value instanceof Number number)) {
            throw new IllegalStateException("Browser returned a non-numeric viewport dimension.");
        }
        double dimension = number.doubleValue();
        if (!Double.isFinite(dimension) || Math.rint(dimension) != dimension || dimension < 1
                || dimension > Integer.MAX_VALUE) {
            throw new IllegalStateException("Browser returned an invalid viewport dimension.");
        }
        return (int) dimension;
    }

    private ViewportResizeOutcome.Failure failure(ViewportDimensions requested, Optional<ViewportDimensions> measured,
            String operation, RuntimeException exception) {
        String diagnostic = cause(operation, exception);
        return new ViewportResizeOutcome.Failure(requested, measured, diagnostic);
    }

    private String cause(String operation, RuntimeException exception) {
        return operation + ": " + cause(exception);
    }

    private String cause(RuntimeException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? "browser driver did not provide a cause" : message;
    }

    private JavascriptExecutor javascript() {
        return (JavascriptExecutor) driver;
    }

}
