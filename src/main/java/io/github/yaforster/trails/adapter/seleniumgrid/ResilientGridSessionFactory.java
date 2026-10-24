package io.github.yaforster.trails.adapter.seleniumgrid;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.yaforster.trails.core.test.Browser;
import java.net.URL;
import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import java.util.Locale;
import java.util.function.Supplier;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

/**
 * Adds per-browser admission limits, circuit breaking and explicitly configured retries around remote session creation.
 * The admission slot covers all creation attempts, not the resulting session's lifetime; existing browser actions are
 * outside these protections.
 */
final class ResilientGridSessionFactory {

    private final GridProperties properties;
    private final Map<Browser, ResilienceProtections> protections;

    ResilientGridSessionFactory(GridProperties properties) {
        this.properties = properties;
        this.protections = createProtectionsByBrowser();
    }

    private Map<Browser, ResilienceProtections> createProtectionsByBrowser() {
        Map<Browser, ResilienceProtections> configuredProtections = new EnumMap<>(Browser.class);
        for (Browser browser : Browser.values()) {
            ResilienceProtections browserProtections = createProtections(browser);
            configuredProtections.put(browser, browserProtections);
        }
        return Map.copyOf(configuredProtections);
    }

    private ResilienceProtections createProtections(Browser browser) {
        String resilienceId = buildId(browser);
        CircuitBreaker circuitBreaker = createCircuitBreaker(resilienceId);
        Retry retry = createSessionRetry(resilienceId);
        Bulkhead bulkhead = createAdmissionLimit(resilienceId);
        return new ResilienceProtections(circuitBreaker, retry, bulkhead);
    }

    RemoteWebDriver create(Browser browser, URL gridUrl, Capabilities capabilities) {
        Supplier<RemoteWebDriver> openSession = () -> new RemoteWebDriver(gridUrl, capabilities);
        return create(browser, openSession);
    }

    /**
     * Allows tests to supply session-creation attempts without connecting to a Grid. The create method using an URL
     * argument also calls this to apply the resilience protections.
     */
    RemoteWebDriver create(Browser browser, Supplier<RemoteWebDriver> session) {
        Supplier<RemoteWebDriver> resilientSession = getResilientWebDriverSupplier(browser, session);
        return resilientSession.get();
    }

    private Supplier<RemoteWebDriver> getResilientWebDriverSupplier(Browser browser,
            Supplier<RemoteWebDriver> session) {
        // Wrappers run from the outside in: reserve one creation slot for all attempts,
        // then retry as configured. Each attempt consults the failure-based circuit
        // breaker, which can temporarily stop requests to an unhealthy Grid.
        ResilienceProtections browserProtections = protections.get(browser);
        CircuitBreaker circuitBreaker = browserProtections.circuitBreaker();
        Supplier<RemoteWebDriver> stopWhenGridIsUnhealthy = CircuitBreaker.decorateSupplier(circuitBreaker, session);
        Retry retry = browserProtections.retry();
        Supplier<RemoteWebDriver> retryWithinAttemptLimit = Retry.decorateSupplier(retry, stopWhenGridIsUnhealthy);
        Bulkhead bulkhead = browserProtections.bulkhead();
        return Bulkhead.decorateSupplier(bulkhead, retryWithinAttemptLimit);
    }

    private CircuitBreaker createCircuitBreaker(String resilienceId) {
        return CircuitBreaker.ofDefaults(resilienceId);
    }

    private Retry createSessionRetry(String resilienceId) {
        int maxAttempts = properties.sessionMaxAttempts();
        RetryConfig retryConfig = RetryConfig.custom()
                                             .maxAttempts(maxAttempts)
                                             .waitDuration(Duration.ZERO)
                                             .build();
        return Retry.of(resilienceId, retryConfig);
    }

    private Bulkhead createAdmissionLimit(String resilienceId) {
        int maximumConcurrentCreations = properties.maximumConcurrentSessions();
        BulkheadConfig admissionConfig = BulkheadConfig.custom()
                                                       .maxConcurrentCalls(maximumConcurrentCreations)
                                                       .maxWaitDuration(Duration.ZERO)
                                                       .build();
        return Bulkhead.of(resilienceId, admissionConfig);
    }

    private static String buildId(Browser browser) {
        String browserName = browser.name()
                                    .toLowerCase(Locale.ROOT);
        return "selenium-grid-" + browserName;
    }

}
