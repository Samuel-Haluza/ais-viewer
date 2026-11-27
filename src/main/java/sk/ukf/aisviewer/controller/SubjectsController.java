package sk.ukf.aisviewer.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import sk.ukf.aisviewer.model.Subject;
import sk.ukf.aisviewer.model.SubjectCategory;

import java.util.List;
import java.util.function.Function;

public class SubjectsController {

    private static final String WINTER = "Zimný semester";
    private static final String SUMMER = "Letný semester";
    private static final String BOTH = "Oba semestre";

    @FXML private TableView<Subject> mandatoryTable;
    @FXML private TableView<Subject> optionalTable;
    @FXML private TableView<Subject> electiveTable;
    @FXML private VBox mandatoryGroup;
    @FXML private VBox optionalGroup;
    @FXML private VBox electiveGroup;
    @FXML private Label mandatoryCreditsLabel;
    @FXML private Label optionalCreditsLabel;
    @FXML private Label electiveCreditsLabel;
    @FXML private ComboBox<String> semesterFilterCombo;
    private List<Subject> allSubjects = List.of();

    @FXML
    public void initialize() {
        setupSubjectTable(mandatoryTable);
        setupSubjectTable(optionalTable);
        setupSubjectTable(electiveTable);
        semesterFilterCombo.setItems(FXCollections.observableArrayList(WINTER, SUMMER, BOTH));
        semesterFilterCombo.getSelectionModel().select(BOTH);
        semesterFilterCombo.setOnAction(event -> refreshSubjectTables());
    }

    public void displaySubjects(List<Subject> subjects) {
        allSubjects = subjects;
        refreshSubjectTables();
    }

    public void clear() {
        allSubjects = List.of();
        refreshSubjectTables();
    }

    public void showUnavailable() {
        for (TableView<Subject> table : List.of(mandatoryTable, optionalTable, electiveTable)) {
            table.setPlaceholder(new Label("Údaje sa nepodarilo načítať z AIS. Skús ich obnoviť."));
        }
    }

    private void setupSubjectTable(TableView<Subject> table) {
        TableColumn<Subject, String> nameCol = new TableColumn<>("Predmet");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setCellFactory(TableUi.textWithTooltip());
        nameCol.setPrefWidth(320);

        TableColumn<Subject, String> abbrCol = textColumn("Skratka", 75, Subject::getAbbreviation);
        TableColumn<Subject, String> creditsCol = textColumn("Kr.", 60,
                subject -> subject.getCreditsValue() > 0 ? String.valueOf(subject.getCreditsValue()) : "");
        TableColumn<Subject, String> semCol = textColumn("Sem.", 50, Subject::getSemester);
        TableColumn<Subject, String> typeCol = textColumn("Typ", 90, subject -> gradeTypeName(subject.getGradeType()));

        TableColumn<Subject, String> gradeCol = new TableColumn<>("Hodnotenie");
        gradeCol.setCellValueFactory(new PropertyValueFactory<>("grade"));
        gradeCol.setCellFactory(TableUi.gradeCell());
        gradeCol.setPrefWidth(180);

        TableColumn<Subject, String> teacherCol = new TableColumn<>("Vyučujúci");
        teacherCol.setCellValueFactory(new PropertyValueFactory<>("teacher"));
        teacherCol.setCellFactory(TableUi.textWithTooltip());
        teacherCol.setPrefWidth(230);

        TableUi.code(abbrCol);
        TableUi.numeric(creditsCol);
        TableUi.hideWhenEmpty(gradeCol);
        TableUi.hideWhenEmpty(teacherCol);
        table.getColumns().setAll(List.of(nameCol, abbrCol, creditsCol, semCol, typeCol, gradeCol, teacherCol));
        TableUi.columnWeights(table, 0.36, 0.09, 0.06, 0.06, 0.12, 0.14, 0.17);
        table.setPlaceholder(new Label("V tomto semestri tu nemáš žiadne predmety."));
        TableUi.fitHeightToRows(table);
    }

    private static TableColumn<Subject, String> textColumn(String title, double width,
                                                           Function<Subject, String> value) {
        TableColumn<Subject, String> column = new TableColumn<>(title);
        column.setCellValueFactory(data -> new SimpleStringProperty(value.apply(data.getValue())));
        column.setPrefWidth(width);
        return column;
    }

    /** S / PH / A z AIS na zrozumiteľné slovo. */
    static String gradeTypeName(String type) {
        if (type == null) return "-";
        return switch (type.trim().toUpperCase()) {
            case "S" -> "Skúška";
            case "PH" -> "Priebežné";
            case "A" -> "Absolvovanie";
            default -> type.isBlank() ? "-" : type;
        };
    }

    private void refreshSubjectTables() {
        String selectedSemester = semesterFilterCombo.getValue();
        List<Subject> filteredSubjects = allSubjects.stream()
                .filter(subject -> matchesSemester(subject, selectedSemester))
                .toList();

        List<Subject> mandatory = SubjectCategory.MANDATORY.filter(filteredSubjects);
        List<Subject> optional = SubjectCategory.COMPULSORY_ELECTIVE.filter(filteredSubjects);
        List<Subject> elective = SubjectCategory.ELECTIVE.filter(filteredSubjects);

        // Ak kategórie chýbajú, všetko pôjde medzi povinné.
        if (mandatory.isEmpty() && optional.isEmpty() && elective.isEmpty()) {
            mandatory = filteredSubjects;
        }

        mandatoryTable.setItems(FXCollections.observableArrayList(mandatory));
        optionalTable.setItems(FXCollections.observableArrayList(optional));
        electiveTable.setItems(FXCollections.observableArrayList(elective));

        mandatoryCreditsLabel.setText(sumCredits(mandatory) + " kr.");
        optionalCreditsLabel.setText(sumCredits(optional) + " kr.");
        electiveCreditsLabel.setText(sumCredits(elective) + " kr.");

        // Prázdne skupiny (napr. výberové predmety) sa skryjú – povinné ostanú vždy viditeľné.
        boolean anySubjects = !filteredSubjects.isEmpty();
        setVisible(optionalGroup, !optional.isEmpty() || !anySubjects);
        setVisible(electiveGroup, !elective.isEmpty());
    }

    private static int sumCredits(List<Subject> subjects) {
        return subjects.stream().mapToInt(Subject::getCreditsValue).sum();
    }

    private static void setVisible(VBox group, boolean visible) {
        group.setVisible(visible);
        group.setManaged(visible);
    }

    private boolean matchesSemester(Subject subject, String selectedSemester) {
        if (selectedSemester == null || BOTH.equals(selectedSemester)) {
            return true;
        }
        String semester = subject.getSemester();
        if (semester == null) return false;
        if (WINTER.equals(selectedSemester)) {
            return "ZS".equalsIgnoreCase(semester.trim());
        }
        return "LS".equalsIgnoreCase(semester.trim());
    }
}
