package sk.ukf.aisviewer.controller;

import javafx.animation.FadeTransition;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;
import sk.ukf.aisviewer.model.Subject;
import sk.ukf.aisviewer.service.LocalCacheService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StudyTreeController {

    private static final int TARGET_CREDITS = 180;
    private static final double TREE_BASE_OFFSET_Y = 150;

    private final ImageView baseLayer;
    private final ImageView branchesLayer;
    private final ImageView leavesLayer;
    private final ImageView flowersLayer;
    private final ImageView effectsLayer;
    private final Label phaseLabel;

    public StudyTreeController(ImageView baseLayer, ImageView branchesLayer, ImageView leavesLayer,
                               ImageView flowersLayer, ImageView effectsLayer,
                               Label phaseLabel) {
        this.baseLayer = baseLayer;
        this.branchesLayer = branchesLayer;
        this.leavesLayer = leavesLayer;
        this.flowersLayer = flowersLayer;
        this.effectsLayer = effectsLayer;
        this.phaseLabel = phaseLabel;
        baseLayer.setTranslateY(TREE_BASE_OFFSET_Y);
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

    public void update(List<Subject> currentSubjects,
                       LocalCacheService.CacheSnapshot cacheSnapshot) {
        int acquiredCredits = calculateAcquiredCredits(currentSubjects, cacheSnapshot);
        double progress = Math.min(1.0, (double) acquiredCredits / TARGET_CREDITS);

        phaseLabel.setText(getPhase(progress) + "  •  Level " + getGrowthLevel(acquiredCredits));

        animateLayer(branchesLayer, Math.max(0.15, layerOpacity(progress, 0.05, 0.45)));
        animateLayer(leavesLayer, layerOpacity(progress, 0.30, 0.70));
        animateLayer(flowersLayer, layerOpacity(progress, 0.55, 0.85));
        animateLayer(effectsLayer, layerOpacity(progress, 0.80, 1.00));
    }

    private int calculateAcquiredCredits(List<Subject> currentSubjects,
                                         LocalCacheService.CacheSnapshot cacheSnapshot) {
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
        return uniqueSubjects.values().stream()
                .mapToInt(Subject::getCreditsValue)
                .sum();
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
