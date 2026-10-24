package io.github.yaforster.trails.adapter.seleniumgrid.browser;

import io.github.yaforster.trails.adapter.seleniumgrid.browser.download.SeleniumManagedDownloadStore;
import io.github.yaforster.trails.core.test.BrowserElementText;
import io.github.yaforster.trails.core.test.BrowserSession;
import io.github.yaforster.trails.core.test.FileData;
import io.github.yaforster.trails.core.test.action.browser.viewport.ViewportMove;
import io.github.yaforster.trails.core.test.action.browser.viewport.ViewportMoveDirection;
import io.github.yaforster.trails.core.test.action.browser.viewport.ViewportMoveUnit;
import io.github.yaforster.trails.core.test.action.browser.ViewportCoordinates;
import io.github.yaforster.trails.core.test.action.browser.viewport.ViewportDimensions;
import io.github.yaforster.trails.core.test.action.browser.viewport.ViewportResizeOutcome;
import io.github.yaforster.trails.core.test.action.element.ElementValueSource;
import io.github.yaforster.trails.core.test.action.element.Locator;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Interaction;
import org.openqa.selenium.interactions.Interactive;
import org.openqa.selenium.interactions.Sequence;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Adapts a Selenium WebDriver session and its managed downloads to the Trails browser port.
 */
public class SeleniumBrowserSession implements BrowserSession {

    private static final String SCROLL_TO_ELEMENT = """
            var viewPortHeight = Math.max(document.documentElement.clientHeight, window.innerHeight || 0);
            var elementTop = arguments[0].getBoundingClientRect().top;
            window.scrollBy(0, elementTop-(viewPortHeight/2));
            """;
    private static final String RENDER_HTML = """
            const proofDocument = new DOMParser().parseFromString(arguments[0], 'text/html');
            const headNodes = Array.from(proofDocument.head.childNodes).map(node => document.importNode(node, true));
            const bodyNodes = Array.from(proofDocument.body.childNodes).map(node => document.importNode(node, true));
            document.documentElement.lang = proofDocument.documentElement.lang;
            document.head.replaceChildren(...headNodes);
            document.body.replaceChildren(...bodyNodes);
            const proofImage = document.querySelector('main img');
            window.scrollTo(0, (proofImage ? proofImage.offsetTop : 0) + arguments[1]);
            """;
    private final WebDriver driver;
    private final SeleniumManagedDownloadStore downloads;
    private final SeleniumViewportOperations viewportOperations;
    private final SeleniumElementInteractions elements;

    /**
     * Creates a browser session backed by one managed-download-capable WebDriver.
     *
     * @param driver
     *            Selenium session that supports managed downloads
     * @param downloadTimeoutMillis
     *            positive upper bound for waiting on stable downloads
     */
    public SeleniumBrowserSession(WebDriver driver, int downloadTimeoutMillis) {
        this.driver = driver;
        this.downloads = new SeleniumManagedDownloadStore(driver, downloadTimeoutMillis);
        this.viewportOperations = new SeleniumViewportOperations(driver);
        this.elements = new SeleniumElementInteractions(driver);
    }

    @Override
    public void open(String url) {
        driver.navigate()
              .to(url);
    }

    @Override
    public String captureScreenshot() {
        return ((TakesScreenshot) driver).getScreenshotAs(OutputType.BASE64);
    }

    @Override
    public String captureScreenshot(Locator locator) {
        centerElementIfPresent(locator);
        return captureScreenshot();
    }

    private void centerElementIfPresent(Locator locator) {
        try {
            elements.findIfPresent(locator)
                    .ifPresent(element -> javascript().executeScript(SCROLL_TO_ELEMENT, element));
        } catch (StaleElementReferenceException _) {
            // A disappearing element must not prevent a screenshot of the remaining page.
        }
    }

    @Override
    public boolean elementExists(Locator locator) {
        return elements.exists(locator);
    }

    @Override
    public String click(Locator locator) {
        return elements.click(locator);
    }

    @Override
    public void click(ViewportCoordinates coordinates) {
        Sequence click = coordinateClick(coordinates);
        List<Sequence> actions = List.of(click);
        Interactive interactiveDriver = (Interactive) driver;
        interactiveDriver.perform(actions);
    }

    private Sequence coordinateClick(ViewportCoordinates coordinates) {
        PointerInput pointer = new PointerInput(PointerInput.Kind.MOUSE, "coordinate-click");
        // Selenium describes a real mouse click as movement, press, then release.
        // Viewport origin keeps the coordinates relative to the visible page, not an element.
        PointerInput.Origin viewportOrigin = PointerInput.Origin.viewport();
        int x = coordinates.xCoordinate();
        int y = coordinates.yCoordinate();
        int leftButton = PointerInput.MouseButton.LEFT.asArg();
        Interaction moveToPosition = pointer.createPointerMove(Duration.ZERO, viewportOrigin, x, y);
        Interaction pressButton = pointer.createPointerDown(leftButton);
        Interaction releaseButton = pointer.createPointerUp(leftButton);
        Sequence click = new Sequence(pointer, 0);
        click.addAction(moveToPosition);
        click.addAction(pressButton);
        click.addAction(releaseButton);
        return click;
    }

    @Override
    public ViewportResizeOutcome resizeViewport(ViewportDimensions dimensions) {
        return viewportOperations.resize(dimensions);
    }

    @Override
    public String type(Locator locator, String value, boolean clearBeforeTyping, long delayAfterClearMillis) {
        return elements.type(locator, value, clearBeforeTyping, delayAfterClearMillis);
    }

    @Override
    public BrowserElementText readText(Locator locator) {
        return elements.readText(locator);
    }

    @Override
    public String readValue(Locator locator, ElementValueSource valueSource, String valueName) {
        return elements.readValue(locator, valueSource, valueName);
    }

    @Override
    public String select(Locator locator, String label) {
        return elements.select(locator, label);
    }

    @Override
    public Optional<String> cookieValue(String name) {
        Cookie cookie = driver.manage()
                              .getCookieNamed(name);
        return Optional.ofNullable(cookie)
                       .map(Cookie::getValue);
    }

    @Override
    public String localStorageValue(String key) {
        return (String) javascript().executeScript("return window.localStorage.getItem(arguments[0]);", key);
    }

    @Override
    public String sessionStorageValue(String key) {
        return (String) javascript().executeScript("return window.sessionStorage.getItem(arguments[0]);", key);
    }

    @Override
    public void moveViewport(ViewportMove movement, ViewportMoveDirection direction, double amount,
            ViewportMoveUnit unit) {
        viewportOperations.move(movement, direction, amount, unit);
    }

    @Override
    public void renderHtml(String html, int scrollTop) {
        // Display the generated document proof in the current browser tab. Its text
        // position is relative to the page image, so the script also includes the
        // space taken by the proof header before scrolling to the match.
        javascript().executeScript(RENDER_HTML, html, scrollTop);
    }

    @Override
    public List<FileData> refreshDownloadedFiles() {
        return downloads.downloadedFiles();
    }

    @Override
    public void clearDownloadedFiles() {
        downloads.clear();
    }

    @Override
    public void close() {
        driver.quit();
    }

    private JavascriptExecutor javascript() {
        return (JavascriptExecutor) driver;
    }

}
