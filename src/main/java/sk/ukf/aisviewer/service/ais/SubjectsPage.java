package sk.ukf.aisviewer.service.ais;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import sk.ukf.aisviewer.model.Subject;
import sk.ukf.aisviewer.model.SubjectCategory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import static sk.ukf.aisviewer.service.ais.AisSession.PREDMETY_URL;

/** Stránka „Moje predmety“ v AIS2 – predmety jedného zápisného listu. */
public class SubjectsPage extends AisPage {

    public SubjectsPage(AisSession session) {
        super(session);
    }

    public List<Subject> fetchSubjects(String enrollmentListId) {
        List<Subject> subjects = new ArrayList<>();

        if (driver == null || enrollmentListId == null || enrollmentListId.isBlank()) {
            return subjects;
        }

        try {
            String url = PREDMETY_URL + "?zl=" + enrollmentListId;
            log.info("Načítavam predmety (zl=" + enrollmentListId + ")...");
            driver.get(url);
            waitForStableCount(By.cssSelector("mat-card, table tr"),
                    Duration.ofSeconds(20), Duration.ofMillis(1500), "predmety");

            // Stratégia 1: Tabuľka
            subjects = parseSubjectsFromTableSelenium();
            log.fine("Subjects parser 1: parseSubjectsFromTableSelenium() -> "
                    + subjects.size() + " položiek");
            if (!subjects.isEmpty()) {
                log.info("Načítaných " + subjects.size() + " predmetov (tabuľka)");
                return subjects;
            }

            // Stratégia 2: Sekvenčné parsovanie – kategórie + karty
            subjects = parseSubjectsSequential();
            log.fine("Subjects parser 2: parseSubjectsSequential() -> "
                    + subjects.size() + " položiek");
            if (!subjects.isEmpty()) {
                log.info("Načítaných " + subjects.size() + " predmetov (sekvenčne)");
                return subjects;
            }

            // Stratégia 3: Desktop rows
            subjects = parseSubjectsFromDesktopRows();
            log.fine("Subjects parser 3: parseSubjectsFromDesktopRows() -> "
                    + subjects.size() + " položiek");
            if (!subjects.isEmpty()) {
                log.info("Načítaných " + subjects.size() + " predmetov (desktop rows)");
                return subjects;
            }

            log.info("Žiadne predmety nenájdené!");

        } catch (Exception e) {
            log.warning("Chyba pri načítaní predmetov: " + e.getMessage());
            log.log(Level.FINE, "Detail chyby", e);
        }

        return subjects;
    }

