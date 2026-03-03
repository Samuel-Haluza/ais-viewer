package sk.ukf.aisviewer.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import sk.ukf.aisviewer.App;
import sk.ukf.aisviewer.model.Exam;
import sk.ukf.aisviewer.model.StudentInfo;
import sk.ukf.aisviewer.model.Subject;
import sk.ukf.aisviewer.service.AisClient;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for the main window with tabs.
 */
public class MainController {

    // --- Student Info Bar ---
    @FXML private Label studentNameLabel;
    @FXML private Label studentProgramLabel;
    @FXML private ComboBox<String> enrollmentListCombo;

    // --- Subjects Tab ---
    @FXML private TableView<Subject> mandatoryTable;
    @FXML private TableView<Subject> optionalTable;
    @FXML private TableView<Subject> electiveTable;
    @FXML private Label mandatoryCreditsLabel;
    @FXML private Label optionalCreditsLabel;
    @FXML private Label electiveCreditsLabel;

    // --- Exams Tab ---
    @FXML private TableView<Exam> examsTable;
    @FXML private Label examsStatusLabel;

    // --- Credits Tab ---
    @FXML private Label totalCreditsLabel;
    @FXML private Label mandatoryCreditsTotal;
    @FXML private Label optionalCreditsTotal;
    @FXML private Label electiveCreditsTotal;
    @FXML private Label avgGradeLabel;

    // --- Schedule Tab ---
    @FXML private TextArea scheduleTextArea;
    @FXML private Label scheduleStatusLabel;

    private static AisClient aisClient;
    private StudentInfo studentInfo;

    public static void setAisClient(AisClient client) {
        aisClient = client;
    }

