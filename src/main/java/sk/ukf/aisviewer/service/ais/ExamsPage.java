package sk.ukf.aisviewer.service.ais;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import sk.ukf.aisviewer.model.Exam;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import static sk.ukf.aisviewer.service.ais.AisSession.PREDMETY_URL;

/** Stránka „Skúšky“ v AIS2 – termíny skúšok vrátane miestnosti z detailu termínu. */
public class ExamsPage extends AisPage {

    public ExamsPage(AisSession session) {
        super(session);
    }

    public List<Exam> fetchExams(String enrollmentListId) {
        List<Exam> exams = new ArrayList<>();
        if (driver == null || enrollmentListId == null || enrollmentListId.isBlank()) return exams;

        try {
            String url = PREDMETY_URL + "skusky?zl=" + enrollmentListId;
            log.info("Načítavam skúšky...");
            driver.get(url);
            waitForStableCount(By.cssSelector(".card-body .badge"),
                    Duration.ofSeconds(20), Duration.ofMillis(1500), "skúšky");

            Set<WebElement> termRows = new LinkedHashSet<>();
            for (WebElement badge : driver.findElements(By.cssSelector(".card-body .badge"))) {
                try {
                    String time = badge.getText().trim();
                    if (time.matches("\\d{1,2}:\\d{2}")) {
                        termRows.add(badge.findElement(By.xpath(
                                "./ancestor::div[contains(concat(' ', normalize-space(@class), ' '), ' row ')][1]")));
                    }
                } catch (NoSuchElementException | StaleElementReferenceException ignored) {}
            }

            for (WebElement termRow : termRows) {
                try {
                    String subjectText = safeGetText(termRow, ".fw-bold.ms-2.black");
                    if (subjectText.isBlank()) {
                        subjectText = safeGetText(termRow, ".col-12.col-md .fw-bold");
                    }
                    if (!subjectText.isBlank() && !subjectText.equalsIgnoreCase("predmet")) {
                        String[] subject = splitExamSubject(subjectText);
                        String time = safeGetText(termRow, ".badge");
                        String date = findExamDate(termRow);
                        String status = findExamStatus(termRow);
                        String room = fetchExamRoomFromDetail(termRow);
                        String type = safeGetText(termRow, ".small.font-italic");
                        if (type.contains(",")) {
                            type = type.substring(0, type.indexOf(',')).trim();
                        }

                        Exam exam = new Exam(subject[1], subject[0], date, time, room,
                                "", "", "", status);
                        exam.setType(type);
                        exams.add(exam);
                    }
                } catch (StaleElementReferenceException ignored) {}
            }

            log.info("Načítaných " + exams.size() + " skúšok");

        } catch (Exception e) {
            log.log(Level.FINE, "Detail chyby", e);
        }
        return exams;
    }

    private String findExamDate(WebElement termRow) {
        try {
            WebElement dateContainer = termRow.findElement(By.xpath(
                    "./ancestor::div[contains(concat(' ', normalize-space(@class), ' '), ' row ')"
                            + " and .//div[contains(concat(' ', normalize-space(@class), ' '), ' col-lg-2 ')]]"
                            + "//div[contains(concat(' ', normalize-space(@class), ' '), ' col-lg-2 ')]"));
            return dateContainer.getText().trim()
                    .replaceFirst("(?i)^(Po|Ut|St|Št|Pi|So|Ne)\\s+", "");
        } catch (NoSuchElementException | StaleElementReferenceException e) {
            return "";
        }
    }

    private String[] splitExamSubject(String subjectText) {
        int separator = subjectText.indexOf(" - ");
        if (separator < 0) {
            return new String[]{"", subjectText};
        }
        return new String[]{
                subjectText.substring(0, separator).trim(),
                subjectText.substring(separator + 3).trim()
        };
    }

    private String findExamStatus(WebElement termRow) {
        for (WebElement span : termRow.findElements(By.cssSelector("button span"))) {
            String text = span.getText().trim();
            if (!text.isBlank()) {
                return text;
            }
        }
        return "";
    }

    private String fetchExamRoomFromDetail(WebElement termRow) {
        By detailIcon = By.cssSelector("mat-icon[title='Detail termínu']");
        By dialogLocator = By.cssSelector("mat-dialog-container");
        By dialogContentLocator = By.cssSelector("mat-dialog-content");

        WebElement dialog = null;
        try {
            WebElement icon = termRow.findElement(detailIcon);
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].scrollIntoView({block: 'center'});", icon);
            icon.click();

            dialog = wait.until(driver -> {
                List<WebElement> dialogs = driver.findElements(dialogLocator);
                for (int i = dialogs.size() - 1; i >= 0; i--) {
                    WebElement candidate = dialogs.get(i);
                    try {
                        if (candidate.isDisplayed()
                                && !candidate.findElements(dialogContentLocator).isEmpty()) {
                            return candidate;
                        }
                    } catch (StaleElementReferenceException ignored) {
                    }
                }
                return null;
            });
            return waitForExamRoom(dialog, dialogContentLocator);
        } catch (NoSuchElementException | StaleElementReferenceException | TimeoutException e) {
            log.warning("Detail skúšky sa nepodarilo načítať: " + e.getMessage());
            return "";
        } finally {
            if (dialog == null) {
                for (WebElement candidate : driver.findElements(dialogLocator)) {
                    try {
                        if (candidate.isDisplayed()) {
                            dialog = candidate;
                            break;
                        }
                    } catch (StaleElementReferenceException ignored) {
                    }
                }
            }
            if (dialog != null) {
                try {
                    driver.findElement(By.tagName("body")).sendKeys(Keys.ESCAPE);
                    wait.until(ExpectedConditions.invisibilityOf(dialog));
                } catch (NoSuchElementException | StaleElementReferenceException | TimeoutException ignored) {
                    // The dialog may already have been closed by AIS.
                }
            }
        }
    }

    /**
     * Počká, kým sa v dialógu skúšky objaví miestnosť (napr. THB01140).
     * Ak sa do 4 sekúnd neobjaví, skúška miestnosť asi nemá – vráti "".
     */
    private String waitForExamRoom(WebElement dialog, By dialogContentLocator) {
        long start = System.currentTimeMillis();
        try {
            String room = new WebDriverWait(driver, Duration.ofSeconds(4))
                    .pollingEvery(Duration.ofMillis(200))
                    .ignoring(StaleElementReferenceException.class)
                    .until(d -> {
                        String value = extractRoomFromExamDialog(dialog, dialogContentLocator);
                        return value.isEmpty() ? null : value;
                    });
            log.fine("Čakanie: miestnosť skúšky: " + room
                    + " (po " + (System.currentTimeMillis() - start) + " ms)");
            return room;
        } catch (TimeoutException e) {
            log.info("Čakanie: miestnosť skúšky: nenájdená (timeout po "
                    + (System.currentTimeMillis() - start) + " ms)");
            return "";
        }
    }

    private String extractRoomFromExamDialog(WebElement dialog, By dialogContentLocator) {
        for (WebElement content : dialog.findElements(dialogContentLocator)) {
            for (WebElement item : content.findElements(By.tagName("li"))) {
                String value = item.getText().trim().replaceAll("\\s+", "");
                if (value.matches("[A-Z]{2,4}\\d{5}")) {
                    return value;
                }
            }
        }
        return "";
    }
}
