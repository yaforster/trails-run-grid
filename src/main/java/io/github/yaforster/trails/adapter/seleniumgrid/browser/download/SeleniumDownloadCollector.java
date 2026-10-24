package io.github.yaforster.trails.adapter.seleniumgrid.browser.download;

import io.github.yaforster.trails.core.test.FileData;
import org.openqa.selenium.HasDownloads;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Combines Grid metadata polling with artifact retrieval so Trails receives collected bytes rather than a remote file
 * listing. Each collection has its own stability history and bounded wait; a timeout yields no artifacts.
 */
final class SeleniumDownloadCollector {

    private static final Logger LOGGER = LoggerFactory.getLogger(SeleniumDownloadCollector.class);
    private final WebDriver driver;
    private final HasDownloads downloads;
    private final int timeoutMillis;
    private final DownloadStabilityTracker tracker = new DownloadStabilityTracker();

    SeleniumDownloadCollector(WebDriver driver, HasDownloads downloads, int timeoutMillis) {
        this.driver = driver;
        this.downloads = downloads;
        this.timeoutMillis = timeoutMillis;
    }

    List<FileData> collectDownloadedFiles() {
        try {
            List<DownloadSnapshot> files = waitForStableDownloads();
            return retrieveDownloads(files);
        } catch (TimeoutException _) {
            LOGGER.debug("Grid downloads did not stabilize within {} ms; returning no artifacts.", timeoutMillis);
            return Collections.emptyList();
        }
    }

    private List<DownloadSnapshot> waitForStableDownloads() {
        // Selenium keeps polling while the callback returns null; an empty list would
        // finish the wait before the browser has completed its downloads.
        Duration timeout = Duration.ofMillis(timeoutMillis);
        Duration pollingInterval = Duration.ofMillis(100);
        WebDriverWait wait = new WebDriverWait(driver, timeout);
        wait.pollingEvery(pollingInterval);
        return wait.until(_ -> {
            List<HasDownloads.DownloadedFile> downloadedFiles = downloads.getDownloadedFiles();
            Optional<List<DownloadSnapshot>> stableDownloads = tracker.observe(downloadedFiles);
            return stableDownloads.orElse(null);
        });
    }

    private List<FileData> retrieveDownloads(List<DownloadSnapshot> files) {
        SeleniumDownloadRetriever retriever = new SeleniumDownloadRetriever(downloads);
        List<FileData> artifacts = new ArrayList<>();
        for (DownloadSnapshot file : files) {
            String fileName = file.name();
            Optional<FileData> artifact = retriever.retrieve(fileName);
            artifact.ifPresent(artifacts::add);
        }
        return List.copyOf(artifacts);
    }

}
