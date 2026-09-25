package sk.ukf.aisviewer.controller;

import javafx.collections.FXCollections;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import sk.ukf.aisviewer.model.Exam;

import java.util.List;

public class ExamsController {

    private final TableView<Exam> examsTable;
    private final Label examsStatusLabel;

    public ExamsController(TableView<Exam> examsTable, Label examsStatusLabel) {
        this.examsTable = examsTable;
        this.examsStatusLabel = examsStatusLabel;
    }

    public void setup() {
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

        TableColumn<Exam, String> statusCol = new TableColumn<>("Stav");
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setPrefWidth(100);

        TableColumn<Exam, String> typeCol = new TableColumn<>("Typ");
        typeCol.setCellValueFactory(new PropertyValueFactory<>("type"));
        typeCol.setPrefWidth(120);

        examsTable.getColumns().setAll(subjectCol, dateCol, timeCol, roomCol,
                statusCol, typeCol);
        examsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        examsTable.setPlaceholder(new Label("Žiadne skúškové termíny"));
    }

    public void displayExams(List<Exam> exams) {
        examsTable.setItems(FXCollections.observableArrayList(exams));
        if (exams.isEmpty()) {
            examsStatusLabel.setText("Žiadne skúškové termíny neboli nájdené.");
            examsStatusLabel.setVisible(true);
        } else {
            examsStatusLabel.setVisible(false);
        }
    }

    public void clear() {
        examsTable.setItems(FXCollections.observableArrayList());
    }

    public void showUnavailable() {
        examsStatusLabel.setText("Dáta sa nepodarilo načítať.");
        examsStatusLabel.setVisible(true);
    }
}