    /**
     * Hlavná stratégia – JavaScript zistí poradie kategórií a kariet na stránke.
     * Prechádza DOM sekvenčne, takže kategórie sa správne priradzujú.
     */
    private List<Subject> parseSubjectsSequential() {
        List<Subject> subjects = new ArrayList<>();
        try {
            // JavaScript prejde celý DOM sekvenčne a vráti JSON s kategóriami a predmetmi
            JavascriptExecutor js = (JavascriptExecutor) driver;
            Object result = js.executeScript(
                    // arguments[0..2] = názvy kategórií z enumu SubjectCategory
                    "var results = [];" +
                            "var MANDATORY = arguments[0], COMPULSORY_ELECTIVE = arguments[1], ELECTIVE = arguments[2];" +
                            "var category = MANDATORY;" +
                            // Nájdi v��etky elementy na stránke
                            "var allElements = document.querySelectorAll('*');" +
                            "for (var i = 0; i < allElements.length; i++) {" +
                            "  var el = allElements[i];" +
                            "  var text = (el.textContent || '').trim();" +
                            "  var tag = el.tagName.toLowerCase();" +
                            "  var cls = el.getAttribute('class') || '';" +
                            // Detekcia kategórie – hľadaj h2, h3, h4, alebo div s textom kategórie
                            "  if ((tag === 'h2' || tag === 'h3' || tag === 'h4' || tag === 'h5'" +
                            "       || cls.indexOf('typ-vyucby') >= 0 || cls.indexOf('studijna-cast') >= 0" +
                            "       || cls.indexOf('category') >= 0 || cls.indexOf('header') >= 0)" +
                            "      && text.length < 100 && text.length > 3) {" +
                            "    var lower = text.toLowerCase();" +
                            "    if ((lower.indexOf('povinne') >= 0 || lower.indexOf('povinné') >= 0)" +
                            "        && (lower.indexOf('voliteľ') >= 0 || lower.indexOf('volitel') >= 0)) {" +
                            "      category = COMPULSORY_ELECTIVE;" +
                            "    } else if (lower.indexOf('výberové predmety') >= 0" +
                            "        || lower.indexOf('vyberove predmety') >= 0" +
                            "        || lower.indexOf('voliteľné predmety') >= 0" +
                            "        || lower.indexOf('volitelne predmety') >= 0) {" +
                            "      category = ELECTIVE;" +
                            "    } else if (lower.indexOf('povinné') >= 0 || lower.indexOf('povinne') >= 0) {" +
                            "      if (lower.indexOf('voliteľ') < 0 && lower.indexOf('volitel') < 0) {" +
                            "        category = MANDATORY;" +
                            "      }" +
                            "    }" +
                            "  }" +
                            // Detekcia predmetu – mat-card elementy
                            "  if (tag === 'mat-card' && text.length > 10) {" +
                            "    var nameEl = el.querySelector('b, strong, .fw-bold, .font-weight-bold');" +
                            "    if (!nameEl) continue;" +
                            "    var name = nameEl.textContent.trim();" +
                            "    if (!name || name.length < 3) continue;" +
                            // Preskočí ak to vyzerá ako kategória
                            "    var nameLower = name.toLowerCase();" +
                            "    if (nameLower.indexOf('povinné') >= 0 || nameLower.indexOf('povinne') >= 0" +
                            "        || nameLower.indexOf('výberové predmety') >= 0" +
                            "        || nameLower.indexOf('vyberove predmety') >= 0" +
                            "        || nameLower.indexOf('voliteľné predmety') >= 0" +
                            "        || nameLower.indexOf('volitelne predmety') >= 0) continue;" +
                            // Kód predmetu
                            "    var codeEl = el.querySelector('.grey, .text-muted');" +
                            "    var code = codeEl ? codeEl.textContent.trim().split(' ')[0] : '-';" +
                            // Badge-y
                            "    var credits = '-', semester = '-', gradeType = '-';" +
                            "    var badges = el.querySelectorAll('.badge b, .badge');" +
                            "    for (var j = 0; j < badges.length; j++) {" +
                            "      var bt = badges[j].textContent.trim();" +
                            "      if (bt.endsWith('K') || bt.endsWith('k')) credits = bt;" +
                            "      else if (bt === 'ZS' || bt === 'LS') semester = bt;" +
                            "      else if (bt === 'S' || bt === 'PH' || bt === 'A') gradeType = bt;" +
                            "    }" +
                            // Známka
                            "    var gradeEl = el.querySelector('.col-3 b');" +
                            "    var grade = gradeEl ? gradeEl.textContent.trim() : '-';" +
                            "    if (!grade) grade = '-';" +
                            // Vyučujúci
                            "    var teacherEl = el.querySelector('.col-3 .text-black-50');" +
                            "    var teacher = teacherEl ? teacherEl.textContent.trim() : '-';" +
                            // Ulož
                            "    results.push(category + '|||' + code + '|||' + name + '|||' + credits + '|||'" +
                            "                 + semester + '|||' + gradeType + '|||' + grade + '|||' + teacher);" +
                            "  }" +
                            "}" +
                            "return results;",
                    SubjectCategory.MANDATORY.displayName(),
                    SubjectCategory.COMPULSORY_ELECTIVE.displayName(),
                    SubjectCategory.ELECTIVE.displayName()
            );

            if (result instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> rows = (List<String>) result;
                log.warning("JS našiel " + rows.size() + " predmetov");
                for (String row : rows) {
                    String[] parts = row.split("\\|\\|\\|", -1);
                    if (parts.length >= 8) {
                        String category = parts[0].trim();
                        String code = parts[1].trim();
                        String name = parts[2].trim();
                        String credits = parts[3].trim();
                        String semester = parts[4].trim();
                        String gradeType = parts[5].trim();
                        String grade = parts[6].trim();
                        String teacher = parts[7].trim();

                        if (!name.isBlank()) {
                            subjects.add(new Subject(name, code, credits, semester, gradeType,
                                    grade.isBlank() ? "-" : grade,
                                    teacher.isBlank() ? "-" : teacher,
                                    category));
                            log.fine("  " + category + " | " + name + " | " + grade);
                        }
                    }
                }
            }

        } catch (Exception e) {
            log.warning("JS parsovanie chyba: " + e.getMessage());
            log.log(Level.FINE, "Detail chyby", e);
        }
        return subjects;
    }

