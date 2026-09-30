package sk.ukf.aisviewer.controller;

import javafx.animation.FadeTransition;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import sk.ukf.aisviewer.model.StudentInfo;
import sk.ukf.aisviewer.model.Subject;
import sk.ukf.aisviewer.service.LocalCacheService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javafx.util.Duration;

public class StudyTreeController {

    private static final int TARGET_CREDITS = 180;
    private final ImageView baseLayer;
    private final ImageView branchesLayer;
    private final ImageView leavesLayer;
    private final ImageView flowersLayer;
    private final ImageView effectsLayer;
    private final Label phaseLabel;
    private final Label subtitleLabel;
    private final Label creditsLabel;
    private final Label percentageLabel;
    private final Label levelLabel;
    private final Label nextLevelLabel;
    private final Label milestoneLabel;
    private final Label milestoneRemainingLabel;
    private final Label[] milestoneStatusLabels;
    private final ProgressBar progressBar;
    private final VBox[] phaseCards;
    private final VBox[] milestoneCards;

    public StudyTreeController(ImageView baseLayer, ImageView branchesLayer, ImageView leavesLayer,
                               ImageView flowersLayer, ImageView effectsLayer,
                               Label phaseLabel, Label subtitleLabel, Label creditsLabel,
                               Label percentageLabel, Label levelLabel, Label nextLevelLabel,
                               Label milestoneLabel, Label milestoneRemainingLabel,
                               Label[] milestoneStatusLabels, ProgressBar progressBar,
                               VBox[] phaseCards, VBox[] milestoneCards) {
        this.baseLayer = baseLayer;
        this.branchesLayer = branchesLayer;
        this.leavesLayer = leavesLayer;
        this.flowersLayer = flowersLayer;
        this.effectsLayer = effectsLayer;
        this.phaseLabel = phaseLabel;
        this.subtitleLabel = subtitleLabel;
        this.creditsLabel = creditsLabel;
        this.percentageLabel = percentageLabel;
        this.levelLabel = levelLabel;
        this.nextLevelLabel = nextLevelLabel;
        this.milestoneLabel = milestoneLabel;
        this.milestoneRemainingLabel = milestoneRemainingLabel;
        this.milestoneStatusLabels = milestoneStatusLabels;
        this.progressBar = progressBar;
        this.phaseCards = phaseCards;
        this.milestoneCards = milestoneCards;
        loadTreeImages();
    }

    private void loadTreeImages() {
        baseLayer.setImage(loadImage("tree-base.png"));
        branchesLayer.setImage(loadImage("tree-branches.png"));
        leavesLayer.setImage(loadImage("tree-leaves.png"));
        flowersLayer.setImage(loadImage("tree-flowers.png"));
        effectsLayer.setImage(loadImage("tree-effects.png"));
    }

    private Image loadImage(String fileName) {
        String resourcePath = "/sk/ukf/aisviewer/images/tree/" + fileName;
        java.net.URL resource = getClass().getResource(resourcePath);
        if (resource == null) {
            throw new IllegalStateException("Chýbajúci obrázok Study Tree: " + resourcePath);
        }
        return new Image(resource.toExternalForm());
    }

