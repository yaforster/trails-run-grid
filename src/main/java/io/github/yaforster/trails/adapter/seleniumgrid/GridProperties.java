package io.github.yaforster.trails.adapter.seleniumgrid;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * External configuration for the one Selenium Grid browser capability.
 *
 * @param gridUrl
 *            required Selenium Grid endpoint
 * @param browserBaseUrl
 *            optional container-reachable replacement for loopback stage URLs
 * @param fileDownloadTimeout
 *            upper bound for waiting on managed downloads
 * @param sessionMaxAttempts
 *            explicit bounded remote-session attempts; defaults to one
 * @param maximumConcurrentSessions
 *            maximum in-flight session-creation calls per browser
 */
@ConfigurationProperties("trails.adapter.selenium-grid")
public record GridProperties(URI gridUrl, URI browserBaseUrl, Duration fileDownloadTimeout, Integer sessionMaxAttempts,
        Integer maximumConcurrentSessions) {

    private static final Duration DEFAULT_FILE_DOWNLOAD_TIMEOUT = Duration.ofSeconds(30);
    private static final int DEFAULT_SESSION_MAX_ATTEMPTS = 1;
    private static final int DEFAULT_MAXIMUM_CONCURRENT_SESSIONS = 10;
    private static final String GRID_URL_REQUIRED_MESSAGE = "trails.adapter.selenium-grid.grid-url must be configured as an absolute HTTP(S) URL";
    private static final String GRID_URL_INVALID_MESSAGE = "trails.adapter.selenium-grid.grid-url must be an absolute HTTP(S) URL";
    private static final String BROWSER_BASE_URL_INVALID_MESSAGE = "trails.adapter.selenium-grid.browser-base-url must be an absolute HTTP(S) URL";
    private static final String FILE_DOWNLOAD_TIMEOUT_INVALID_MESSAGE = "trails.adapter.selenium-grid.file-download-timeout must be positive and no greater than 2147483647ms";
    private static final String SESSION_MAX_ATTEMPTS_INVALID_MESSAGE = "trails.adapter.selenium-grid.session-max-attempts must be positive";
    private static final String MAXIMUM_CONCURRENT_SESSIONS_INVALID_MESSAGE = "trails.adapter.selenium-grid.maximum-concurrent-sessions must be positive";

    /**
     * Applies defaults and rejects invalid endpoint, timeout, retry, and concurrency settings.
     */
    public GridProperties {
        if (gridUrl == null) {
            throw new IllegalArgumentException(GRID_URL_REQUIRED_MESSAGE);
        }
        if (isNotAbsoluteHttpUrl(gridUrl)) {
            throw new IllegalArgumentException(GRID_URL_INVALID_MESSAGE);
        }
        if (browserBaseUrl != null && isNotAbsoluteHttpUrl(browserBaseUrl)) {
            throw new IllegalArgumentException(BROWSER_BASE_URL_INVALID_MESSAGE);
        }
        if (fileDownloadTimeout == null) {
            fileDownloadTimeout = DEFAULT_FILE_DOWNLOAD_TIMEOUT;
        }
        if (sessionMaxAttempts == null) {
            sessionMaxAttempts = DEFAULT_SESSION_MAX_ATTEMPTS;
        }
        if (maximumConcurrentSessions == null) {
            maximumConcurrentSessions = DEFAULT_MAXIMUM_CONCURRENT_SESSIONS;
        }
        if (isInvalidTimeout(fileDownloadTimeout)) {
            throw new IllegalArgumentException(FILE_DOWNLOAD_TIMEOUT_INVALID_MESSAGE);
        }
        if (sessionMaxAttempts < 1) {
            throw new IllegalArgumentException(SESSION_MAX_ATTEMPTS_INVALID_MESSAGE);
        }
        if (maximumConcurrentSessions < 1) {
            throw new IllegalArgumentException(MAXIMUM_CONCURRENT_SESSIONS_INVALID_MESSAGE);
        }
    }

    private static boolean isNotAbsoluteHttpUrl(URI url) {
        String scheme = url.getScheme();
        boolean httpScheme = "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
        return !url.isAbsolute() || url.getHost() == null || !httpScheme;
    }

    private static boolean isInvalidTimeout(Duration fileDownloadTimeout) {
        return fileDownloadTimeout.isNegative() || fileDownloadTimeout.isZero()
                || fileDownloadTimeout.toMillis() > Integer.MAX_VALUE;
    }

    int fileDownloadTimeoutMillis() {
        long timeoutMillis = fileDownloadTimeout.toMillis();
        return Math.toIntExact(timeoutMillis);
    }

}