    private List<Subject> parseSubjectsFromTableSelenium() {
        List<Subject> subjects = new ArrayList<>();
        try {
            List<WebElement> tables = driver.findElements(By.tagName("table"));
            String currentCategory = SubjectCategory.MANDATORY.displayName();

            for (WebElement table : tables) {
                List<WebElement> rows = table.findElements(By.cssSelector("tbody tr, tr"));
                for (WebElement row : rows) {
                    try {
                        List<WebElement> cells = row.findElements(By.tagName("td"));
                        if (cells.size() >= 5) {
                            String name = cells.get(0).getText().trim();
                            String abbr = cells.size() > 1 ? cells.get(1).getText().trim() : "-";
                            String credits = cells.size() > 2 ? cells.get(2).getText().trim() : "-";
                            String semester = cells.size() > 3 ? cells.get(3).getText().trim() : "-";
                            String gradeType = cells.size() > 4 ? cells.get(4).getText().trim() : "-";
                            String grade = cells.size() > 5 ? cells.get(5).getText().trim() : "-";
                            String teacher = cells.size() > 6 ? cells.get(6).getText().trim() : "-";

                            if (!name.isBlank() && !name.equalsIgnoreCase("Predmet")
                                    && !name.equalsIgnoreCase("Názov") && !name.equalsIgnoreCase("name")) {
                                subjects.add(new Subject(name, abbr, credits, semester, gradeType,
                                        grade.isBlank() ? "-" : grade,
                                        teacher.isBlank() ? "-" : teacher,
                                        currentCategory));
                            }
                        }
                    } catch (StaleElementReferenceException ignored) {}
                }
            }
        } catch (Exception e) {
            log.log(Level.FINE, "Detail chyby", e);
        }
        return subjects;
    }

    private List<Subject> parseSubjectsFromDesktopRows() {
        List<Subject> subjects = new ArrayList<>();
        try {
            List<WebElement> allElements = driver.findElements(
                    By.cssSelector("div.typ-vyucby, div.studijna-cast, .row.d-none.d-md-flex")
            );
            String currentCategory = SubjectCategory.MANDATORY.displayName();

            for (WebElement el : allElements) {
                try {
                    String cssClass = el.getAttribute("class");

                    if (cssClass != null && (cssClass.contains("typ-vyucby") || cssClass.contains("studijna-cast"))) {
                        currentCategory = SubjectCategory.fromHeading(el.getText())
                                .map(SubjectCategory::displayName)
                                .orElse(currentCategory);
                        continue;
                    }

                    String name = safeGetText(el, ".col-5 b, .col-4 b");
                    if (name.isBlank()) continue;

                    String code = safeGetText(el, ".col-5 .grey, .col-5 .text-muted");
                    if (code.contains(" ")) code = code.split("\\s")[0];

                    String credits = "-";
                    String semester = "-";
                    String gradeType = "-";
                    List<WebElement> badges = el.findElements(By.cssSelector(".badge b"));
                    for (WebElement badge : badges) {
                        String bText = badge.getText().trim();
                        if (bText.endsWith("K")) credits = bText;
                        else if (bText.equals("ZS") || bText.equals("LS")) semester = bText;
                        else if (bText.equals("S") || bText.equals("PH") || bText.equals("A")) gradeType = bText;
                    }

                    String grade = safeGetText(el, ".col-3 b");
                    if (grade.isBlank()) grade = "-";

                    String teacher = safeGetText(el, ".col-3 .text-black-50");

                    subjects.add(new Subject(name, code, credits, semester, gradeType,
                            grade, teacher, currentCategory));

                } catch (StaleElementReferenceException ignored) {}
            }
        } catch (Exception e) {
            log.log(Level.FINE, "Detail chyby", e);
        }
        return subjects;
    }
}
