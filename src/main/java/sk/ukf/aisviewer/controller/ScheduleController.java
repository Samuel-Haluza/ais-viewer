package sk.ukf.aisviewer.controller;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import sk.ukf.aisviewer.model.ScheduleEntry;

import java.util.List;

public class ScheduleController {

    private static final String[] DAY_NAMES = {"Pondelok", "Utorok", "Streda", "Štvrtok", "Piatok"};
    private static final int MINUTES_PER_SLOT = 30; // each row = 30 min

    private final VBox scheduleContainer;
    private final Label scheduleStatusLabel;

    public ScheduleController(VBox scheduleContainer, Label scheduleStatusLabel) {
        this.scheduleContainer = scheduleContainer;
        this.scheduleStatusLabel = scheduleStatusLabel;
    }

    public void displaySchedule(List<ScheduleEntry> entries) {
        buildScheduleGrid(entries);
        scheduleStatusLabel.setVisible(false);
    }

    public void clear() {
        scheduleContainer.getChildren().clear();
    }

    public void showUnavailable() {
        scheduleStatusLabel.setText("Dáta sa nepodarilo načítať.");
        scheduleStatusLabel.setVisible(true);
    }

    public void showLoading(String message) {
        scheduleStatusLabel.setText(message);
        scheduleStatusLabel.setVisible(true);
        scheduleContainer.getChildren().clear();
        Label loadingLabel = new Label(message);
        loadingLabel.getStyleClass().add("schedule-loading-label");
        scheduleContainer.getChildren().add(loadingLabel);
    }

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
