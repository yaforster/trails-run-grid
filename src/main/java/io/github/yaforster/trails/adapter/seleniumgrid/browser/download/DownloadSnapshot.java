package io.github.yaforster.trails.adapter.seleniumgrid.browser.download;

import org.openqa.selenium.HasDownloads;

/**
 * Captures download metadata by value so successive polls compare name, size and modification time rather than Selenium
 * object identity, and earlier observations remain fixed.
 */
record DownloadSnapshot(String name, long size, long lastModifiedTime) {

    static DownloadSnapshot from(HasDownloads.DownloadedFile file) {
        String name = file.getName();
        long size = file.getSize();
        long lastModifiedTime = file.getLastModifiedTime();
        return new DownloadSnapshot(name, size, lastModifiedTime);
    }
}
