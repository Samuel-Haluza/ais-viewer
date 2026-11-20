package sk.ukf.aisviewer.controller;

import sk.ukf.aisviewer.AppLog;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import sk.ukf.aisviewer.App;
import sk.ukf.aisviewer.model.EnrollmentName;
import sk.ukf.aisviewer.model.Exam;
import sk.ukf.aisviewer.model.ScheduleEntry;
import sk.ukf.aisviewer.model.StudentInfo;
import sk.ukf.aisviewer.model.Subject;
import sk.ukf.aisviewer.service.AisClient;
import sk.ukf.aisviewer.service.LocalCacheService;
import sk.ukf.aisviewer.service.AppTheme;
import sk.ukf.aisviewer.service.GradeChanges;
import sk.ukf.aisviewer.service.ThemeManager;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controller for the main window with tabs.
 */
public class MainController {
    private static final Logger LOG = AppLog.get(MainController.class);

    // --- Student Info Bar ---
    @FXML private Label studentNameLabel;
    @FXML private Label studentProgramLabel;
    @FXML private ComboBox<String> enrollmentListCombo;
    @FXML private Label dataStatusLabel;
    @FXML private Label lastUpdatedLabel;
    @FXML private ProgressIndicator syncProgressIndicator;
    @FXML private Button refreshButton;

    @FXML private Button themeToggleButton;
    @FXML private HBox topBar;
    @FXML private Label enrollmentLabel;
    @FXML private Button exportButton;
    @FXML private Button logoutButton;

    /** Pod touto šírkou okna sa tlačidlá v hornej lište zmenia len na ikony (texty ostanú v tooltipoch). */
    private static final double COMPACT_WIDTH = 1180;
    private boolean compactTopBar;
    /** Text „Naposledy aktualizované…“ – v kompaktnom režime sa ukáže len v tooltipe tlačidla Obnoviť. */
    private String lastUpdatedText = "";

    private static AisClient aisClient;
    private static String runtimePassword;
    private StudentInfo studentInfo;
    private final LocalCacheService cacheService = new LocalCacheService();
    private LocalCacheService.CacheSnapshot cacheSnapshot;
    private boolean dataDisplayed;
    private boolean syncInProgress;
    /** Nové / zmenené známky zistené počas jednej synchronizácie. */
    private final List<GradeChanges.Change> gradeChanges = new java.util.ArrayList<>();
    private int requiredCredits = 180;

    // Controllery záložiek – každá záložka má vlastný .fxml (fx:include v main.fxml).
    // JavaFX ich vloží podľa fx:id + "Controller", napr. fx:id="subjects" → subjectsController.
    @FXML private SubjectsController subjectsController;
    @FXML private ExamsController examsController;
    @FXML private CreditsController creditsController;
    @FXML private ScheduleController scheduleController;
    @FXML private ProgressController progressController;
    private final ExportController exportController = new ExportController();
    @FXML private StudyPlanController studyPlanController;

    public static void setAisClient(AisClient client) {
        aisClient = client;
    }

    public static void setRuntimePassword(String password) {
        runtimePassword = password;
    }

    @FXML
    public void initialize() {
        setupTopBar();
        updateThemeToggleText();

        cacheSnapshot = cacheService.load().orElse(null);
        studentInfo = aisClient != null ? aisClient.getCurrentStudent() : null;
        if (studentInfo == null && cacheSnapshot != null) {
            studentInfo = cacheSnapshot.getStudentInfo();
        }

        if (studentInfo == null) return;

        restoreCachedEnrollmentLists();
        requiredCredits = resolveRequiredCredits(findLatestEnrollmentName());
        exportController.setExportData(studentInfo, cacheSnapshot);
        studyPlanController.update(studentInfo, List.of(), cacheSnapshot);
        updateStudentInfoBar();
        loadInitialData();
    }

    @FXML
    private void handleThemeToggle() {
        ThemeManager.getInstance().toggleTheme();
        updateThemeToggleText();
    }

    private void updateThemeToggleText() {
        if (themeToggleButton == null) {
            return;
        }
        boolean dark = ThemeManager.getInstance().getCurrentTheme() == AppTheme.DARK;
        String full = dark ? "☀ Svetlý režim" : "🌙 Tmavý režim";
        themeToggleButton.setText(compactTopBar ? full.substring(0, full.indexOf(' ')) : full);
        themeToggleButton.setTooltip(new Tooltip(dark ? "Prepnúť na svetlý režim" : "Prepnúť na tmavý režim"));
    }

    // ---------------------------------------------------------------- horná lišta

