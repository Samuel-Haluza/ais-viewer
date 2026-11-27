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

    static final double ROW_HEIGHT = 30;
    private static final double HEADER_HEIGHT = 31;
    private static final double EMPTY_HEIGHT = 80;
    private static final String WEIGHT_KEY = "ais.columnWeight";

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
            column.getProperties().put(WEIGHT_KEY, weights[i]);
            column.visibleProperty().addListener((obs, was, now) -> bindWidths(table));
        }
        bindWidths(table);
    }

    /** Skryté stĺpce sa nerátajú – ich miesto si rozdelia viditeľné stĺpce podľa váh. */
    private static void bindWidths(TableView<?> table) {
        double sum = table.getColumns().stream()
                .filter(TableColumn::isVisible)
                .mapToDouble(column -> weight(column))
                .sum();
        if (sum <= 0) return;
        for (TableColumn<?, ?> column : table.getColumns()) {
            // stĺpec bez váhy si ponechá vlastnú šírku (inak by dostal šírku 0 a zmizol)
            if (!column.getProperties().containsKey(WEIGHT_KEY)) continue;
            column.prefWidthProperty().unbind();
            if (column.isVisible()) {
                column.prefWidthProperty().bind(table.widthProperty().subtract(14).multiply(weight(column) / sum));
            }
        }
    }

    private static double weight(TableColumn<?, ?> column) {
        Object value = column.getProperties().get(WEIGHT_KEY);
        return value instanceof Double weight ? weight : 0;
    }

    /** Čísla zarovnané doprava písmom s rovnako širokými číslicami (aj v hlavičke). */
    static void numeric(TableColumn<?, ?> column) {
        column.getStyleClass().add("numeric-column");
    }

    /** Kódy predmetov a skratky neproporcionálnym písmom. */
    static void code(TableColumn<?, ?> column) {
        column.getStyleClass().add("code-column");
    }

    /** Stĺpec sa zobrazí, len ak má aspoň jeden riadok skutočnú hodnotu (nie prázdnu ani „-“). */
    static <S> void hideWhenEmpty(TableColumn<S, String> column) {
        Runnable update = () -> {
            TableView<S> table = column.getTableView();
            if (table == null || table.getItems() == null) return;
            boolean hasValue = table.getItems().stream().anyMatch(item -> {
                Object value = column.getCellObservableValue(item) == null
                        ? null : column.getCellObservableValue(item).getValue();
                return value != null && !value.toString().isBlank() && !"-".equals(value.toString().trim());
            });
            column.setVisible(hasValue || table.getItems().isEmpty());
        };
        column.tableViewProperty().addListener((obs, oldTable, table) -> {
            if (table == null) return;
            table.itemsProperty().addListener((o, oldItems, items) -> {
                update.run();
                if (items != null) items.addListener((ListChangeListener<S>) change -> update.run());
            });
            update.run();
        });
    }

    /** Bunka so známkou: písmeno ako vo výkaze, farba len dopĺňa text (A, B zelená, FX červená). */
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
                boolean blank = value == null || value.isBlank() || "-".equals(value.trim());
                setText(empty ? null : blank ? "—" : value);
                getStyleClass().remove("muted-cell");
                if (!empty && blank) getStyleClass().add("muted-cell");
                setTooltip(empty || blank ? null : new Tooltip(value));
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
