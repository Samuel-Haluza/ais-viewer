package sk.ukf.aisviewer.service;

import sk.ukf.aisviewer.model.*;

import java.util.*;
import java.util.stream.Collectors;

public class RecommendationService {
    public List<Recommendation> mandatory(StudyPlanCatalog catalog, List<Subject> aisSubjects,
                                           List<PlannedSubject> planned) {
        return recommendations(catalog, aisSubjects, planned, SubjectType.MANDATORY, true);
    }

    public List<Recommendation> recommendations(StudyPlanCatalog catalog, List<Subject> aisSubjects,
                                                List<PlannedSubject> planned) {
        return recommendations(catalog, aisSubjects, planned, SubjectType.COMPULSORY_ELECTIVE, false);
    }

    public List<Recommendation> otherSubjects(StudyPlanCatalog catalog, List<Subject> aisSubjects,
                                              List<PlannedSubject> planned) {
        return otherSubjects(catalog, aisSubjects, planned, null, null);
    }

    public List<Recommendation> otherSubjects(StudyPlanCatalog catalog, List<Subject> aisSubjects,
                                              List<PlannedSubject> planned,
                                              StudyDirection primary, StudyDirection secondary) {
        Set<String> completed = completedCodes(catalog, aisSubjects);
        Set<String> plannedCodes = plannedCodes(planned);
        return catalog.getSubjects().stream()
                .filter(subject -> belongsToCatalog(subject, catalog))
                .filter(subject -> subject.getType() == SubjectType.COMPULSORY_ELECTIVE)
                .filter(subject -> directionReason(subject, primary, secondary) == RecommendationReason.OTHER)
                .filter(subject -> !completed.contains(subjectAbbreviation(subject)))
                .filter(subject -> !plannedCodes.contains(subjectAbbreviation(subject)))
                .map(subject -> recommendation(subject, RecommendationReason.OTHER,
                        completed, planned))
                .sorted(comparator())
                .toList();
    }

    private List<Recommendation> recommendations(StudyPlanCatalog catalog, List<Subject> aisSubjects,
                                                 List<PlannedSubject> planned, SubjectType type,
                                                 boolean mandatory) {
        Set<String> completed = completedCodes(catalog, aisSubjects);
        Set<String> plannedCodes = plannedCodes(planned);
        return catalog.getSubjects().stream()
                .filter(subject -> belongsToCatalog(subject, catalog))
                .filter(subject -> subject.getType() == type)
                .filter(subject -> !completed.contains(subjectAbbreviation(subject)))
                .filter(subject -> !plannedCodes.contains(subjectAbbreviation(subject)))
                .map(subject -> {
                    RecommendationReason reason = mandatory ? RecommendationReason.MANDATORY
                            : directionReason(subject, null, null);
                    return recommendation(subject, reason, completed, planned);
                })
                .sorted(comparator())
                .toList();
    }

    public List<Recommendation> recommendations(StudyPlanCatalog catalog, List<Subject> aisSubjects,
                                                List<PlannedSubject> planned,
                                                StudyDirection primary, StudyDirection secondary) {
        Set<String> completed = completedCodes(catalog, aisSubjects);
        Set<String> plannedCodes = plannedCodes(planned);
        return catalog.getSubjects().stream()
                .filter(subject -> belongsToCatalog(subject, catalog))
                .filter(subject -> subject.getType() == SubjectType.COMPULSORY_ELECTIVE)
                .filter(subject -> !completed.contains(subjectAbbreviation(subject)))
                .filter(subject -> !plannedCodes.contains(subjectAbbreviation(subject)))
                .map(subject -> recommendation(subject, directionReason(subject, primary, secondary),
                        completed, planned))
                .filter(recommendation -> recommendation.getReason() != RecommendationReason.OTHER)
                .sorted(comparator())
                .toList();
    }

    private Recommendation recommendation(CurriculumSubject subject, RecommendationReason reason,
                                           Set<String> completed, List<PlannedSubject> planned) {
        List<String> prerequisites = subject.getPrerequisiteCodes().stream()
                .filter(Objects::nonNull)
                .map(SubjectCompletionService::extractSubjectAbbreviation)
                .filter(code -> !code.isBlank())
                .toList();
        if (prerequisites.isEmpty()) {
            return new Recommendation(subject, reason, PrerequisiteStatus.NO_PREREQUISITE);
        }
        boolean prerequisitesMet = prerequisites.stream()
                .allMatch(code -> completed.contains(code)
                        || plannedPrerequisiteCanBeMet(code, subject, planned));
        return new Recommendation(subject, reason, prerequisitesMet
                ? PrerequisiteStatus.SATISFIED
                : PrerequisiteStatus.NOT_SATISFIED);
    }

    private boolean plannedPrerequisiteCanBeMet(String code, CurriculumSubject subject,
                                                List<PlannedSubject> planned) {
        return planned != null && planned.stream().anyMatch(item ->
                SubjectCompletionService.extractSubjectAbbreviation(item.getSubjectCode()).equals(code)
                && (item.getPlannedYear() < subject.getRecommendedYear()
                || item.getPlannedYear() == subject.getRecommendedYear()
                && semesterBefore(item.getPlannedSemester(), subject.getSemester())));
    }

    private boolean semesterBefore(Semester planned, Semester target) {
        return planned == Semester.WINTER && target == Semester.SUMMER;
    }

    private RecommendationReason directionReason(CurriculumSubject subject,
                                                  StudyDirection primary, StudyDirection secondary) {
        boolean inPrimary = primary != null && subject.getDirections().contains(primary);
        boolean inSecondary = secondary != null && subject.getDirections().contains(secondary);
        if (inPrimary && inSecondary) return RecommendationReason.BOTH_DIRECTIONS;
        if (inPrimary) return RecommendationReason.PRIMARY_DIRECTION;
        if (inSecondary) return RecommendationReason.SECONDARY_DIRECTION;
        return RecommendationReason.OTHER;
    }

    public Set<String> completedCodes(StudyPlanCatalog catalog, List<Subject> subjects) {
        return CompletedSubjectsService.completedCodes(catalog, subjects);
    }

    public boolean codesEquivalent(String aisCode, CurriculumSubject curriculumSubject) {
        return CompletedSubjectsService.codesEquivalent(aisCode, curriculumSubject);
    }

    private String subjectAbbreviation(CurriculumSubject subject) {
        return CompletedSubjectsService.abbreviation(subject);
    }

    private boolean belongsToCatalog(CurriculumSubject subject, StudyPlanCatalog catalog) {
        return CompletedSubjectsService.belongsToCatalog(subject, catalog);
    }

    private Set<String> plannedCodes(List<PlannedSubject> planned) {
        return planned == null ? Set.of() : planned.stream()
                .map(PlannedSubject::getSubjectCode)
                .filter(Objects::nonNull)
                .map(SubjectCompletionService::extractSubjectAbbreviation)
                .collect(Collectors.toSet());
    }

    private Comparator<Recommendation> comparator() {
        return Comparator.comparingInt((Recommendation r) -> priority(r.getReason()))
                .thenComparingInt(r -> r.getSubject().getRecommendedYear())
                .thenComparing(r -> r.getSubject().getSemester().ordinal())
                .thenComparing(r -> r.getSubject().getCode());
    }

    private int priority(RecommendationReason reason) {
        return switch (reason) {
            case MANDATORY -> 0;
            case BOTH_DIRECTIONS -> 1;
            case PRIMARY_DIRECTION -> 2;
            case SECONDARY_DIRECTION -> 3;
            case OTHER -> 4;
        };
    }
}
