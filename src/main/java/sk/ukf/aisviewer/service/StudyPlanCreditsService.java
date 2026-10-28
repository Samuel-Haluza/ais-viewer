package sk.ukf.aisviewer.service;

import sk.ukf.aisviewer.model.*;

import java.util.*;
import java.util.stream.Collectors;

public class StudyPlanCreditsService {
    public int plannedCredits(StudyPlanCatalog catalog, UserStudyPlan plan, List<Subject> aisSubjects) {
        Set<String> completed = completedCodes(catalog, aisSubjects);
        Set<String> counted = new HashSet<>();
        return plan.getPlannedSubjects().stream()
                .filter(item -> {
                    String code = SubjectCompletionService.extractSubjectAbbreviation(item.getSubjectCode());
                    return !completed.contains(code) && counted.add(code);
                })
                .mapToInt(item -> catalog.getSubjects().stream()
                        .filter(subject -> SubjectCompletionService.extractSubjectAbbreviation(subject.getCode())
                                .equals(SubjectCompletionService.extractSubjectAbbreviation(
                                        item.getSubjectCode())))
                        .mapToInt(CurriculumSubject::getCredits).findFirst().orElse(0))
                .sum();
    }

    public int acquiredTotalCredits(StudyPlanCatalog catalog, List<Subject> aisSubjects) {
        Set<String> completed = completedCodes(catalog, aisSubjects);
        return catalog.getSubjects().stream()
                .filter(subject -> completed.contains(
                        SubjectCompletionService.extractSubjectAbbreviation(subject.getCode())))
                .mapToInt(CurriculumSubject::getCredits)
                .sum();
    }

    public int plannedCompulsoryElectiveCredits(StudyPlanCatalog catalog, UserStudyPlan plan,
                                                List<Subject> aisSubjects) {
        Set<String> completed = completedCodes(catalog, aisSubjects);
        Set<String> counted = new HashSet<>();
        return plan.getPlannedSubjects().stream()
                .filter(item -> {
                    String code = SubjectCompletionService.extractSubjectAbbreviation(item.getSubjectCode());
                    return !completed.contains(code) && counted.add(code);
                })
                .map(item -> catalog.getSubjects().stream()
                        .filter(subject -> SubjectCompletionService.extractSubjectAbbreviation(subject.getCode())
                                .equals(SubjectCompletionService.extractSubjectAbbreviation(
                                        item.getSubjectCode())))
                        .filter(subject -> subject.getType() == SubjectType.COMPULSORY_ELECTIVE)
                        .mapToInt(CurriculumSubject::getCredits).findFirst().orElse(0))
                .reduce(0, Integer::sum);
    }

    public int remainingCompulsoryElectiveCredits(StudyPlanCatalog catalog, List<Subject> aisSubjects,
                                                  UserStudyPlan plan) {
        int acquired = acquiredCompulsoryElectiveCredits(catalog, aisSubjects);
        return Math.max(0, catalog.getRequiredCompulsoryElectiveCredits()
                - acquired - plannedCompulsoryElectiveCredits(catalog, plan, aisSubjects));
    }

    private int acquiredCompulsoryElectiveCredits(StudyPlanCatalog catalog, List<Subject> subjects) {
        Set<String> completed = completedCodes(catalog, subjects);
        return catalog.getSubjects().stream()
                .filter(subject -> subject.getType() == SubjectType.COMPULSORY_ELECTIVE)
                .filter(subject -> completed.contains(
                        SubjectCompletionService.extractSubjectAbbreviation(subject.getCode())))
                .mapToInt(CurriculumSubject::getCredits).sum();
    }

    private Set<String> completedCodes(StudyPlanCatalog catalog, List<Subject> subjects) {
        Set<String> aisCodes = subjects == null ? Set.of() : subjects.stream()
                .filter(SubjectCompletionService::isCompleted)
                .map(Subject::getAbbreviation).filter(Objects::nonNull)
                .map(SubjectCompletionService::normalizeCode).collect(Collectors.toSet());
        return catalog.getSubjects().stream()
                .filter(subject -> aisCodes.contains(
                        SubjectCompletionService.extractSubjectAbbreviation(subject.getCode()))
                        || subject.getEquivalentCodes().stream()
                        .map(SubjectCompletionService::extractSubjectAbbreviation)
                        .anyMatch(aisCodes::contains))
                .map(subject -> SubjectCompletionService.extractSubjectAbbreviation(subject.getCode()))
                .collect(Collectors.toSet());
    }
}