    /** Tooltipy a prepínanie medzi plnou a kompaktnou lištou podľa šírky okna. */
    private void setupTopBar() {
        exportButton.setTooltip(new Tooltip("Exportovať študijný prehľad do CSV"));
        logoutButton.setTooltip(new Tooltip("Odhlásiť sa z aplikácie"));
        enrollmentListCombo.setTooltip(new Tooltip("Zápisný list (ročník), ktorého údaje sa zobrazujú"));
        studentNameLabel.setMinWidth(Region.USE_PREF_SIZE); // meno sa nikdy neoreže
        dataStatusLabel.setMaxWidth(260);
        topBar.widthProperty().addListener((obs, oldWidth, width) -> applyTopBarLayout(width.doubleValue()));
        applyTopBarLayout(topBar.getWidth());
    }

    private void applyTopBarLayout(double width) {
        compactTopBar = width > 0 && width < COMPACT_WIDTH;
        enrollmentLabel.setVisible(!compactTopBar);
        enrollmentLabel.setManaged(!compactTopBar);
        exportButton.setText(compactTopBar ? "⬇" : "⬇ Export CSV");
        refreshButton.setText(compactTopBar ? "⟳" : "⟳ Obnoviť údaje");
        enrollmentListCombo.setPrefWidth(width > 0 && width < 900 ? 150 : 180);
        dataStatusLabel.setMaxWidth(compactTopBar ? 170 : 260);
        updateThemeToggleText();
        updateLastUpdatedLabel();
    }

