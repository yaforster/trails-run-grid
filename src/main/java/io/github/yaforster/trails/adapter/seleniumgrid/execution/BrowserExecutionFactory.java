package io.github.yaforster.trails.adapter.seleniumgrid.execution;

import io.github.yaforster.trails.adapter.seleniumgrid.browser.SeleniumBrowserSession;
import io.github.yaforster.trails.core.test.BrowserExecution;
import io.github.yaforster.trails.core.test.BrowserSession;
import io.github.yaforster.trails.core.test.action.document.DocumentExecution;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Owns the gap between opening a Selenium driver and handing a complete browser execution to Trails. If subsequent
 * setup fails, closes the partially initialized session so it does not keep occupying Grid resources.
 */
public final class BrowserExecutionFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(BrowserExecutionFactory.class);

    private BrowserExecutionFactory() {}

    /**
     * Initializes a complete browser execution, closing the driver if any port cannot be created.
     *
     * @param sessionDescription
     *            safe browser/session description for diagnostics
     * @param driverFactory
     *            creates the underlying Selenium session
     * @param initializer
     *            applies browser-specific post-creation configuration
     * @param downloadTimeoutMillis
     *            upper bound for managed download collection
     * @param documents
     *            document executor associated with the browser session
     * @return a complete execution, or empty if initialization failed
     */
    public static Optional<BrowserExecution> createWithDefaultBrowserSession(String sessionDescription,
            Supplier<? extends WebDriver> driverFactory, Consumer<? super WebDriver> initializer,
            int downloadTimeoutMillis, DocumentExecution documents) {
        try {
            WebDriver driver = driverFactory.get();
            BrowserExecution execution = initializeExecution(driver, initializer, downloadTimeoutMillis, documents);
            return Optional.of(execution);
        } catch (RuntimeException exception) {
            String failureType = exception.getClass()
                                          .getSimpleName();
            LOGGER.warn("{} WebDriver session initialization failed: {}", sessionDescription, failureType);
            return Optional.empty();
        }
    }

    private static BrowserExecution initializeExecution(WebDriver driver, Consumer<? super WebDriver> initializer,
            int downloadTimeoutMillis, DocumentExecution documents) {
        try {
            initializer.accept(driver);
            BrowserSession browser = new SeleniumBrowserSession(driver, downloadTimeoutMillis);
            return new BrowserExecution(browser, documents);
        } catch (RuntimeException initializationFailure) {
            closeIncompleteSession(driver, initializationFailure);
            throw initializationFailure;
        }
    }

    private static void closeIncompleteSession(WebDriver driver, RuntimeException initializationFailure) {
        try {
            driver.quit();
        } catch (RuntimeException cleanupFailure) {
            // Keep the original reason for failure, while retaining cleanup diagnostics.
            initializationFailure.addSuppressed(cleanupFailure);
        }
    }

}
