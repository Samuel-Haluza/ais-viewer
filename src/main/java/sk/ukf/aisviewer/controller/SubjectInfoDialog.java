package sk.ukf.aisviewer.controller;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import sk.ukf.aisviewer.model.CurriculumSubject;
import sk.ukf.aisviewer.model.SubjectInfo;
import sk.ukf.aisviewer.model.StudyPlanCatalog;
import sk.ukf.aisviewer.service.SubjectInfoService;
import sk.ukf.aisviewer.service.ThemeManager;

import java.util.List;
import java.util.stream.Collectors;

public final class SubjectInfoDialog {
    private SubjectInfoDialog() {
    }

    public static void show(CurriculumSubject subject, StudyPlanCatalog catalog,
                            SubjectInfoService service) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Detail predmetu");
        DialogPane pane = dialog.getDialogPane();
        ThemeManager.getInstance().applyTheme(pane);
        pane.getStyleClass().add("subject-info-dialog");
        pane.getButtonTypes().add(new ButtonType("Zavrieť", ButtonBar.ButtonData.CANCEL_CLOSE));

        VBox content = new VBox(8);
        content.setPadding(new Insets(16));
        content.setFillWidth(true);
        content.setMinWidth(0);
        content.setMaxWidth(Double.MAX_VALUE);
        SubjectInfo info = service.findByCode(catalog, subject.getCode()).orElse(null);
        if (info == null) {
            Label unavailable = textLabel("Detail predmetu nie je k dispozícii.");
            content.getChildren().add(unavailable);
        } else {
            Label title = textLabel(info.getName());
            title.getStyleClass().add("group-title");
            Label code = textLabel(info.getCode());
            code.getStyleClass().add("status-label");
            content.getChildren().addAll(title, code);
            addSection(content, "Základné informácie",
                    "Kredity: " + subject.getCredits()
                            + "\nOdporúčaný ročník: " + subject.getRecommendedYear()
                            + "\nOdporúčaný semester: " + subject.getSemester()
                            + "\nForma výučby: " + info.getTeachingForm()
                            + "\nRozsah výučby: " + info.getTeachingExtent()
                            + "\nMetóda štúdia: " + info.getStudyMethod());
            addSection(content, "Podmienky absolvovania", info.getCompletionRequirements());
            addSection(content, "Výsledky vzdelávania", info.getLearningOutcomes());
            addSection(content, "Stručná osnova", info.getSyllabus());
            addSection(content, "Odporúčaná literatúra", join(info.getRecommendedLiterature()));
            addSection(content, "Jazyk", info.getLanguage());
            addSection(content, "Vyučujúci", join(info.getTeachers()));
            addSection(content, "Dátum poslednej zmeny", info.getLastChangeDate());
        }
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(false);
        scroll.setPrefViewportWidth(900);
        scroll.setPrefViewportHeight(700);
        scroll.getStyleClass().add("subject-info-scroll");
        pane.setContent(scroll);
        dialog.setResizable(true);
        dialog.showAndWait();
    }

    private static void addSection(VBox content, String title, String value) {
        Label heading = textLabel(title);
        heading.getStyleClass().add("subject-info-heading");
        Label text = textLabel(value == null || value.isBlank() ? "—" : value);
        text.getStyleClass().add("subject-info-text");
        content.getChildren().addAll(heading, text);
    }

    private static Label textLabel(String value) {
        Label label = new Label(value);
        label.setWrapText(true);
        label.setTextOverrun(OverrunStyle.CLIP);
        label.setMaxWidth(Double.MAX_VALUE);
        label.setMinHeight(Label.USE_PREF_SIZE);
        return label;
    }

    private static String join(List<String> values) {
        return values == null ? "" : values.stream().collect(Collectors.joining("\n"));
    }
}
