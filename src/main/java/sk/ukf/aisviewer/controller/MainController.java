package sk.ukf.aisviewer.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.chart.PieChart;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import sk.ukf.aisviewer.App;
import sk.ukf.aisviewer.model.Exam;
import sk.ukf.aisviewer.model.ScheduleEntry;
import sk.ukf.aisviewer.model.StudentInfo;
import sk.ukf.aisviewer.model.Subject;
import sk.ukf.aisviewer.service.AisClient;
import sk.ukf.aisviewer.service.LocalCacheService;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controller for the main window with tabs.
 */
public class MainController {

    // --- Student Info Bar ---
    @FXML private Label studentNameLabel;
    @FXML private Label studentProgramLabel;
    @FXML private ComboBox<String> enrollmentListCombo;
    @FXML private Label dataStatusLabel;
    @FXML private Label lastUpdatedLabel;
    @FXML private ProgressIndicator syncProgressIndicator;
    @FXML private Button refreshButton;

    // --- Subjects Tab ---
    @FXML private TableView<Subject> mandatoryTable;
    @FXML private TableView<Subject> optionalTable;
    @FXML private Label mandatoryCreditsLabel;
    @FXML private Label optionalCreditsLabel;
    @FXML private ComboBox<String> semesterFilterCombo;

    // --- Exams Tab ---
    @FXML private TableView<Exam> examsTable;
    @FXML private Label examsStatusLabel;

    // --- Credits Tab ---
    @FXML private Label totalCreditsLabel;
    @FXML private Label mandatoryCreditsTotal;
    @FXML private Label optionalCreditsTotal;
    @FXML private Label avgGradeLabel;
    @FXML private Label yearAvgGradeLabel;
    @FXML private Label graduationCreditsLabel;
    @FXML private ProgressBar graduationProgressBar;
    @FXML private Label graduationProgressLabel;
    @FXML private Label graduationRemainingLabel;
    @FXML private PieChart creditsByCategoryChart;

    // --- Study Tree Tab ---
    @FXML private ImageView treeBaseLayer;
    @FXML private ImageView treeBranchesLayer;
    @FXML private ImageView treeLeavesLayer;
    @FXML private ImageView treeFlowersLayer;
    @FXML private ImageView treeEffectsLayer;
    @FXML private Label treePhaseLabel;

    // --- Schedule Tab ---
    @FXML private VBox scheduleContainer;
    @FXML private Label scheduleStatusLabel;

    private static AisClient aisClient;
    private static String runtimePassword;
    private StudentInfo studentInfo;
    private final LocalCacheService cacheService = new LocalCacheService();
    private LocalCacheService.CacheSnapshot cacheSnapshot;
    private boolean dataDisplayed;
    private boolean syncInProgress;
    private SubjectsController subjectsController;
    private ExamsController examsController;
    private CreditsController creditsController;
    private ScheduleController scheduleController;
    private StudyTreeController studyTreeController;

    public static void setAisClient(AisClient client) {
        aisClient = client;
    }

    public static void setRuntimePassword(String password) {
        runtimePassword = password;
    }

    @FXML
    public void initialize() {
        subjectsController = new SubjectsController(mandatoryTable, optionalTable,
                mandatoryCreditsLabel, optionalCreditsLabel, semesterFilterCombo);
        examsController = new ExamsController(examsTable, examsStatusLabel);
        creditsController = new CreditsController(totalCreditsLabel, mandatoryCreditsTotal,
                optionalCreditsTotal, avgGradeLabel, yearAvgGradeLabel,
                graduationCreditsLabel, graduationProgressBar, graduationProgressLabel,
                graduationRemainingLabel, creditsByCategoryChart);
        scheduleController = new ScheduleController(scheduleContainer, scheduleStatusLabel);
        studyTreeController = new StudyTreeController(treeBaseLayer, treeBranchesLayer, treeLeavesLayer,
                treeFlowersLayer, treeEffectsLayer, treePhaseLabel);

        cacheSnapshot = cacheService.load().orElse(null);
        studentInfo = aisClient != null ? aisClient.getCurrentStudent() : null;
        if (studentInfo == null && cacheSnapshot != null) {
            studentInfo = cacheSnapshot.getStudentInfo();
        }
        if (studentInfo == null) return;

        restoreCachedEnrollmentLists();
        updateStudentInfoBar();
        subjectsController.setup();
        examsController.setup();
        loadInitialData();
    }

