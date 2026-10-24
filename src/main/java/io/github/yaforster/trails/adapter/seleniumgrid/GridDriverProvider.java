package io.github.yaforster.trails.adapter.seleniumgrid;

import io.github.yaforster.trails.adapter.seleniumgrid.document.PdfBoxDocumentExecution;
import io.github.yaforster.trails.adapter.seleniumgrid.execution.BrowserExecutionFactory;
import io.github.yaforster.trails.core.test.Browser;
import io.github.yaforster.trails.core.test.BrowserExecution;
import io.github.yaforster.trails.core.test.DriverProvider;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.LocalFileDetector;
import org.openqa.selenium.remote.RemoteWebDriver;

/** Opens one configured remote browser session with managed-download support. */
final class GridDriverProvider implements DriverProvider {

    private final Browser browser;
    private final GridProperties properties;
    private final ResilientGridSessionFactory sessions;

    GridDriverProvider(Browser browser, GridProperties properties, ResilientGridSessionFactory sessions) {
        this.browser = browser;
        this.properties = properties;
        this.sessions = sessions;
    }

    @Override
    public Optional<BrowserExecution> createDriver() {
        String browserDescription = "Selenium Grid " + browser;
        Supplier<WebDriver> openSession = this::openRemoteSession;
        Consumer<WebDriver> initializeSession = this::configureFileUploads;
        int downloadTimeoutMillis = properties.fileDownloadTimeoutMillis();
        PdfBoxDocumentExecution documents = new PdfBoxDocumentExecution();
        return BrowserExecutionFactory.createWithDefaultBrowserSession(browserDescription, openSession,
                initializeSession, downloadTimeoutMillis, documents);
    }

    private RemoteWebDriver openRemoteSession() {
        URL endpoint = gridEndpoint();
        MutableCapabilities options = browserOptions();
        return sessions.create(browser, endpoint, options);
    }

    private void configureFileUploads(WebDriver driver) {
        if (browser == Browser.CHROME) {
            // The browser runs on another machine. Selenium must transfer local upload
            // files to that machine before placing their paths into a file input.
            LocalFileDetector localFileDetector = new LocalFileDetector();
            RemoteWebDriver remoteDriver = (RemoteWebDriver) driver;
            remoteDriver.setFileDetector(localFileDetector);
        }
    }

    private URL gridEndpoint() {
        try {
            return properties.gridUrl()
                             .toURL();
        } catch (MalformedURLException exception) {
            throw new IllegalStateException("Selenium Grid URL cannot initialize a browser session", exception);
        }
    }

    private MutableCapabilities browserOptions() {
        return switch (browser) {
            case CHROME -> chromeOptions();
            case EDGE -> edgeOptions();
            case FIREFOX -> firefoxOptions();
        };
    }

    private ChromeOptions chromeOptions() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new", "--start-maximized");
        List<String> excludedSwitches = List.of("enable-automation");
        options.setExperimentalOption("excludeSwitches", excludedSwitches);
        Map<String, Object> preferences = unattendedChromePreferences();
        options.setExperimentalOption("prefs", preferences);
        return enableManagedDownloads(options);
    }

    private Map<String, Object> unattendedChromePreferences() {
        // Save PDFs instead of opening the built-in viewer, and prevent download or
        // password prompts from blocking a test that runs without a person present.
        Map.Entry<String, Object> saveDownloadsToGridDirectory = Map.entry("download.default_directory",
                "/home/seluser/Downloads");
        Map.Entry<String, Object> disableDownloadPrompts = Map.entry("download.prompt_for_download", false);
        Map.Entry<String, Object> preventSystemPdfReader = Map.entry("download.open_pdf_in_system_reader", false);
        Map.Entry<String, Object> savePdfsAsDownloads = Map.entry("plugins.always_open_pdf_externally", true);
        Map.Entry<String, Object> disableCredentialService = Map.entry("credentials_enable_service", false);
        Map.Entry<String, Object> disablePasswordManager = Map.entry("profile.password_manager_enabled", false);
        return Map.ofEntries(saveDownloadsToGridDirectory, disableDownloadPrompts, preventSystemPdfReader,
                savePdfsAsDownloads, disableCredentialService, disablePasswordManager);
    }

    private EdgeOptions edgeOptions() {
        EdgeOptions options = new EdgeOptions();
        options.addArguments("--headless=new", "--remote-allow-origins=*");
        return enableManagedDownloads(options);
    }

    private FirefoxOptions firefoxOptions() {
        FirefoxOptions options = new FirefoxOptions();
        options.addArguments("--headless", "--allow-elevated-browser");
        return enableManagedDownloads(options);
    }

    private <T extends MutableCapabilities> T enableManagedDownloads(T options) {
        // Grid keeps downloads on its remote machine and exposes them for retrieval by Trails.
        options.setCapability("se:downloadsEnabled", true);
        return options;
    }

}
