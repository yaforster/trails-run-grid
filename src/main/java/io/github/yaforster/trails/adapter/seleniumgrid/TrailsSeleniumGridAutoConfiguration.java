package io.github.yaforster.trails.adapter.seleniumgrid;

import io.github.yaforster.trails.app.services.CapabilityContribution;
import io.github.yaforster.trails.app.services.CapabilityContributor;
import io.github.yaforster.trails.app.services.WebdriverConfigurationFactory;
import io.github.yaforster.trails.core.CapabilityLabel;
import io.github.yaforster.trails.core.test.Browser;
import io.github.yaforster.trails.core.test.DriverProvider;
import io.github.yaforster.trails.sdk.composition.AdapterId;
import io.github.yaforster.trails.sdk.composition.TrailsBrowserRegistration;
import io.github.yaforster.trails.sdk.composition.TrailsComposition;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Contributes the complete Selenium Grid browser capability as one Trails registration.
 */
@AutoConfiguration
@EnableConfigurationProperties(GridProperties.class)
public class TrailsSeleniumGridAutoConfiguration {

    /** Creates the Selenium Grid autoconfiguration. */
    public TrailsSeleniumGridAutoConfiguration() {}

    @Bean
    ResilientGridSessionFactory resilientGridSessionFactory(GridProperties properties) {
        return new ResilientGridSessionFactory(properties);
    }

    @Bean
    DriverProvider gridEdgeDriverProvider(GridProperties properties, ResilientGridSessionFactory sessions) {
        return new GridDriverProvider(Browser.EDGE, properties, sessions);
    }

    @Bean
    DriverProvider gridChromeDriverProvider(GridProperties properties, ResilientGridSessionFactory sessions) {
        return new GridDriverProvider(Browser.CHROME, properties, sessions);
    }

    @Bean
    DriverProvider gridFirefoxDriverProvider(GridProperties properties, ResilientGridSessionFactory sessions) {
        return new GridDriverProvider(Browser.FIREFOX, properties, sessions);
    }

    @Bean
    WebdriverConfigurationFactory gridWebdriverConfigurationFactory(ObjectProvider<TrailsComposition> composition,
            GridProperties properties, @Qualifier("gridEdgeDriverProvider") DriverProvider gridEdgeDriverProvider,
            @Qualifier("gridChromeDriverProvider") DriverProvider gridChromeDriverProvider,
            @Qualifier("gridFirefoxDriverProvider") DriverProvider gridFirefoxDriverProvider) {
        return new GridWebdriverConfigurationFactory(composition, properties, gridEdgeDriverProvider,
                gridChromeDriverProvider, gridFirefoxDriverProvider);
    }

    @Bean
    TrailsBrowserRegistration trailsSeleniumGridBrowserRegistration(
            @Qualifier("gridWebdriverConfigurationFactory") WebdriverConfigurationFactory gridWebdriverConfigurationFactory) {
        AdapterId adapterId = new AdapterId("selenium-grid");
        return new TrailsBrowserRegistration(adapterId, gridWebdriverConfigurationFactory);
    }

    @Bean
    CapabilityContributor seleniumGridFileDownloadCapabilityContributor(GridProperties properties) {
        return () -> {
            int timeoutMillis = properties.fileDownloadTimeoutMillis();
            String timeoutValue = String.valueOf(timeoutMillis);
            CapabilityContribution downloadTimeout = new CapabilityContribution(
                    CapabilityLabel.FILE_DOWNLOAD_TIMEOUT_MILLIS, timeoutValue);
            return List.of(downloadTimeout);
        };
    }

}
