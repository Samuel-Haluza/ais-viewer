package sk.ukf.aisviewer.service;

import org.junit.jupiter.api.Test;
import sk.ukf.aisviewer.model.*;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class UserStudyPlanServiceTest {
    @Test
    void planRoundTripsThroughGson() throws Exception {
        Path file = Files.createTempFile("study-plan", ".json");
        try {
            UserStudyPlanService service = new UserStudyPlanService(file);
            UserStudyPlan plan = new UserStudyPlan();
            service.setDirections(plan, StudyDirection.SOFTWARE_DEVELOPMENT, null);
            service.addSubject(plan, "KI/PJC/26", 2, Semester.SUMMER);
            service.save(plan);

            UserStudyPlan loaded = service.load();
            assertEquals(StudyDirection.SOFTWARE_DEVELOPMENT, loaded.getPrimaryDirection());
            assertNull(loaded.getSecondaryDirection());
            assertEquals("KI/PJC/26", loaded.getPlannedSubjects().get(0).getSubjectCode());
            assertEquals(Semester.SUMMER, loaded.getPlannedSubjects().get(0).getPlannedSemester());
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    void equalDirectionsAreRejected() {
        UserStudyPlanService service = new UserStudyPlanService();
        assertThrows(IllegalArgumentException.class, () -> service.validateDirections(
                StudyDirection.ALGORITHMS, StudyDirection.ALGORITHMS));
    }

    @Test
    void defaultPlannedYearAndSemesterComeFromCurriculum() {
        UserStudyPlanService service = new UserStudyPlanService();
        UserStudyPlan plan = new UserStudyPlan();
        CurriculumSubject subject = new CurriculumSubject("CODE", "Predmet", 3,
                DegreeLevel.BACHELOR, StudyForm.FULL_TIME, SubjectType.COMPULSORY_ELECTIVE,
                3, Semester.SUMMER);
        service.addSubject(plan, subject);
        assertEquals(3, plan.getPlannedSubjects().get(0).getPlannedYear());
        assertEquals(Semester.SUMMER, plan.getPlannedSubjects().get(0).getPlannedSemester());
    }
}
