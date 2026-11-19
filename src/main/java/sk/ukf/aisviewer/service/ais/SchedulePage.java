package sk.ukf.aisviewer.service.ais;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import sk.ukf.aisviewer.model.ScheduleEntry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import static sk.ukf.aisviewer.service.ais.AisSession.BASE_URL;
import static sk.ukf.aisviewer.service.ais.AisSession.STUDENT_HOME_URL;

/**
 * Stránka „Rozvrh“ v AIS2.
 * Pozn.: AIS parameter zl ignoruje a vždy vráti rozvrh aktuálneho semestra.
 */
public class SchedulePage extends AisPage {

    public SchedulePage(AisSession session) {
        super(session);
    }

    /**
     * Parses schedule entries from AIS2 timetable page.
     * Tries multiple strategies to extract structured schedule data.
     */
    public List<ScheduleEntry> fetchScheduleEntries(String enrollmentListId) {
        List<ScheduleEntry> entries = new ArrayList<>();
        if (driver == null || enrollmentListId == null) return entries;

        try {
            log.info("Načítavam rozvrh (štruktúrovane)...");

            // 1. Priame URL – prvá je tá, ktorá v AIS2 UKF reálne funguje.
            boolean navigated = false;
            String[] rozvrhUrls = {
                    BASE_URL + "/ais/apps/rozvrh/sk/?zl=" + enrollmentListId,
                    BASE_URL + "/ais/apps/student-rozvrh/?zl=" + enrollmentListId,
                    BASE_URL + "/ais/apps/student/sk/rozvrh?zl=" + enrollmentListId
            };
            for (String tryUrl : rozvrhUrls) {
                try {
                    driver.get(tryUrl);
                    int count = waitForStableCount(By.cssSelector("app-rozvrh-okienko, table tr"),
                            Duration.ofSeconds(10), Duration.ofMillis(1000), "rozvrh");
                    String src = driver.getPageSource();
                    if (count > 0 || (!src.contains("404") && src.length() > 500)) {
                        navigated = true;
                        log.info("Rozvrh URL: " + tryUrl);
                        break;
                    }
                } catch (Exception ignored) {}
            }

            // 2. Záloha – odkaz „Rozvrh“ na domovskej stránke.
            if (!navigated) {
                driver.get(STUDENT_HOME_URL);
                waitForStableCount(By.tagName("a"),
                        Duration.ofSeconds(20), Duration.ofMillis(1000), "odkazy na domovskej stránke");
                for (WebElement link : driver.findElements(By.tagName("a"))) {
                    try {
                        String text = link.getText().trim().toLowerCase();
                        if (text.contains("rozvrh")) {
                            link.click();
                            waitForStableCount(By.cssSelector("app-rozvrh-okienko"),
                                    Duration.ofSeconds(20), Duration.ofMillis(1500), "rozvrh (odkaz)");
                            navigated = true;
                            log.info("Rozvrh URL: " + driver.getCurrentUrl());
                            break;
                        }
                    } catch (StaleElementReferenceException ignored) {}
                }
            }

            if (!navigated) {
                log.info("Rozvrh stránku sa nepodarilo nájsť.");
                return entries;
            }

            // Parse the AIS day columns and lesson components directly from the DOM.
            entries = parseScheduleFromAisDom();
            log.fine("Schedule parser 1: parseScheduleFromAisDom() -> "
                    + entries.size() + " položiek");
            if (!entries.isEmpty()) {
                log.info("Rozvrh: " + entries.size() + " položiek (AIS DOM)");
                return entries;
            }

            // Keep the older parsers as a fallback for older AIS layouts.
            entries = parseScheduleViaJS();
            log.fine("Schedule parser 2: parseScheduleViaJS() -> "
                    + entries.size() + " položiek");
            if (!entries.isEmpty()) {
                log.info("Rozvrh: " + entries.size() + " položiek (JS fallback)");
                return entries;
            }

            entries = parseScheduleFromPageSource();
            log.fine("Schedule parser 3: parseScheduleFromPageSource() -> "
                    + entries.size() + " položiek");
            if (!entries.isEmpty()) {
                log.info("Rozvrh: " + entries.size() + " položiek (text fallback)");
                return entries;
            }

            log.info("Rozvrh sa nepodarilo rozparsovať.");

        } catch (Exception e) {
            log.warning("Chyba pri načítaní rozvrhu: " + e.getMessage());
            log.log(Level.FINE, "Detail chyby", e);
        }
        return entries;
    }