    private void updateStudentInfoBar() {
        if (studentInfo == null) return;
        String name = studentInfo.getFullName();
        studentNameLabel.setText(name != null && !name.isBlank() ? name : "Prihlásený používateľ");
        String program = studentInfo.getStudyProgram();
        studentProgramLabel.setText(program != null && !program.isBlank() ? program : "");

        // Populate enrollment list combo
        List<String> names = studentInfo.getEnrollmentListNames();
        if (names != null && !names.isEmpty()) {
            enrollmentListCombo.setItems(FXCollections.observableArrayList(names));
            enrollmentListCombo.getSelectionModel().selectFirst();
        } else {
            enrollmentListCombo.setItems(FXCollections.observableArrayList("Aktuálny zápisný list"));
            enrollmentListCombo.getSelectionModel().selectFirst();
        }
        enrollmentListCombo.setOnAction(e -> onEnrollmentListChanged());
    }

    private void loadInitialData() {
        boolean usableCache = cacheSnapshot != null
                && cacheSnapshot.getStudentInfo() != null
                && (aisClient == null || isCacheForCurrentStudent(cacheSnapshot));
        if (usableCache) {
            displaySelectedEnrollmentFromCache();
            showLastUpdated(cacheSnapshot.getUpdatedAt());
            if (!dataDisplayed) {
                showSelectionUnavailable();
            }
        } else {
            if (aisClient != null) {
                setLoadingState("Načítavam údaje z AIS…");
                startSynchronization(true);
            } else {
                showSelectionUnavailable();
            }
        }
    }

    private void restoreCachedEnrollmentLists() {
        if (!isCacheForCurrentStudent(cacheSnapshot)
                || studentInfo.getEnrollmentListIds() == null
                || !studentInfo.getEnrollmentListIds().isEmpty()) {
            return;
        }

        StudentInfo cachedStudent = cacheSnapshot.getStudentInfo();
        studentInfo.setEnrollmentListId(cachedStudent.getEnrollmentListId());
        if (cachedStudent.getEnrollmentListIds() == null
                || cachedStudent.getEnrollmentListNames() == null) {
            return;
        }
        studentInfo.setEnrollmentListIds(
                new java.util.ArrayList<>(cachedStudent.getEnrollmentListIds()));
        studentInfo.setEnrollmentListNames(
                new java.util.ArrayList<>(cachedStudent.getEnrollmentListNames()));
    }

    private void displaySelectedEnrollmentFromCache() {
        if (cacheSnapshot == null) {
            showSelectionUnavailable();
            return;
        }

        String zl = getSelectedEnrollmentId();
        LocalCacheService.EnrollmentData cachedData =
                cacheSnapshot.getEnrollmentData().get(zl);
        if (cachedData == null) {
            dataDisplayed = false;
            showSelectionUnavailable();
            return;
        }

        applyData(cachedData.getSubjects(), cachedData.getExams(),
                cachedData.getScheduleEntries());
        dataDisplayed = true;
    }

    private void startSynchronization(boolean initialLoad) {
        if (syncInProgress || studentInfo == null || aisClient == null) return;

        List<String> enrollmentIds = studentInfo.getEnrollmentListIds();
        if (enrollmentIds == null || enrollmentIds.isEmpty()) {
            showNoDataMessage();
            return;
        }

        syncInProgress = true;
        refreshButton.setDisable(true);
        if (!initialLoad) {
            enrollmentListCombo.getSelectionModel().selectFirst();
            enrollmentListCombo.setDisable(true);
            showSyncStatus("Aktualizujem údaje z AIS…", true);
        } else {
            showSyncStatus("Načítavam údaje z AIS…", true);
        }

        Thread loadThread = new Thread(() -> {
            boolean currentLoaded = false;
            boolean olderLoadFailed = false;
            for (int i = 0; i < enrollmentIds.size(); i++) {
                String enrollmentListId = enrollmentIds.get(i);
                try {
                    LoadedData loadedData = loadEnrollmentData(enrollmentListId);
                    if (loadedData.isEmpty()) {
                        throw new IllegalStateException("AIS nevrátil žiadne údaje");
                    }
                    boolean isCurrent = i == 0;
                    if (isCurrent) currentLoaded = true;

                    Platform.runLater(() -> {
                        saveCache(enrollmentListId, loadedData.subjects,
                                loadedData.exams, loadedData.scheduleEntries);
                        if (enrollmentListId.equals(getSelectedEnrollmentId())) {
                            applyData(loadedData.subjects, loadedData.exams,
                                    loadedData.scheduleEntries);
                            dataDisplayed = true;
                        }
                    });
                } catch (Exception e) {
                    boolean isCurrent = i == 0;
                    Platform.runLater(() -> {
                        if (isCurrent && !dataDisplayed) {
                            showNoDataMessage();
                            showSyncStatus("Údaje sa nepodarilo načítať z AIS.", false);
                        } else if (!isCurrent) {
                            showSyncStatus("Niektoré staršie údaje sa nepodarilo načítať.", false);
                        }
                    });
                    if (isCurrent) break;
                    olderLoadFailed = true;
                }
            }

            boolean synchronizationLoadedCurrent = currentLoaded;
            boolean synchronizationOlderLoadFailed = olderLoadFailed;
            Platform.runLater(() -> {
                syncInProgress = false;
                refreshButton.setDisable(false);
                enrollmentListCombo.setDisable(false);
                if (synchronizationLoadedCurrent && synchronizationOlderLoadFailed) {
                    showSyncStatus("Niektoré staršie údaje sa nepodarilo načítať.", false);
                } else if (synchronizationLoadedCurrent) {
                    showSyncStatus("Údaje boli aktualizované.", false);
                }
            });
        });
        loadThread.setDaemon(true);
        loadThread.start();
    }

