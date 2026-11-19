package sk.ukf.aisviewer.service.ais;

import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import sk.ukf.aisviewer.model.StudentInfo;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static sk.ukf.aisviewer.service.ais.AisSession.START_URL;
import static sk.ukf.aisviewer.service.ais.AisSession.STUDENT_HOME_URL;

/** Prihlásenie do AIS2 a zistenie zápisných listov študenta. */
public class LoginPage extends AisPage {

    private static final String ENROLLMENT_LINK_TEXT = "Moje predmety v";
    private static final String USER_NAME_ATTRIBUTE = "ng-reflect-user-name=\"";

    public LoginPage(AisSession session) {
        super(session);
    }

    /** Vyplní prihlasovací formulár. Vráti false, ak AIS hlási nesprávne meno alebo heslo. */
    public boolean submit(String username, String password) {
        driver.get(START_URL);

        WebElement loginField = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("login")));
        loginField.clear();
        loginField.sendKeys(username);

        WebElement passField = driver.findElement(By.id("heslo"));
        passField.clear();
        passField.sendKeys(password);

        driver.findElement(By.id("login-form-submit-btn")).click();

        // Čaká, kým AIS presmeruje preč z prihlasovacej stránky alebo zobrazí chybu.
        try {
            wait.until(d -> !d.getCurrentUrl().contains("start.do") || isInvalidCredentialsPage());
        } catch (TimeoutException e) {
            log.info("Po prihlásení sa stránka nezmenila do 20 s.");
        }
        log.fine("URL po login-e: " + driver.getCurrentUrl() + ", title: " + driver.getTitle());
        return !isInvalidCredentialsPage();
    }

    /** Otvorí domovskú stránku študenta a prečíta jeho meno (ak sa nenájde, použije AIS ID). */
    public StudentInfo openStudentHome(String username) {
        driver.get(STUDENT_HOME_URL);
        waitForEnrollmentLinks();
        log.fine("Študentská stránka URL: " + driver.getCurrentUrl() + ", title: " + driver.getTitle());

        StudentInfo student = new StudentInfo();
        student.setFullName(username);
        student.setIdo(username);
        try {
            String src = driver.getPageSource();
            int idx = src.indexOf(USER_NAME_ATTRIBUTE);
            if (idx > 0) {
                int start = idx + USER_NAME_ATTRIBUTE.length();
                int end = src.indexOf('"', start);
                if (end > start) {
                    String name = src.substring(start, end).trim();
                    if (!name.isBlank()) {
                        student.setFullName(name);
                        log.info("Študent: " + name);
                    }
                }
            }
        } catch (Exception e) {
            log.fine("Meno študenta sa nepodarilo prečítať: " + e.getMessage());
        }
        return student;
    }

    /**
     * Postupne otvorí každý odkaz „Moje predmety v …“ a z URL prečíta id zápisného listu (zl=…).
     * Nové zápisné listy pridá na koniec zoznamu študenta (už známe sa nepridajú druhýkrát).
     */
    public void discoverEnrollments(StudentInfo student) {
        List<WebElement> links = findEnrollmentLinks();
        for (WebElement link : links) {
            try {
                log.fine("Zápisný list element: text=\"" + link.getText().trim() + "\"");
            } catch (StaleElementReferenceException ignored) {
                // odkaz sa medzičasom prekreslil
            }
        }
        log.info("Relevantných odkazov: " + links.size());

        int count = links.size();
        for (int i = 0; i < count; i++) {
            try {
                if (i > 0) {
                    driver.get(STUDENT_HOME_URL);
                    waitForEnrollmentLinks();
                    links = findEnrollmentLinks();
                    if (i >= links.size()) {
                        break;
                    }
                }

                WebElement link = links.get(i);
                String linkText = link.getText().trim();
                link.click();
                try {
                    wait.until(d -> d.getCurrentUrl().contains("zl="));
                } catch (TimeoutException e) {
                    log.info("Zápisný list sa neotvoril (URL bez zl=).");
                }
                String newUrl = driver.getCurrentUrl();
                log.fine("URL po otvorení zápisného listu: " + newUrl);

                String zl = newUrl.contains("zl=") ? extractParam(newUrl, "zl") : "";
                if (!zl.isBlank() && !student.getEnrollmentListIds().contains(zl)) {
                    String displayName = linkText
                            .replace("open_in_new", "")
                            .replace(ENROLLMENT_LINK_TEXT + " ", "")
                            .trim();
                    student.getEnrollmentListIds().add(zl);
                    student.getEnrollmentListNames().add(displayName);
                    if (student.getEnrollmentListId() == null) {
                        student.setEnrollmentListId(zl);
                    }
                    log.info("Zápisný list: " + displayName + " (zl=" + zl + ")");
                }
            } catch (Exception e) {
                log.warning("Chyba pri hľadaní zápisného listu: " + e.getMessage());
            }
        }
    }

    private List<WebElement> findEnrollmentLinks() {
        List<WebElement> result = new ArrayList<>();
        for (WebElement link : driver.findElements(By.tagName("a"))) {
            try {
                if (link.getText().trim().contains(ENROLLMENT_LINK_TEXT)) {
                    result.add(link);
                }
            } catch (StaleElementReferenceException ignored) {
                // odkaz sa medzičasom prekreslil
            }
        }
        return result;
    }

    /** Odkazy "Moje predmety v …" na domovskej stránke – Angular ich dokresľuje postupne. */
    private void waitForEnrollmentLinks() {
        waitForStableCount(By.xpath("//a[contains(normalize-space(.), '" + ENROLLMENT_LINK_TEXT + "')]"),
                Duration.ofSeconds(20), Duration.ofMillis(1000), "zápisné listy");
    }

    private boolean isInvalidCredentialsPage() {
        List<WebElement> errorElements = driver.findElements(By.cssSelector(".login-error"));
        return !errorElements.isEmpty()
                && !errorElements.get(0).getText().trim().isEmpty();
    }
}
