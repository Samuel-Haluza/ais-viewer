package sk.ukf.aisviewer.controller;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import sk.ukf.aisviewer.model.EnrollmentName;
import sk.ukf.aisviewer.model.StudentInfo;
import sk.ukf.aisviewer.model.StudyPlanCatalog;
import sk.ukf.aisviewer.model.Subject;
import sk.ukf.aisviewer.model.SubjectCategory;
import sk.ukf.aisviewer.service.LocalCacheService;
import sk.ukf.aisviewer.service.StudyPlanCatalogService;
import sk.ukf.aisviewer.service.SubjectCompletionService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CreditsController {

    @FXML private Label totalCreditsLabel;
    @FXML private Label mandatoryCreditsTotal;
    @FXML private Label optionalCreditsTotal;
    @FXML private Label electiveCreditsTotal;
    @FXML private Label avgGradeLabel;
    @FXML private Label yearAvgGradeLabel;
    @FXML private Label yearAvgHintLabel;
    @FXML private Label graduationCreditsLabel;
    @FXML private ProgressBar graduationProgressBar;
    @FXML private Label graduationProgressLabel;
    @FXML private Label graduationRemainingLabel;
    @FXML private VBox categoryBox;
    @FXML private VBox yearAveragesBox;

    private final StudyPlanCatalogService catalogService = new StudyPlanCatalogService();
    private LocalCacheService.CacheSnapshot cacheSnapshot;
    private int acquiredCredits;
    private String averageGrade = "-";

    public void updateCredits(List<Subject> subjects,
                              LocalCacheService.CacheSnapshot cacheSnapshot,
                              int requiredCredits,
                              StudentInfo studentInfo) {
        this.cacheSnapshot = cacheSnapshot;
        requiredCredits = requiredCredits > 0 ? requiredCredits : 180;
        List<Subject> completedSubjects = getCompletedSubjectsFromAllEnrollments(subjects);
        int mandatory = SubjectCategory.MANDATORY.sumCredits(completedSubjects);
        int optional = SubjectCategory.COMPULSORY_ELECTIVE.sumCredits(completedSubjects);
        int elective = SubjectCategory.ELECTIVE.sumCredits(completedSubjects);
        int total = completedSubjects.stream().mapToInt(Subject::getCreditsValue).sum();
        acquiredCredits = total;

        totalCreditsLabel.setText(String.valueOf(total));
        mandatoryCreditsTotal.setText(String.valueOf(mandatory));
        optionalCreditsTotal.setText(String.valueOf(optional));
        electiveCreditsTotal.setText(String.valueOf(elective));

        double progress = (double) total / requiredCredits;
        int remaining = Math.max(0, requiredCredits - total);
        graduationCreditsLabel.setText(total + " / " + requiredCredits + " kr.");
        graduationProgressBar.setProgress(Math.min(1.0, progress));
        graduationProgressLabel.setText(String.format("%.0f %%", Math.min(100, progress * 100)));
        graduationRemainingLabel.setText(remaining == 0 ? "Kredity potrebné na ukončenie štúdia máš splnené."
                : "Zostáva " + remaining + " kr. do " + requiredCredits + " kr.");

        StudyPlanCatalog catalog = loadCatalog(studentInfo);
        categoryBox.getChildren().setAll(
                categoryRow(SubjectCategory.MANDATORY, mandatory,
                        catalog == null ? 0 : catalog.getRequiredMandatoryCredits(), "category-mandatory"),
                categoryRow(SubjectCategory.COMPULSORY_ELECTIVE, optional,
                        catalog == null ? 0 : catalog.getRequiredCompulsoryElectiveCredits(), "category-optional"),
                categoryRow(SubjectCategory.ELECTIVE, elective, 0, "category-elective"));

        avgGradeLabel.setText(calculateWeightedAverage(completedSubjects));
        String yearAverage = calculateWeightedAverage(subjects);
        yearAvgGradeLabel.setText("-".equals(yearAverage) ? "—" : yearAverage);
        yearAvgHintLabel.setText("-".equals(yearAverage)
                ? "Vážený priemer, vybraný ročník (zatiaľ bez známok)" : "Vážený priemer, vybraný ročník");
        averageGrade = calculateWeightedAverage(completedSubjects);

        fillYearAverages(studentInfo);
    }

    public int getAcquiredCredits() {
        return acquiredCredits;
    }

    public String getAverageGrade() {
        return averageGrade;
    }

    /** Riadok "Povinné predmety   71 / 104" + pruh vo farbe kategórie. */
    private VBox categoryRow(SubjectCategory category, int acquired, int required, String styleClass) {
        Label name = new Label(category.displayName());
        name.getStyleClass().add("category-name");
        Label value = new Label(required > 0 ? acquired + " / " + required + " kr." : acquired + " kr.");
        value.getStyleClass().add("category-value");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(8, name, spacer, value);
        header.setAlignment(Pos.CENTER_LEFT);

        ProgressBar bar = new ProgressBar(required > 0 ? Math.min(1.0, (double) acquired / required) : 0);
        bar.setMaxWidth(Double.MAX_VALUE);
        bar.getStyleClass().addAll("category-bar", styleClass);

        Label hint = new Label(required <= 0
                ? "Nepovinné. Kredity sa rátajú do celkového súčtu."
                : acquired >= required ? "Splnené" : "Chýba " + (required - acquired) + " kr.");
        hint.getStyleClass().add("category-hint");
        if (required > 0 && acquired >= required) hint.getStyleClass().add("category-done");
        return new VBox(4, header, bar, hint);
    }

    /** Pre každý zápisný list (od najstaršieho): ročník, akademický rok, priemer a získané kredity. */
    private void fillYearAverages(StudentInfo studentInfo) {
        yearAveragesBox.getChildren().clear();
        if (studentInfo == null || cacheSnapshot == null || cacheSnapshot.getEnrollmentData() == null
                || studentInfo.getEnrollmentListIds() == null) {
            return;
        }
        List<String> ids = new ArrayList<>(studentInfo.getEnrollmentListIds());
        List<String> names = studentInfo.getEnrollmentListNames();
        ids.sort(Comparator.comparingInt(id -> EnrollmentName.startYear(nameOf(id, ids, names, studentInfo))));
        for (String id : ids) {
            LocalCacheService.EnrollmentData data = cacheSnapshot.getEnrollmentData().get(id);
            if (data == null || data.getSubjects() == null) continue;
            String enrollmentName = nameOf(id, ids, names, studentInfo);
            int studyYear = EnrollmentName.studyYear(EnrollmentName.programCode(enrollmentName));
            String title = (studyYear > 0 ? studyYear + ". ročník" : "Ročník") + ", "
                    + EnrollmentName.academicYear(enrollmentName);
            int credits = data.getSubjects().stream()
                    .filter(SubjectCompletionService::isCompleted)
                    .mapToInt(Subject::getCreditsValue).sum();
            String average = calculateWeightedAverage(data.getSubjects());

            Label titleLabel = new Label(title);
            titleLabel.getStyleClass().add("year-row-title");
            Label creditsLabel = new Label(credits + " kr.");
            creditsLabel.getStyleClass().add("year-row-credits");
            Label averageLabel = new Label("-".equals(average) ? "bez známok" : "Ø " + average);
            averageLabel.getStyleClass().add("-".equals(average) ? "year-row-empty" : "year-row-average");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            HBox row = new HBox(12, titleLabel, spacer, creditsLabel, averageLabel);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("year-row");
            yearAveragesBox.getChildren().add(row);
        }
    }

    private static String nameOf(String id, List<String> sortedIds, List<String> names, StudentInfo info) {
        int index = info.getEnrollmentListIds().indexOf(id);
        return names != null && index >= 0 && index < names.size() ? names.get(index) : id;
    }

    private StudyPlanCatalog loadCatalog(StudentInfo studentInfo) {
        try {
            return studentInfo == null ? null : catalogService.loadForStudent(studentInfo).orElse(null);
        } catch (RuntimeException e) {
            return null;
        }
    }

    static String calculateWeightedAverage(List<Subject> subjects) {
        if (subjects == null) {
            return "-";
        }
        double weightedGradeSum = 0;
        int gradedCredits = 0;
        for (Subject subject : subjects) {
            if (!SubjectCompletionService.isCompleted(subject)) {
                continue;
            }
            double grade = subject.getGradeNumeric();
            int credits = subject.getCreditsValue();
            if (grade > 0 && credits > 0) {
                weightedGradeSum += grade * credits;
                gradedCredits += credits;
            }
        }
        return gradedCredits > 0
                ? String.format("%.2f", weightedGradeSum / gradedCredits)
                : "-";
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
        if (!SubjectCompletionService.isCompleted(subject)) {
            return;
        }
        uniqueSubjects.putIfAbsent(SubjectCompletionService.identityKey(subject), subject);
    }
}
