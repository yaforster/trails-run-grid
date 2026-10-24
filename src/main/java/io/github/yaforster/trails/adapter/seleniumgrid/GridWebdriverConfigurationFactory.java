package io.github.yaforster.trails.adapter.seleniumgrid;

import io.github.yaforster.trails.adapter.seleniumgrid.execution.TestExecutionDataFactory;
import io.github.yaforster.trails.app.services.WebdriverConfigurationFactory;
import io.github.yaforster.trails.app.services.TestPlanDatabaseService;
import io.github.yaforster.trails.app.services.StageDatabaseService;
import io.github.yaforster.trails.core.test.DriverProvider;
import io.github.yaforster.trails.core.test.TestExecutionData;
import io.github.yaforster.trails.core.test.TestPlanRunDefinition;
import io.github.yaforster.trails.sdk.composition.TrailsComposition;
import io.github.yaforster.trails.sdk.composition.TrailsPersistenceRegistration;
import java.net.URI;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;

/**
 * Defers persistence-port resolution until execution is requested, allowing the SDK to validate the complete adapter
 * composition before its selected persistence services are used to prepare a Grid run.
 */
final class GridWebdriverConfigurationFactory implements WebdriverConfigurationFactory {

    private final ObjectProvider<TrailsComposition> composition;
    private final GridProperties properties;
    private final DriverProvider edge;
    private final DriverProvider chrome;
    private final DriverProvider firefox;

    GridWebdriverConfigurationFactory(ObjectProvider<TrailsComposition> composition, GridProperties properties,
            DriverProvider edge, DriverProvider chrome, DriverProvider firefox) {
        this.composition = composition;
        this.properties = properties;
        this.edge = edge;
        this.chrome = chrome;
        this.firefox = firefox;
    }

    @Override
    public TestExecutionData getTestExecutionData(TestPlanRunDefinition definition) {
        TestExecutionDataFactory factory = executionDataFactory();
        return factory.executionData(definition);
    }

    private TestExecutionDataFactory executionDataFactory() {
        // Resolve lazily: the SDK must validate the complete adapter composition before
        // an execution can use its selected persistence services.
        TrailsComposition readyComposition = composition.getObject();
        TrailsPersistenceRegistration persistence = readyComposition.persistence();
        TrailsPersistenceRegistration.Ports ports = persistence.ports();
        TestPlanDatabaseService testPlans = ports.testPlanDatabaseService();
        StageDatabaseService stages = ports.stageDatabaseService();
        URI configuredBrowserBaseUrl = properties.browserBaseUrl();
        Optional<URI> browserBaseUrl = Optional.ofNullable(configuredBrowserBaseUrl);
        return new TestExecutionDataFactory(testPlans, stages, browserBaseUrl, edge, chrome, firefox);
    }

}
