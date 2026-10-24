package io.github.yaforster.trails.adapter.seleniumgrid.execution;

import io.github.yaforster.trails.app.services.StageDatabaseService;
import io.github.yaforster.trails.app.services.TestPlanDatabaseService;
import io.github.yaforster.trails.core.EntityMissingException;
import io.github.yaforster.trails.core.data.TestPlan;
import io.github.yaforster.trails.core.persisted.PersistedStage;
import io.github.yaforster.trails.core.test.Browser;
import io.github.yaforster.trails.core.test.Action;
import io.github.yaforster.trails.core.test.DriverProvider;
import io.github.yaforster.trails.core.test.TestExecutionData;
import io.github.yaforster.trails.core.test.TestPlanRunDefinition;
import io.github.yaforster.trails.core.test.WebDriverData;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Assembles execution input with the remote browser's network perspective: localhost on a Grid node is not the Trails
 * host. Applies the configured loopback-origin replacement without changing the persisted stage URL.
 */
public final class TestExecutionDataFactory {

    private final TestPlanDatabaseService testPlans;
    private final StageDatabaseService stages;
    private final Optional<URI> browserBaseUri;
    private final Map<Browser, DriverProvider> drivers;

    /**
     * Creates an execution-data factory backed by the already selected persistence ports.
     *
     * @param testPlans
     *            executable test-plan port
     * @param stages
     *            stage lookup port
     * @param browserBaseUri
     *            optional browser-network base URI for loopback stage URLs
     * @param edge
     *            Edge driver provider
     * @param chrome
     *            Chrome driver provider
     * @param firefox
     *            Firefox driver provider
     */
    public TestExecutionDataFactory(TestPlanDatabaseService testPlans, StageDatabaseService stages,
            Optional<URI> browserBaseUri, DriverProvider edge, DriverProvider chrome, DriverProvider firefox) {
        this.testPlans = testPlans;
        this.stages = stages;
        this.browserBaseUri = browserBaseUri;
        this.drivers = registerBrowserProviders(edge, chrome, firefox);
    }

    /**
     * Resolves an executable plan, its stage, and each requested browser driver.
     *
     * @param definition
     *            requested plan and browsers
     * @return complete input for the SDK execution host
     */
    public TestExecutionData executionData(TestPlanRunDefinition definition) {
        Long requestedPlanId = definition.testPlanID();
        TestPlan plan = requireExecutablePlan(requestedPlanId);
        Long applicationId = plan.getApplicationID();
        Long stageId = plan.getStageID();
        URL applicationUrl = resolveBrowserStageUrl(applicationId, stageId);
        List<Browser> requestedBrowsers = definition.browsersToTest();
        List<WebDriverData> webDrivers = configureRequestedBrowsers(requestedBrowsers, applicationUrl);
        List<Action> actions = plan.getActions();
        Long planId = plan.getId();
        String planLabel = plan.getLabel();
        return new TestExecutionData(webDrivers, actions, applicationId, stageId, planId, planLabel);
    }

    private List<WebDriverData> configureRequestedBrowsers(List<Browser> browsers, URL applicationUrl) {
        return browsers.stream()
                       .map(browser -> {
                           DriverProvider provider = drivers.get(browser);
                           return new WebDriverData(provider, applicationUrl, browser);
                       })
                       .toList();
    }

    private TestPlan requireExecutablePlan(Long testPlanId) {
        return testPlans.getExecutableTestPlan(testPlanId)
                        .orElseThrow(() -> new EntityMissingException(
                                "Test plan with id " + testPlanId + " could not be found."));
    }

    private URL resolveBrowserStageUrl(Long applicationId, Long stageId) {
        StageDatabaseService.StageDetails stageDetails = new StageDatabaseService.StageDetails(applicationId, stageId,
                true);
        PersistedStage stage = stages.getStage(stageDetails)
                                     .orElseThrow(() -> new EntityMissingException(
                                             "Stage with id " + stageId + " could not be found."));
        String stageUrl = stage.url();
        URI stageUri = URI.create(stageUrl);
        URI browserUri = replaceLoopbackOrigin(stageUri);
        return toStageUrl(browserUri);
    }

    private URI replaceLoopbackOrigin(URI stageUri) {
        if (browserBaseUri.isEmpty() || !isLoopback(stageUri)) {
            return stageUri;
        }
        URI base = browserBaseUri.get();
        // A remote browser's localhost points to its own machine. Replace only the
        // origin and preserve encoded path, query and fragment from the persisted URL.
        String replacementOrigin = base.getScheme() + "://" + base.getRawAuthority();
        String stagePath = stageUri.getRawPath() == null ? "" : stageUri.getRawPath();
        String stageQuery = stageUri.getRawQuery() == null ? "" : "?" + stageUri.getRawQuery();
        String stageFragment = stageUri.getRawFragment() == null ? "" : "#" + stageUri.getRawFragment();
        String stageLocation = stagePath + stageQuery + stageFragment;
        URI browserOrigin = URI.create(replacementOrigin);
        return browserOrigin.resolve(stageLocation);
    }

    private boolean isLoopback(URI uri) {
        String host = uri.getHost();
        return "localhost".equalsIgnoreCase(host) || "127.0.0.1".equals(host) || "::1".equals(host);
    }

    private URL toStageUrl(URI uri) {
        try {
            return uri.toURL();
        } catch (MalformedURLException exception) {
            throw new IllegalArgumentException("The persisted stage URL is invalid.", exception);
        }
    }

    private Map<Browser, DriverProvider> registerBrowserProviders(DriverProvider edge, DriverProvider chrome,
            DriverProvider firefox) {
        EnumMap<Browser, DriverProvider> configured = new EnumMap<>(Browser.class);
        configured.put(Browser.EDGE, edge);
        configured.put(Browser.CHROME, chrome);
        configured.put(Browser.FIREFOX, firefox);
        return Map.copyOf(configured);
    }

}