    @SuppressWarnings("unchecked")
    private List<ScheduleEntry> parseScheduleFromAisDom() {
        List<ScheduleEntry> entries = new ArrayList<>();
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            Object result = js.executeScript(
                    "var result = [];"
                            + "var dayMap = {'Pondelok':0,'Utorok':1,'Streda':2,'Štvrtok':3,'Piatok':4};"
                            + "var columns = document.querySelectorAll('div.row.h-100 > div.col');"
                            + "for (var d = 0; d < columns.length; d++) {"
                            + "  var column = columns[d];"
                            + "  var header = column.querySelector('.rozvrh-day-header');"
                            + "  if (!header) continue;"
                            + "  var day = header.textContent.trim();"
                            + "  var dayIndex = dayMap[day];"
                            + "  if (dayIndex === undefined) continue;"
                            + "  var windows = column.querySelectorAll('app-rozvrh-okienko');"
                            + "  for (var i = 0; i < windows.length; i++) {"
                            + "    var window = windows[i];"
                            + "    var timeNode = window.querySelector('.rozvrh-okienko > div:first-child');"
                            + "    var time = timeNode ? timeNode.textContent.trim() : '';"
                            + "    var match = time.match(/(\\d{1,2})[:.](\\d{2})\\s*[-–]\\s*(\\d{1,2})[:.](\\d{2})/);"
                            + "    if (!match) continue;"
                            + "    var shorthand = window.querySelector('.predmet-skratka');"
                            + "    var shorthandText = shorthand ? shorthand.textContent.trim() : '';"
                            + "    var split = shorthandText.split(/\\s+-\\s+/);"
                            + "    var code = split.length > 0 ? split[0].trim() : '';"
                            + "    var type = split.length > 1 ? split[1].trim() : '';"
                            + "    var text = function(selector) {"
                            + "      var node = window.querySelector(selector);"
                            + "      return node ? node.textContent.trim() : '';"
                            + "    };"
                            + "    result.push(day + '|||' + dayIndex + '|||'"
                            + "      + match[1].padStart(2,'0') + ':' + match[2] + '|||'"
                            + "      + match[3].padStart(2,'0') + ':' + match[4] + '|||'"
                            + "      + code + '|||' + text('.predmet-nazov') + '|||'"
                            + "      + text('.predmet-miestnosti') + '|||'"
                            + "      + text('.predmet-vyucujuci') + '|||' + type);"
                            + "  }"
                            + "}"
                            + "return result;");

            if (result instanceof List) {
                for (String row : (List<String>) result) {
                    String[] parts = row.split("\\|\\|\\|", -1);
                    if (parts.length < 9) {
                        continue;
                    }
                    ScheduleEntry entry = new ScheduleEntry(
                            parts[0].trim(),
                            Integer.parseInt(parts[1].trim()),
                            parts[2].trim(),
                            parts[3].trim(),
                            parts[5].trim(),
                            parts[4].trim(),
                            parts[6].trim(),
                            parts[7].trim(),
                            parts[8].trim());
                    entries.add(entry);
                    log.fine("  " + entry);
                }
            }
        } catch (Exception e) {
            log.warning("AIS DOM rozvrh parsing chyba: " + e.getMessage());
        }
        return entries;
    }

    @SuppressWarnings("unchecked")
    private List<ScheduleEntry> parseScheduleViaJS() {
        List<ScheduleEntry> entries = new ArrayList<>();
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            Object result = js.executeScript(
                "var results = [];" +
                "var dayMap = {'Po':0,'Ut':1,'St':2,'Št':3,'Pi':4,'So':5," +
                "  'Pondelok':0,'Utorok':1,'Streda':2,'Štvrtok':3,'Piatok':4,'Sobota':5," +
                "  'Mon':0,'Tue':1,'Wed':2,'Thu':3,'Fri':4,'Sat':5," +
                "  'po':0,'ut':1,'st':2,'št':3,'pi':4,'so':5};" +
                "var dayNames = ['Pondelok','Utorok','Streda','Štvrtok','Piatok','Sobota'];" +

                // Strategy A: look for table rows with schedule data
                "var tables = document.querySelectorAll('table');" +
                "for (var t = 0; t < tables.length; t++) {" +
                "  var rows = tables[t].querySelectorAll('tr');" +
                "  for (var r = 0; r < rows.length; r++) {" +
                "    var cells = rows[r].querySelectorAll('td');" +
                "    if (cells.length >= 4) {" +
                "      var rowText = rows[r].textContent;" +
                "      var timeMatch = rowText.match(/(\\d{1,2})[:.](\\d{2})\\s*[-–]\\s*(\\d{1,2})[:.](\\d{2})/);" +
                "      if (!timeMatch) continue;" +
                "      var dayText = '';" +
                "      for (var c = 0; c < cells.length; c++) {" +
                "        var ct = cells[c].textContent.trim();" +
                "        for (var dk in dayMap) {" +
                "          if (ct === dk || ct.indexOf(dk) === 0) { dayText = dk; break; }" +
                "        }" +
                "        if (dayText) break;" +
                "      }" +
                "      if (!dayText) continue;" +
                "      var di = dayMap[dayText]; if (di === undefined) continue;" +
                "      var tf = timeMatch[1].padStart(2,'0') + ':' + timeMatch[2];" +
                "      var tt = timeMatch[3].padStart(2,'0') + ':' + timeMatch[4];" +
                "      var name = '', code = '', room = '', teacher = '', type = '';" +
                "      for (var c = 0; c < cells.length; c++) {" +
                "        var ct = cells[c].textContent.trim();" +
                "        if (ct.length > 5 && ct !== dayText && !ct.match(/^\\d/) && !name) {" +
                "          name = ct;" +
                "        }" +
                "      }" +
                "      results.push(dayNames[di]+'|||'+di+'|||'+tf+'|||'+tt+'|||'+name+'|||'+code+'|||'+room+'|||'+teacher+'|||'+type);" +
                "    }" +
                "  }" +
                "}" +

                // Strategy B: look for Angular-style event elements positioned in a grid
                "if (results.length === 0) {" +
                "  var events = document.querySelectorAll('[class*=event], [class*=rozvrh], [class*=schedule], [class*=lesson], mat-card, .cal-event');" +
                "  for (var i = 0; i < events.length; i++) {" +
                "    var el = events[i];" +
                "    var text = el.textContent.trim();" +
                "    if (text.length < 5) continue;" +
                "    var tm = text.match(/(\\d{1,2})[:.](\\d{2})\\s*[-–]\\s*(\\d{1,2})[:.](\\d{2})/);" +
                "    if (!tm) continue;" +
                "    var tf = tm[1].padStart(2,'0') + ':' + tm[2];" +
                "    var tt = tm[3].padStart(2,'0') + ':' + tm[4];" +
                "    var dayText = '';" +
                "    for (var dk in dayMap) {" +
                "      if (text.indexOf(dk) >= 0) { dayText = dk; break; }" +
                "    }" +
                "    var di = dayMap[dayText];" +
                "    if (di === undefined) di = 0;" +
                "    if (!dayText) dayText = dayNames[0];" +
                "    var lines = text.split('\\n').map(function(l){return l.trim();}).filter(function(l){return l.length>0;});" +
                "    var name = lines.length > 0 ? lines[0] : text.substring(0, 50);" +
                "    var room = '', teacher = '', type = '';" +
                "    for (var li = 0; li < lines.length; li++) {" +
                "      if (lines[li].match(/^[A-Z]{1,3}\\d/)) room = lines[li];" +
                "      if (lines[li].match(/^(P|C|S|Pr|Cv|Se)$/i)) type = lines[li];" +
                "    }" +
                "    name = name.replace(tm[0], '').replace(dayText, '').trim();" +
                "    if (name.length < 2) name = text.substring(0, Math.min(text.length, 60));" +
                "    results.push((dayText in dayMap ? dayNames[dayMap[dayText]] : dayText)+'|||'+di+'|||'+tf+'|||'+tt+'|||'+name+'|||'+'|||'+room+'|||'+teacher+'|||'+type);" +
                "  }" +
                "}" +

                // Strategy C: generic - find any text containing time patterns and day names
                "if (results.length === 0) {" +
                "  var allEls = document.querySelectorAll('div, span, td, li, p');" +
                "  var seen = {};" +
                "  for (var i = 0; i < allEls.length; i++) {" +
                "    var el = allEls[i];" +
                "    if (el.children.length > 3) continue;" + // skip container elements
                "    var text = el.textContent.trim();" +
                "    if (text.length < 10 || text.length > 300) continue;" +
                "    var tm = text.match(/(\\d{1,2})[:.](\\d{2})\\s*[-–]\\s*(\\d{1,2})[:.](\\d{2})/);" +
                "    if (!tm) continue;" +
                "    var key = tm[0] + text.substring(0, 30);" +
                "    if (seen[key]) continue; seen[key] = true;" +
                "    var dayText = '', di = -1;" +
                "    for (var dk in dayMap) {" +
                "      if (text.indexOf(dk) >= 0) { dayText = dk; di = dayMap[dk]; break; }" +
                "    }" +
                "    if (di < 0) continue;" +
                "    var tf = tm[1].padStart(2,'0') + ':' + tm[2];" +
                "    var tt = tm[3].padStart(2,'0') + ':' + tm[4];" +
                "    var name = text.replace(tm[0], '').replace(dayText, '').trim();" +
                "    if (name.length > 80) name = name.substring(0, 80);" +
                "    results.push(dayNames[di]+'|||'+di+'|||'+tf+'|||'+tt+'|||'+name+'|||'+'|||'+'|||'+'|||');" +
                "  }" +
                "}" +

                "return results;"
            );

            if (result instanceof List) {
                List<String> rows = (List<String>) result;
                for (String row : rows) {
                    String[] parts = row.split("\\|\\|\\|", -1);
                    if (parts.length >= 5) {
                        ScheduleEntry entry = new ScheduleEntry();
                        entry.setDay(parts[0].trim());
                        try { entry.setDayIndex(Integer.parseInt(parts[1].trim())); } catch (Exception e) { entry.setDayIndex(0); }
                        entry.setTimeFrom(parts[2].trim());
                        entry.setTimeTo(parts[3].trim());
                        entry.setSubjectName(parts[4].trim());
                        entry.setSubjectCode(parts.length > 5 ? parts[5].trim() : "");
                        entry.setRoom(parts.length > 6 ? parts[6].trim() : "");
                        entry.setTeacher(parts.length > 7 ? parts[7].trim() : "");
                        entry.setType(parts.length > 8 ? parts[8].trim() : "");

                        if (!entry.getSubjectName().isBlank()) {
                            entries.add(entry);
                            log.fine("  " + entry);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warning("JS rozvrh parsing chyba: " + e.getMessage());
        }
        return entries;
    }

    private List<ScheduleEntry> parseScheduleFromPageSource() {
        List<ScheduleEntry> entries = new ArrayList<>();
        try {
            String src = driver.getPageSource();
            // Clean HTML tags
            String text = src
                    .replaceAll("<script[^>]*>[\\s\\S]*?</script>", "")
                    .replaceAll("<style[^>]*>[\\s\\S]*?</style>", "")
                    .replaceAll("<[^>]+>", " ")
                    .replaceAll("&nbsp;", " ")
                    .replaceAll("\\s{2,}", " ");

            String[] dayNames = {"Pondelok", "Utorok", "Streda", "Štvrtok", "Piatok", "Sobota"};
            String[] dayShort = {"Po", "Ut", "St", "Št", "Pi", "So"};

            // Look for patterns like: "Po 08:00-09:30 Názov predmetu"
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                    "(Po(?:ndelok)?|Ut(?:orok)?|St(?:reda)?|Št(?:vrtok)?|Pi(?:atok)?|So(?:bota)?)\\s+" +
                    "(\\d{1,2})[:.](\\d{2})\\s*[-–]\\s*(\\d{1,2})[:.](\\d{2})\\s+" +
                    "([^\\d][^\\n]{3,80})"
            );
            java.util.regex.Matcher matcher = pattern.matcher(text);

            while (matcher.find()) {
                String dayStr = matcher.group(1);
                String hFrom = matcher.group(2);
                String mFrom = matcher.group(3);
                String hTo = matcher.group(4);
                String mTo = matcher.group(5);
                String rest = matcher.group(6).trim();

                int dayIdx = 0;
                for (int d = 0; d < dayNames.length; d++) {
                    if (dayStr.startsWith(dayShort[d]) || dayStr.equals(dayNames[d])) {
                        dayIdx = d;
                        break;
                    }
                }

                ScheduleEntry entry = new ScheduleEntry();
                entry.setDay(dayNames[dayIdx]);
                entry.setDayIndex(dayIdx);
                entry.setTimeFrom(String.format("%02d:%s", Integer.parseInt(hFrom), mFrom));
                entry.setTimeTo(String.format("%02d:%s", Integer.parseInt(hTo), mTo));
                entry.setSubjectName(rest.length() > 60 ? rest.substring(0, 60) : rest);
                entries.add(entry);
            }

        } catch (Exception e) {
            log.warning("Text schedule parsing error: " + e.getMessage());
        }
        return entries;
    }
}
