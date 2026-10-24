package io.github.yaforster.trails.adapter.seleniumgrid;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class GridPropertiesTest {

    @Test
    void shouldRequireAnEndpoint_whenConfiguringGrid() {
        assertThatThrownBy(() -> new GridProperties(null, null, null, null,
                null)).isInstanceOf(IllegalArgumentException.class)
                      .hasMessage(
                              "trails.adapter.selenium-grid.grid-url must be configured as an absolute HTTP(S) URL");
    }

    @ParameterizedTest
    @ValueSource(strings = {"relative/grid", "ftp://grid.example", "http:/missing-host"})
    void shouldRejectInvalidEndpoint_whenConfiguringGrid(String endpoint) {
        assertThatThrownBy(() -> new GridProperties(URI.create(endpoint), null, null, null,
                null)).isInstanceOf(IllegalArgumentException.class)
                      .hasMessage("trails.adapter.selenium-grid.grid-url must be an absolute HTTP(S) URL");
    }

    @ParameterizedTest
    @ValueSource(longs = {-1, 0, 2147483648L})
    void shouldRejectTimeoutOutsideSupportedRange_whenConfiguringDownloads(long milliseconds) {
        assertThatThrownBy(
                () -> new GridProperties(URI.create("http://grid.example"), null, Duration.ofMillis(milliseconds), null,
                        null)).isInstanceOf(IllegalArgumentException.class)
                              .hasMessageContaining("file-download-timeout must be positive and no greater than");
    }
}
