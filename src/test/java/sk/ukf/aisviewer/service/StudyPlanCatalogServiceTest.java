package sk.ukf.aisviewer.service;

import org.junit.jupiter.api.Test;
import sk.ukf.aisviewer.model.*;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class StudyPlanCatalogServiceTest {
    @Test
    void loadsLimitsFromBachelorCatalog() {
        Optional<StudyPlanCatalog> catalog =
                new StudyPlanCatalogService().load(DegreeLevel.BACHELOR, StudyForm.FULL_TIME);
        assertTrue(catalog.isPresent());
        assertEquals(180, catalog.get().getRequiredTotalCredits());
        assertEquals(170, catalog.get().getRequiredStudyCredits());
        assertEquals(10, catalog.get().getStateExamCredits());
        assertEquals(104, catalog.get().getRequiredMandatoryCredits());
        assertEquals(66, catalog.get().getRequiredCompulsoryElectiveCredits());
    }

    @Test
    void externalCatalogIsNotSilentlyReplacedByFullTimeCatalog() {
        assertTrue(new StudyPlanCatalogService()
                .load(DegreeLevel.BACHELOR, StudyForm.EXTERNAL).isEmpty());
    }

    @Test
    void masterCatalogContainsBothSemesterPractice() {
        CurriculumSubject practice = new StudyPlanCatalogService()
                .load(DegreeLevel.MASTER, StudyForm.FULL_TIME).orElseThrow()
                .getSubjects().stream()
                .filter(subject -> "KI/mOP/22".equals(subject.getCode()))
                .findFirst().orElseThrow();
        assertEquals(Semester.BOTH, practice.getSemester());
    }

    @Test
    void resolvesBachelorFromEnrollmentProgramCode() {
        StudentInfo student = new StudentInfo();
        student.setEnrollmentListNames(java.util.List.of(
                "Moje predmety v 2026/2027 (AI22b - aplikovaná informatika - denná forma)"));
        StudyPlanCatalogService service = new StudyPlanCatalogService();
        assertEquals(DegreeLevel.BACHELOR, service.resolveDegreeLevel(student));
        assertEquals(StudyForm.FULL_TIME, service.resolveStudyForm(student));
        assertEquals("/study-plans/ai22b-fulltime.json",
                service.resourceName(service.resolveDegreeLevel(student),
                        service.resolveStudyForm(student)));
    }

    @Test
    void resolvesMasterFromStudyProgram() {
        StudentInfo student = new StudentInfo();
        student.setStudyProgram("AI22m - aplikovaná informatika - magisterský II. st.");
        StudyPlanCatalogService service = new StudyPlanCatalogService();
        assertEquals(DegreeLevel.MASTER, service.resolveDegreeLevel(student));
    }
}
