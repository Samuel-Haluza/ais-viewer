package sk.ukf.aisviewer.service.ais;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/** Jeden spustený (neviditeľný) Chrome prehliadač prihlásený do AIS2. */
public final class AisSession implements AutoCloseable {

    public static final String BASE_URL = "https://ais2.ukf.sk";
    public static final String START_URL = BASE_URL + "/ais/start.do";
    public static final String STUDENT_HOME_URL = BASE_URL + "/ais/apps/student/sk/";
    public static final String PREDMETY_URL = BASE_URL + "/ais/apps/student-predmety/sk/";

    private static final Duration DEFAULT_WAIT = Duration.ofSeconds(20);

    private WebDriver driver;
    private final WebDriverWait wait;

    private AisSession(WebDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, DEFAULT_WAIT);
    }

    /** Stiahne ovládač Chrome (ak treba) a spustí prehliadač bez okna. */
    public static AisSession open() {
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--lang=sk");
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
                + "(KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36");
        return new AisSession(new ChromeDriver(options));
    }

    WebDriver driver() {
        return driver;
    }

    WebDriverWait waitFor() {
        return wait;
    }

    public boolean isOpen() {
        return driver != null;
    }

    @Override
    public void close() {
        if (driver != null) {
            try {
                driver.quit();
            } catch (Exception ignored) {
                // prehliadač už mohol byť zatvorený
            }
            driver = null;
        }
    }
}
