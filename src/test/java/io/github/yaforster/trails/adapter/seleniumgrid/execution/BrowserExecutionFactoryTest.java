package io.github.yaforster.trails.adapter.seleniumgrid.execution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.doThrow;

import io.github.yaforster.trails.adapter.seleniumgrid.document.PdfBoxDocumentExecution;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;

class BrowserExecutionFactoryTest {

    @Test
    void shouldKeepInitializationFailure_whenClosingThePartialSessionAlsoFails() {
        WebDriver driver = mock();
        RuntimeException initializationFailure = new IllegalStateException("Initialization failed");
        RuntimeException cleanupFailure = new IllegalStateException("Grid disconnected");
        doThrow(cleanupFailure).when(driver)
                               .quit();

        PdfBoxDocumentExecution documents = new PdfBoxDocumentExecution();
        BrowserExecutionFactory.createWithDefaultBrowserSession("Grid Chrome", () -> driver, ignored -> {
            throw initializationFailure;
        }, 100, documents);

        assertThat(initializationFailure.getSuppressed()).containsExactly(cleanupFailure);
    }

    @Test
    void shouldCloseTheDriverWhenCompleteBrowserExecutionCannotBeInitialized() {
        WebDriver driver = mock();
        PdfBoxDocumentExecution documents = new PdfBoxDocumentExecution();

        Optional<?> execution = BrowserExecutionFactory.createWithDefaultBrowserSession("Grid Chrome", () -> driver,
                ignored -> {
                }, 100, documents);

        assertThat(execution).isEmpty();
        verify(driver).quit();
    }

}
