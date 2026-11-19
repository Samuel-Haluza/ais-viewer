package sk.ukf.aisviewer.service;

import org.junit.jupiter.api.Test;
import sk.ukf.aisviewer.model.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StudyPlanCreditsServiceTest {
    @Test
    void plannedCreditsExcludeCompletedAndDuplicateSubjects() {
        CurriculumSubject completed = subject("DONE", 4, SubjectType.COMPULSORY_ELECTIVE);
        CurriculumSubject planned = subject("PLAN", 6, SubjectType.COMPULSORY_ELECTIVE);
        StudyPlanCatalog catalog = catalog(completed, planned);
        UserStudyPlan plan = new UserStudyPlan();
        plan.setPlannedSubjects(List.of(new PlannedSubject("DONE", 1, Semester.WINTER),
                new PlannedSubject("PLAN", 2, Semester.WINTER),
                new PlannedSubject("PLAN", 2, Semester.WINTER)));
        Subject aisDone = new Subject("Done", "DONE", "4", "ZS", "S", "A", "", "");
        assertEquals(6, new StudyPlanCreditsService().plannedCredits(
                catalog, plan, List.of(aisDone)));
    }

    @Test
    void remainingPvpCreditsUseCatalogMinimum() {
        CurriculumSubject completed = subject("DONE", 4, SubjectType.COMPULSORY_ELECTIVE);
        CurriculumSubject planned = subject("PLAN", 6, SubjectType.COMPULSORY_ELECTIVE);
        StudyPlanCatalog catalog = catalog(completed, planned);
        catalog.setRequiredCompulsoryElectiveCredits(16);
        UserStudyPlan plan = new UserStudyPlan();
        plan.setPlannedSubjects(List.of(new PlannedSubject("PLAN", 2, Semester.WINTER)));
        Subject aisDone = new Subject("Done", "DONE", "4", "ZS", "S", "A", "", "");
        assertEquals(6, new StudyPlanCreditsService().remainingCompulsoryElectiveCredits(
                catalog, List.of(aisDone), plan));
    }

    @Test
    void creditsAndRecommendationsAgreeOnFullAisCode() {
        CurriculumSubject pjc = subject("KI/PJC/22", 5, SubjectType.COMPULSORY_ELECTIVE);
        CurriculumSubject other = subject("KI/OTHER/22", 3, SubjectType.COMPULSORY_ELECTIVE);
        StudyPlanCatalog catalog = catalog(pjc, other);
        // AIS niekedy vráti kód v plnom tvare – predtým ho výpočet kreditov nespároval.
        Subject aisPjc = new Subject("Programovanie v C", "KI/PJC/22", "5K", "ZS", "S",
                "B - veľmi dobre (1,5)", "", "");

        assertEquals(5, new StudyPlanCreditsService().acquiredTotalCredits(List.of(aisPjc)));
        assertEquals(java.util.Set.of("pjc"),
                new RecommendationService().completedCodes(catalog, List.of(aisPjc)));
    }

    @Test
    void acquiredCreditsIncludeSubjectsMissingFromCatalog() {
        // MPI1 (Matematika pre informatikov 1) nie je v katalógu KI, ale v AIS je splnený.
        Subject prog = new Subject("Programovanie", "PROG", "6K", "ZS", "S", "C - dobre (2)", "", "");
        Subject mpi = new Subject("Matematika pre informatikov 1", "MPI1", "3K", "ZS", "A",
                "ABS - absolvoval/a", "", "");
        Subject notDone = new Subject("Internet vecí", "IV", "6K", "LS", "S", "-", "", "");
        Subject failed = new Subject("Iný", "X", "4K", "LS", "S", "FX - nedostatočne (4)", "", "");
        Subject duplicate = new Subject("Programovanie", "PROG", "6K", "ZS", "S", "C - dobre (2)", "", "");

        assertEquals(9, new StudyPlanCreditsService().acquiredTotalCredits(
                List.of(prog, mpi, notDone, failed, duplicate)));
    }

    private CurriculumSubject subject(String code, int credits, SubjectType type) {
        return new CurriculumSubject(code, code, credits, DegreeLevel.BACHELOR,
                StudyForm.FULL_TIME, type, 1, Semester.WINTER);
    }

    private StudyPlanCatalog catalog(CurriculumSubject... subjects) {
        StudyPlanCatalog catalog = new StudyPlanCatalog();
        catalog.setSubjects(List.of(subjects));
        return catalog;
    }
}
