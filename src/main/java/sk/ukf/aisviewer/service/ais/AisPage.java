package sk.ukf.aisviewer.service.ais;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import sk.ukf.aisviewer.AppLog;

import java.time.Duration;
import java.util.logging.Logger;

/**
 * Spoločný základ pre jednotlivé stránky AIS2 (predmety, skúšky, rozvrh, prihlásenie).
 * Drží prehliadač z {@link AisSession} a pomocné metódy na čakanie a čítanie textu.
 */
abstract class AisPage {

    protected final WebDriver driver;
    protected final WebDriverWait wait;
    protected final Logger log = AppLog.get(getClass());

    protected AisPage(AisSession session) {
        this.driver = session.driver();
        this.wait = session.waitFor();
    }

    /**
     * Čaká, kým sa počet prvkov na stránke prestane meniť
     * (Angular vykresľuje predmety postupne). Vráti posledný zistený počet.
     */
    protected int waitForStableCount(By locator, Duration timeout,
                                   Duration quietPeriod, String label) {
        long start = System.currentTimeMillis();
        long end = start + timeout.toMillis();
        long emptyQuietMillis = Math.max(quietPeriod.toMillis(), 5000);
        int lastCount = -1;
        long stableSince = start;
        StringBuilder history = new StringBuilder();

        while (System.currentTimeMillis() < end) {
            int count;
            try {
                count = driver.findElements(locator).size();
            } catch (WebDriverException e) {
                count = lastCount; // stránka sa práve prekresľuje
            }
            long now = System.currentTimeMillis();
            if (count != lastCount) {
                if (history.length() > 0) history.append(" → ");
                history.append(count);
                lastCount = count;
                stableSince = now;
            } else {
                long needed = count > 0 ? quietPeriod.toMillis() : emptyQuietMillis;
                if (now - stableSince >= needed) {
                    log.fine("Čakanie: " + label + ": " + history
                            + " (ustálené po " + (now - start) + " ms)");
                    return count;
                }
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        log.info("Čakanie: " + label + ": " + history
                + " (timeout po " + (System.currentTimeMillis() - start) + " ms)");
        return Math.max(lastCount, 0);
    }

    protected String safeGetText(WebElement parent, String cssSelector) {
        try {
            WebElement el = parent.findElement(By.cssSelector(cssSelector));
            return el.getText().trim();
        } catch (NoSuchElementException | StaleElementReferenceException e) {
            return "";
        }
    }

    protected String extractParam(String url, String param) {
        int idx = url.indexOf(param + "=");
        if (idx < 0) return "";
        int start = idx + param.length() + 1;
        int end = url.indexOf("&", start);
        if (end < 0) end = url.length();
        return url.substring(start, end);
    }
}
