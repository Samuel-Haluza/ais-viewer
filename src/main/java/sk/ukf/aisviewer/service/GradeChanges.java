package sk.ukf.aisviewer.service;

import sk.ukf.aisviewer.model.Subject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Porovná predmety pred a po obnovení z AIS a nájde nové alebo zmenené hodnotenia. */
public final class GradeChanges {

    /** Jedna zmena, napr. Operačné systémy: "-" → "B - veľmi dobre (1,5)". */
    public record Change(String subjectName, String oldGrade, String newGrade) {
        public boolean isNew() {
            return oldGrade == null || oldGrade.isBlank() || "-".equals(oldGrade.trim());
        }
    }

    private GradeChanges() {
    }

    /**
     * @param before predmety z cache (null = prvé načítanie → žiadne upozornenia)
     * @param after  práve načítané predmety
     */
    public static List<Change> detect(List<Subject> before, List<Subject> after) {
        List<Change> changes = new ArrayList<>();
        if (before == null || after == null) {
            return changes;
        }
        Map<String, Subject> old = new HashMap<>();
        for (Subject subject : before) {
            if (subject != null) old.put(SubjectCompletionService.identityKey(subject), subject);
        }
        for (Subject subject : after) {
            if (subject == null || !hasGrade(subject.getGrade())) continue;
            Subject previous = old.get(SubjectCompletionService.identityKey(subject));
            String previousGrade = previous == null ? null : previous.getGrade();
            if (!Objects.equals(normalize(previousGrade), normalize(subject.getGrade()))) {
                changes.add(new Change(subject.getName(), previousGrade, subject.getGrade()));
            }
        }
        return changes;
    }

    private static boolean hasGrade(String grade) {
        return grade != null && !grade.isBlank() && !"-".equals(grade.trim());
    }

    private static String normalize(String grade) {
        return hasGrade(grade) ? grade.trim() : "";
    }
}
