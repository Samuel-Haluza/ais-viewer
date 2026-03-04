package sk.ukf.aisviewer.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import sk.ukf.aisviewer.App;
import sk.ukf.aisviewer.model.Exam;
import sk.ukf.aisviewer.model.ScheduleEntry;
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
    @FXML private VBox scheduleContainer;
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
                List<ScheduleEntry> scheduleEntries = aisClient.fetchScheduleEntries(zl);

                Platform.runLater(() -> {
                    populateSubjectTables(subjects);
                    populateExamTable(exams);
                    updateCreditsTab(subjects);
                    buildScheduleGrid(scheduleEntries);
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
        scheduleContainer.getChildren().clear();
        Label loadingLabel = new Label(message);
        loadingLabel.getStyleClass().add("schedule-loading-label");
        scheduleContainer.getChildren().add(loadingLabel);
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

    // ==================== SCHEDULE GRID ====================

    private static final String[] DAY_NAMES = {"Pondelok", "Utorok", "Streda", "Štvrtok", "Piatok"};
    private static final int MINUTES_PER_SLOT = 30; // each row = 30 min

    /**
     * Builds a visual timetable grid from ScheduleEntry list.
     */
    private void buildScheduleGrid(List<ScheduleEntry> entries) {
        scheduleContainer.getChildren().clear();

        if (entries == null || entries.isEmpty()) {
            Label noData = new Label("Rozvrh nie je dostupný alebo sa nepodarilo načítať údaje.");
            noData.getStyleClass().add("schedule-no-data");
            scheduleContainer.getChildren().add(noData);
            return;
        }

        // Determine time range
        int minHour = entries.stream().mapToInt(ScheduleEntry::getStartHour).min().orElse(8);
        int maxHour = entries.stream().mapToInt(ScheduleEntry::getEndHour).max().orElse(18);
        minHour = Math.max(7, minHour);
        maxHour = Math.min(21, maxHour + 1);

        int totalSlots = (maxHour - minHour) * (60 / MINUTES_PER_SLOT);

        // Determine which days have entries
        int maxDayIndex = entries.stream().mapToInt(ScheduleEntry::getDayIndex).max().orElse(4);
        int numDays = Math.min(Math.max(maxDayIndex + 1, 5), 6);

        // Build GridPane
        GridPane grid = new GridPane();
        grid.getStyleClass().add("schedule-grid");
        grid.setGridLinesVisible(false);

        // Column constraints: time label + one per day
        ColumnConstraints timeCol = new ColumnConstraints();
        timeCol.setMinWidth(60);
        timeCol.setPrefWidth(65);
        timeCol.setMaxWidth(70);
        grid.getColumnConstraints().add(timeCol);

        for (int d = 0; d < numDays; d++) {
            ColumnConstraints dayCol = new ColumnConstraints();
            dayCol.setHgrow(Priority.ALWAYS);
            dayCol.setMinWidth(120);
            dayCol.setFillWidth(true);
            grid.getColumnConstraints().add(dayCol);
        }

        // Row 0: Day headers
        RowConstraints headerRow = new RowConstraints();
        headerRow.setMinHeight(36);
        headerRow.setPrefHeight(36);
        grid.getRowConstraints().add(headerRow);

        // Empty top-left corner
        Label corner = new Label("");
        corner.getStyleClass().addAll("schedule-header", "schedule-corner");
        corner.setMaxWidth(Double.MAX_VALUE);
        corner.setMaxHeight(Double.MAX_VALUE);
        GridPane.setFillWidth(corner, true);
        GridPane.setFillHeight(corner, true);
        grid.add(corner, 0, 0);

        for (int d = 0; d < numDays; d++) {
            Label dayLabel = new Label(DAY_NAMES[d]);
            dayLabel.getStyleClass().add("schedule-header");
            dayLabel.setMaxWidth(Double.MAX_VALUE);
            dayLabel.setMaxHeight(Double.MAX_VALUE);
            dayLabel.setAlignment(Pos.CENTER);
            GridPane.setFillWidth(dayLabel, true);
            GridPane.setFillHeight(dayLabel, true);
            grid.add(dayLabel, d + 1, 0);
        }

        // Time slot rows
        for (int slot = 0; slot < totalSlots; slot++) {
            RowConstraints rc = new RowConstraints();
            rc.setMinHeight(28);
            rc.setPrefHeight(28);
            grid.getRowConstraints().add(rc);

            int hour = minHour + (slot * MINUTES_PER_SLOT) / 60;
            int minute = (slot * MINUTES_PER_SLOT) % 60;

            // Time label (only on full hours)
            if (minute == 0) {
                Label timeLabel = new Label(String.format("%02d:00", hour));
                timeLabel.getStyleClass().add("schedule-time-label");
                timeLabel.setMaxWidth(Double.MAX_VALUE);
                timeLabel.setMaxHeight(Double.MAX_VALUE);
                timeLabel.setAlignment(Pos.TOP_RIGHT);
                timeLabel.setPadding(new Insets(2, 8, 0, 4));
                GridPane.setFillWidth(timeLabel, true);
                GridPane.setFillHeight(timeLabel, true);
                GridPane.setRowSpan(timeLabel, 60 / MINUTES_PER_SLOT);
                GridPane.setValignment(timeLabel, VPos.TOP);
                grid.add(timeLabel, 0, slot + 1);
            }

            // Background cells for each day (for grid lines)
            for (int d = 0; d < numDays; d++) {
                Pane cellBg = new Pane();
                cellBg.getStyleClass().add("schedule-cell");
                if (minute == 0) {
                    cellBg.getStyleClass().add("schedule-cell-hour-border");
                }
                GridPane.setFillWidth(cellBg, true);
                GridPane.setFillHeight(cellBg, true);
                grid.add(cellBg, d + 1, slot + 1);
            }
        }

        // Place schedule entries
        for (ScheduleEntry entry : entries) {
            int dayIdx = entry.getDayIndex();
            if (dayIdx < 0 || dayIdx >= numDays) continue;

            int startMinutes = entry.getStartHour() * 60 + entry.getStartMinute();
            int endMinutes = entry.getEndHour() * 60 + entry.getEndMinute();
            int gridStartMinutes = minHour * 60;

            int startSlot = (startMinutes - gridStartMinutes) / MINUTES_PER_SLOT;
            int endSlot = (endMinutes - gridStartMinutes + MINUTES_PER_SLOT - 1) / MINUTES_PER_SLOT;
            int span = Math.max(1, endSlot - startSlot);

            if (startSlot < 0 || startSlot >= totalSlots) continue;

            VBox card = createScheduleCard(entry);
            GridPane.setRowIndex(card, startSlot + 1);
            GridPane.setColumnIndex(card, dayIdx + 1);
            GridPane.setRowSpan(card, span);
            GridPane.setFillWidth(card, true);
            GridPane.setFillHeight(card, true);
            GridPane.setMargin(card, new Insets(1, 2, 1, 2));
            grid.add(card, dayIdx + 1, startSlot + 1, 1, span);
        }

        scheduleContainer.getChildren().add(grid);
        VBox.setVgrow(grid, Priority.ALWAYS);
    }

    /**
     * Creates a styled card for a schedule entry.
     */
    private VBox createScheduleCard(ScheduleEntry entry) {
        VBox card = new VBox(2);
        card.getStyleClass().add("schedule-entry-card");
        card.setPadding(new Insets(4, 6, 4, 6));
        card.setAlignment(Pos.TOP_LEFT);

        // Determine type for coloring
        String type = entry.getType() != null ? entry.getType().toUpperCase() : "";
        if (type.startsWith("P") || type.contains("PR")) {
            card.getStyleClass().add("schedule-entry-lecture");
        } else if (type.startsWith("C") || type.startsWith("S") || type.contains("CV") || type.contains("SE")) {
            card.getStyleClass().add("schedule-entry-seminar");
        } else {
            card.getStyleClass().add("schedule-entry-other");
        }

        // Subject name
        Label nameLabel = new Label(entry.getSubjectName());
        nameLabel.getStyleClass().add("schedule-entry-name");
        nameLabel.setWrapText(true);
        nameLabel.setMaxWidth(Double.MAX_VALUE);
        card.getChildren().add(nameLabel);

        // Time
        Label timeLabel = new Label(entry.getTimeFrom() + " – " + entry.getTimeTo());
        timeLabel.getStyleClass().add("schedule-entry-time");
        card.getChildren().add(timeLabel);

        // Room + Teacher
        StringBuilder detail = new StringBuilder();
        if (entry.getRoom() != null && !entry.getRoom().isBlank()) {
            detail.append("📍 ").append(entry.getRoom());
        }
        if (entry.getTeacher() != null && !entry.getTeacher().isBlank()) {
            if (detail.length() > 0) detail.append("  •  ");
            detail.append(entry.getTeacher());
        }
        if (detail.length() > 0) {
            Label detailLabel = new Label(detail.toString());
            detailLabel.getStyleClass().add("schedule-entry-detail");
            detailLabel.setWrapText(true);
            card.getChildren().add(detailLabel);
        }

        // Tooltip with full info
        StringBuilder tooltipText = new StringBuilder();
        tooltipText.append(entry.getSubjectName());
        tooltipText.append("\n").append(entry.getDay()).append(" ").append(entry.getTimeFrom()).append(" – ").append(entry.getTimeTo());
        if (entry.getRoom() != null && !entry.getRoom().isBlank())
            tooltipText.append("\nMiestnosť: ").append(entry.getRoom());
        if (entry.getTeacher() != null && !entry.getTeacher().isBlank())
            tooltipText.append("\nVyučujúci: ").append(entry.getTeacher());
        if (entry.getType() != null && !entry.getType().isBlank())
            tooltipText.append("\nTyp: ").append(entry.getType());

        Tooltip tooltip = new Tooltip(tooltipText.toString());
        Tooltip.install(card, tooltip);

        return card;
    }
}
