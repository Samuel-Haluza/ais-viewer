package sk.ukf.aisviewer.service;

import org.junit.jupiter.api.Test;
import sk.ukf.aisviewer.model.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CurriculumCatalogImportServiceTest {
    @Test
    void newestVersionKeepsOnlyNewestPropertiesAndStoresOlderCode() throws Exception {
        String html = html(
                row("Povinné predmety"),
                subjectRow("KI/PJC/22", "Programovanie v jazyku C", "3", "1", "Z", ""),
                subjectRow("KI/PJC/26", "Programovanie v jazyku C", "5", "2", "L", ""));
        CurriculumCatalogImportService.ImportResult result = importHtml(html);

        CurriculumSubject subject = result.catalog().getSubjects().get(0);
        assertEquals("KI/PJC/26", subject.getCode());
        assertEquals(5, subject.getCredits());
        assertEquals(2, subject.getRecommendedYear());
        assertEquals(List.of("KI/PJC/22"), subject.getEquivalentCodes());
    }

    @Test
    void sameCodeInWinterAndSummerBecomesBoth() throws Exception {
        String html = html(
                row("Povinne voliteľné predmety"),
                subjectRow("KI/mOP/22", "Odborná prax", "4", "2", "Z", ""),
                subjectRow("KI/mOP/22", "Odborná prax", "4", "2", "L", ""));
        CurriculumSubject subject = importHtml(html).catalog().getSubjects().get(0);
        assertEquals(Semester.BOTH, subject.getSemester());
    }

    private CurriculumCatalogImportService.ImportResult importHtml(String content) throws Exception {
        Path file = Files.createTempFile("catalog", ".html");
        Files.writeString(file, content);
        StudyPlanCatalog catalog = new StudyPlanCatalog();
        catalog.setProgramCode("AI22b");
        catalog.setDegreeLevel(DegreeLevel.BACHELOR);
        catalog.setStudyForm(StudyForm.FULL_TIME);
        try {
            return new CurriculumCatalogImportService().importCatalog(file, catalog);
        } finally {
            Files.deleteIfExists(file);
        }
    }

    private String subjectRow(String code, String name, String credits,
                              String year, String semester, String prerequisite) {
        return "<tr><td>" + code + "</td><td>" + name + "</td><td></td><td>" + credits
                + "</td><td></td><td></td><td>" + year + "</td><td>" + semester
                + "</td><td>P</td><td>" + prerequisite + "</td></tr>";
    }

    private String row(String text) {
        return "<tr><td>" + text + "</td></tr>";
    }

    private String html(String... rows) {
        return "<html><body><table>" + String.join("", rows) + "</table></body></html>";
    }
}
