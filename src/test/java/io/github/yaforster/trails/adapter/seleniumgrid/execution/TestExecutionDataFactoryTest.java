package io.github.yaforster.trails.adapter.seleniumgrid.execution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.yaforster.trails.app.services.StageDatabaseService;
import io.github.yaforster.trails.app.services.TestPlanDatabaseService;
import io.github.yaforster.trails.core.data.TestPlan;
import io.github.yaforster.trails.core.persisted.PersistedStage;
import io.github.yaforster.trails.core.test.Browser;
import io.github.yaforster.trails.core.test.DriverProvider;
import io.github.yaforster.trails.core.test.TestExecutionData;
import io.github.yaforster.trails.core.test.TestPlanRunDefinition;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TestExecutionDataFactoryTest {

    @Test
    void shouldRewriteLoopbackStageForTheConfiguredBrowserNetwork() {
        TestPlanDatabaseService plans = mock();
        StageDatabaseService stages = mock();
        TestPlan plan = new TestPlan(34L, 12L, 23L, "Checkout", List.of());
        when(plans.getExecutableTestPlan(34L)).thenReturn(Optional.of(plan));
        when(stages.getStage(new StageDatabaseService.StageDetails(12L, 23L, true))).thenReturn(
                Optional.of(new PersistedStage(23L, 12L, "Local", "http://localhost:8080/shop?cart=1#pay")));

        TestExecutionData execution = factory(plans, stages,
                Optional.of(URI.create("http://trails-server:8080"))).executionData(
                        new TestPlanRunDefinition(34L, List.of(Browser.CHROME)));

        assertThat(execution.webDrivers()
                            .getFirst()
                            .webAppURL()
                            .toString()).isEqualTo("http://trails-server:8080/shop?cart=1#pay");
    }

    @Test
    void shouldKeepAnExternalStageWhenABrowserBaseUrlIsConfigured() {
        TestPlanDatabaseService plans = mock();
        StageDatabaseService stages = mock();
        TestPlan plan = new TestPlan(34L, 12L, 23L, "Checkout", List.of());
        when(plans.getExecutableTestPlan(34L)).thenReturn(Optional.of(plan));
        when(stages.getStage(new StageDatabaseService.StageDetails(12L, 23L, true))).thenReturn(
                Optional.of(new PersistedStage(23L, 12L, "External", "https://example.test/shop")));

        TestExecutionData execution = factory(plans, stages,
                Optional.of(URI.create("http://trails-server:8080"))).executionData(
                        new TestPlanRunDefinition(34L, List.of(Browser.FIREFOX)));

        assertThat(execution.webDrivers()
                            .getFirst()
                            .webAppURL()
                            .toString()).isEqualTo("https://example.test/shop");
    }

    private TestExecutionDataFactory factory(TestPlanDatabaseService plans, StageDatabaseService stages,
            Optional<URI> browserBaseUrl) {
        DriverProvider unavailable = () -> Optional.empty();
        return new TestExecutionDataFactory(plans, stages, browserBaseUrl, unavailable, unavailable, unavailable);
    }

}
