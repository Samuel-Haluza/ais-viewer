package sk.ukf.aisviewer.controller;

import javafx.scene.control.Button;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import sk.ukf.aisviewer.model.StudentInfo;
import sk.ukf.aisviewer.model.Subject;
import sk.ukf.aisviewer.service.LocalCacheService;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExportController {
    private final Button csvButton;
    private StudentInfo studentInfo;
    private LocalCacheService.CacheSnapshot cacheSnapshot;

    public ExportController(Button csvButton) {
        this.csvButton = csvButton;
        if (csvButton != null) {
            this.csvButton.setOnAction(event -> exportCsv());
        }
    }

    public void setExportData(StudentInfo studentInfo,
                              LocalCacheService.CacheSnapshot cacheSnapshot) {
        this.studentInfo = studentInfo;
        this.cacheSnapshot = cacheSnapshot;
    }

    private void exportCsv() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exportovať študijný prehľad do CSV");
        chooser.setInitialFileName(createFileName());
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV súbory (*.csv)", "*.csv"));
        File file = chooser.showSaveDialog(window());
        if (file == null) {
            return;
        }
        try (BufferedWriter writer = Files.newBufferedWriter(
                file.toPath(), StandardCharsets.UTF_8)) {
            writer.write('\uFEFF');
            writer.write(createCsv());
            UiDialogs.showExportSuccess(window(), "CSV export bol úspešne vytvorený.");
        } catch (IOException | RuntimeException e) {
            UiDialogs.showExportError(window(), "CSV export sa nepodarilo vytvoriť.");
        }
    }

    String createCsv() {
        StringBuilder csv = new StringBuilder();
        appendRow(csv, "ŠTUDIJNÝ PREHĽAD", "", "", "", "", "", "");

        if (studentInfo == null || cacheSnapshot == null
                || cacheSnapshot.getEnrollmentData() == null) {
            return csv.toString();
        }

        List<String> ids = studentInfo.getEnrollmentListIds();
        List<String> names = studentInfo.getEnrollmentListNames();
        if (ids == null) {
            return csv.toString();
        }

        Map<String, LocalCacheService.EnrollmentData> enrollmentData =
                cacheSnapshot.getEnrollmentData();
        List<EnrollmentSection> sections = new ArrayList<>();
        for (int i = 0; i < ids.size(); i++) {
            String id = ids.get(i);
            String enrollmentName = names != null && i < names.size()
                    ? names.get(i) : id;
            LocalCacheService.EnrollmentData data = enrollmentData.get(id);
            if (data == null || data.getSubjects() == null
                    || data.getSubjects().stream().noneMatch(subject -> subject != null)) {
                continue;
            }
            sections.add(new EnrollmentSection(enrollmentName, data.getSubjects().stream()
                    .filter(subject -> subject != null)
                    .toList()));
        }

        sections.sort(Comparator
                .comparingInt((EnrollmentSection section) -> academicYearSortKey(
                        section.enrollmentName))
                .thenComparingInt(section -> enrollmentYearSortKey(
                        extractEnrollmentCode(section.enrollmentName)))
                .thenComparing(section -> extractEnrollmentCode(section.enrollmentName),
                        String.CASE_INSENSITIVE_ORDER));

        for (int i = 0; i < sections.size(); i++) {
            if (i > 0) {
                csv.append('\n');
            }
            appendEnrollmentSection(csv, sections.get(i));
        }
        return csv.toString();
    }

    private void appendEnrollmentSection(StringBuilder csv, EnrollmentSection section) {
        String academicYear = extractAcademicYear(section.enrollmentName);
        String enrollmentCode = extractEnrollmentCode(section.enrollmentName);
        String yearNumber = enrollmentYearSortKey(enrollmentCode) > 0
                ? enrollmentYearSortKey(enrollmentCode) + ". ročník"
                : "Študijný zápisný list";
        appendRow(csv, yearNumber + " – " + academicYear + " (" + enrollmentCode + ")",
                "", "", "", "", "", "");
        csv.append('\n');

        List<Subject> winter = subjectsForSemester(section.subjects, "ZS");
        List<Subject> summer = subjectsForSemester(section.subjects, "LS");
        List<Subject> unspecified = section.subjects.stream()
                .filter(subject -> normalizeSemester(subject.getSemester()).isEmpty())
                .sorted(subjectComparator())
                .toList();

        appendSemesterSection(csv, winter, "ZIMNÝ SEMESTER", "zimný semester");
        appendSemesterSection(csv, summer, "LETNÝ SEMESTER", "letný semester");
        appendSemesterSection(csv, unspecified, "BEZ URČENÉHO SEMESTRA", "neurčený semester");

        int totalCredits = section.subjects.stream()
                .mapToInt(Subject::getCreditsValue)
                .sum();
        appendRow(csv, "Spolu za " + yearNumber, "", "",
                totalCredits + " kreditov", "", "", "");
    }

    private void appendSemesterSection(StringBuilder csv, List<Subject> subjects,
                                       String title, String summaryName) {
        if (subjects.isEmpty()) {
            return;
        }
        csv.append('\n');
        appendRow(csv, title, "", "", "", "", "", "");
        csv.append('\n');
        appendRow(csv, "Kategória", "Názov predmetu", "Kód predmetu", "Kredity",
                "Typ hodnotenia", "Známka", "Učiteľ");
        for (Subject subject : subjects) {
            appendRow(csv, subject.getCategory(), subject.getName(), subject.getAbbreviation(),
                    subject.getCredits(), subject.getGradeType(), subject.getGrade(),
                    subject.getTeacher());
        }
        int credits = subjects.stream().mapToInt(Subject::getCreditsValue).sum();
        csv.append('\n');
        appendRow(csv, "Spolu za " + summaryName, "", "",
                credits + " kreditov", "", "", "");
    }

    private void appendRow(StringBuilder csv, String... values) {
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                csv.append(';');
            }
            csv.append(escape(values[i]));
        }
        csv.append('\n');
    }

    private List<Subject> subjectsForSemester(List<Subject> subjects, String semester) {
        return subjects.stream()
                .filter(subject -> semester.equals(normalizeSemester(subject.getSemester())))
                .sorted(subjectComparator())
                .toList();
    }

    private Comparator<Subject> subjectComparator() {
        return Comparator
                .comparingInt((Subject subject) -> categorySortKey(subject.getCategory()))
                .thenComparing(subject -> valueOrDash(subject.getCategory()),
                        String.CASE_INSENSITIVE_ORDER)
                .thenComparing(subject -> valueOrDash(subject.getName()),
                        String.CASE_INSENSITIVE_ORDER);
    }

    private int categorySortKey(String category) {
        return switch (valueOrDash(category)) {
            case "Povinné predmety" -> 0;
            case "Povinne voliteľné predmety" -> 1;
            case "Výberové predmety" -> 2;
            default -> 3;
        };
    }

    private String normalizeSemester(String semester) {
        if (semester == null || semester.isBlank()) {
            return "";
        }
        String normalized = semester.trim().toUpperCase();
        return switch (normalized) {
            case "ZS" -> "ZS";
            case "LS" -> "LS";
            default -> "";
        };
    }

    private int academicYearSortKey(String enrollmentName) {
        Matcher matcher = Pattern.compile("(20\\d{2})").matcher(
                valueOrDash(extractAcademicYear(enrollmentName)));
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : Integer.MAX_VALUE;
    }

    private int enrollmentYearSortKey(String enrollmentCode) {
        Matcher matcher = Pattern.compile("^(\\d+)").matcher(valueOrDash(enrollmentCode));
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : Integer.MAX_VALUE;
    }

    private String extractAcademicYear(String enrollmentName) {
        if (enrollmentName == null || enrollmentName.isBlank()) {
            return "-";
        }
        int openingBracket = enrollmentName.lastIndexOf('(');
        return openingBracket > 0
                ? valueOrDash(enrollmentName.substring(0, openingBracket).trim())
                : valueOrDash(enrollmentName);
    }

    private String extractEnrollmentCode(String enrollmentName) {
        if (enrollmentName == null || enrollmentName.isBlank()) {
            return "-";
        }
        int openingBracket = enrollmentName.lastIndexOf('(');
        int closingBracket = enrollmentName.lastIndexOf(')');
        if (openingBracket >= 0 && closingBracket > openingBracket) {
            String code = enrollmentName.substring(openingBracket + 1, closingBracket).trim();
            if (!code.isBlank()) {
                return code;
            }
        }
        return valueOrDash(enrollmentName);
    }

    private String createFileName() {
        String name = studentInfo == null ? null : studentInfo.getFullName();
        if (name == null || name.isBlank()) {
            return "studijny_prehlad.csv";
        }
        String safeName = name.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        return (safeName.isBlank() ? "studijny_prehlad" : safeName)
                + "_studijny_prehlad.csv";
    }

    private String escape(String value) {
        if (value != null && value.isEmpty()) {
            return "";
        }
        String normalized = valueOrDash(value);
        if (normalized.contains(";") || normalized.contains("\"")
                || normalized.contains("\n") || normalized.contains("\r")) {
            return "\"" + normalized.replace("\"", "\"\"") + "\"";
        }
        return normalized;
    }

    private String valueOrDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private Window window() {
        return csvButton.getScene().getWindow();
    }

    private static final class EnrollmentSection {
        private final String enrollmentName;
        private final List<Subject> subjects;

        private EnrollmentSection(String enrollmentName, List<Subject> subjects) {
            this.enrollmentName = enrollmentName;
            this.subjects = subjects;
        }
    }
}