    private void updateLastUpdatedLabel() {
        boolean show = !lastUpdatedText.isBlank() && !compactTopBar;
        lastUpdatedLabel.setText(lastUpdatedText);
        lastUpdatedLabel.setVisible(show);
        lastUpdatedLabel.setManaged(show);
        refreshButton.setTooltip(new Tooltip("Obnoviť údaje z AIS"
                + (lastUpdatedText.isBlank() ? "" : System.lineSeparator() + lastUpdatedText)));
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
                && (aisClient == null || isCacheForCurrentStudent(cacheSnapshot))
                && hasAllEnrollmentData();
        if (cacheSnapshot != null && cacheSnapshot.getStudentInfo() != null) {
            LOG.fine("enrollment metadata="
                    + studentInfo.getEnrollmentListIds()
                    + ", cached data="
                    + cacheSnapshot.getEnrollmentData().keySet()
                    + ", total enrollment years="
                    + cacheSnapshot.getEnrollmentData().size());
        }
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
        gradeChanges.clear();
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
                String enrollmentName = enrollmentName(enrollmentListId, i);
                LOG.info("enrollment=" + enrollmentName + ", id="
                        + enrollmentListId + ", selected=" + (i == 0)
                        + ", cachedBefore="
                        + (cacheSnapshot != null
                        && cacheSnapshot.getEnrollmentData().containsKey(enrollmentListId)));
                try {
                    LoadedData loadedData = loadEnrollmentData(enrollmentListId, i == 0);
                    if (loadedData.isEmpty()) {
                        throw new IllegalStateException("AIS nevrátil žiadne údaje");
                    }
                    boolean isCurrent = i == 0;
                    if (isCurrent) currentLoaded = true;
                    LOG.info("enrollment=" + enrollmentName + ", id="
                            + enrollmentListId + ", loaded=true, subjects="
                            + loadedData.subjects.size());

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
                    LOG.info("enrollment=" + enrollmentName + ", id="
                            + enrollmentListId + ", loaded=false, error=" + e.getMessage());
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
                if (!gradeChanges.isEmpty()) {
                    showSyncStatus("🎉 Nové hodnotenia: " + gradeChanges.size(), false);
                    UiDialogs.showGradeChanges(refreshButton.getScene().getWindow(), List.copyOf(gradeChanges));
                }
            });
        });
        loadThread.setDaemon(true);
        loadThread.start();
    }

    /**
     * Rozvrh sa načíta len pre aktuálny (prvý) zápisný list – stránka rozvrhu v AIS parameter
     * zl ignoruje a vždy vráti aktuálny semester, takže pre staršie ročníky by bol nesprávny.
     */
    private LoadedData loadEnrollmentData(String enrollmentListId, boolean current) {
        List<Subject> subjects = aisClient.fetchSubjects(enrollmentListId);
        List<Exam> exams = aisClient.fetchExams(enrollmentListId);
        List<ScheduleEntry> scheduleEntries = current
                ? aisClient.fetchScheduleEntries(enrollmentListId)
                : List.of();
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
        creditsController.updateCredits(subjects, cacheSnapshot, requiredCredits, studentInfo);
        progressController.update(creditsController.getAcquiredCredits(), subjects,
                cacheSnapshot, studentInfo, requiredCredits);
        exportController.setExportData(studentInfo, cacheSnapshot);
        scheduleController.displaySchedule(scheduleEntries, isCurrentEnrollment(getSelectedEnrollmentId()));
        studyPlanController.update(studentInfo, subjects, cacheSnapshot);
    }

    private String findLatestEnrollmentName() {
        return EnrollmentName.latest(studentInfo == null ? null : studentInfo.getEnrollmentListNames());
    }

    private int resolveRequiredCredits(String enrollmentName) {
        char studyType = EnrollmentName.degreeLetter(enrollmentName);
        if (studyType == 'b') {
            LOG.info("Typ štúdia: b, požadované kredity: 180");
            return 180;
        }
        if (studyType == 'm') {
            LOG.info("Typ štúdia: m, požadované kredity: 120");
            return 120;
        }
        LOG.info("Typ štúdia sa nepodarilo určiť, používam 180 kreditov");
        return 180;
    }

    private void saveCache(String enrollmentListId, List<Subject> subjects,
                           List<Exam> exams, List<ScheduleEntry> scheduleEntries) {
        if (cacheSnapshot == null) {
            cacheSnapshot = LocalCacheService.createSnapshot(studentInfo);
        } else {
            cacheSnapshot.setStudentInfo(studentInfo);
        }
        LocalCacheService.EnrollmentData previous = cacheSnapshot.getEnrollmentData().get(enrollmentListId);
        gradeChanges.addAll(GradeChanges.detect(previous == null ? null : previous.getSubjects(), subjects));
        cacheSnapshot.getEnrollmentData().put(enrollmentListId,
                LocalCacheService.EnrollmentData.of(subjects, exams, scheduleEntries));
        LOG.info("enrollment=" + enrollmentName(enrollmentListId, -1)
                + ", id=" + enrollmentListId + ", savedToCache=true, subjects="
                + subjects.size());
        String updatedAt = LocalDateTime.now()
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        cacheSnapshot.setUpdatedAt(updatedAt);
        exportController.setExportData(studentInfo, cacheSnapshot);
        showLastUpdated(updatedAt);
        try {
            cacheService.save(cacheSnapshot);
        } catch (IOException e) {
            LOG.info("Cache sa nepodarilo uložiť: " + e.getMessage());
        }
    }

    private boolean hasAllEnrollmentData() {
        if (studentInfo == null || studentInfo.getEnrollmentListIds() == null
                || cacheSnapshot == null || cacheSnapshot.getEnrollmentData() == null) {
            return false;
        }
        return studentInfo.getEnrollmentListIds().stream()
                .allMatch(cacheSnapshot.getEnrollmentData()::containsKey);
    }

    private String enrollmentName(String enrollmentId, int fallbackIndex) {
        if (studentInfo != null && studentInfo.getEnrollmentListIds() != null
                && studentInfo.getEnrollmentListNames() != null) {
            int index = studentInfo.getEnrollmentListIds().indexOf(enrollmentId);
            if (index >= 0 && index < studentInfo.getEnrollmentListNames().size()) {
                return studentInfo.getEnrollmentListNames().get(index);
            }
        }
        return fallbackIndex >= 0 ? "index-" + fallbackIndex : "unknown";
    }

    private void showLastUpdated(String timestamp) {
        if (timestamp == null || timestamp.isBlank()) {
            lastUpdatedText = "";
        } else {
            try {
                LocalDateTime dateTime = LocalDateTime.parse(timestamp,
                        DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                lastUpdatedText = "Naposledy aktualizované: "
                        + dateTime.format(DateTimeFormatter.ofPattern("d. M. uuuu, HH:mm"));
            } catch (Exception e) {
                lastUpdatedText = "Naposledy aktualizované: " + timestamp;
            }
        }
        updateLastUpdatedLabel();
    }

    private void showSyncStatus(String message, boolean running) {
        dataStatusLabel.setText(message);
        dataStatusLabel.setTooltip(new Tooltip(message));
        // predtým bol label managed="false" → mal nulovú šírku a stav sa nikdy nezobrazil
        dataStatusLabel.setVisible(true);
        dataStatusLabel.setManaged(true);
        syncProgressIndicator.setVisible(running);
        syncProgressIndicator.setManaged(running);
    }

    private void setLoadingState(String message) {
        examsController.showLoading(message);
        scheduleController.showLoading(message);
        showSyncStatus(message, true);
        lastUpdatedText = "Prvé načítanie môže trvať dlhšie.";
        updateLastUpdatedLabel();
    }

    private void showNoDataMessage() {
        subjectsController.showUnavailable();
        examsController.showUnavailable();
        scheduleController.showUnavailable();
    }

    /** Aktuálny zápisný list je prvý v zozname z AIS (najnovší ročník). */
    private boolean isCurrentEnrollment(String enrollmentId) {
        List<String> ids = studentInfo == null ? null : studentInfo.getEnrollmentListIds();
        return ids == null || ids.isEmpty() || ids.get(0).equals(enrollmentId);
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
            LOG.log(Level.WARNING, "Prihlasovacie okno sa nepodarilo otvoriť.", e);
        }
    }

    @FXML
    private void handleExport() {
        exportController.exportCsv(refreshButton.getScene().getWindow());
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
