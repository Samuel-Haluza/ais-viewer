package sk.ukf.aisviewer.controller;

import javafx.geometry.Insets;
import javafx.geometry.VPos;
import javafx.scene.control.*;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import sk.ukf.aisviewer.App;
import sk.ukf.aisviewer.model.CurriculumSubject;
import sk.ukf.aisviewer.model.Semester;
import sk.ukf.aisviewer.model.SubjectInfo;
import sk.ukf.aisviewer.model.StudyPlanCatalog;
import sk.ukf.aisviewer.service.SubjectInfoService;
import sk.ukf.aisviewer.service.ThemeManager;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Detail predmetu z informačného listu. Prázdne údaje sa nezobrazujú. */
public final class SubjectInfoDialog {
    /** Šírka textu ~75 znakov na riadok – dlhé odseky sa lepšie čítajú. */
    private static final double TEXT_WIDTH = 620;

    private SubjectInfoDialog() {
    }

    public static void show(CurriculumSubject subject, StudyPlanCatalog catalog,
                            SubjectInfoService service) {
        Dialog<Void> dialog = new Dialog<>();
        // dialóg sa otvorí na tom monitore, kde je aktuálne okno (a podľa neho sa aj zmenší)
        javafx.stage.Window owner = App.activeWindow();
        dialog.initOwner(owner);
        dialog.setTitle("Detail predmetu");
        DialogPane pane = dialog.getDialogPane();
        ThemeManager.getInstance().applyTheme(pane);
        pane.getStyleClass().add("subject-info-dialog");
        pane.getButtonTypes().add(new ButtonType("Zavrieť", ButtonBar.ButtonData.CANCEL_CLOSE));

        VBox content = new VBox(4);
        content.setPadding(new Insets(20, 24, 20, 24));
        content.setFillWidth(false);
        SubjectInfo info = service.findByCode(catalog, subject.getCode()).orElse(null);

        Label title = textLabel(info != null ? info.getName() : subject.getName());
        title.getStyleClass().add("subject-info-name");
        Label code = textLabel(subject.getCode());
        code.getStyleClass().add("subject-info-code");
        content.getChildren().addAll(title, code);

        Map<String, String> basics = new LinkedHashMap<>();
        basics.put("Kredity", subject.getCredits() > 0 ? String.valueOf(subject.getCredits()) : null);
        basics.put("Odporúčaný ročník", subject.getRecommendedYear() > 0
                ? subject.getRecommendedYear() + ". ročník" : "záver štúdia");
        basics.put("Semester", semesterText(subject.getSemester()));
        if (info != null) {
            basics.put("Forma výučby", clean(info.getTeachingForm()));
            basics.put("Rozsah výučby", clean(info.getTeachingExtent()));
            basics.put("Metóda štúdia", clean(info.getStudyMethod()));
            basics.put("Jazyk", clean(info.getLanguage()));
            basics.put("Posledná zmena", clean(info.getLastChangeDate()));
        }
        content.getChildren().add(heading("Základné informácie"));
        content.getChildren().add(basicsGrid(basics));

        if (info == null) {
            Label unavailable = textLabel("Informačný list tohto predmetu nie je v aplikácii k dispozícii.");
            unavailable.getStyleClass().add("subject-info-text");
            content.getChildren().add(unavailable);
        } else {
            addSection(content, "Vyučujúci", join(info.getTeachers()));
            addSection(content, "Podmienky absolvovania", info.getCompletionRequirements());
            addSection(content, "Výsledky vzdelávania", info.getLearningOutcomes());
            addSection(content, "Stručná osnova", info.getSyllabus());
            addSection(content, "Odporúčaná literatúra", join(info.getRecommendedLiterature()));
        }
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(false);
        scroll.setPrefViewportWidth(TEXT_WIDTH + 64);
        // dialóg sa musí zmestiť na obrazovku aj s tlačidlom Zavrieť (zväčšenie Windows 150 %)
        double screenHeight = App.screenBounds(owner).getHeight();
        scroll.setPrefViewportHeight(Math.max(240, Math.min(640, screenHeight - 160)));
        scroll.getStyleClass().add("subject-info-scroll");
        pane.setContent(scroll);
        dialog.setResizable(true);
        dialog.showAndWait();
    }

    static String semesterText(Semester semester) {
        if (semester == null) return null;
        return switch (semester) {
            case WINTER -> "zimný";
            case SUMMER -> "letný";
            case BOTH -> "zimný alebo letný";
        };
    }

    /** Dvojstĺpcová mriežka „názov · hodnota“; riadky bez hodnoty vynechá. */
    private static GridPane basicsGrid(Map<String, String> values) {
        GridPane grid = new GridPane();
        grid.setHgap(24);
        grid.setVgap(4);
        ColumnConstraints keys = new ColumnConstraints();
        keys.setMinWidth(150);
        ColumnConstraints vals = new ColumnConstraints();
        vals.setMaxWidth(TEXT_WIDTH - 174);
        grid.getColumnConstraints().addAll(keys, vals);
        int row = 0;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (entry.getValue() == null || entry.getValue().isBlank()) continue;
            Label key = new Label(entry.getKey());
            key.getStyleClass().add("subject-info-key");
            Label value = textLabel(entry.getValue());
            value.getStyleClass().add("subject-info-text");
            GridPane.setValignment(key, VPos.TOP);
            grid.addRow(row++, key, value);
        }
        return grid;
    }

    /** Odstráni prázdne „šablónové“ hodnoty z informačného listu, napr. „( v hodinách ):“. */
    private static String clean(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        String lettersOnly = trimmed.replaceAll("\\(\\s*v hodinách\\s*\\)\\s*:?", "")
                .replaceAll("Týždenný:|Za obdobie štúdia:", "")
                .replaceAll("[\\s:()]", "");
        return lettersOnly.isEmpty() ? null : trimmed;
    }

    private static Label heading(String text) {
        Label heading = textLabel(text);
        heading.getStyleClass().add("subject-info-heading");
        return heading;
    }

    private static void addSection(VBox content, String title, String value) {
        if (value == null || value.isBlank()) return;
        Label text = textLabel(value);
        text.getStyleClass().add("subject-info-text");
        content.getChildren().addAll(heading(title), text);
    }

    private static Label textLabel(String value) {
        Label label = new Label(value);
        label.setWrapText(true);
        label.setTextOverrun(OverrunStyle.CLIP);
        label.setMaxWidth(TEXT_WIDTH);
        label.setMinHeight(Label.USE_PREF_SIZE);
        return label;
    }

    private static String join(List<String> values) {
        return values == null ? "" : values.stream().collect(Collectors.joining("\n"));
    }
}