    public void update(int acquiredCredits, List<Subject> currentSubjects,
                       LocalCacheService.CacheSnapshot cacheSnapshot,
                       StudentInfo studentInfo) {
        double progress = Math.min(1.0, (double) acquiredCredits / TARGET_CREDITS);
        int level = getGrowthLevel(acquiredCredits);
        int nextLevelCredits = Math.min(TARGET_CREDITS, level * 10);
        int creditsToNextLevel = level >= 20 ? 0 : Math.max(0, nextLevelCredits - acquiredCredits);
        int nextMilestone = getNextMilestone(acquiredCredits);

        phaseLabel.setText(getPhase(progress));
        levelLabel.setText("Úroveň " + level + " / 20");
        subtitleLabel.setText("Rastie spolu s tvojím štúdiom");
        creditsLabel.setText(acquiredCredits + " / " + TARGET_CREDITS);
        percentageLabel.setText(String.format("%.0f %% splnené", progress * 100));
        nextLevelLabel.setText(level >= 20
                ? "🎓 Maximálna úroveň dosiahnutá"
                : "Do úrovne " + (level + 1) + ":\n" + creditsToNextLevel + " "
                        + (creditsToNextLevel == 1 ? "kredit" : "kredity"));
        if (nextMilestone == TARGET_CREDITS && acquiredCredits >= TARGET_CREDITS) {
            milestoneLabel.setText("🎓 Štúdium splnené\n180 / 180 kreditov");
            milestoneRemainingLabel.setText("");
        } else {
            milestoneLabel.setText("Ďalší míľnik:\n" + nextMilestone + " kreditov");
            milestoneRemainingLabel.setText("Zostáva: "
                    + Math.max(0, nextMilestone - acquiredCredits) + " kreditov");
        }
        progressBar.setProgress(progress);
        updateStudyMilestones(currentSubjects, cacheSnapshot, studentInfo);
        updatePhaseHighlight(level);

        animateLayer(branchesLayer, Math.max(0.15, layerOpacity(progress, 0.05, 0.45)));
        animateLayer(leavesLayer, layerOpacity(progress, 0.30, 0.70));
        animateLayer(flowersLayer, layerOpacity(progress, 0.55, 0.85));
        animateLayer(effectsLayer, layerOpacity(progress, 0.80, 1.00));
    }

    private void updatePhaseHighlight(int level) {
        int activeIndex = level <= 4 ? 0
                : level <= 8 ? 1
                : level <= 12 ? 2
                : level <= 16 ? 3
                : level <= 19 ? 4 : 5;
        for (int i = 0; i < phaseCards.length; i++) {
            phaseCards[i].getStyleClass().remove("tree-phase-current");
            if (i == activeIndex) {
                phaseCards[i].getStyleClass().add("tree-phase-current");
            }
        }
    }

    private int getNextMilestone(int acquiredCredits) {
        int[] milestones = {30, 50, 100, 150, TARGET_CREDITS};
        for (int milestone : milestones) {
            if (acquiredCredits < milestone) {
                return milestone;
            }
        }
        return TARGET_CREDITS;
    }

    private void updateStudyMilestones(List<Subject> currentSubjects,
                                       LocalCacheService.CacheSnapshot snapshot,
                                       StudentInfo studentInfo) {
        Map<String, List<Subject>> enrollmentSubjects =
                getEnrollmentSubjects(snapshot, studentInfo, currentSubjects);
        List<Subject> allSubjects = new ArrayList<>();
        for (List<Subject> subjects : enrollmentSubjects.values()) {
            allSubjects.addAll(subjects);
        }
        List<Subject> completedSubjects = uniqueSubjects(allSubjects, true);
        List<String> orderedIds = new ArrayList<>(enrollmentSubjects.keySet());
        setMilestone(0, isYearComplete(enrollmentSubjects, orderedIds, 0),
                "🔒 Ešte neukončený 1. ročník", "✓ Splnené");
        setMilestone(1, isYearComplete(enrollmentSubjects, orderedIds, 1),
                "🔒 Ešte neukončený 2. ročník", "✓ Splnené");

        boolean hasA = completedSubjects.stream().anyMatch(this::isGradeA);
        setMilestone(2, hasA, "🔒 Zatiaľ nezískané", "✓ Dosiahnuté");

        int missingSubjects = Math.max(0, 10 - completedSubjects.size());
        setMilestone(3, missingSubjects == 0,
                "🔒 Ešte " + missingSubjects + " predmetov", "✓ Dosiahnuté");

        List<Subject> allKnownSubjects = uniqueSubjects(allSubjects, false);
        long mandatoryIncomplete = allKnownSubjects.stream()
                .filter(s -> "Povinné predmety".equals(s.getCategory()))
                .filter(s -> !CreditsController.isCompletedSubject(s))
                .count();
        boolean mandatoryComplete = !allKnownSubjects.isEmpty() && mandatoryIncomplete == 0
                && allKnownSubjects.stream().anyMatch(s -> "Povinné predmety".equals(s.getCategory()));
        setMilestone(4, mandatoryComplete,
                "🔒 Zostáva " + mandatoryIncomplete + " predmetov", "✓ Splnené");
    }

    private void setMilestone(int index, boolean completed, String lockedText, String completedText) {
        milestoneStatusLabels[index].setText(completed ? completedText : lockedText);
        milestoneStatusLabels[index].setOpacity(completed ? 1.0 : 0.55);
        milestoneCards[index].setOpacity(completed ? 1.0 : 0.65);
    }

