package io.github.yaforster.trails.adapter.seleniumgrid;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import io.github.resilience4j.bulkhead.BulkheadFullException;

import io.github.yaforster.trails.core.test.Browser;
import java.net.URI;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.remote.RemoteWebDriver;

class ResilientGridSessionFactoryTest {

    @Test
    void shouldRejectAdditionalCreation_whenBrowserAdmissionLimitIsOccupied() {
        GridProperties properties = new GridProperties(URI.create("http://selenium-hub:4444"), null, null, 1, 1);
        ResilientGridSessionFactory sessions = new ResilientGridSessionFactory(properties);
        RemoteWebDriver driver = mock();

        assertThatThrownBy(() -> sessions.create(Browser.CHROME,
                () -> sessions.create(Browser.CHROME, () -> driver))).isInstanceOf(BulkheadFullException.class);
    }

    @Test
    void shouldAdmitOtherBrowser_whenChromeCreationLimitIsOccupied() {
        GridProperties properties = new GridProperties(URI.create("http://selenium-hub:4444"), null, null, 1, 1);
        ResilientGridSessionFactory sessions = new ResilientGridSessionFactory(properties);
        RemoteWebDriver driver = mock();

        RemoteWebDriver actual = sessions.create(Browser.CHROME, () -> sessions.create(Browser.FIREFOX, () -> driver));

        assertThat(actual).isSameAs(driver);
    }

    @Test
    void shouldReleaseAdmissionSlot_whenCreationFails() {
        GridProperties properties = new GridProperties(URI.create("http://selenium-hub:4444"), null, null, 1, 1);
        ResilientGridSessionFactory sessions = new ResilientGridSessionFactory(properties);
        RemoteWebDriver driver = mock();
        try {
            sessions.create(Browser.CHROME, () -> {
                throw new IllegalStateException("Grid unavailable");
            });
        } catch (IllegalStateException expected) {
            // The next call verifies that the failed creation did not retain its slot.
        }

        assertThat(sessions.create(Browser.CHROME, () -> driver)).isSameAs(driver);
    }

    @Test
    void shouldStopAfterConfiguredAttempts_whenEveryCreationFails() {
        GridProperties properties = new GridProperties(URI.create("http://selenium-hub:4444"), null, null, 2, 1);
        ResilientGridSessionFactory sessions = new ResilientGridSessionFactory(properties);
        AtomicInteger attempts = new AtomicInteger();
        try {
            sessions.create(Browser.CHROME, () -> {
                attempts.incrementAndGet();
                throw new IllegalStateException("Grid unavailable");
            });
        } catch (IllegalStateException expected) {
            // Verify the externally observable attempt count below.
        }

        assertThat(attempts).hasValue(2);
    }

    @Test
    void shouldRetryRemoteSessionCreationOnlyForTheConfiguredNumberOfAttempts() {
        GridProperties properties = new GridProperties(URI.create("http://selenium-hub:4444"), null, null, 2, null);
        ResilientGridSessionFactory sessions = new ResilientGridSessionFactory(properties);
        RemoteWebDriver expected = org.mockito.Mockito.mock();
        AtomicInteger attempts = new AtomicInteger();

        RemoteWebDriver actual = sessions.create(Browser.CHROME, () -> {
            if (attempts.incrementAndGet() == 1) {
                throw new IllegalStateException("transient session failure");
            }
            return expected;
        });

        assertThat(actual).isSameAs(expected);
        assertThat(attempts).hasValue(2);
    }

}
