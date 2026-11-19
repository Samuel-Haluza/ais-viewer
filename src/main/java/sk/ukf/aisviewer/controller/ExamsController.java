package sk.ukf.aisviewer.controller;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import sk.ukf.aisviewer.model.Exam;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Záložka Skúšky – karty termínov rozdelené na nadchádzajúce a minulé. */
public class ExamsController {

    private static final DateTimeFormatter AIS_DATE = DateTimeFormatter.ofPattern("d.M.uuuu");
    private static final String[] MONTHS = {"jan", "feb", "mar", "apr", "máj", "jún",
            "júl", "aug", "sep", "okt", "nov", "dec"};
    private static final String[] DAYS = {"pondelok", "utorok", "streda", "štvrtok", "piatok", "sobota", "nedeľa"};

    @FXML private Label examsStatusLabel;
    @FXML private Label upcomingTitle;
    @FXML private Label pastTitle;
    @FXML private VBox upcomingBox;
    @FXML private VBox pastBox;

    public void displayExams(List<Exam> exams) {
        displayExams(exams, LocalDate.now());
    }

    void displayExams(List<Exam> exams, LocalDate today) {
        upcomingBox.getChildren().clear();
        pastBox.getChildren().clear();

        List<Exam> upcoming = new ArrayList<>();
        List<Exam> past = new ArrayList<>();
        for (Exam exam : exams) {
            LocalDate date = parseDate(exam.getDate());
            if (date != null && date.isBefore(today)) {
                past.add(exam);
            } else {
                upcoming.add(exam);
            }
        }
        Comparator<Exam> byDate = Comparator.comparing(
                exam -> parseDate(exam.getDate()), Comparator.nullsLast(Comparator.naturalOrder()));
        upcoming.sort(byDate.thenComparing(exam -> exam.getTime() == null ? "" : exam.getTime()));
        past.sort(byDate.reversed());

        upcoming.forEach(exam -> upcomingBox.getChildren().add(createCard(exam, today, false)));
        past.forEach(exam -> pastBox.getChildren().add(createCard(exam, today, true)));

        upcomingTitle.setText("Nadchádzajúce (" + upcoming.size() + ")");
        pastTitle.setText("Minulé termíny (" + past.size() + ")");
        show(upcomingTitle, true);
        show(pastTitle, !past.isEmpty());
        if (upcoming.isEmpty()) {
            Label none = new Label(exams.isEmpty()
                    ? "Žiadne skúškové termíny neboli nájdené."
                    : "Žiadny nadchádzajúci termín – všetky skúšky sú už za tebou.");
            none.getStyleClass().add("empty-hint");
            upcomingBox.getChildren().add(none);
        }
        show(examsStatusLabel, false);
    }

    public void clear() {
        upcomingBox.getChildren().clear();
        pastBox.getChildren().clear();
    }

    public void showLoading(String message) {
        examsStatusLabel.setText(message);
        show(examsStatusLabel, true);
    }

    public void showUnavailable() {
        examsStatusLabel.setText("Dáta sa nepodarilo načítať.");
        show(examsStatusLabel, true);
    }

    private HBox createCard(Exam exam, LocalDate today, boolean past) {
        LocalDate date = parseDate(exam.getDate());

        // dátum vľavo ako "kalendárový lístok"
        Label day = new Label(date == null ? "?" : String.valueOf(date.getDayOfMonth()));
        day.getStyleClass().add("exam-date-day");
        Label month = new Label(date == null ? "" : MONTHS[date.getMonthValue() - 1] + " " + date.getYear());
        month.getStyleClass().add("exam-date-month");
        VBox dateBox = new VBox(0, day, month);
        dateBox.setAlignment(Pos.CENTER);
        dateBox.getStyleClass().add("exam-date");

        Label name = new Label(exam.getSubjectName());
        name.getStyleClass().add("exam-name");
        name.setWrapText(true);
        StringBuilder detail = new StringBuilder();
        if (date != null) detail.append(DAYS[date.getDayOfWeek().getValue() - 1]);
        append(detail, exam.getTime());
        append(detail, exam.getRoom() == null || exam.getRoom().isBlank() ? null : "📍 " + exam.getRoom());
        append(detail, exam.getType());
        Label details = new Label(detail.toString());
        details.getStyleClass().add("exam-detail");
        VBox info = new VBox(2, name, details);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label status = new Label(exam.getStatus() == null || exam.getStatus().isBlank() ? "–" : exam.getStatus());
        status.getStyleClass().addAll("exam-status", statusStyle(exam.getStatus()));
        Label countdown = new Label(countdownText(date, today));
        countdown.getStyleClass().add("exam-countdown");
        VBox right = new VBox(4, status, countdown);
        right.setAlignment(Pos.CENTER_RIGHT);

        HBox card = new HBox(14, dateBox, info, spacer(), right);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("exam-card");
        if (past) {
            card.getStyleClass().add("exam-card-past");
        } else if (date != null && ChronoUnit.DAYS.between(today, date) <= 7) {
            card.getStyleClass().add("exam-card-soon");
        }
        return card;
    }

    /** "dnes", "zajtra", "o 3 dni", "o 57 dní", "pred 3 dňami"… */
    static String countdownText(LocalDate date, LocalDate today) {
        if (date == null) return "";
        long days = ChronoUnit.DAYS.between(today, date);
        if (days == 0) return "dnes";
        if (days == 1) return "zajtra";
        if (days == -1) return "včera";
        if (days > 1) return "o " + days + (days < 5 ? " dni" : " dní");
        return "pred " + (-days) + " dňami";
    }

    static LocalDate parseDate(String text) {
        if (text == null || text.isBlank()) return null;
        try {
            return LocalDate.parse(text.trim(), AIS_DATE);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static String statusStyle(String status) {
        String value = status == null ? "" : status.toLowerCase(Locale.ROOT);
        if (value.contains("prihlás")) return "exam-status-registered";
        if (value.contains("odhlás")) return "exam-status-cancelled";
        return "exam-status-other";
    }

    private static void append(StringBuilder text, String part) {
        if (part == null || part.isBlank()) return;
        if (text.length() > 0) text.append("  •  ");
        text.append(part);
    }

    private static Region spacer() {
        Region region = new Region();
        HBox.setHgrow(region, Priority.SOMETIMES);
        return region;
    }

    private static void show(Label label, boolean visible) {
        label.setVisible(visible);
        label.setManaged(visible);
    }
}
