package sk.ukf.aisviewer.service;

import sk.ukf.aisviewer.AppLog;
import java.util.logging.Level;
import java.util.logging.Logger;

import sk.ukf.aisviewer.model.Subject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class StudyPlanCompletionService {
    private static final Logger LOG = AppLog.get(StudyPlanCompletionService.class);

    private StudyPlanCompletionService() {
    }

    public static List<Subject> aggregateSubjects(List<Subject> current,
                                                   LocalCacheService.CacheSnapshot snapshot) {
        Map<String, Subject> subjects = new LinkedHashMap<>();
        if (snapshot != null && snapshot.getEnrollmentData() != null) {
            snapshot.getEnrollmentData().forEach((year, data) -> {
                if (data == null || data.getSubjects() == null) return;
                data.getSubjects().forEach(subject -> {
                    logSubject(year, subject);
                    merge(subjects, subject);
                });
            });
        }
        if (current != null) current.forEach(subject -> merge(subjects, subject));
        return new ArrayList<>(subjects.values());
    }

    private static void logSubject(String enrollmentYear, Subject subject) {
        if (!LOG.isLoggable(Level.FINE)) return; // diagnostika len s -Dais.debug=true
        LOG.fine("enrollment=" + enrollmentYear
                + ", name=[" + (subject == null ? null : subject.getName())
                + "], abbreviation=[" + (subject == null ? null : subject.getAbbreviation())
                + "], credits=[" + (subject == null ? null : subject.getCredits())
                + "], semester=[" + (subject == null ? null : subject.getSemester())
                + "], gradeType=[" + (subject == null ? null : subject.getGradeType())
                + "], grade/result/status=[" + (subject == null ? null : subject.getGrade())
                + "], " + SubjectCompletionService.decisionDetails(subject));
    }

    private static void merge(Map<String, Subject> subjects, Subject subject) {
        if (subject == null || subject.getAbbreviation() == null) return;
        String code = SubjectCompletionService.normalizeCode(subject.getAbbreviation());
        subjects.merge(code, subject, (oldValue, newValue) ->
                SubjectCompletionService.isCompleted(newValue)
                        || !SubjectCompletionService.isCompleted(oldValue) ? newValue : oldValue);
    }
}
