package sk.ukf.aisviewer.service;

import sk.ukf.aisviewer.model.CurriculumSubject;
import sk.ukf.aisviewer.model.StudyPlanCatalog;
import sk.ukf.aisviewer.model.Subject;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Jediné miesto, ktoré rozhoduje, ktoré predmety z katalógu má študent splnené.
 * Používajú ho odporúčania aj výpočet kreditov, aby dávali rovnaký výsledok.
 */
public final class CompletedSubjectsService {

    private CompletedSubjectsService() {
    }

    /**
     * Vráti skratky (napr. "pjc") splnených predmetov katalógu
     * vrátane ich ekvivalentných (starších) kódov.
     */
    public static Set<String> completedCodes(StudyPlanCatalog catalog, List<Subject> aisSubjects) {
        Set<String> aisCodes = aisSubjects == null ? Set.of() : aisSubjects.stream()
                .filter(SubjectCompletionService::isCompleted)
                .map(Subject::getAbbreviation)
                .filter(Objects::nonNull)
                .map(SubjectCompletionService::extractSubjectAbbreviation)
                .collect(Collectors.toSet());
        Set<String> completed = new HashSet<>();
        if (catalog == null) {
            return completed;
        }
        catalog.getSubjects().stream()
                .filter(subject -> belongsToCatalog(subject, catalog))
                .filter(subject -> aisCodes.stream().anyMatch(aisCode -> codesEquivalent(aisCode, subject)))
                .forEach(subject -> {
                    completed.add(abbreviation(subject));
                    subject.getEquivalentCodes().stream()
                            .map(SubjectCompletionService::extractSubjectAbbreviation)
                            .forEach(completed::add);
                });
        return completed;
    }

    /**
     * Súčet kreditov všetkých splnených predmetov z AIS – aj tých, ktoré v lokálnom
     * katalógu chýbajú (napr. predmety iných katedier). Každý predmet sa ráta len raz.
     */
    public static int acquiredAisCredits(List<Subject> aisSubjects) {
        if (aisSubjects == null) {
            return 0;
        }
        Map<String, Subject> unique = new LinkedHashMap<>();
        for (Subject subject : aisSubjects) {
            if (!SubjectCompletionService.isCompleted(subject)) {
                continue;
            }
            unique.putIfAbsent(SubjectCompletionService.identityKey(subject), subject);
        }
        return unique.values().stream().mapToInt(Subject::getCreditsValue).sum();
    }

    /** Zhoduje sa kód z AIS s predmetom katalógu (aktuálny alebo ekvivalentný kód)? */
    public static boolean codesEquivalent(String aisCode, CurriculumSubject curriculumSubject) {
        String normalizedAisCode = SubjectCompletionService.normalizeCode(aisCode);
        return normalizedAisCode.equals(abbreviation(curriculumSubject))
                || curriculumSubject.getEquivalentCodes().stream()
                .map(SubjectCompletionService::extractSubjectAbbreviation)
                .anyMatch(normalizedAisCode::equals);
    }

    /** Patrí predmet do daného katalógu (Bc./Mgr., denná/externá forma)? */
    public static boolean belongsToCatalog(CurriculumSubject subject, StudyPlanCatalog catalog) {
        return subject.getDegreeLevel() == null || catalog.getDegreeLevel() == null
                || subject.getDegreeLevel() == catalog.getDegreeLevel()
                && (subject.getStudyForm() == null || catalog.getStudyForm() == null
                || subject.getStudyForm() == catalog.getStudyForm());
    }

    /** "KI/PJC/22" → "pjc". */
    public static String abbreviation(CurriculumSubject subject) {
        return SubjectCompletionService.extractSubjectAbbreviation(subject.getCode());
    }
}
