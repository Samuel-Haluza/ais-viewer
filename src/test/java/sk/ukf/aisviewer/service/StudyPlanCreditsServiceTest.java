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
