package io.github.yaforster.trails.adapter.seleniumgrid.browser.download;

import java.nio.file.Path;

/**
 * Keeps browser-specific temporary-name rules consistent between download polling and retrieval. Grid-reported names
 * can retain a temporary suffix while the transferred file already has its final name.
 */
final class DownloadFileNamePolicy {

    private static final String[] TEMPORARY_SUFFIXES = {".crdownload", ".part", ".tmp", ".download"};

    private DownloadFileNamePolicy() {}

    static boolean isTemporaryBrowserFile(String name) {
        return name.startsWith(".com.microsoft.Edge.") || name.startsWith(".com.google.Chrome.")
                || name.startsWith(".org.chromium.Chromium.");
    }

    static boolean matches(Path path, String reportedName) {
        String name = path.getFileName()
                          .toString();
        String finalizedReportedName = finalizedName(reportedName);
        return name.equals(reportedName) || name.equals(finalizedReportedName);
    }

    static String artifactName(Path path) {
        String fileName = path.getFileName()
                              .toString();
        return finalizedName(fileName);
    }

    private static String finalizedName(String name) {
        for (String suffix : TEMPORARY_SUFFIXES) {
            if (name.endsWith(suffix)) {
                int finalNameLength = name.length() - suffix.length();
                return name.substring(0, finalNameLength);
            }
        }
        return name;
    }

}
