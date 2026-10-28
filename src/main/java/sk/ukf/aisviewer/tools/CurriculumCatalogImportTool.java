package sk.ukf.aisviewer.tools;

import com.google.gson.GsonBuilder;
import sk.ukf.aisviewer.model.*;
import sk.ukf.aisviewer.service.CurriculumCatalogImportService;

import java.nio.file.Files;
import java.nio.file.Path;

public final class CurriculumCatalogImportTool {
    private CurriculumCatalogImportTool() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 4) {
            throw new IllegalArgumentException(
                    "Použitie: <html> <programCode> <degreeLevel> <outputJson>");
        }
        DegreeLevel level = DegreeLevel.valueOf(args[2]);
        StudyPlanCatalog catalog = metadata(args[1], level);
        CurriculumCatalogImportService.ImportResult result =
                new CurriculumCatalogImportService().importCatalog(Path.of(args[0]), catalog);
        result.catalog().getSubjects().forEach(subject -> {
            System.out.println(subject.getCode() + " | " + subject.getName()
                    + " | " + subject.getSemester() + " | " + subject.getEquivalentCodes());
        });
        result.warnings().forEach(warning -> System.err.println("[CATALOG] " + warning));
        Files.createDirectories(Path.of(args[3]).getParent());
        Files.writeString(Path.of(args[3]),
                new GsonBuilder().setPrettyPrinting().create().toJson(result.catalog()));
    }

    private static StudyPlanCatalog metadata(String code, DegreeLevel level) {
        StudyPlanCatalog catalog = new StudyPlanCatalog();
        catalog.setProgramCode(code);
        catalog.setDegreeLevel(level);
        catalog.setStudyForm(StudyForm.FULL_TIME);
        if (level == DegreeLevel.BACHELOR) {
            catalog.setRequiredTotalCredits(180);
            catalog.setRequiredStudyCredits(170);
            catalog.setStateExamCredits(10);
            catalog.setRequiredMandatoryCredits(104);
            catalog.setRequiredCompulsoryElectiveCredits(66);
        } else {
            catalog.setRequiredTotalCredits(120);
            catalog.setRequiredStudyCredits(100);
            catalog.setStateExamCredits(20);
            catalog.setRequiredMandatoryCredits(36);
            catalog.setRequiredCompulsoryElectiveCredits(64);
        }
        return catalog;
    }
}
