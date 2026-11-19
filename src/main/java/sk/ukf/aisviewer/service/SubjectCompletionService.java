package sk.ukf.aisviewer.service;

import sk.ukf.aisviewer.model.Subject;

import java.util.Locale;

public final class SubjectCompletionService {
    private SubjectCompletionService() {
    }

    public static boolean isCompleted(Subject subject) {
        if (subject == null || subject.getGrade() == null) {
            return false;
        }
        String grade = subject.getGrade().trim();
        if (grade.isBlank() || "-".equals(grade)) {
            return false;
        }
        String normalized = grade.toLowerCase(Locale.ROOT);
        // Neúspech: FX alebo "neabsolvoval/a" (obsahuje "abs", preto skôr než kontrola nižšie).
        if (subject.isFailingGrade() || normalized.contains("neabsolv")) {
            return false;
        }
        if (subject.getGradeNumeric() > 0) {
            return true;
        }
        return normalized.startsWith("abs") || normalized.contains("absolv")
                || normalized.matches("^[a-e](?:\\b|\\s|-|\\().*");
    }

    public static String decisionDetails(Subject subject) {
        if (subject == null) return "subject=null";
        return "grade=[" + subject.getGrade() + "], gradeType=[" + subject.getGradeType()
                + "], credits=[" + subject.getCredits() + "], semester=[" + subject.getSemester()
                + "], completed=" + isCompleted(subject);
    }

    public static String normalizeCode(String code) {
        if (code == null) {
            return "";
        }
        return code.replace('\u00a0', ' ')
                .replaceAll("[\\p{Z}\\p{C}]", "")
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    /**
     * Kľúč, podľa ktorého sa spozná ten istý predmet v rôznych zápisných listoch:
     * skratka predmetu ("PROG", "KI/PROG/22" → "prog"), ak chýba, tak názov.
     */
    public static String identityKey(Subject subject) {
        if (subject == null) {
            return "";
        }
        String code = extractSubjectAbbreviation(subject.getAbbreviation());
        if (!code.isEmpty() && !"-".equals(code)) {
            return code;
        }
        return subject.getName() == null ? "" : subject.getName().trim().toLowerCase(Locale.ROOT);
    }

    public static String extractSubjectAbbreviation(String curriculumCode) {
        String normalized = normalizeCode(curriculumCode);
        String[] parts = normalized.split("/");
        if (parts.length >= 3) {
            return parts[parts.length - 2];
        }
        return normalized;
    }
}
