package sk.ukf.aisviewer.controller;

import javafx.collections.FXCollections;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import sk.ukf.aisviewer.model.Subject;
import sk.ukf.aisviewer.service.LocalCacheService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CreditsController {

    private final Label totalCreditsLabel;
    private final Label mandatoryCreditsTotal;
    private final Label optionalCreditsTotal;
    private final Label avgGradeLabel;
    private final Label yearAvgGradeLabel;
    private final Label graduationCreditsLabel;
    private final ProgressBar graduationProgressBar;
    private final Label graduationProgressLabel;
    private final Label graduationRemainingLabel;
    private final PieChart creditsByCategoryChart;

    private LocalCacheService.CacheSnapshot cacheSnapshot;

    public CreditsController(Label totalCreditsLabel,
                             Label mandatoryCreditsTotal,
                             Label optionalCreditsTotal,
                             Label avgGradeLabel,
                             Label yearAvgGradeLabel,
                             Label graduationCreditsLabel,
                             ProgressBar graduationProgressBar,
                             Label graduationProgressLabel,
                             Label graduationRemainingLabel,
                             PieChart creditsByCategoryChart) {
        this.totalCreditsLabel = totalCreditsLabel;
        this.mandatoryCreditsTotal = mandatoryCreditsTotal;
        this.optionalCreditsTotal = optionalCreditsTotal;
        this.avgGradeLabel = avgGradeLabel;
        this.yearAvgGradeLabel = yearAvgGradeLabel;
        this.graduationCreditsLabel = graduationCreditsLabel;
        this.graduationProgressBar = graduationProgressBar;
        this.graduationProgressLabel = graduationProgressLabel;
        this.graduationRemainingLabel = graduationRemainingLabel;
        this.creditsByCategoryChart = creditsByCategoryChart;
    }

    public void updateCredits(List<Subject> subjects,
                              LocalCacheService.CacheSnapshot cacheSnapshot) {
        this.cacheSnapshot = cacheSnapshot;
        final int requiredCredits = 180;
        List<Subject> completedSubjects = getCompletedSubjectsFromAllEnrollments(subjects);
        int mandatory = completedSubjects.stream()
                .filter(s -> "Povinné predmety".equals(s.getCategory()))
                .mapToInt(Subject::getCreditsValue).sum();
        int optional = completedSubjects.stream()
                .filter(s -> "Povinne voliteľné predmety".equals(s.getCategory()))
                .mapToInt(Subject::getCreditsValue).sum();
        int total = completedSubjects.stream().mapToInt(Subject::getCreditsValue).sum();

        totalCreditsLabel.setText(String.valueOf(total));
        mandatoryCreditsTotal.setText(String.valueOf(mandatory));
        optionalCreditsTotal.setText(String.valueOf(optional));

        double progress = (double) total / requiredCredits;
        int remaining = Math.max(0, requiredCredits - total);
        double percentage = Math.min(100, progress * 100);
        graduationCreditsLabel.setText(total + " / " + requiredCredits + " kreditov");
        graduationProgressBar.setProgress(Math.min(1.0, progress));
        graduationProgressLabel.setText(String.format("%.0f %%", percentage));
        graduationRemainingLabel.setText("Zostáva " + remaining + " kreditov");

        creditsByCategoryChart.setData(FXCollections.observableArrayList(
                new PieChart.Data("Povinné predmety", mandatory),
                new PieChart.Data("Povinne voliteľné predmety", optional)));

        setWeightedAverageLabel(avgGradeLabel, completedSubjects);
        setWeightedAverageLabel(yearAvgGradeLabel, subjects);
    }

    private void setWeightedAverageLabel(Label label, List<Subject> subjects) {
        if (label == null || subjects == null) {
            return;
        }
        double weightedGradeSum = 0;
        int gradedCredits = 0;
        for (Subject subject : subjects) {
            if (!isCompletedSubject(subject)) {
                continue;
            }
            double grade = subject.getGradeNumeric();
            int credits = subject.getCreditsValue();
            if (grade > 0 && credits > 0) {
                weightedGradeSum += grade * credits;
                gradedCredits += credits;
            }
        }

        label.setText(gradedCredits > 0
                ? String.format("%.2f", weightedGradeSum / gradedCredits)
                : "-");
    }

    private List<Subject> getCompletedSubjectsFromAllEnrollments(List<Subject> currentSubjects) {
        Map<String, Subject> uniqueSubjects = new LinkedHashMap<>();
        if (cacheSnapshot != null && cacheSnapshot.getEnrollmentData() != null) {
            for (LocalCacheService.EnrollmentData enrollmentData
                    : cacheSnapshot.getEnrollmentData().values()) {
                if (enrollmentData == null || enrollmentData.getSubjects() == null) {
                    continue;
                }
                for (Subject subject : enrollmentData.getSubjects()) {
                    addCompletedSubject(uniqueSubjects, subject);
                }
            }
        }
        if (currentSubjects != null) {
            for (Subject subject : currentSubjects) {
                addCompletedSubject(uniqueSubjects, subject);
            }
        }
        return new ArrayList<>(uniqueSubjects.values());
    }

    private void addCompletedSubject(Map<String, Subject> uniqueSubjects, Subject subject) {
        if (!isCompletedSubject(subject)) {
            return;
        }
        String abbreviation = subject.getAbbreviation() == null
                ? "" : subject.getAbbreviation().trim();
        String name = subject.getName() == null ? "" : subject.getName().trim();
        String key = (abbreviation + "|" + name).toLowerCase();
        uniqueSubjects.putIfAbsent(key, subject);
    }

    private boolean isCompletedSubject(Subject subject) {
        if (subject == null || subject.getGrade() == null) {
            return false;
        }
        String grade = subject.getGrade().trim();
        if (grade.isBlank() || "-".equals(grade)) {
            return false;
        }

        if (subject.getGradeNumeric() > 0) {
            return true;
        }

        String normalizedGrade = grade.toLowerCase(Locale.ROOT);
        if (normalizedGrade.contains("abs") || normalizedGrade.contains("absolv")) {
            return true;
        }

        return normalizedGrade.matches("^[a-e](?:\\b|\\s|-|\\().*");
    }
}