    private Map<String, List<Subject>> getEnrollmentSubjects(
            LocalCacheService.CacheSnapshot snapshot, StudentInfo studentInfo,
            List<Subject> currentSubjects) {
        Map<String, List<Subject>> result = new LinkedHashMap<>();
        if (snapshot != null && snapshot.getEnrollmentData() != null) {
            List<String> ids = new ArrayList<>(snapshot.getEnrollmentData().keySet());
            if (studentInfo != null && studentInfo.getEnrollmentListIds() != null) {
                ids = orderEnrollmentIds(studentInfo, ids);
            }
            for (String id : ids) {
                LocalCacheService.EnrollmentData data = snapshot.getEnrollmentData().get(id);
                if (data != null && data.getSubjects() != null) {
                    result.put(id, data.getSubjects());
                }
            }
        }
        if (currentSubjects != null && !currentSubjects.isEmpty()
                && studentInfo != null && studentInfo.getEnrollmentListId() != null) {
            result.put(studentInfo.getEnrollmentListId(), currentSubjects);
        }
        return result;
    }

    private List<String> orderEnrollmentIds(StudentInfo info, List<String> ids) {
        List<String> ordered = new ArrayList<>(ids);
        List<String> names = info.getEnrollmentListNames();
        ordered.sort(Comparator.comparingInt(id -> {
            int index = info.getEnrollmentListIds().indexOf(id);
            String name = index >= 0 && names != null && index < names.size() ? names.get(index) : id;
            Matcher year = Pattern.compile("(\\d{4})").matcher(name == null ? "" : name);
            if (year.find()) return Integer.parseInt(year.group(1));
            Matcher studyYear = Pattern.compile("(\\d+)\\.?\\s*ro[cč]n").matcher(
                    name == null ? "" : name.toLowerCase(Locale.ROOT));
            return studyYear.find() ? Integer.parseInt(studyYear.group(1)) * 10000 : 50000 + index;
        }));
        return ordered;
    }

    private boolean isYearComplete(Map<String, List<Subject>> data, List<String> ids, int index) {
        if (index >= ids.size()) return false;
        List<Subject> subjects = data.get(ids.get(index));
        return subjects != null && !subjects.isEmpty()
                && subjects.stream().allMatch(CreditsController::isCompletedSubject);
    }

    private List<Subject> uniqueSubjects(List<Subject> subjects, boolean completedOnly) {
        Map<String, Subject> unique = new LinkedHashMap<>();
        for (Subject subject : subjects) {
            if (subject == null || (completedOnly && !CreditsController.isCompletedSubject(subject))) continue;
            String key = ((subject.getAbbreviation() == null ? "" : subject.getAbbreviation().trim())
                    + "|" + (subject.getName() == null ? "" : subject.getName().trim())).toLowerCase(Locale.ROOT);
            unique.putIfAbsent(key, subject);
        }
        return new ArrayList<>(unique.values());
    }

    private boolean isGradeA(Subject subject) {
        String grade = subject.getGrade();
        return grade != null && grade.toUpperCase(Locale.ROOT).matches(".*\\bA\\b.*|.*\\(1\\).*");
    }

    private int getGrowthLevel(int acquiredCredits) {
        return Math.min(20, acquiredCredits >= TARGET_CREDITS
                ? 20 : acquiredCredits / 10 + 1);
    }

    private String getPhase(double progress) {
        if (progress >= 1.0) return "🌳✨ Dokončené štúdium";
        if (progress >= 0.8) return "🌲 Veľký strom";
        if (progress >= 0.6) return "🌳 Rozkvitnutý strom";
        if (progress >= 0.4) return "🌳 Rastúci strom";
        if (progress >= 0.2) return "🌿 Mladý strom";
        return "🌱 Semienko";
    }

    private double layerOpacity(double progress, double start, double end) {
        if (progress <= start) return 0;
        if (progress >= end) return 1;
        return (progress - start) / (end - start);
    }

    private void animateLayer(ImageView layer, double targetOpacity) {
        FadeTransition transition = new FadeTransition(Duration.millis(250), layer);
        transition.setToValue(targetOpacity);
        transition.play();
    }
}
