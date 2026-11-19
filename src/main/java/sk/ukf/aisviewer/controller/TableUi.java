package sk.ukf.aisviewer.controller;

import javafx.collections.ListChangeListener;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.util.Callback;
import sk.ukf.aisviewer.model.Subject;

import java.util.Locale;

/** Spoločné úpravy tabuliek: výška podľa počtu riadkov, farebná známka, tooltip pri orezanom texte. */
final class TableUi {

    static final double ROW_HEIGHT = 34;
    private static final double HEADER_HEIGHT = 36;
    private static final double EMPTY_HEIGHT = 90;

    private TableUi() {
    }

    /**
     * Tabuľka bude vysoká presne na počet riadkov – bez vlastného posuvníka,
     * stránka sa tak posúva iba raz (predtým sa posúvala stránka aj tabuľka).
     */
    static void fitHeightToRows(TableView<?> table) {
        table.setFixedCellSize(ROW_HEIGHT);
        Runnable update = () -> {
            int rows = table.getItems() == null ? 0 : table.getItems().size();
            double height = rows == 0 ? EMPTY_HEIGHT : HEADER_HEIGHT + rows * ROW_HEIGHT + 4;
            table.setMinHeight(height);
            table.setPrefHeight(height);
            table.setMaxHeight(height);
        };
        table.itemsProperty().addListener((obs, oldItems, newItems) -> {
            update.run();
            if (newItems != null) {
                newItems.addListener((ListChangeListener<Object>) change -> update.run());
            }
        });
        update.run();
    }

    /**
     * Šírka stĺpcov ako podiel zo šírky tabuľky (napr. 0.30 = 30 %). JavaFX by inak
     * rozdelil miesto rovnomerne a dlhé názvy predmetov by sa orezávali.
     */
    static void columnWeights(TableView<?> table, double... weights) {
        table.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        for (int i = 0; i < weights.length && i < table.getColumns().size(); i++) {
            TableColumn<?, ?> column = table.getColumns().get(i);
            column.setResizable(false);
            column.prefWidthProperty().bind(table.widthProperty().subtract(12).multiply(weights[i]));
        }
    }

    /** Bunka so známkou ako farebný štítok: A zelená … E žltá, FX červená, ABS sivozelená. */
    static Callback<TableColumn<Subject, String>, TableCell<Subject, String>> gradeCell() {
        return column -> new TableCell<>() {
            private final Label chip = new Label();

            @Override
            protected void updateItem(String grade, boolean empty) {
                super.updateItem(grade, empty);
                setText(null);
                if (empty || grade == null || grade.isBlank() || "-".equals(grade.trim())) {
                    setGraphic(null);
                    setTooltip(null);
                    return;
                }
                chip.setText(grade);
                chip.getStyleClass().setAll("label", "grade-chip", gradeStyleClass(grade));
                setGraphic(chip);
                setTooltip(new Tooltip(grade));
            }
        };
    }

    /** Text bunky + tooltip s celým textom (pri dlhých názvoch, ktoré sa orežú). */
    static <S> Callback<TableColumn<S, String>, TableCell<S, String>> textWithTooltip() {
        return column -> new TableCell<>() {
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                setText(empty ? null : value);
                setTooltip(empty || value == null || value.isBlank() ? null : new Tooltip(value));
            }
        };
    }

    static String gradeStyleClass(String grade) {
        String value = grade.trim().toUpperCase(Locale.ROOT);
        if (value.startsWith("FX") || value.startsWith("NEABS")) return "grade-fx";
        if (value.startsWith("ABS")) return "grade-abs";
        if (value.startsWith("A")) return "grade-a";
        if (value.startsWith("B")) return "grade-b";
        if (value.startsWith("C")) return "grade-c";
        if (value.startsWith("D")) return "grade-d";
        if (value.startsWith("E")) return "grade-e";
        return "grade-other";
    }
}