    private LoadedData loadEnrollmentData(String enrollmentListId) {
        List<Subject> subjects = aisClient.fetchSubjects(enrollmentListId);
        List<Exam> exams = aisClient.fetchExams(enrollmentListId);
        List<ScheduleEntry> scheduleEntries = aisClient.fetchScheduleEntries(enrollmentListId);
        return new LoadedData(subjects, exams, scheduleEntries);
    }

    private void showSelectionUnavailable() {
        subjectsController.clear();
        examsController.clear();
        scheduleController.clear();
        showNoDataMessage();
        showSyncStatus("Údaje pre tento zápisný list ešte nie sú dostupné.", syncInProgress);
    }

    private static class LoadedData {
        private final List<Subject> subjects;
        private final List<Exam> exams;
        private final List<ScheduleEntry> scheduleEntries;

        private LoadedData(List<Subject> subjects, List<Exam> exams,
                           List<ScheduleEntry> scheduleEntries) {
            this.subjects = subjects;
            this.exams = exams;
            this.scheduleEntries = scheduleEntries;
        }

        private boolean isEmpty() {
            return subjects.isEmpty() && exams.isEmpty() && scheduleEntries.isEmpty();
        }
    }

    private boolean isCacheForCurrentStudent(LocalCacheService.CacheSnapshot snapshot) {
        if (snapshot == null || snapshot.getStudentInfo() == null || studentInfo == null) {
            return false;
        }
        String cachedName = snapshot.getStudentInfo().getFullName();
        String currentName = studentInfo.getFullName();
        return cachedName != null && currentName != null
                && cachedName.equalsIgnoreCase(currentName);
    }

    private void applyData(List<Subject> subjects, List<Exam> exams,
                           List<ScheduleEntry> scheduleEntries) {
        subjectsController.displaySubjects(subjects);
        examsController.displayExams(exams);
        creditsController.updateCredits(subjects, cacheSnapshot);
        studyTreeController.update(subjects, cacheSnapshot);
        scheduleController.displaySchedule(scheduleEntries);
    }

    private void saveCache(String enrollmentListId, List<Subject> subjects,
                           List<Exam> exams, List<ScheduleEntry> scheduleEntries) {
        if (cacheSnapshot == null) {
            cacheSnapshot = LocalCacheService.createSnapshot(studentInfo);
        } else {
            cacheSnapshot.setStudentInfo(studentInfo);
        }
        cacheSnapshot.getEnrollmentData().put(enrollmentListId,
                LocalCacheService.EnrollmentData.of(subjects, exams, scheduleEntries));
        String updatedAt = LocalDateTime.now()
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        cacheSnapshot.setUpdatedAt(updatedAt);
        showLastUpdated(updatedAt);
        try {
            cacheService.save(cacheSnapshot);
        } catch (IOException e) {
            System.out.println("[CACHE] Cache sa nepodarilo uložiť: " + e.getMessage());
        }
    }

