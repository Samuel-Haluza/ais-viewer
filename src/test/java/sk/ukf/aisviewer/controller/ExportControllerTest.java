package sk.ukf.aisviewer.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import sk.ukf.aisviewer.model.Subject;
import sk.ukf.aisviewer.model.StudentInfo;
import sk.ukf.aisviewer.service.LocalCacheService;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExportControllerTest {
    @Test
    void createsChronologicalSemesterAndCategorySectionsWithoutDroppingSubjects() {
        StudentInfo student = new StudentInfo();
        student.setEnrollmentListIds(List.of("3AI22b", "1AI22b", "2AI22b"));
        student.setEnrollmentListNames(List.of(
                "2026/2027 (3AI22b)",
                "2024/2025 (1AI22b)",
                "2025/2026 (2AI22b)"));
        LocalCacheService.CacheSnapshot snapshot = LocalCacheService.createSnapshot(student);
        snapshot.getEnrollmentData().put("1AI22b", data(
                subject("Letný predmet", "L1", "3 K", "LS", "A", "Povinné predmety"),
                subject("Zimný voliteľný", "Z1", "4 K", "ZS", "A",
                        "Výberové predmety")));
        snapshot.getEnrollmentData().put("2AI22b", data(
                subject("Neurčený", "N1", "6 K", "-", "-", "Povinné predmety")));
        snapshot.getEnrollmentData().put("3AI22b", data(
                subject("Zimný povinný", "Z2", "5 K", "ZS", "S",
                        "Povinné predmety")));

        ExportController controller = new ExportController();
        controller.setExportData(student, snapshot);
        String csv = controller.createCsv();

        assertTrue(csv.indexOf("1. ročník – 2024/2025 (1AI22b)")
                < csv.indexOf("2. ročník – 2025/2026 (2AI22b)"));
        assertTrue(csv.indexOf("2. ročník – 2025/2026 (2AI22b)")
                < csv.indexOf("3. ročník – 2026/2027 (3AI22b)"));
        assertTrue(csv.indexOf("ZIMNÝ SEMESTER") < csv.indexOf("LETNÝ SEMESTER"));
        assertTrue(csv.contains("BEZ URČENÉHO SEMESTRA"));
        assertTrue(csv.contains("Kategória;Názov predmetu;Kód predmetu;Kredity;"
                + "Typ hodnotenia;Známka;Učiteľ"));
        assertEquals(1, occurrences(csv, "Letný predmet"));
        assertEquals(1, occurrences(csv, "Zimný voliteľný"));
        assertEquals(1, occurrences(csv, "Neurčený"));
        assertTrue(csv.contains("Spolu za zimný semester;;;4 kreditov;;;"), csv);
        assertTrue(csv.contains("Spolu za letný semester;;;3 kreditov;;;"));
        assertTrue(csv.contains("Spolu za 1. ročník;;;7 kreditov;;;"));
    }

    private static LocalCacheService.EnrollmentData data(Subject... subjects) {
        return LocalCacheService.EnrollmentData.of(List.of(subjects), List.of(), List.of());
    }

    private static Subject subject(String name, String code, String credits,
                                   String semester, String gradeType, String category) {
        return new Subject(name, code, credits, semester, gradeType, "A", "Učiteľ", category);
    }

    @Test
    void writesCsvWithBomAndReplacesOldFileWithoutLeavingTempFiles(@TempDir Path dir) throws Exception {
        Path target = dir.resolve("prehlad.csv");
        Files.writeString(target, "stary obsah", StandardCharsets.UTF_8);

        ExportController.writeAtomically(target, "a;b\n");

        assertEquals("\uFEFFa;b\n", Files.readString(target, StandardCharsets.UTF_8));
        try (var files = Files.list(dir)) {
            assertEquals(List.of(target), files.toList(), "dočasný súbor nesmie zostať");
        }
    }

    private static int occurrences(String text, String value) {
        return text.split(java.util.regex.Pattern.quote(value), -1).length - 1;
    }
}
