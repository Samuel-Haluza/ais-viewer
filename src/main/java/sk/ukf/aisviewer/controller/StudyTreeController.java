package sk.ukf.aisviewer.controller;

import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import sk.ukf.aisviewer.model.EnrollmentName;
import sk.ukf.aisviewer.model.StudentInfo;
import sk.ukf.aisviewer.model.Subject;
import sk.ukf.aisviewer.model.SubjectCategory;
import sk.ukf.aisviewer.service.LocalCacheService;
import sk.ukf.aisviewer.service.SubjectCompletionService;

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

    /** Napr. "2. ročník" – keď názov zápisného listu nemá rok. */
    private static final Pattern STUDY_YEAR_TEXT = Pattern.compile("(\\d+)\\.?\\s*ro[cč]n");

    @FXML private ImageView treeBaseLayer;
    @FXML private ImageView treeBranchesLayer;
    @FXML private ImageView treeLeavesLayer;
    @FXML private ImageView treeFlowersLayer;
    @FXML private ImageView treeEffectsLayer;
    @FXML private Label treePhaseLabel;
    @FXML private Label treeSubtitleLabel;
    @FXML private Label treeCreditsLabel;
    @FXML private Label treePercentageLabel;
    @FXML private Label treeLevelLabel;
    @FXML private Label treeNextLevelLabel;
    @FXML private Label treeMilestoneLabel;
    @FXML private Label treeMilestoneRemainingLabel;
    @FXML private ProgressBar treeProgressBar;

    // Karty fáz a míľnikov – v FXML sú jednotlivo, tu sa spoja do polí podľa poradia.
    @FXML private VBox treePhaseCard1, treePhaseCard2, treePhaseCard3,
            treePhaseCard4, treePhaseCard5, treePhaseCard6;
    @FXML private Label treePhaseDescription1, treePhaseDescription2, treePhaseDescription3,
            treePhaseDescription4, treePhaseDescription5, treePhaseDescription6;
    @FXML private VBox treeMilestoneCardYear1, treeMilestoneCardYear2, treeMilestoneCardFirstA,
            treeMilestoneCardTenSubjects, treeMilestoneCardMandatory;
    @FXML private Label treeMilestoneYear1Label, treeMilestoneYear2Label, treeMilestoneFirstALabel,
            treeMilestoneTenSubjectsLabel, treeMilestoneMandatoryLabel;

    private VBox[] phaseCards;
    private Label[] phaseDescriptionLabels;
    private VBox[] milestoneCards;
    private Label[] milestoneStatusLabels;

    @FXML
    public void initialize() {
        phaseCards = new VBox[]{treePhaseCard1, treePhaseCard2, treePhaseCard3,
                treePhaseCard4, treePhaseCard5, treePhaseCard6};
        phaseDescriptionLabels = new Label[]{treePhaseDescription1, treePhaseDescription2,
                treePhaseDescription3, treePhaseDescription4, treePhaseDescription5, treePhaseDescription6};
        milestoneCards = new VBox[]{treeMilestoneCardYear1, treeMilestoneCardYear2,
                treeMilestoneCardFirstA, treeMilestoneCardTenSubjects, treeMilestoneCardMandatory};
        milestoneStatusLabels = new Label[]{treeMilestoneYear1Label, treeMilestoneYear2Label,
                treeMilestoneFirstALabel, treeMilestoneTenSubjectsLabel, treeMilestoneMandatoryLabel};
        loadTreeImages();
    }

    private void loadTreeImages() {
        treeBaseLayer.setImage(loadImage("tree-base.png"));
        treeBranchesLayer.setImage(loadImage("tree-branches.png"));
        treeLeavesLayer.setImage(loadImage("tree-leaves.png"));
        treeFlowersLayer.setImage(loadImage("tree-flowers.png"));
        treeEffectsLayer.setImage(loadImage("tree-effects.png"));
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
                       StudentInfo studentInfo, int requiredCredits) {
        requiredCredits = requiredCredits > 0 ? requiredCredits : 180;
        double progress = Math.min(1.0, (double) acquiredCredits / requiredCredits);
        int level = getGrowthLevel(acquiredCredits, requiredCredits);
        int nextLevelCredits = getNextLevelCredits(level, requiredCredits);
        int creditsToNextLevel = level >= 20 ? 0 : Math.max(0, nextLevelCredits - acquiredCredits);
        int nextMilestone = getNextMilestone(acquiredCredits, requiredCredits);

        treePhaseLabel.setText(getPhase(progress));
        treeLevelLabel.setText("Úroveň " + level + " / 20");
        treeSubtitleLabel.setText("Rastie spolu s tvojím štúdiom");
        treeCreditsLabel.setText(acquiredCredits + " / " + requiredCredits);
        treePercentageLabel.setText(String.format("%.0f %% splnené", progress * 100));
        treeNextLevelLabel.setText(level >= 20
                ? "🎓 Maximálna úroveň dosiahnutá"
                : "Do úrovne " + (level + 1) + ":\n" + creditsToNextLevel + " "
                        + (creditsToNextLevel == 1 ? "kredit" : "kredity"));
        if (nextMilestone == requiredCredits && acquiredCredits >= requiredCredits) {
            treeMilestoneLabel.setText("🎓 Štúdium splnené\n"
                    + requiredCredits + " / " + requiredCredits + " kreditov");
            treeMilestoneRemainingLabel.setText("");
        } else {
            treeMilestoneLabel.setText("Ďalší míľnik:\n" + nextMilestone + " kreditov");
            treeMilestoneRemainingLabel.setText("Zostáva: "
                    + Math.max(0, nextMilestone - acquiredCredits) + " kreditov");
        }
        treeProgressBar.setProgress(progress);
        updatePhaseDescriptions(requiredCredits);
        updateStudyMilestones(currentSubjects, cacheSnapshot, studentInfo);
        updatePhaseHighlight(level);

        animateLayer(treeBranchesLayer, Math.max(0.15, layerOpacity(progress, 0.05, 0.45)));
        animateLayer(treeLeavesLayer, layerOpacity(progress, 0.30, 0.70));
        animateLayer(treeFlowersLayer, layerOpacity(progress, 0.55, 0.85));
        animateLayer(treeEffectsLayer, layerOpacity(progress, 0.80, 1.00));
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

    private int getNextMilestone(int acquiredCredits, int requiredCredits) {
        int[] milestones = {
                scaledMilestone(1.0 / 6.0, requiredCredits),
                scaledMilestone(5.0 / 18.0, requiredCredits),
                scaledMilestone(5.0 / 9.0, requiredCredits),
                scaledMilestone(5.0 / 6.0, requiredCredits),
                requiredCredits
        };
        for (int milestone : milestones) {
            if (acquiredCredits < milestone) {
                return milestone;
            }
        }
        return requiredCredits;
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
        List<Subject> mandatorySubjects = SubjectCategory.MANDATORY.filter(allKnownSubjects);
        long mandatoryIncomplete = mandatorySubjects.stream()
                .filter(s -> !SubjectCompletionService.isCompleted(s))
                .count();
        boolean mandatoryComplete = !mandatorySubjects.isEmpty() && mandatoryIncomplete == 0;
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
            int startYear = EnrollmentName.startYear(name);
            if (startYear > 0) return startYear;
            Matcher studyYear = STUDY_YEAR_TEXT.matcher(
                    name == null ? "" : name.toLowerCase(Locale.ROOT));
            return studyYear.find() ? Integer.parseInt(studyYear.group(1)) * 10000 : 50000 + index;
        }));
        return ordered;
    }

    private boolean isYearComplete(Map<String, List<Subject>> data, List<String> ids, int index) {
        if (index >= ids.size()) return false;
        List<Subject> subjects = data.get(ids.get(index));
        return subjects != null && !subjects.isEmpty()
                && subjects.stream().allMatch(SubjectCompletionService::isCompleted);
    }

    private List<Subject> uniqueSubjects(List<Subject> subjects, boolean completedOnly) {
        Map<String, Subject> unique = new LinkedHashMap<>();
        for (Subject subject : subjects) {
            if (subject == null || (completedOnly && !SubjectCompletionService.isCompleted(subject))) continue;
            unique.putIfAbsent(SubjectCompletionService.identityKey(subject), subject);
        }
        return new ArrayList<>(unique.values());
    }

    private boolean isGradeA(Subject subject) {
        // Predtým regex \bA\b zachytil aj "ABS - absolvoval/A" (koncové "/A").
        return subject.getGradeNumeric() == 1.0;
    }

    private int getGrowthLevel(int acquiredCredits, int requiredCredits) {
        if (acquiredCredits >= requiredCredits) {
            return 20;
        }
        double progress = Math.max(0, acquiredCredits) / (double) requiredCredits;
        return Math.max(1, Math.min(19, (int) Math.floor(progress * 19) + 1));
    }

    private int getNextLevelCredits(int level, int requiredCredits) {
        if (level >= 20) {
            return requiredCredits;
        }
        return Math.min(requiredCredits,
                (int) Math.ceil(requiredCredits * level / 19.0));
    }

    private int scaledMilestone(double ratio, int requiredCredits) {
        return Math.min(requiredCredits,
                Math.max(1, (int) Math.round(requiredCredits * ratio)));
    }

    private void updatePhaseDescriptions(int requiredCredits) {
        int phase2Start = phaseStart(requiredCredits, 0.20);
        int phase3Start = phaseStart(requiredCredits, 0.40);
        int phase4Start = phaseStart(requiredCredits, 0.60);
        int phase5Start = phaseStart(requiredCredits, 0.80);
        phaseDescriptionLabels[0].setText("0–" + (phase2Start - 1) + " kreditov");
        phaseDescriptionLabels[1].setText(phase2Start + "–" + (phase3Start - 1) + " kreditov");
        phaseDescriptionLabels[2].setText(phase3Start + "–" + (phase4Start - 1) + " kreditov");
        phaseDescriptionLabels[3].setText(phase4Start + "–" + (phase5Start - 1) + " kreditov");
        phaseDescriptionLabels[4].setText(phase5Start + "–" + (requiredCredits - 1) + " kreditov");
        phaseDescriptionLabels[5].setText(requiredCredits + " kreditov\nŠtúdium splnené");
    }

    private int phaseStart(int requiredCredits, double fraction) {
        return Math.max(1, (int) Math.ceil(requiredCredits * fraction));
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
