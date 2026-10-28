package sk.ukf.aisviewer.service;

import org.junit.jupiter.api.Test;
import sk.ukf.aisviewer.model.*;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RecommendationServiceTest {
    @Test
    void olderCompletedVersionCompletesCurrentCatalogSubject() {
        CurriculumSubject current = subject("KI/PJC/26", SubjectType.COMPULSORY_ELECTIVE,
                Set.of(StudyDirection.ALGORITHMS), 5);
        current.setEquivalentCodes(List.of("KI/PJC/22"));
        StudyPlanCatalog catalog = catalog(current);
        Subject completed = new Subject("Programovanie v jazyku C", "KI/PJC/22",
                "3", "ZS", "S", "A", "", "");
        assertTrue(new RecommendationService().recommendations(catalog, List.of(completed), List.of(),
                StudyDirection.ALGORITHMS, null).isEmpty());
    }

    @Test
    void directionPriorityIsDeterministic() {
        CurriculumSubject both = subject("B", SubjectType.COMPULSORY_ELECTIVE,
                Set.of(StudyDirection.ALGORITHMS, StudyDirection.WEB_DEVELOPMENT), 1);
        CurriculumSubject primary = subject("P", SubjectType.COMPULSORY_ELECTIVE,
                Set.of(StudyDirection.ALGORITHMS), 1);
        CurriculumSubject secondary = subject("S", SubjectType.COMPULSORY_ELECTIVE,
                Set.of(StudyDirection.WEB_DEVELOPMENT), 1);
        List<Recommendation> result = new RecommendationService().recommendations(
                catalog(both, primary, secondary), List.of(), List.of(),
                StudyDirection.ALGORITHMS, StudyDirection.WEB_DEVELOPMENT);
        assertEquals("B", result.get(0).getSubject().getCode());
        assertEquals(RecommendationReason.BOTH_DIRECTIONS, result.get(0).getReason());
    }

    @Test
    void prerequisiteCanBePlannedBeforeDependentSubject() {
        CurriculumSubject prerequisite = subject("PRE", SubjectType.COMPULSORY_ELECTIVE,
                Set.of(), 1);
        CurriculumSubject dependent = subject("DEP", SubjectType.COMPULSORY_ELECTIVE,
                Set.of(StudyDirection.ALGORITHMS), 2);
        dependent.setPrerequisiteCodes(List.of("PRE"));
        List<Recommendation> result = new RecommendationService().recommendations(
                catalog(prerequisite, dependent), List.of(),
                List.of(new PlannedSubject("PRE", 1, Semester.WINTER)),
                StudyDirection.ALGORITHMS, null);
        assertTrue(result.stream().filter(r -> r.getSubject().getCode().equals("DEP"))
                .findFirst().orElseThrow().isPrerequisitesMet());
    }

    @Test
    void exactCompletedPrerequisiteIsSatisfied() {
        CurriculumSubject prerequisite = subject("PRE", SubjectType.COMPULSORY_ELECTIVE,
                Set.of(), 1);
        CurriculumSubject dependent = subject("DEP", SubjectType.COMPULSORY_ELECTIVE,
                Set.of(StudyDirection.ALGORITHMS), 2);
        dependent.setPrerequisiteCodes(List.of("PRE"));
        Recommendation recommendation = new RecommendationService().recommendations(
                catalog(prerequisite, dependent),
                List.of(new Subject("Pre", " PRE ", "3", "ZS", "S", "A", "", "")),
                List.of(), StudyDirection.ALGORITHMS, null).stream()
                .filter(item -> item.getSubject().getCode().equals("DEP"))
                .findFirst().orElseThrow();
        assertTrue(recommendation.isPrerequisitesMet());
    }

    @Test
    void equivalentCompletedPrerequisiteIsSatisfied() {
        CurriculumSubject prerequisite = subject("PRE/26", SubjectType.COMPULSORY_ELECTIVE,
                Set.of(), 1);
        prerequisite.setEquivalentCodes(List.of("PRE/22"));
        CurriculumSubject dependent = subject("DEP", SubjectType.COMPULSORY_ELECTIVE,
                Set.of(StudyDirection.ALGORITHMS), 2);
        dependent.setPrerequisiteCodes(List.of("PRE/26"));
        Recommendation recommendation = new RecommendationService().recommendations(
                catalog(prerequisite, dependent),
                List.of(new Subject("Pre", "PRE/22\u00a0", "3", "ZS", "S", "A", "", "")),
                List.of(), StudyDirection.ALGORITHMS, null).stream()
                .filter(item -> item.getSubject().getCode().equals("DEP"))
                .findFirst().orElseThrow();
        assertTrue(recommendation.isPrerequisitesMet());
    }

    @Test
    void completedSubjectCannotBePlannedAgain() {
        CurriculumSubject subject = subject("DONE", SubjectType.COMPULSORY_ELECTIVE,
                Set.of(), 1);
        List<Subject> completed = List.of(
                new Subject("Done", "DONE", "3", "ZS", "S", "A", "", ""));
        RecommendationService service = new RecommendationService();
        assertTrue(service.recommendations(catalog(subject), completed, List.of(), null, null).isEmpty());
        assertTrue(service.otherSubjects(catalog(subject), completed, List.of()).isEmpty());
    }

    @Test
    void bachelorAndMasterSubjectsDoNotCrossCatalogBoundary() {
        CurriculumSubject bachelor = subject("BC", SubjectType.COMPULSORY_ELECTIVE,
                Set.of(StudyDirection.SOFTWARE_DEVELOPMENT), 1);
        bachelor.setDegreeLevel(DegreeLevel.BACHELOR);
        CurriculumSubject master = subject("MGR", SubjectType.COMPULSORY_ELECTIVE,
                Set.of(StudyDirection.SOFTWARE_DEVELOPMENT), 1);
        master.setDegreeLevel(DegreeLevel.MASTER);
        StudyPlanCatalog bachelorCatalog = catalog(bachelor, master);
        bachelorCatalog.setDegreeLevel(DegreeLevel.BACHELOR);
        bachelorCatalog.setStudyForm(StudyForm.FULL_TIME);
        assertEquals(List.of("BC"), new RecommendationService()
                .recommendations(bachelorCatalog, List.of(), List.of(),
                        StudyDirection.SOFTWARE_DEVELOPMENT, null).stream()
                .map(item -> item.getSubject().getCode()).toList());
        StudyPlanCatalog masterCatalog = catalog(bachelor, master);
        masterCatalog.setDegreeLevel(DegreeLevel.MASTER);
        masterCatalog.setStudyForm(StudyForm.FULL_TIME);
        assertEquals(List.of("MGR"), new RecommendationService()
                .otherSubjects(masterCatalog, List.of(), List.of()).stream()
                .map(item -> item.getSubject().getCode()).toList());
    }

    @Test
    void subjectWithoutPrerequisiteShowsNoPrerequisite() {
        CurriculumSubject subject = subject("NONE", SubjectType.COMPULSORY_ELECTIVE,
                Set.of(StudyDirection.ALGORITHMS), 1);
        Recommendation recommendation = new RecommendationService().recommendations(
                catalog(subject), List.of(), List.of(),
                StudyDirection.ALGORITHMS, null).get(0);
        assertEquals(PrerequisiteStatus.NO_PREREQUISITE, recommendation.getPrerequisiteStatus());
    }

    @Test
    void subjectWithCompletedPrerequisiteShowsSatisfied() {
        CurriculumSubject prerequisite = subject("PRE", SubjectType.COMPULSORY_ELECTIVE,
                Set.of(), 1);
        CurriculumSubject dependent = subject("DEP", SubjectType.COMPULSORY_ELECTIVE,
                Set.of(StudyDirection.ALGORITHMS), 2);
        dependent.setPrerequisiteCodes(List.of("PRE"));
        Recommendation recommendation = new RecommendationService().recommendations(
                catalog(prerequisite, dependent),
                List.of(new Subject("Pre", "PRE", "3", "ZS", "S", "A", "", "")),
                List.of(), StudyDirection.ALGORITHMS, null).stream()
                .filter(item -> item.getSubject().getCode().equals("DEP"))
                .findFirst().orElseThrow();
        assertEquals(PrerequisiteStatus.SATISFIED, recommendation.getPrerequisiteStatus());
    }

    @Test
    void subjectWithMissingPrerequisiteShowsNotSatisfied() {
        CurriculumSubject dependent = subject("DEP", SubjectType.COMPULSORY_ELECTIVE,
                Set.of(StudyDirection.ALGORITHMS), 2);
        dependent.setPrerequisiteCodes(List.of("PRE"));
        Recommendation recommendation = new RecommendationService().recommendations(
                catalog(dependent), List.of(), List.of(),
                StudyDirection.ALGORITHMS, null).get(0);
        assertEquals(PrerequisiteStatus.NOT_SATISFIED, recommendation.getPrerequisiteStatus());
    }

    @Test
    void otherSubjectIsNotRecommended() {
        CurriculumSubject subject = subject("OTHER", SubjectType.COMPULSORY_ELECTIVE,
                Set.of(), 1);
        RecommendationService service = new RecommendationService();
        assertTrue(service.recommendations(catalog(subject), List.of(), List.of(),
                StudyDirection.ALGORITHMS, null).isEmpty());
        assertEquals(List.of("OTHER"), service.otherSubjects(catalog(subject), List.of(),
                List.of(), StudyDirection.ALGORITHMS, null).stream()
                .map(item -> item.getSubject().getCode()).toList());
    }

    @Test
    void recommendedSubjectIsNotInOtherCollection() {
        CurriculumSubject subject = subject("PRIMARY", SubjectType.COMPULSORY_ELECTIVE,
                Set.of(StudyDirection.ALGORITHMS), 1);
        RecommendationService service = new RecommendationService();
        List<Recommendation> recommended = service.recommendations(catalog(subject), List.of(),
                List.of(), StudyDirection.ALGORITHMS, null);
        List<Recommendation> other = service.otherSubjects(catalog(subject), List.of(), List.of(),
                StudyDirection.ALGORITHMS, null);
        assertEquals(Set.of("PRIMARY"), codes(recommended));
        assertTrue(other.isEmpty());
    }

    @Test
    void noSubjectAppearsInMultipleSections() {
        CurriculumSubject mandatory = subject("MANDATORY", SubjectType.MANDATORY, Set.of(), 1);
        CurriculumSubject recommended = subject("RECOMMENDED", SubjectType.COMPULSORY_ELECTIVE,
                Set.of(StudyDirection.ALGORITHMS), 1);
        CurriculumSubject other = subject("OTHER", SubjectType.COMPULSORY_ELECTIVE, Set.of(), 1);
        RecommendationService service = new RecommendationService();
        Set<String> mandatoryCodes = codes(service.mandatory(catalog(mandatory, recommended, other),
                List.of(), List.of()));
        Set<String> recommendedCodes = codes(service.recommendations(
                catalog(mandatory, recommended, other), List.of(), List.of(),
                StudyDirection.ALGORITHMS, null));
        Set<String> otherCodes = codes(service.otherSubjects(catalog(mandatory, recommended, other),
                List.of(), List.of(), StudyDirection.ALGORITHMS, null));
        assertTrue(disjoint(mandatoryCodes, recommendedCodes));
        assertTrue(disjoint(mandatoryCodes, otherCodes));
        assertTrue(disjoint(recommendedCodes, otherCodes));
    }

    @Test
    void mandatorySubjectIsOnlyInMandatorySection() {
        CurriculumSubject mandatory = subject("MANDATORY", SubjectType.MANDATORY,
                Set.of(StudyDirection.ALGORITHMS), 1);
        RecommendationService service = new RecommendationService();
        assertEquals(Set.of("MANDATORY"), codes(service.mandatory(catalog(mandatory),
                List.of(), List.of())));
        assertTrue(service.recommendations(catalog(mandatory), List.of(), List.of(),
                StudyDirection.ALGORITHMS, null).isEmpty());
        assertTrue(service.otherSubjects(catalog(mandatory), List.of(), List.of(),
                StudyDirection.ALGORITHMS, null).isEmpty());
    }

    @Test
    void completedSubjectFromOlderEnrollmentIsNotRecommended() {
        CurriculumSubject subject = subject("OLD", SubjectType.COMPULSORY_ELECTIVE,
                Set.of(StudyDirection.ALGORITHMS), 1);
        LocalCacheService.CacheSnapshot snapshot = LocalCacheService.createSnapshot(null);
        snapshot.getEnrollmentData().put("1AI22b", LocalCacheService.EnrollmentData.of(
                List.of(new Subject("Old", "OLD", "3", "ZS", "S", "A", "", "")),
                List.of(), List.of()));
        List<Subject> allSubjects = StudyPlanCompletionService.aggregateSubjects(List.of(), snapshot);
        RecommendationService service = new RecommendationService();
        assertTrue(service.recommendations(catalog(subject), allSubjects, List.of(),
                StudyDirection.ALGORITHMS, null).isEmpty());
        assertTrue(service.mandatory(catalog(subject), allSubjects, List.of()).isEmpty());
    }

    @Test
    void completedSubjectFromRealisticHistoricalEnrollmentIsFilteredEverywhere() {
        CurriculumSubject subject = subject("KI/PJC/26", SubjectType.COMPULSORY_ELECTIVE,
                Set.of(StudyDirection.ALGORITHMS), 1);
        LocalCacheService.CacheSnapshot snapshot = LocalCacheService.createSnapshot(null);
        snapshot.getEnrollmentData().put("1AI22b", LocalCacheService.EnrollmentData.of(
                List.of(new Subject("Programovanie v jazyku C", "KI/PJC/26",
                        "5 K", "ZS", "S", "A", "-", "Povinne voliteľné predmety")),
                List.of(), List.of()));
        List<Subject> all = StudyPlanCompletionService.aggregateSubjects(
                List.of(), snapshot);
        RecommendationService service = new RecommendationService();
        StudyPlanCatalog studyPlanCatalog = catalog(subject);
        UserStudyPlan plan = new UserStudyPlan();
        assertTrue(service.mandatory(studyPlanCatalog, all, plan.getPlannedSubjects()).isEmpty());
        assertTrue(service.recommendations(studyPlanCatalog, all, plan.getPlannedSubjects(),
                StudyDirection.ALGORITHMS, null).isEmpty());
        assertTrue(service.otherSubjects(studyPlanCatalog, all, plan.getPlannedSubjects(),
                StudyDirection.ALGORITHMS, null).isEmpty());
    }

    @Test
    void completedEquivalentOldVersionIsFilteredEverywhere() {
        CurriculumSubject subject = subject("KI/PJC/26", SubjectType.COMPULSORY_ELECTIVE,
                Set.of(StudyDirection.ALGORITHMS), 1);
        subject.setEquivalentCodes(List.of("KI/PJC/22"));
        Subject oldVersion = new Subject("Programovanie v jazyku C", " KI/PJC/22\u00a0",
                "5 K", "ZS", "S", "A", "-", "Povinne voliteľné predmety");
        List<Subject> all = StudyPlanCompletionService.aggregateSubjects(List.of(oldVersion), null);
        RecommendationService service = new RecommendationService();
        StudyPlanCatalog studyPlanCatalog = catalog(subject);
        assertTrue(service.completedCodes(studyPlanCatalog, all).contains("pjc"));
        assertTrue(service.recommendations(studyPlanCatalog, all, List.of(),
                StudyDirection.ALGORITHMS, null).isEmpty());
        assertTrue(service.otherSubjects(studyPlanCatalog, all, List.of(),
                StudyDirection.ALGORITHMS, null).isEmpty());
    }

    @Test
    void aisAbbreviationMatchesFullCurriculumCode() {
        CurriculumSubject subject = subject("KI/APG/22", SubjectType.COMPULSORY_ELECTIVE,
                Set.of(StudyDirection.ALGORITHMS), 1);
        Subject aisSubject = new Subject("Aplikovaná počítačová grafika", "APG",
                "5 K", "ZS", "S", "A", "-", "Povinne voliteľné predmety");
        RecommendationService service = new RecommendationService();
        StudyPlanCatalog studyPlanCatalog = catalog(subject);
        assertEquals("apg", SubjectCompletionService.extractSubjectAbbreviation(subject.getCode()));
        assertTrue(service.completedCodes(studyPlanCatalog, List.of(aisSubject)).contains("apg"));
        assertTrue(service.recommendations(studyPlanCatalog, List.of(aisSubject), List.of(),
                StudyDirection.ALGORITHMS, null).isEmpty());
    }

    @Test
    void aisAbbreviationMatchesCurrentVersion() {
        CurriculumSubject subject = subject("KI/PJC/26", SubjectType.COMPULSORY_ELECTIVE,
                Set.of(StudyDirection.ALGORITHMS), 1);
        subject.setEquivalentCodes(List.of("KI/PJC/22"));
        Subject aisSubject = new Subject("Programovanie v jazyku C", "PJC",
                "5 K", "ZS", "S", "A", "-", "Povinne voliteľné predmety");
        RecommendationService service = new RecommendationService();
        assertTrue(service.codesEquivalent("PJC", subject));
        assertTrue(service.recommendations(catalog(subject), List.of(aisSubject), List.of(),
                StudyDirection.ALGORITHMS, null).isEmpty());
    }

    @Test
    void completedAbbreviationIsFilteredFromAllPlanningCollections() {
        CurriculumSubject subject = subject("KI/HVP/22", SubjectType.COMPULSORY_ELECTIVE,
                Set.of(StudyDirection.ALGORITHMS), 1);
        Subject aisSubject = new Subject("Herné vývojové prostredia", "HVP",
                "5 K", "ZS", "S", "A", "-", "Povinne voliteľné predmety");
        RecommendationService service = new RecommendationService();
        StudyPlanCatalog studyPlanCatalog = catalog(subject);
        assertTrue(service.mandatory(studyPlanCatalog, List.of(aisSubject), List.of()).isEmpty());
        assertTrue(service.recommendations(studyPlanCatalog, List.of(aisSubject), List.of(),
                StudyDirection.ALGORITHMS, null).isEmpty());
        assertTrue(service.otherSubjects(studyPlanCatalog, List.of(aisSubject), List.of(),
                StudyDirection.ALGORITHMS, null).isEmpty());
    }

    @Test
    void ThirdYearStudentUsesAllThreeEnrollmentYears() {
        CurriculumSubject a = subject("KI/A/22", SubjectType.COMPULSORY_ELECTIVE,
                Set.of(StudyDirection.ALGORITHMS), 1);
        CurriculumSubject b = subject("KI/B/22", SubjectType.COMPULSORY_ELECTIVE,
                Set.of(StudyDirection.ALGORITHMS), 1);
        CurriculumSubject c = subject("KI/C/22", SubjectType.COMPULSORY_ELECTIVE,
                Set.of(StudyDirection.ALGORITHMS), 2);
        CurriculumSubject d = subject("KI/D/22", SubjectType.COMPULSORY_ELECTIVE,
                Set.of(StudyDirection.ALGORITHMS), 2);
        CurriculumSubject e = subject("KI/E/22", SubjectType.COMPULSORY_ELECTIVE,
                Set.of(StudyDirection.ALGORITHMS), 3);
        StudyPlanCatalog studyPlanCatalog = catalog(a, b, c, d, e);
        LocalCacheService.CacheSnapshot snapshot = LocalCacheService.createSnapshot(null);
        snapshot.getEnrollmentData().put("1AI22b", LocalCacheService.EnrollmentData.of(
                List.of(ais("A"), ais("B")), List.of(), List.of()));
        snapshot.getEnrollmentData().put("2AI22b", LocalCacheService.EnrollmentData.of(
                List.of(ais("C"), ais("D")), List.of(), List.of()));
        snapshot.getEnrollmentData().put("3AI22b", LocalCacheService.EnrollmentData.of(
                List.of(ais("E", "-"), ais("F", "-")), List.of(), List.of()));
        List<Subject> all = StudyPlanCompletionService.aggregateSubjects(List.of(), snapshot);
        RecommendationService service = new RecommendationService();
        assertEquals(Set.of("a", "b", "c", "d"), service.completedCodes(studyPlanCatalog, all));
        assertTrue(service.mandatory(studyPlanCatalog, all, List.of()).stream()
                .noneMatch(item -> Set.of("KI/A/22", "KI/B/22", "KI/C/22", "KI/D/22")
                        .contains(item.getSubject().getCode())));
        assertTrue(service.recommendations(studyPlanCatalog, all, List.of(),
                StudyDirection.ALGORITHMS, null).stream()
                .noneMatch(item -> Set.of("KI/A/22", "KI/B/22", "KI/C/22", "KI/D/22")
                        .contains(item.getSubject().getCode())));
        assertTrue(service.otherSubjects(studyPlanCatalog, all, List.of(),
                StudyDirection.ALGORITHMS, null).stream()
                .noneMatch(item -> Set.of("KI/A/22", "KI/B/22", "KI/C/22", "KI/D/22")
                        .contains(item.getSubject().getCode())));
        assertEquals(12, new StudyPlanCreditsService().acquiredTotalCredits(
                studyPlanCatalog, all));
    }

    private Subject ais(String code) {
        return ais(code, "A");
    }

    private Subject ais(String code, String grade) {
        return new Subject(code, code, "3 K", "ZS", "S", grade, "-", "");
    }

    @Test
    void onlyDirectionSubjectsAppearInRecommendedSection() {
        CurriculumSubject primary = subject("PRIMARY", SubjectType.COMPULSORY_ELECTIVE,
                Set.of(StudyDirection.ALGORITHMS), 1);
        CurriculumSubject other = subject("OTHER", SubjectType.COMPULSORY_ELECTIVE,
                Set.of(StudyDirection.WEB_DEVELOPMENT), 1);
        List<Recommendation> result = new RecommendationService().recommendations(
                catalog(primary, other), List.of(), List.of(),
                StudyDirection.ALGORITHMS, null);
        assertEquals(List.of("PRIMARY"), result.stream()
                .map(item -> item.getSubject().getCode()).toList());
    }

    @Test
    void otherSubjectsAreAvailableOnlyThroughAllSubjectsView() {
        CurriculumSubject other = subject("OTHER", SubjectType.COMPULSORY_ELECTIVE,
                Set.of(StudyDirection.WEB_DEVELOPMENT), 1);
        RecommendationService service = new RecommendationService();
        assertTrue(service.recommendations(catalog(other), List.of(), List.of(),
                StudyDirection.ALGORITHMS, null).isEmpty());
        assertEquals(List.of("OTHER"), service.otherSubjects(catalog(other), List.of(), List.of(),
                StudyDirection.ALGORITHMS, null).stream()
                .map(item -> item.getSubject().getCode()).toList());
    }

    private Set<String> codes(List<Recommendation> recommendations) {
        Set<String> codes = new HashSet<>();
        recommendations.forEach(item -> codes.add(item.getSubject().getCode()));
        return codes;
    }

    private boolean disjoint(Set<String> left, Set<String> right) {
        Set<String> intersection = new HashSet<>(left);
        intersection.retainAll(right);
        return intersection.isEmpty();
    }

    private CurriculumSubject subject(String code, SubjectType type, Set<StudyDirection> directions,
                                      int year) {
        CurriculumSubject result = new CurriculumSubject(code, code, 3, DegreeLevel.BACHELOR,
                StudyForm.FULL_TIME, type, year, Semester.WINTER);
        result.setDirections(directions);
        return result;
    }

    private StudyPlanCatalog catalog(CurriculumSubject... subjects) {
        StudyPlanCatalog catalog = new StudyPlanCatalog();
        catalog.setSubjects(List.of(subjects));
        return catalog;
    }
}
