package sk.ukf.aisviewer.service;

import org.junit.jupiter.api.Test;
import sk.ukf.aisviewer.model.Exam;
import sk.ukf.aisviewer.model.Subject;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProgressServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 20);
    private final ProgressService service = new ProgressService();

    private static Subject subject(String code, String credits, String semester, String grade) {
        return new Subject("Predmet " + code, code, credits, semester, "S", grade, "", "Povinné predmety");
    }

    private static Exam exam(String code, String date, String time) {
        Exam exam = new Exam("Predmet " + code, code, date, time, "", "", "", "", "Prihlásený");
        exam.setType("skúška");
        return exam;
    }

    private static ProgressService.Achievement find(ProgressService.Report report, String id) {
        return report.achievements().stream().filter(a -> a.id().equals(id)).findFirst().orElseThrow();
    }

    @Test
    void xpRewardsBetterGrades() {
        assertEquals(180, ProgressService.xpFor(subject("A", "6K", "ZS", "A - výborne (1)")));
        assertEquals(150, ProgressService.xpFor(subject("B", "6K", "ZS", "B - veľmi dobre (1,5)")));
        assertEquals(60, ProgressService.xpFor(subject("E", "6K", "ZS", "E - dostatočne (3)")));
        assertEquals(45, ProgressService.xpFor(subject("ABS", "3K", "ZS", "ABS - absolvoval/a")));
    }

    @Test
    void ranksScaleWithRequiredCredits() {
        List<ProgressService.Rank> bachelor = ProgressService.ranks(180);
        assertEquals(12, bachelor.size());
        assertEquals(0, bachelor.get(0).minXp());
        assertEquals(3600, bachelor.get(11).minXp());
        assertEquals("Absolvent", bachelor.get(11).name());
        assertEquals(2400, ProgressService.ranks(120).get(11).minXp());
    }

    @Test
    void tieredAchievementAndDates() {
        ProgressService.EnrollmentInput year = new ProgressService.EnrollmentInput("2024/2025 (1AI22b)",
                List.of(subject("X", "6K", "ZS", "A - výborne (1)"), subject("Y", "6K", "LS", "B - veľmi dobre (1,5)"),
                        subject("Z", "6K", "LS", "-")),
                List.of(exam("X", "14.01.2025", "08:00"), exam("Y", "20.05.2025", "10:00")));
        ProgressService.Report report = service.compute(List.of(year), 180, null, TODAY);

        ProgressService.Achievement collector = find(report, "collector-a");
        assertEquals(1, collector.achievedTier());
        assertEquals(LocalDate.of(2025, 1, 14), collector.achievedOn());
        assertEquals("1 / 5", collector.progressText());

        assertFalse(find(report, "year-1").unlocked(), "predmet Z ešte nie je splnený");
        assertTrue(find(report, "first-step").unlocked());
        assertEquals(330, report.xp());
        assertEquals(2, report.firstTryCount());
    }

    @Test
    void examAchievements() {
        ProgressService.EnrollmentInput year = new ProgressService.EnrollmentInput("2024/2025 (1AI22b)",
                List.of(subject("P", "6K", "ZS", "C - dobre (2)"), subject("Q", "6K", "ZS", "B - veľmi dobre (1,5)")),
                List.of(exam("P", "08.01.2025", "08:00"), exam("P", "10.01.2025", "08:00"),
                        exam("P", "13.01.2025", "08:00"), exam("Q", "20.01.2025", "10:00"),
                        exam("Q", "20.01.2027", "07:30"))); // budúci termín sa neráta

        ProgressService.Report report = service.compute(List.of(year), 180, null, TODAY);

        assertTrue(find(report, "never-give-up").unlocked());
        ProgressService.Achievement marathon = find(report, "marathon");
        assertTrue(marathon.unlocked());
        assertEquals(LocalDate.of(2025, 1, 13), marathon.achievedOn());
        ProgressService.Achievement early = find(report, "early-bird");
        assertEquals(1, early.achievedTier());
        assertEquals("3 / 5", early.progressText());
        assertEquals(1, report.firstTryCount());
    }

    @Test
    void timelineSplitsSemesters() {
        ProgressService.EnrollmentInput year = new ProgressService.EnrollmentInput("2025/2026 (2AI22b)",
                List.of(subject("X", "6K", "ZS", "A - výborne (1)"), subject("Y", "4K", "LS", "-")), List.of());
        List<ProgressService.SemesterStat> timeline = service.compute(List.of(year), 180, null, TODAY).timeline();
        assertEquals(2, timeline.size());
        assertEquals("ZS 2025/2026", timeline.get(0).label());
        assertEquals(6, timeline.get(0).credits());
        assertEquals("1,00", timeline.get(0).average());
        assertEquals(0, timeline.get(1).completed());
        assertNotNull(service.compute(List.of(), 180, null, TODAY).rank());
    }
}