    private void showLastUpdated(String timestamp) {
        if (timestamp == null || timestamp.isBlank()) {
            lastUpdatedLabel.setText("");
            lastUpdatedLabel.setVisible(false);
            lastUpdatedLabel.setManaged(false);
            return;
        }
        try {
            LocalDateTime dateTime = LocalDateTime.parse(timestamp,
                    DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            lastUpdatedLabel.setText("Naposledy aktualizované: "
                    + dateTime.format(DateTimeFormatter.ofPattern("d. M. uuuu, HH:mm")));
        } catch (Exception e) {
            lastUpdatedLabel.setText("Naposledy aktualizované: " + timestamp);
        }
        lastUpdatedLabel.setVisible(true);
        lastUpdatedLabel.setManaged(true);
    }

    private void showSyncStatus(String message, boolean running) {
        dataStatusLabel.setText(message);
        dataStatusLabel.setVisible(true);
        syncProgressIndicator.setVisible(running);
        syncProgressIndicator.setManaged(running);
    }

    private void setLoadingState(String message) {
        examsStatusLabel.setText(message);
        examsStatusLabel.setVisible(true);
        scheduleController.showLoading(message);
        showSyncStatus(message, true);
        lastUpdatedLabel.setText("Prvé načítanie môže trvať dlhšie.");
        lastUpdatedLabel.setVisible(true);
        lastUpdatedLabel.setManaged(true);
    }

    private void showNoDataMessage() {
        subjectsController.showUnavailable();
        examsController.showUnavailable();
        scheduleController.showUnavailable();
    }

    private String getSelectedEnrollmentId() {
        if (studentInfo == null) return null;
        List<String> ids = studentInfo.getEnrollmentListIds();
        if (ids == null || ids.isEmpty()) return null;
        int idx = enrollmentListCombo.getSelectionModel().getSelectedIndex();
        if (idx < 0 || idx >= ids.size()) return ids.get(0);
        return ids.get(idx);
    }

    private void onEnrollmentListChanged() {
        displaySelectedEnrollmentFromCache();
    }

    @FXML
    private void handleLogout() {
        if (aisClient != null) {
            aisClient.logout();
            aisClient = null;
        }
        runtimePassword = null;
        try {
            App.showLoginScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleRefresh() {
        if (!syncInProgress) {
            if (UiDialogs.showRefreshNotice(refreshButton.getScene().getWindow())) {
                if (runtimePassword != null && !runtimePassword.isBlank()
                        && studentInfo != null && studentInfo.getIdo() != null
                        && !studentInfo.getIdo().isBlank()) {
                    refreshWithCredentials(studentInfo.getIdo(), runtimePassword);
                } else {
                    synchronizeAfterPasswordPrompt();
                }
            }
        }
    }

    private void synchronizeAfterPasswordPrompt() {
        if (studentInfo == null || studentInfo.getIdo() == null
                || studentInfo.getIdo().isBlank()) {
            showSyncStatus("AIS ID nie je dostupné v cache.", false);
            return;
        }

        UiDialogs.showPasswordPrompt(refreshButton.getScene().getWindow()).ifPresent(password -> {
            if (password.isBlank()) {
                showSyncStatus("Heslo nesmie byť prázdne.", false);
                return;
            }
            refreshWithCredentials(studentInfo.getIdo(), password);
        });
    }

    private void refreshWithCredentials(String aisId, String password) {
        refreshButton.setDisable(true);
        showSyncStatus("Prihlasovanie a aktualizácia údajov z AIS…", true);
        Thread loginThread = new Thread(() -> {
            try {
                AisClient client = new AisClient();
                if (!client.login(aisId, password)) {
                    Platform.runLater(() -> {
                        refreshButton.setDisable(false);
                        showSyncStatus("Nesprávne heslo alebo AIS ID.", false);
                        UiDialogs.showInvalidPassword(refreshButton.getScene().getWindow());
                        synchronizeAfterPasswordPrompt();
                    });
                    return;
                }
                Platform.runLater(() -> {
                    AisClient previousClient = aisClient;
                    aisClient = client;
                    studentInfo = client.getCurrentStudent();
                    runtimePassword = password;
                    if (previousClient != null) {
                        previousClient.logout();
                    }
                    startSynchronization(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    refreshButton.setDisable(false);
                    showSyncStatus("Údaje sa nepodarilo načítať z AIS.", false);
                });
            }
        });
        loginThread.setDaemon(true);
        loginThread.start();
    }

}
