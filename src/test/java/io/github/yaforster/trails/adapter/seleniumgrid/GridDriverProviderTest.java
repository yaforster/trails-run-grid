package io.github.yaforster.trails.adapter.seleniumgrid;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.yaforster.trails.core.test.Browser;
import java.net.URI;
import java.net.URL;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

class GridDriverProviderTest {

    @ParameterizedTest
    @EnumSource(Browser.class)
    void shouldEnableManagedDownloads_whenOpeningAnySupportedBrowser(Browser browser) {
        GridProperties properties = new GridProperties(URI.create("http://grid.example"), null, null, null, null);
        ResilientGridSessionFactory sessions = mock();
        RemoteWebDriver driver = mock();
        when(sessions.create(eq(browser), any(URL.class), any(Capabilities.class))).thenReturn(driver);

        new GridDriverProvider(browser, properties, sessions).createDriver();

        ArgumentCaptor<Capabilities> capabilities = ArgumentCaptor.forClass(Capabilities.class);
        verify(sessions).create(eq(browser), any(URL.class), capabilities.capture());
        assertThat(capabilities.getValue()
                               .getCapability("se:downloadsEnabled")).isEqualTo(true);
    }

    @Test
    void shouldSavePdfWithoutPrompts_whenOpeningChrome() {
        GridProperties properties = new GridProperties(URI.create("http://grid.example"), null, null, null, null);
        ResilientGridSessionFactory sessions = mock();
        RemoteWebDriver driver = mock();
        when(sessions.create(eq(Browser.CHROME), any(URL.class), any(Capabilities.class))).thenReturn(driver);

        new GridDriverProvider(Browser.CHROME, properties, sessions).createDriver();

        ArgumentCaptor<Capabilities> capabilities = ArgumentCaptor.forClass(Capabilities.class);
        verify(sessions).create(eq(Browser.CHROME), any(URL.class), capabilities.capture());
        Map<?, ?> chromeOptions = (Map<?, ?>) capabilities.getValue()
                                                          .getCapability("goog:chromeOptions");
        assertThat(chromeOptions.get("prefs")).isEqualTo(
                Map.of("download.default_directory", "/home/seluser/Downloads", "download.prompt_for_download", false,
                        "download.open_pdf_in_system_reader", false, "plugins.always_open_pdf_externally", true,
                        "credentials_enable_service", false, "profile.password_manager_enabled", false));
    }
}
