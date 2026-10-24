package io.github.yaforster.trails.adapter.seleniumgrid;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.yaforster.trails.sdk.composition.TrailsBrowserRegistration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class TrailsSeleniumGridAutoConfigurationTest {

    @Test
    void shouldRegisterOneCompleteGridBrowserCapability() {
        contextRunner().withPropertyValues("trails.adapter.selenium-grid.grid-url=http://selenium-hub:4444")
                       .run(context -> {
                           assertThat(context).hasNotFailed();
                           assertThat(context).hasSingleBean(TrailsBrowserRegistration.class);
                           assertThat(context.getBean(TrailsBrowserRegistration.class)
                                             .adapterId()
                                             .value()).isEqualTo("selenium-grid");
                           assertThat(context).hasSingleBean(
                                   io.github.yaforster.trails.app.services.CapabilityContributor.class);
                       });
    }

    @Test
    void shouldRejectAMissingGridUrlBeforeReadiness() {
        contextRunner().run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasRootCauseMessage(
                    "trails.adapter.selenium-grid.grid-url must be configured as an absolute HTTP(S) URL");
        });
    }

    @Test
    void shouldDefaultToOneExplicitSessionAttemptAndThirtySecondDownloadTimeout() {
        contextRunner().withPropertyValues("trails.adapter.selenium-grid.grid-url=http://selenium-hub:4444")
                       .run(context -> {
                           GridProperties properties = context.getBean(GridProperties.class);
                           assertThat(properties.sessionMaxAttempts()).isEqualTo(1);
                           assertThat(properties.fileDownloadTimeoutMillis()).isEqualTo(30_000);
                       });
    }

    private ApplicationContextRunner contextRunner() {
        return new ApplicationContextRunner().withConfiguration(
                AutoConfigurations.of(TrailsSeleniumGridAutoConfiguration.class));
    }

}
