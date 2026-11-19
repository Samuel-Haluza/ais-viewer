package sk.ukf.aisviewer.service;

import sk.ukf.aisviewer.model.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public class StudyPlanCreditsService {

    public int plannedCredits(StudyPlanCatalog catalog, UserStudyPlan plan, List<Subject> aisSubjects) {
        return plannedCredits(catalog, plan, aisSubjects, subject -> true);
    }

    public int plannedCompulsoryElectiveCredits(StudyPlanCatalog catalog, UserStudyPlan plan,
                                                List<Subject> aisSubjects) {
        return plannedCredits(catalog, plan, aisSubjects, StudyPlanCreditsService::isCompulsoryElective);
    }

    /**
     * Celkové získané kredity – zo všetkých splnených predmetov v AIS, rovnako ako
     * záložka Kredity. Nezávisí od toho, či je predmet v lokálnom katalógu.
     */
    public int acquiredTotalCredits(List<Subject> aisSubjects) {
        return CompletedSubjectsService.acquiredAisCredits(aisSubjects);
    }

    public int remainingCompulsoryElectiveCredits(StudyPlanCatalog catalog, List<Subject> aisSubjects,
                                                  UserStudyPlan plan) {
        int acquired = acquiredCredits(catalog, aisSubjects, StudyPlanCreditsService::isCompulsoryElective);
        return Math.max(0, catalog.getRequiredCompulsoryElectiveCredits()
                - acquired - plannedCompulsoryElectiveCredits(catalog, plan, aisSubjects));
    }

    /** Kredity naplánovaných predmetov – bez už splnených a bez duplicít v pláne. */
    private int plannedCredits(StudyPlanCatalog catalog, UserStudyPlan plan, List<Subject> aisSubjects,
                               Predicate<CurriculumSubject> filter) {
        Set<String> completed = CompletedSubjectsService.completedCodes(catalog, aisSubjects);
        Set<String> counted = new HashSet<>();
        return plan.getPlannedSubjects().stream()
                .map(item -> SubjectCompletionService.extractSubjectAbbreviation(item.getSubjectCode()))
                .filter(code -> !completed.contains(code) && counted.add(code))
                .mapToInt(code -> creditsOf(catalog, code, filter))
                .sum();
    }

    /** Kredity splnených predmetov katalógu. */
    private int acquiredCredits(StudyPlanCatalog catalog, List<Subject> aisSubjects,
                                Predicate<CurriculumSubject> filter) {
        Set<String> completed = CompletedSubjectsService.completedCodes(catalog, aisSubjects);
        return catalog.getSubjects().stream()
                .filter(filter)
                .filter(subject -> completed.contains(CompletedSubjectsService.abbreviation(subject)))
                .mapToInt(CurriculumSubject::getCredits)
                .sum();
    }

    private int creditsOf(StudyPlanCatalog catalog, String code, Predicate<CurriculumSubject> filter) {
        return catalog.getSubjects().stream()
                .filter(subject -> CompletedSubjectsService.abbreviation(subject).equals(code))
                .filter(filter)
                .mapToInt(CurriculumSubject::getCredits)
                .findFirst()
                .orElse(0);
    }

    private static boolean isCompulsoryElective(CurriculumSubject subject) {
        return subject.getType() == SubjectType.COMPULSORY_ELECTIVE;
    }
}
