package sk.ukf.aisviewer.controller;

import javafx.collections.FXCollections;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import sk.ukf.aisviewer.model.Subject;

import java.util.List;
import java.util.stream.Collectors;

public class SubjectsController {

    private final TableView<Subject> mandatoryTable;
    private final TableView<Subject> optionalTable;
    private final Label mandatoryCreditsLabel;
    private final Label optionalCreditsLabel;
    private final ComboBox<String> semesterFilterCombo;
    private List<Subject> allSubjects = List.of();

    public SubjectsController(TableView<Subject> mandatoryTable,
                              TableView<Subject> optionalTable,
                              Label mandatoryCreditsLabel,
                              Label optionalCreditsLabel,
                              ComboBox<String> semesterFilterCombo) {
        this.mandatoryTable = mandatoryTable;
        this.optionalTable = optionalTable;
        this.mandatoryCreditsLabel = mandatoryCreditsLabel;
        this.optionalCreditsLabel = optionalCreditsLabel;
        this.semesterFilterCombo = semesterFilterCombo;
    }

    public void setup() {
        setupSubjectTable(mandatoryTable);
        setupSubjectTable(optionalTable);
        semesterFilterCombo.setItems(FXCollections.observableArrayList(
                "Zimný semester", "Letný semester", "Oba semestre"));
        semesterFilterCombo.getSelectionModel().select("Oba semestre");
        semesterFilterCombo.setOnAction(event -> refreshSubjectTables());
    }

    public void displaySubjects(List<Subject> subjects) {
        allSubjects = subjects;
        refreshSubjectTables();
    }

    public void clear() {
        allSubjects = List.of();
        mandatoryTable.setItems(FXCollections.observableArrayList());
        optionalTable.setItems(FXCollections.observableArrayList());
    }

    public void showUnavailable() {
        mandatoryTable.setPlaceholder(new Label("Dáta sa nepodarilo načítať."));
        optionalTable.setPlaceholder(new Label("Dáta sa nepodarilo načítať."));
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

    private void refreshSubjectTables() {
        String selectedSemester = semesterFilterCombo.getValue();
        List<Subject> filteredSubjects = allSubjects.stream()
                .filter(subject -> matchesSemester(subject, selectedSemester))
                .collect(Collectors.toList());

        List<Subject> mandatory = filteredSubjects.stream()
                .filter(s -> "Povinné predmety".equals(s.getCategory()))
                .collect(Collectors.toList());
        List<Subject> optional = filteredSubjects.stream()
                .filter(s -> "Povinne voliteľné predmety".equals(s.getCategory()))
                .collect(Collectors.toList());

        // If categories are not set, put everything in mandatory
        if (mandatory.isEmpty() && optional.isEmpty()) {
            mandatory = filteredSubjects;
        }

        mandatoryTable.setItems(FXCollections.observableArrayList(mandatory));
        optionalTable.setItems(FXCollections.observableArrayList(optional));

        int mandCredits = mandatory.stream().mapToInt(Subject::getCreditsValue).sum();
        int optCredits = optional.stream().mapToInt(Subject::getCreditsValue).sum();

        mandatoryCreditsLabel.setText("Kredity: " + mandCredits);
        optionalCreditsLabel.setText("Kredity: " + optCredits);
    }

    private boolean matchesSemester(Subject subject, String selectedSemester) {
        if (selectedSemester == null || "Oba semestre".equals(selectedSemester)) {
            return true;
        }
        String semester = subject.getSemester();
        if (semester == null) return false;
        if ("Zimný semester".equals(selectedSemester)) {
            return "ZS".equalsIgnoreCase(semester.trim());
        }
        return "LS".equalsIgnoreCase(semester.trim());
    }
}
