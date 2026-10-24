package io.github.yaforster.trails.adapter.seleniumgrid.browser.download;

import io.github.yaforster.trails.core.test.FileData;
import java.util.List;
import org.openqa.selenium.HasDownloads;
import org.openqa.selenium.WebDriver;

/** Collects managed downloads exposed by a Selenium remote session. */
public final class SeleniumManagedDownloadStore {

    private final WebDriver driver;
    private final HasDownloads downloads;
    private final int timeoutMillis;

    /**
     * Creates the download store for a browser session.
     *
     * @param driver
     *            Selenium driver supporting managed downloads
     * @param timeoutMillis
     *            upper bound for waiting for stable download metadata
     */
    public SeleniumManagedDownloadStore(WebDriver driver, int timeoutMillis) {
        this.driver = driver;
        this.downloads = downloadsFrom(driver);
        this.timeoutMillis = timeoutMillis;
    }

    /**
     * Collects stable remote downloads into in-memory artifacts.
     *
     * @return collected artifacts, or an empty list when the wait times out
     */
    public List<FileData> downloadedFiles() {
        SeleniumDownloadCollector collector = new SeleniumDownloadCollector(driver, downloads, timeoutMillis);
        return collector.collectDownloadedFiles();
    }

    /** Removes managed downloads from the remote session. */
    public void clear() {
        downloads.deleteDownloadableFiles();
    }

    private static HasDownloads downloadsFrom(WebDriver driver) {
        if (driver instanceof HasDownloads managedDownloads) {
            return managedDownloads;
        }
        throw new IllegalArgumentException("WebDriver must support Selenium managed downloads.");
    }

}