    @FXML
    public void initialize() {
        if (aisClient == null) return;

        studentInfo = aisClient.getCurrentStudent();
        updateStudentInfoBar();
        setupSubjectTables();
        setupExamTable();
        loadData();
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

    private void setupSubjectTables() {
        setupSubjectTable(mandatoryTable);
        setupSubjectTable(optionalTable);
        setupSubjectTable(electiveTable);
    }

    @SuppressWarnings("unchecked")
    private void setupSubjectTable(TableView<Subject> table) {
        TableColumn<Subject, String> nameCol = new TableColumn<>("Predmet");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(220);

        TableColumn<Subject, String> abbrCol = new TableColumn<>("Skratka");
        abbrCol.setCellValueFactory(new PropertyValueFactory<>("abbreviation"));
        abbrCol.setPrefWidth(70);

        TableColumn<Subject, String> creditsCol = new TableColumn<>("Kredity");
        creditsCol.setCellValueFactory(new PropertyValueFactory<>("credits"));
        creditsCol.setPrefWidth(65);

        TableColumn<Subject, String> semCol = new TableColumn<>("Sem.");
        semCol.setCellValueFactory(new PropertyValueFactory<>("semester"));
        semCol.setPrefWidth(50);

        TableColumn<Subject, String> typeCol = new TableColumn<>("Typ");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("gradeType"));
        typeCol.setPrefWidth(55);

        TableColumn<Subject, String> gradeCol = new TableColumn<>("Hodnotenie");
        gradeCol.setCellValueFactory(new PropertyValueFactory<>("grade"));
        gradeCol.setPrefWidth(160);

        TableColumn<Subject, String> teacherCol = new TableColumn<>("Vyučujúci");
        teacherCol.setCellValueFactory(new PropertyValueFactory<>("teacher"));
        teacherCol.setPrefWidth(220);

        table.getColumns().setAll(nameCol, abbrCol, creditsCol, semCol, typeCol, gradeCol, teacherCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(new Label("Žiadne predmety"));
    }

    @SuppressWarnings("unchecked")
    private void setupExamTable() {
        TableColumn<Exam, String> subjectCol = new TableColumn<>("Predmet");
        subjectCol.setCellValueFactory(new PropertyValueFactory<>("subjectName"));
        subjectCol.setPrefWidth(200);

        TableColumn<Exam, String> dateCol = new TableColumn<>("Dátum");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
        dateCol.setPrefWidth(100);

        TableColumn<Exam, String> timeCol = new TableColumn<>("Čas");
        timeCol.setCellValueFactory(new PropertyValueFactory<>("time"));
        timeCol.setPrefWidth(80);

        TableColumn<Exam, String> roomCol = new TableColumn<>("Miestnosť");
        roomCol.setCellValueFactory(new PropertyValueFactory<>("room"));
        roomCol.setPrefWidth(100);

        TableColumn<Exam, String> teacherCol = new TableColumn<>("Skúšajúci");
        teacherCol.setCellValueFactory(new PropertyValueFactory<>("teacher"));
        teacherCol.setPrefWidth(180);

        TableColumn<Exam, String> capCol = new TableColumn<>("Kapacita");
        capCol.setCellValueFactory(new PropertyValueFactory<>("capacity"));
        capCol.setPrefWidth(75);

        TableColumn<Exam, String> enrolledCol = new TableColumn<>("Prihlásení");
        enrolledCol.setCellValueFactory(new PropertyValueFactory<>("enrolled"));
        enrolledCol.setPrefWidth(85);

        TableColumn<Exam, String> statusCol = new TableColumn<>("Stav");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setPrefWidth(100);

        examsTable.getColumns().setAll(subjectCol, dateCol, timeCol, roomCol,
                teacherCol, capCol, enrolledCol, statusCol);
        examsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        examsTable.setPlaceholder(new Label("Žiadne skúškové termíny"));
    }

    private void loadData() {
        if (studentInfo == null || aisClient == null) return;

        String zl = getSelectedEnrollmentId();
        if (zl == null || zl.isBlank()) {
            showNoDataMessage();
            return;
        }

        setLoadingState("Načítavam dáta...");

        Thread loadThread = new Thread(() -> {
            try {
                List<Subject> subjects = aisClient.fetchSubjects(zl);
                List<Exam> exams = aisClient.fetchExams(zl);
                String scheduleHtml = aisClient.fetchScheduleHtml(zl);
                String scheduleText = aisClient.getParser().parseScheduleAsText(scheduleHtml);

                Platform.runLater(() -> {
                    populateSubjectTables(subjects);
                    populateExamTable(exams);
                    updateCreditsTab(subjects);
                    scheduleTextArea.setText(scheduleText.isBlank()
                            ? "Rozvrh nie je dostupný alebo sa nepodarilo načítať."
                            : scheduleText);
                    scheduleStatusLabel.setVisible(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showNoDataMessage();
                    scheduleStatusLabel.setText("Chyba načítania: " + e.getMessage());
                    scheduleStatusLabel.setVisible(true);
                });
            }
        });
        loadThread.setDaemon(true);
        loadThread.start();
    }

    private void populateSubjectTables(List<Subject> subjects) {
        List<Subject> mandatory = subjects.stream()
                .filter(s -> "Povinné predmety".equals(s.getCategory()))
                .collect(Collectors.toList());
        List<Subject> optional = subjects.stream()
                .filter(s -> "Povinne voliteľné predmety".equals(s.getCategory()))
                .collect(Collectors.toList());
        List<Subject> elective = subjects.stream()
                .filter(s -> s.getCategory() != null && s.getCategory().toLowerCase().contains("výber"))
                .collect(Collectors.toList());

        // If categories are not set, put everything in mandatory
        if (mandatory.isEmpty() && optional.isEmpty() && elective.isEmpty()) {
            mandatory = subjects;
        }

        mandatoryTable.setItems(FXCollections.observableArrayList(mandatory));
        optionalTable.setItems(FXCollections.observableArrayList(optional));
        electiveTable.setItems(FXCollections.observableArrayList(elective));

        int mandCredits = mandatory.stream().mapToInt(Subject::getCreditsValue).sum();
        int optCredits = optional.stream().mapToInt(Subject::getCreditsValue).sum();
        int elecCredits = elective.stream().mapToInt(Subject::getCreditsValue).sum();

        mandatoryCreditsLabel.setText("Kredity: " + mandCredits);
        optionalCreditsLabel.setText("Kredity: " + optCredits);
        electiveCreditsLabel.setText("Kredity: " + elecCredits);
    }

    private void populateExamTable(List<Exam> exams) {
        examsTable.setItems(FXCollections.observableArrayList(exams));
        if (exams.isEmpty()) {
            examsStatusLabel.setText("Žiadne skúškové termíny neboli nájdené.");
            examsStatusLabel.setVisible(true);
        } else {
            examsStatusLabel.setVisible(false);
        }
    }

    private void updateCreditsTab(List<Subject> subjects) {
        int mandatory = subjects.stream()
                .filter(s -> "Povinné predmety".equals(s.getCategory()))
                .mapToInt(Subject::getCreditsValue).sum();
        int optional = subjects.stream()
                .filter(s -> "Povinne voliteľné predmety".equals(s.getCategory()))
                .mapToInt(Subject::getCreditsValue).sum();
        int elective = subjects.stream()
                .filter(s -> s.getCategory() != null && s.getCategory().toLowerCase().contains("výber"))
                .mapToInt(Subject::getCreditsValue).sum();
        int total = subjects.stream().mapToInt(Subject::getCreditsValue).sum();

        totalCreditsLabel.setText(String.valueOf(total));
        mandatoryCreditsTotal.setText(String.valueOf(mandatory));
        optionalCreditsTotal.setText(String.valueOf(optional));
        electiveCreditsTotal.setText(String.valueOf(elective));

        // Calculate average grade
        List<Subject> graded = subjects.stream()
                .filter(s -> s.getGradeNumeric() > 0)
                .collect(Collectors.toList());
        if (!graded.isEmpty()) {
            double avg = graded.stream().mapToDouble(Subject::getGradeNumeric).average().orElse(0);
            avgGradeLabel.setText(String.format("%.2f", avg));
        } else {
            avgGradeLabel.setText("-");
        }
    }

    private void setLoadingState(String message) {
        examsStatusLabel.setText(message);
        examsStatusLabel.setVisible(true);
        scheduleStatusLabel.setText(message);
        scheduleStatusLabel.setVisible(true);
        scheduleTextArea.setText(message);
    }

    private void showNoDataMessage() {
        mandatoryTable.setPlaceholder(new Label("Dáta sa nepodarilo načítať."));
        optionalTable.setPlaceholder(new Label("Dáta sa nepodarilo načítať."));
        electiveTable.setPlaceholder(new Label("Dáta sa nepodarilo načítať."));
        examsStatusLabel.setText("Dáta sa nepodarilo načítať.");
        examsStatusLabel.setVisible(true);
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
        loadData();
    }

    @FXML
    private void handleLogout() {
        if (aisClient != null) {
            aisClient.logout();
            aisClient = null;
        }
        try {
            App.showLoginScreen();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleRefresh() {
        loadData();
    }
}
