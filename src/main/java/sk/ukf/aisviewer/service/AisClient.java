package sk.ukf.aisviewer.service;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import sk.ukf.aisviewer.model.Exam;
import sk.ukf.aisviewer.model.StudentInfo;
import sk.ukf.aisviewer.model.Subject;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class AisClient {

    private static final String BASE_URL = "https://ais2.ukf.sk";
    private static final String START_URL = BASE_URL + "/ais/start.do";
    private static final String STUDENT_HOME_URL = BASE_URL + "/ais/apps/student/sk/";
    private static final String PREDMETY_URL = BASE_URL + "/ais/apps/student-predmety/sk/";

    private WebDriver driver;
    private WebDriverWait wait;
    private final AisParser parser;

    private StudentInfo currentStudent;
    private boolean loggedIn = false;

    public AisClient() {
        this.parser = new AisParser();
    }

    public boolean login(String username, String password) throws Exception {
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new");
        options.addArguments("--disable-gpu");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--lang=sk");
        options.addArguments("--disable-blink-features=AutomationControlled");
        options.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36");

        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(20));

        try {
            System.out.println("[AIS] Prihlasovanie...");
            driver.get(START_URL);
            Thread.sleep(2000);

            WebElement loginField = wait.until(
                    ExpectedConditions.presenceOfElementLocated(By.id("login"))
            );
            loginField.clear();
            loginField.sendKeys(username);

            WebElement passField = driver.findElement(By.id("heslo"));
            passField.clear();
            passField.sendKeys(password);

            WebElement submitBtn = driver.findElement(By.id("login-form-submit-btn"));
            submitBtn.click();

            Thread.sleep(4000);
            String currentUrl = driver.getCurrentUrl();

            if (currentUrl.contains("login.do")) {
                System.out.println("[AIS] Login neúspešný!");
                closeDriver();
                return false;
            }

            System.out.println("[AIS] Prihlásený, načítavam údaje...");
            driver.get(STUDENT_HOME_URL);
            Thread.sleep(4000);

            currentStudent = new StudentInfo();
            currentStudent.setFullName(username);

            try {
                String src = driver.getPageSource();
                int idx = src.indexOf("ng-reflect-user-name=\"");
                if (idx > 0) {
                    int start = idx + "ng-reflect-user-name=\"".length();
                    int end = src.indexOf("\"", start);
                    if (end > start) {
                        String name = src.substring(start, end).trim();
                        if (!name.isBlank()) {
                            currentStudent.setFullName(name);
                            System.out.println("[AIS] Študent: " + name);
                        }
                    }
                }
            } catch (Exception ignored) {}

            System.out.println("[AIS] Hľadám zápisné listy...");
            Thread.sleep(3000);

            List<WebElement> allLinks = driver.findElements(By.tagName("a"));
            List<WebElement> predmetyLinks = new ArrayList<>();

            for (WebElement link : allLinks) {
                try {
                    String text = link.getText().trim();
                    if (text.contains("Moje predmety v")) {
                        predmetyLinks.add(link);
                    }
                } catch (StaleElementReferenceException ignored) {}
            }

            System.out.println("[AIS] Počet zápisných listov: " + predmetyLinks.size());

            for (int i = 0; i < predmetyLinks.size(); i++) {
                try {
                    if (i > 0) {
                        driver.get(STUDENT_HOME_URL);
                        Thread.sleep(3000);
                        allLinks = driver.findElements(By.tagName("a"));
                        predmetyLinks.clear();
                        for (WebElement link : allLinks) {
                            try {
                                String text = link.getText().trim();
                                if (text.contains("Moje predmety v")) {
                                    predmetyLinks.add(link);
                                }
                            } catch (StaleElementReferenceException ignored) {}
                        }
                        if (i >= predmetyLinks.size()) break;
                    }

                    WebElement link = predmetyLinks.get(i);
                    String linkText = link.getText().trim();
                    link.click();
                    Thread.sleep(4000);

                    String newUrl = driver.getCurrentUrl();
                    if (!newUrl.contains("zl=")) {
                        Thread.sleep(2000);
                        newUrl = driver.getCurrentUrl();
                    }

                    if (newUrl.contains("zl=")) {
                        String zl = extractParam(newUrl, "zl");
                        if (!zl.isBlank() && !currentStudent.getEnrollmentListIds().contains(zl)) {
                            String displayName = linkText
                                    .replace("open_in_new", "")
                                    .replace("Moje predmety v ", "")
                                    .trim();
                            currentStudent.getEnrollmentListIds().add(zl);
                            currentStudent.getEnrollmentListNames().add(displayName);
                            if (currentStudent.getEnrollmentListId() == null) {
                                currentStudent.setEnrollmentListId(zl);
                            }
                            System.out.println("[AIS] Zápisný list: " + displayName + " (zl=" + zl + ")");
                        }
                    }

                } catch (Exception e) {
                    System.out.println("[AIS] Chyba pri hľadaní zápisného listu: " + e.getMessage());
                }
            }

            System.out.println("[AIS] Zápisné listy: " + currentStudent.getEnrollmentListIds());
            loggedIn = true;
            return true;

        } catch (Exception e) {
            System.out.println("[AIS] CHYBA: " + e.getMessage());
            e.printStackTrace();
            closeDriver();
            return false;
        }
    }

    public List<Subject> fetchSubjects(String enrollmentListId) {
        List<Subject> subjects = new ArrayList<>();

        if (driver == null || enrollmentListId == null || enrollmentListId.isBlank()) {
            return subjects;
        }

        try {
            String url = PREDMETY_URL + "?zl=" + enrollmentListId;
            System.out.println("[AIS] Načítavam predmety (zl=" + enrollmentListId + ")...");
            driver.get(url);
            Thread.sleep(5000);

            try {
                wait.until(ExpectedConditions.presenceOfElementLocated(
                        By.cssSelector("table, mat-card, .card, div.typ-vyucby, div.studijna-cast")
                ));
            } catch (TimeoutException e) {
                Thread.sleep(3000);
            }

            // Stratégia 1: Tabuľka
            subjects = parseSubjectsFromTableSelenium();
            if (!subjects.isEmpty()) {
                System.out.println("[AIS] Načítaných " + subjects.size() + " predmetov (tabuľka)");
                return subjects;
            }

            // Stratégia 2: Sekvenčné parsovanie – kategórie + karty
            subjects = parseSubjectsSequential();
            if (!subjects.isEmpty()) {
                System.out.println("[AIS] Načítaných " + subjects.size() + " predmetov (sekvenčne)");
                return subjects;
            }

            // Stratégia 3: Desktop rows
            subjects = parseSubjectsFromDesktopRows();
            if (!subjects.isEmpty()) {
                System.out.println("[AIS] Načítaných " + subjects.size() + " predmetov (desktop rows)");
                return subjects;
            }

            System.out.println("[AIS] Žiadne predmety nenájdené!");

        } catch (Exception e) {
            System.out.println("[AIS] Chyba pri načítaní predmetov: " + e.getMessage());
            e.printStackTrace();
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
                    "var results = [];" +
                            "var category = 'Povinné predmety';" +
                            // Nájdi v��etky elementy na stránke
                            "var allElements = document.querySelectorAll('*');" +
                            "for (var i = 0; i < allElements.length; i++) {" +
                            "  var el = allElements[i];" +
                            "  var text = (el.textContent || '').trim();" +
                            "  var tag = el.tagName.toLowerCase();" +
                            "  var cls = el.className || '';" +
                            // Detekcia kategórie – hľadaj h2, h3, h4, alebo div s textom kategórie
                            "  if ((tag === 'h2' || tag === 'h3' || tag === 'h4' || tag === 'h5'" +
                            "       || cls.indexOf('typ-vyucby') >= 0 || cls.indexOf('studijna-cast') >= 0" +
                            "       || cls.indexOf('category') >= 0 || cls.indexOf('header') >= 0)" +
                            "      && text.length < 100 && text.length > 3) {" +
                            "    var lower = text.toLowerCase();" +
                            "    if ((lower.indexOf('povinne') >= 0 || lower.indexOf('povinné') >= 0)" +
                            "        && (lower.indexOf('voliteľ') >= 0 || lower.indexOf('volitel') >= 0)) {" +
                            "      category = 'Povinne voliteľné predmety';" +
                            "    } else if (lower.indexOf('povinné') >= 0 || lower.indexOf('povinne') >= 0) {" +
                            "      if (lower.indexOf('voliteľ') < 0 && lower.indexOf('volitel') < 0) {" +
                            "        category = 'Povinné predmety';" +
                            "      }" +
                            "    } else if (lower.indexOf('výber') >= 0 || lower.indexOf('vyber') >= 0) {" +
                            "      category = 'Výberové predmety';" +
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
                            "        || nameLower.indexOf('výberové') >= 0 || nameLower.indexOf('vyberove') >= 0) continue;" +
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
                            "return results;"
            );

            if (result instanceof List) {
                @SuppressWarnings("unchecked")
                List<String> rows = (List<String>) result;
                System.out.println("[AIS] JS našiel " + rows.size() + " predmetov");
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
                            System.out.println("[AIS]   " + category + " | " + name + " | " + grade);
                        }
                    }
                }
            }

        } catch (Exception e) {
            System.out.println("[AIS] JS parsovanie chyba: " + e.getMessage());
            e.printStackTrace();
        }
        return subjects;
    }

    private List<Subject> parseSubjectsFromTableSelenium() {
        List<Subject> subjects = new ArrayList<>();
        try {
            List<WebElement> tables = driver.findElements(By.tagName("table"));
            String currentCategory = "Povinné predmety";

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
            e.printStackTrace();
        }
        return subjects;
    }

    private List<Subject> parseSubjectsFromDesktopRows() {
        List<Subject> subjects = new ArrayList<>();
        try {
            List<WebElement> allElements = driver.findElements(
                    By.cssSelector("div.typ-vyucby, div.studijna-cast, .row.d-none.d-md-flex")
            );
            String currentCategory = "Povinné predmety";

            for (WebElement el : allElements) {
                try {
                    String cssClass = el.getAttribute("class");

                    if (cssClass != null && (cssClass.contains("typ-vyucby") || cssClass.contains("studijna-cast"))) {
                        String text = el.getText().trim().toLowerCase();
                        if (text.contains("povinne voliteľ") || text.contains("povinne volitel")) {
                            currentCategory = "Povinne voliteľné predmety";
                        } else if (text.contains("povinné") || text.contains("povinne")) {
                            currentCategory = "Povinné predmety";
                        } else if (text.contains("výber") || text.contains("vyber")) {
                            currentCategory = "Výberové predmety";
                        }
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
            e.printStackTrace();
        }
        return subjects;
    }

    public List<Exam> fetchExams(String enrollmentListId) {
        List<Exam> exams = new ArrayList<>();
        if (driver == null || enrollmentListId == null || enrollmentListId.isBlank()) return exams;

        try {
            String url = PREDMETY_URL + "skusky?zl=" + enrollmentListId;
            System.out.println("[AIS] Načítavam skúšky...");
            driver.get(url);
            Thread.sleep(5000);

            List<WebElement> tables = driver.findElements(By.tagName("table"));
            for (WebElement table : tables) {
                List<WebElement> rows = table.findElements(By.cssSelector("tbody tr, tr"));
                for (WebElement row : rows) {
                    try {
                        List<WebElement> cells = row.findElements(By.tagName("td"));
                        if (cells.size() >= 3) {
                            String subjectName = cells.get(0).getText().trim();
                            String date = cells.size() > 1 ? cells.get(1).getText().trim() : "-";
                            String time = cells.size() > 2 ? cells.get(2).getText().trim() : "-";
                            String room = cells.size() > 3 ? cells.get(3).getText().trim() : "-";
                            String teacher = cells.size() > 4 ? cells.get(4).getText().trim() : "-";
                            String capacity = cells.size() > 5 ? cells.get(5).getText().trim() : "-";
                            String enrolled = cells.size() > 6 ? cells.get(6).getText().trim() : "-";
                            String status = cells.size() > 7 ? cells.get(7).getText().trim() : "-";

                            if (!subjectName.isBlank() && !subjectName.equalsIgnoreCase("predmet")) {
                                exams.add(new Exam(subjectName, "-", date, time, room,
                                        teacher, capacity, enrolled, status));
                            }
                        }
                    } catch (StaleElementReferenceException ignored) {}
                }
            }

            if (exams.isEmpty()) {
                List<WebElement> panels = driver.findElements(
                        By.cssSelector("mat-expansion-panel, [class*=skuska], [class*=termin], [class*=exam]")
                );
                for (WebElement panel : panels) {
                    try {
                        String text = panel.getText().trim();
                        if (text.length() > 5) {
                            exams.add(new Exam(text, "-", "-", "-", "-", "-", "-", "-", "-"));
                        }
                    } catch (StaleElementReferenceException ignored) {}
                }
            }

            System.out.println("[AIS] Načítaných " + exams.size() + " skúšok");

        } catch (Exception e) {
            e.printStackTrace();
        }
        return exams;
    }

    public String fetchScheduleHtml(String enrollmentListId) {
        if (driver == null || enrollmentListId == null) return "Rozvrh nie je dostupný.";
        try {
            System.out.println("[AIS] Načítavam rozvrh...");

            driver.get(STUDENT_HOME_URL);
            Thread.sleep(3000);

            List<WebElement> allLinks = driver.findElements(By.tagName("a"));
            WebElement rozvrhLink = null;
            for (WebElement link : allLinks) {
                try {
                    String text = link.getText().trim().toLowerCase();
                    if (text.contains("rozvrh")) {
                        rozvrhLink = link;
                        System.out.println("[AIS] Našiel rozvrh link: " + link.getText().trim());
                        break;
                    }
                } catch (StaleElementReferenceException ignored) {}
            }

            if (rozvrhLink != null) {
                rozvrhLink.click();
                Thread.sleep(5000);
                System.out.println("[AIS] Rozvrh URL: " + driver.getCurrentUrl());
                return driver.getPageSource();
            }

            String[] rozvrhUrls = {
                    BASE_URL + "/ais/apps/student-rozvrh/?zl=" + enrollmentListId,
                    BASE_URL + "/ais/apps/rozvrh/sk/?zl=" + enrollmentListId,
                    BASE_URL + "/ais/apps/student/sk/rozvrh?zl=" + enrollmentListId
            };

            for (String tryUrl : rozvrhUrls) {
                try {
                    driver.get(tryUrl);
                    Thread.sleep(3000);
                    String pageSource = driver.getPageSource();
                    if (!pageSource.contains("404") && !pageSource.contains("Nenalezeno")
                            && !pageSource.contains("not available") && pageSource.length() > 500) {
                        System.out.println("[AIS] Rozvrh nájdený na: " + tryUrl);
                        return pageSource;
                    }
                } catch (Exception ignored) {}
            }

            return "Rozvrh nie je dostupný. AIS2 neposkytuje rozvrh cez tento endpoint.";

        } catch (Exception e) {
            return "Chyba pri načítaní rozvrhu: " + e.getMessage();
        }
    }

    private String safeGetText(WebElement parent, String cssSelector) {
        try {
            WebElement el = parent.findElement(By.cssSelector(cssSelector));
            return el.getText().trim();
        } catch (NoSuchElementException | StaleElementReferenceException e) {
            return "";
        }
    }

    private String extractParam(String url, String param) {
        int idx = url.indexOf(param + "=");
        if (idx < 0) return "";
        int start = idx + param.length() + 1;
        int end = url.indexOf("&", start);
        if (end < 0) end = url.length();
        return url.substring(start, end);
    }

    private void closeDriver() {
        if (driver != null) {
            try { driver.quit(); } catch (Exception ignored) {}
            driver = null;
        }
    }

    public StudentInfo getCurrentStudent() { return currentStudent; }
    public boolean isLoggedIn() { return loggedIn; }
    public AisParser getParser() { return parser; }

    public void logout() {
        closeDriver();
        loggedIn = false;
        currentStudent = null;
    }
}