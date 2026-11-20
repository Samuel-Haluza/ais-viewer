package sk.ukf.aisviewer.controller;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.layout.VBox;
import sk.ukf.aisviewer.model.EnrollmentName;
import sk.ukf.aisviewer.model.StudentInfo;
import sk.ukf.aisviewer.model.StudyPlanCatalog;
import sk.ukf.aisviewer.model.Subject;
import sk.ukf.aisviewer.service.LocalCacheService;
import sk.ukf.aisviewer.service.ProgressService;
import sk.ukf.aisviewer.service.ProgressService.Achievement;
import sk.ukf.aisviewer.service.StudyPlanCatalogService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.prefs.Preferences;

/** Záložka „Môj pokrok“ – hodnosť, XP, achievementy a časová os semestrov. */
public class ProgressController {

    /** Len emoji, ktoré JavaFX na Windows vie vykresliť (novšie, napr. 🧩 🧮 🧭, sa zobrazia ako štvorček). */
    private static final String[] RANK_ICONS = {"🌱", "🌿", "🔍", "🗺", "🔬", "💡",
            "🚀", "🎯", "🏆", "🛡", "👑", "🎓"};
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d. M. uuuu");
    /** XP za jeden 6-kreditový predmet so známkou A – na vysvetlenie, koľko chýba. */
    private static final int XP_FOR_A = 180;

    @FXML private VBox rankPanel;
    @FXML private HBox statsBox;
    @FXML private VBox closestBox;
    @FXML private Label achievementCountLabel;
    @FXML private HBox filterBox;
    @FXML private FlowPane achievementsPane;
    @FXML private HBox timelineBox;

    private final ProgressService progressService = new ProgressService();
    private final StudyPlanCatalogService catalogService = new StudyPlanCatalogService();
    private final Preferences preferences = Preferences.userNodeForPackage(ProgressController.class);
    private final ToggleGroup filter = new ToggleGroup();

    private List<Achievement> achievements = List.of();
    /** Achievementy, ktoré používateľ už videl pri predchádzajúcom spustení (načítané raz). */
    private Set<String> seenAtStart;
    private String seenKey;

    @FXML
    public void initialize() {
        filterBox.getChildren().add(filterButton("Všetky", null));
        for (ProgressService.Category category : ProgressService.Category.values()) {
            filterBox.getChildren().add(filterButton(category.label(), category));
        }
        filter.selectToggle(filter.getToggles().get(0));
        filter.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == null) {
                filter.selectToggle(oldToggle); // vždy musí byť vybraný nejaký filter
            } else {
                showAchievements();
            }
        });
    }

    /** Rovnaké parametre ako mal pôvodný strom (StudyTreeController), aby sa MainController menil čo najmenej. */
    public void update(int acquiredCredits, List<Subject> currentSubjects,
                       LocalCacheService.CacheSnapshot cacheSnapshot,
                       StudentInfo studentInfo, int requiredCredits) {
        List<ProgressService.EnrollmentInput> inputs = enrollmentInputs(cacheSnapshot, studentInfo, currentSubjects);
        StudyPlanCatalog catalog = loadCatalog(studentInfo);
        ProgressService.Report report = progressService.compute(inputs, requiredCredits, catalog, LocalDate.now());
        achievements = report.achievements();
        Set<String> newKeys = detectNew(studentInfo, achievements);
        List<Achievement> fresh = achievements.stream().filter(a -> newKeys.contains(a.key())).toList();

        showRank(report, requiredCredits);
        showStats(report, fresh);
        showClosest(report);
        showAchievements(newKeys);
        showTimeline(report);
    }

    // ---------------------------------------------------------------- hodnosť

    /**
     * Veľký panel: prstenec s ikonou hodnosti (vyplní sa podľa XP do ďalšej hodnosti),
     * názov, XP pruh, karta „Ďalej“ a rebríček všetkých 12 hodností.
     */
    private void showRank(ProgressService.Report report, int requiredCredits) {
        ProgressService.Rank rank = report.rank();
        List<ProgressService.Rank> ranks = ProgressService.ranks(requiredCredits > 0 ? requiredCredits : 180);

        // prstenec
        double ringSize = 132;
        double radius = ringSize / 2 - 8;
        Circle track = new Circle(radius);
        track.getStyleClass().add("rank-ring-track");
        Arc arc = new Arc(0, 0, radius, radius, 90, -360 * report.rankProgress());
        arc.setType(ArcType.OPEN);
        arc.getStyleClass().add("rank-ring-arc");
        Label icon = new Label(RANK_ICONS[Math.min(rank.index(), RANK_ICONS.length - 1)]);
        icon.getStyleClass().add("rank-ring-icon");
        Label percent = new Label(Math.round(report.rankProgress() * 100) + " %");
        percent.getStyleClass().add("rank-ring-percent");
        VBox center = new VBox(0, icon, percent);
        center.setAlignment(Pos.CENTER);
        // kruh a oblúk v jednej skupine – StackPane potom vycentruje celý kruh, nie len oblúk
        StackPane ring = new StackPane(new Group(track, arc), center);
        ring.setMinSize(ringSize, ringSize);
        ring.setPrefSize(ringSize, ringSize);
        ring.setMaxSize(ringSize, ringSize);

        // názov a XP
        Label name = new Label(rank.name().toUpperCase());
        name.getStyleClass().add("rank-title");
        Label number = new Label("Hodnosť " + rank.number() + " z " + ranks.size());
        number.getStyleClass().add("rank-subtitle");
        Label xpNow = new Label(formatXp(report.xp()));
        xpNow.getStyleClass().add("rank-xp");
        ProgressBar bar = new ProgressBar(report.rankProgress());
        bar.getStyleClass().add("rank-bar");
        bar.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(bar, Priority.ALWAYS);
        Label xpNext = new Label(report.nextRank() == null ? "" : formatXp(report.nextRank().minXp()));
        xpNext.getStyleClass().add("rank-xp-next");
        HBox xpRow = new HBox(10, xpNow, bar, xpNext);
        xpRow.setAlignment(Pos.CENTER_LEFT);
        Label hint = new Label(report.nextRank() == null
                ? "Najvyššia hodnosť – gratulujeme k ukončeniu štúdia! 🎉"
                : "≈ " + subjectsForA(report.xpToNext()) + " za A (6 kr.)   ·   "
                        + "XP za kredit: A 30 · B 25 · C 20 · D 15 · E 10 · ABS 15");
        hint.getStyleClass().add("rank-hint");
        hint.setWrapText(true);
        VBox info = new VBox(6, name, number, xpRow, hint);
        info.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(info, Priority.ALWAYS);

        HBox top = new HBox(22, ring, info);
        top.setAlignment(Pos.CENTER_LEFT);

        // karta „Ďalej“
        if (report.nextRank() != null) {
            ProgressService.Rank next = report.nextRank();
            Label caption = new Label("ĎALEJ");
            caption.getStyleClass().add("next-caption");
            Label nextName = new Label(RANK_ICONS[Math.min(next.index(), RANK_ICONS.length - 1)] + "  " + next.name());
            nextName.getStyleClass().add("next-name");
            Label missing = new Label("chýba " + formatXp(report.xpToNext()));
            missing.getStyleClass().add("next-missing");
            VBox nextCard = new VBox(4, caption, nextName, missing);
            nextCard.getStyleClass().add("next-card");
            nextCard.setMinWidth(170);
            nextCard.setMaxHeight(Region.USE_PREF_SIZE);
            top.getChildren().add(nextCard);
        }

        rankPanel.getChildren().setAll(top, ladder(ranks, rank));
    }

    /** Rebríček hodností: získané farebné, aktuálna zväčšená, budúce sivé. */
    private HBox ladder(List<ProgressService.Rank> ranks, ProgressService.Rank current) {
        HBox ladder = new HBox(0);
        ladder.setAlignment(Pos.TOP_CENTER);
        ladder.getStyleClass().add("rank-ladder");
        for (int i = 0; i < ranks.size(); i++) {
            ProgressService.Rank rank = ranks.get(i);
            String state = rank.index() < current.index() ? "done"
                    : rank.index() == current.index() ? "current" : "future";
            Label step = new Label(RANK_ICONS[Math.min(i, RANK_ICONS.length - 1)]);
            step.getStyleClass().addAll("ladder-step", "ladder-" + state);
            Tooltip.install(step, new Tooltip(rank.number() + ". " + rank.name() + " – od " + formatXp(rank.minXp())));
            boolean labeled = i == 0 || i == ranks.size() - 1 || "current".equals(state);
            Label caption = new Label(labeled ? rank.name() : " ");
            caption.getStyleClass().addAll("ladder-caption", "ladder-caption-" + state);
            VBox item = new VBox(4, step, caption);
            item.setAlignment(Pos.TOP_CENTER);
            item.setMinWidth(56);
            ladder.getChildren().add(item);
            if (i < ranks.size() - 1) {
                Region line = new Region();
                line.getStyleClass().addAll("ladder-line",
                        rank.index() < current.index() ? "ladder-line-done" : "ladder-line-future");
                line.setPrefHeight(3);
                line.setMaxHeight(3);
                line.setMinWidth(6);
                VBox lineBox = new VBox(line);
                lineBox.getStyleClass().add("ladder-line-box");
                HBox.setHgrow(lineBox, Priority.ALWAYS);
                ladder.getChildren().add(lineBox);
            }
        }
        return ladder;
    }

    private static String subjectsForA(int xp) {
        int count = (int) Math.ceil(xp / (double) XP_FOR_A);
        return count + " " + (count == 1 ? "predmet" : count < 5 ? "predmety" : "predmetov");
    }

    private static String formatXp(int xp) {
        return String.format("%,d XP", xp).replace(',', ' ');
    }

    // ---------------------------------------------------------------- dlaždice

    private void showStats(ProgressService.Report report, List<Achievement> fresh) {
        statsBox.getChildren().setAll(
                achievementsTile(report),
                gradesTile(report),
                firstTryTile(report),
                latestTile(report, fresh));
    }

    private VBox achievementsTile(ProgressService.Report report) {
        long unlocked = report.unlockedCount();
        int total = report.achievements().size();
        double ratio = total == 0 ? 0 : (double) unlocked / total;
        double size = 60;
        double radius = size / 2 - 5;
        Circle track = new Circle(radius);
        track.getStyleClass().add("mini-ring-track");
        Arc arc = new Arc(0, 0, radius, radius, 90, -360 * ratio);
        arc.setType(ArcType.OPEN);
        arc.getStyleClass().add("mini-ring-arc");
        Label pct = new Label(Math.round(ratio * 100) + "%");
        pct.getStyleClass().add("mini-ring-text");
        StackPane ring = new StackPane(new Group(track, arc), pct);
        ring.setMinSize(size, size);
        ring.setMaxSize(size, size);
        Label value = new Label(unlocked + " / " + total);
        value.getStyleClass().add("tile-value");
        Label medals = new Label("🥇 " + report.tierCount(3) + "   🥈 " + report.tierCount(2)
                + "   🥉 " + report.tierCount(1) + "   ✓ " + report.singlesUnlocked());
        medals.getStyleClass().add("tile-caption");
        VBox text = new VBox(2, value, medals);
        text.setAlignment(Pos.CENTER_LEFT);
        HBox body = new HBox(12, ring, text);
        body.setAlignment(Pos.CENTER_LEFT);
        return tile("tile-blue", Icons.TROPHY, "ACHIEVEMENTY", body);
    }

    private VBox gradesTile(ProgressService.Report report) {
        Label value = new Label(report.average() > 0
                ? "⌀ " + String.format("%.2f", report.average()) : "bez známok");
        value.getStyleClass().add("tile-value");
        var counts = report.gradeCounts();
        int max = counts.values().stream().mapToInt(Integer::intValue).max().orElse(1);
        HBox chart = new HBox(5);
        chart.setAlignment(Pos.BOTTOM_LEFT);
        for (var entry : counts.entrySet()) {
            Region column = new Region();
            double height = 4 + 32.0 * entry.getValue() / Math.max(1, max);
            column.setMinSize(18, height);
            column.setPrefSize(18, height);
            column.setMaxSize(18, height);
            column.getStyleClass().addAll("grade-column", "grade-column-" + entry.getKey().toLowerCase());
            Label count = new Label(String.valueOf(entry.getValue()));
            count.getStyleClass().add("grade-column-count");
            Label letter = new Label(entry.getKey());
            letter.getStyleClass().add("grade-column-label");
            letter.setMinWidth(Region.USE_PREF_SIZE);
            VBox item = new VBox(2, count, column, letter);
            item.setAlignment(Pos.BOTTOM_CENTER);
            Tooltip.install(item, new Tooltip(entry.getKey() + ": " + entry.getValue() + "×"));
            chart.getChildren().add(item);
        }
        // priemer nad grafom – vedľa seba by sa v užšom okne orezal
        value.setMinWidth(Region.USE_PREF_SIZE);
        VBox body = new VBox(6, value, chart);
        return tile("tile-purple", Icons.STAR, "ZNÁMKY", body);
    }

    private VBox firstTryTile(ProgressService.Report report) {
        int total = report.examSubjectCount();
        int first = report.firstTryCount();
        double ratio = total == 0 ? 0 : (double) first / total;
        Label value = new Label(first + " / " + total);
        value.getStyleClass().add("tile-value");
        Label pct = new Label(Math.round(ratio * 100) + " %");
        pct.getStyleClass().add("tile-value-small");
        HBox top = new HBox(10, value, pct);
        top.setAlignment(Pos.BASELINE_LEFT);
        ProgressBar bar = new ProgressBar(ratio);
        bar.setMaxWidth(Double.MAX_VALUE);
        bar.getStyleClass().add("tile-bar-green");
        int retakes = total - first;
        Label caption = new Label(total == 0 ? "zatiaľ žiadne skúšky"
                : retakes == 0 ? "všetko na prvý termín 💪"
                : retakes + (retakes == 1 ? " predmet išiel" : retakes < 5 ? " predmety išli" : " predmetov išlo")
                        + " na opravný termín");
        caption.getStyleClass().add("tile-caption");
        VBox body = new VBox(6, top, bar, caption);
        return tile("tile-green", Icons.TARGET, "NA 1. TERMÍN", body);
    }

    /** Keď pribudli nové achievementy, ukáže ich; inak naposledy získaný achievement. */
    private VBox latestTile(ProgressService.Report report, List<Achievement> fresh) {
        if (!fresh.isEmpty()) {
            Label value = new Label("🆕 Nové: " + fresh.size());
            value.getStyleClass().add("tile-value");
            Label names = new Label(fresh.stream().map(a -> a.icon() + " " + a.title())
                    .limit(3).reduce((a, b) -> a + "\n" + b).orElse(""));
            names.getStyleClass().add("tile-caption");
            VBox tile = tile("tile-orange", Icons.SPARKLE, "NOVÉ OD MINULA", new VBox(4, value, names));
            tile.getStyleClass().add("tile-highlight");
            return tile;
        }
        var latest = report.latestAchievement();
        if (latest.isEmpty()) {
            Label none = new Label("Zatiaľ nič – prvý úspech ťa čaká 🙂");
            none.getStyleClass().add("tile-caption");
            return tile("tile-orange", Icons.SPARKLE, "POSLEDNÝ ÚSPECH", new VBox(none));
        }
        Achievement achievement = latest.get();
        Label icon = new Label(achievement.icon());
        icon.getStyleClass().add("latest-icon");
        Label title = new Label(achievement.title());
        title.getStyleClass().add("tile-value-medium");
        title.setWrapText(true);
        Label date = new Label(DATE.format(achievement.achievedOn()));
        date.getStyleClass().add("tile-caption");
        VBox text = new VBox(2, title, date);
        text.setAlignment(Pos.CENTER_LEFT);
        HBox body = new HBox(10, icon, text);
        body.setAlignment(Pos.CENTER_LEFT);
        return tile("tile-orange", Icons.SPARKLE, "POSLEDNÝ ÚSPECH", body);
    }

    /** Spoločný vzhľad dlaždice: farebný ľavý okraj, ikona v krúžku, nadpis a obsah. */
    private VBox tile(String colorClass, String iconPath, String heading, javafx.scene.Node body) {
        Label title = new Label(heading);
        title.getStyleClass().add("tile-heading");
        HBox header = new HBox(8, Icons.circle(iconPath, 28), title);
        header.setAlignment(Pos.CENTER_LEFT);
        VBox tile = new VBox(10, header, body);
        tile.getStyleClass().addAll("credit-card", "stat-tile", colorClass);
        // rovnaká šírka všetkých dlaždíc: základ 0, zvyšok sa rozdelí rovnomerne
        tile.setPrefWidth(0);
        tile.setMinWidth(0);
        tile.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(tile, Priority.ALWAYS);
        return tile;
    }

    // ---------------------------------------------------------------- najbližšie k splneniu

    private void showClosest(ProgressService.Report report) {
        List<Achievement> closest = report.achievements().stream()
                .filter(a -> !a.maxed() && a.progress() > 0 && a.progress() < 1)
                .sorted(Comparator.comparingDouble(Achievement::progress).reversed())
                .limit(4)
                .toList();
        closestBox.getChildren().clear();
        if (closest.isEmpty()) {
            Label none = new Label("Všetko, čo sa dalo, máš splnené. 🎉");
            none.getStyleClass().add("empty-hint");
            closestBox.getChildren().add(none);
            return;
        }
        for (Achievement achievement : closest) {
            Label title = new Label(achievement.icon() + "  " + achievement.title() + tierSuffix(achievement));
            title.getStyleClass().add("closest-title");
            Label value = new Label(achievement.progressText());
            value.getStyleClass().add("closest-value");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            HBox header = new HBox(8, title, spacer, value);
            header.setAlignment(Pos.CENTER_LEFT);
            ProgressBar bar = new ProgressBar(achievement.progress());
            bar.setMaxWidth(Double.MAX_VALUE);
            bar.getStyleClass().add("closest-bar");
            Label description = new Label(achievement.description());
            description.getStyleClass().add("category-hint");
            closestBox.getChildren().add(new VBox(4, header, bar, description));
        }
    }

    // ---------------------------------------------------------------- achievementy

    private Set<String> lastNewKeys = Set.of();

    private void showAchievements(Set<String> newKeys) {
        lastNewKeys = newKeys;
        showAchievements();
    }

    private void showAchievements() {
        ProgressService.Category category = filter.getSelectedToggle() == null ? null
                : (ProgressService.Category) filter.getSelectedToggle().getUserData();
        List<Achievement> visible = achievements.stream()
                .filter(a -> category == null || a.category() == category)
                .sorted(Comparator.comparing((Achievement a) -> !lastNewKeys.contains(a.key()))
                        .thenComparing(a -> !a.unlocked()))
                .toList();
        long unlocked = visible.stream().filter(Achievement::unlocked).count();
        achievementCountLabel.setText(unlocked + " z " + visible.size() + " získaných");
        achievementsPane.getChildren().setAll(visible.stream()
                .map(a -> achievementCard(a, lastNewKeys.contains(a.key()))).toList());
    }

    private VBox achievementCard(Achievement achievement, boolean isNew) {
        Label medal = new Label(medal(achievement));
        medal.getStyleClass().add("achievement-medal");
        Label icon = new Label(achievement.icon());
        icon.getStyleClass().add("achievement-icon");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox top = new HBox(6, icon, spacer, medal);
        top.setAlignment(Pos.CENTER_LEFT);
        if (isNew) {
            Label badge = new Label("NOVÉ");
            badge.getStyleClass().add("achievement-new");
            top.getChildren().add(1, badge);
        }

        Label title = new Label(achievement.title());
        title.getStyleClass().add("achievement-title");
        title.setWrapText(true);
        Label text = new Label(achievement.progressText());
        text.getStyleClass().add("achievement-text");
        text.setWrapText(true);
        ProgressBar bar = new ProgressBar(achievement.progress());
        bar.setMaxWidth(Double.MAX_VALUE);
        bar.getStyleClass().add("achievement-bar");
        Label date = new Label(achievement.achievedOn() == null ? " "
                : "získané " + DATE.format(achievement.achievedOn()));
        date.getStyleClass().add("achievement-date");

        VBox card = new VBox(6, top, title, text, bar, date);
        card.getStyleClass().add("achievement-card");
        if (!achievement.unlocked()) card.getStyleClass().add("achievement-locked");
        if (achievement.maxed()) card.getStyleClass().add("achievement-maxed");
        if (isNew) card.getStyleClass().add("achievement-card-new");
        card.setPrefWidth(178);
        card.setMinWidth(178);
        Tooltip.install(card, new Tooltip(achievement.title() + "\n" + achievement.description()
                + "\n\n" + achievement.progressText()));
        return card;
    }

    private static String medal(Achievement achievement) {
        if (achievement.tierCount() > 1) {
            return switch (achievement.achievedTier()) {
                case 0 -> "🔒";
                case 1 -> "🥉";
                case 2 -> "🥈";
                default -> "🥇";
            };
        }
        return achievement.unlocked() ? "✅" : "🔒";
    }

    private static String tierSuffix(Achievement achievement) {
        if (achievement.tierCount() <= 1) return "";
        String[] next = {"🥉", "🥈", "🥇"};
        return "  → " + next[Math.min(achievement.achievedTier(), 2)];
    }

    private ToggleButton filterButton(String text, ProgressService.Category category) {
        ToggleButton button = new ToggleButton(text);
        button.setUserData(category);
        button.setToggleGroup(filter);
        button.getStyleClass().add("filter-chip");
        return button;
    }

    // ---------------------------------------------------------------- časová os

    private void showTimeline(ProgressService.Report report) {
        timelineBox.getChildren().clear();
        List<ProgressService.SemesterStat> semesters = report.timeline();
        for (int i = 0; i < semesters.size(); i++) {
            ProgressService.SemesterStat semester = semesters.get(i);
            String state = semester.total() > 0 && semester.completed() >= semester.total() ? "done"
                    : semester.completed() > 0 ? "partial" : "future";
            Label dot = new Label(switch (state) {
                case "done" -> "●";
                case "partial" -> "◐";
                default -> "○";
            });
            dot.getStyleClass().addAll("timeline-dot", "timeline-" + state);
            Label label = new Label(semester.label());
            label.getStyleClass().add("timeline-label");
            Label credits = new Label("future".equals(state) ? semester.total() + " predmetov"
                    : semester.credits() + " kr.  ·  ⌀ " + semester.average());
            credits.getStyleClass().add("timeline-detail");
            Label count = new Label(semester.completed() + " / " + semester.total() + " splnených");
            count.getStyleClass().add("timeline-detail");
            VBox item = new VBox(3, dot, label, credits, count);
            item.setAlignment(Pos.TOP_CENTER);
            item.getStyleClass().add("timeline-item");
            HBox.setHgrow(item, Priority.ALWAYS);
            item.setMaxWidth(Double.MAX_VALUE);
            timelineBox.getChildren().add(item);
            if (i < semesters.size() - 1) {
                Region line = new Region();
                line.getStyleClass().add("timeline-line");
                if ("done".equals(state)) line.getStyleClass().add("timeline-line-done");
                line.setMinWidth(16);
                line.setPrefHeight(3);
                line.setMaxHeight(3);
                HBox.setHgrow(line, Priority.SOMETIMES);
                VBox lineBox = new VBox(line);
                lineBox.setAlignment(Pos.TOP_CENTER);
                lineBox.getStyleClass().add("timeline-line-box");
                HBox.setHgrow(lineBox, Priority.SOMETIMES);
                timelineBox.getChildren().add(lineBox);
            }
        }
    }

    // ---------------------------------------------------------------- údaje

    /**
     * Zápisné listy od najstaršieho s predmetmi a skúškami z cache. Predtým sa predmety vybraného
     * ročníka chybne vkladali pod aktuálny ročník – teraz sa vždy použijú údaje každého ročníka zvlášť.
     */
    private List<ProgressService.EnrollmentInput> enrollmentInputs(LocalCacheService.CacheSnapshot snapshot,
                                                                   StudentInfo studentInfo,
                                                                   List<Subject> currentSubjects) {
        List<ProgressService.EnrollmentInput> inputs = new ArrayList<>();
        if (snapshot != null && snapshot.getEnrollmentData() != null && studentInfo != null
                && studentInfo.getEnrollmentListIds() != null) {
            List<String> ids = new ArrayList<>(studentInfo.getEnrollmentListIds());
            List<String> names = studentInfo.getEnrollmentListNames();
            ids.sort(Comparator.comparingInt(id -> EnrollmentName.startYear(nameOf(id, studentInfo, names))));
            for (String id : ids) {
                LocalCacheService.EnrollmentData data = snapshot.getEnrollmentData().get(id);
                if (data != null) {
                    inputs.add(new ProgressService.EnrollmentInput(nameOf(id, studentInfo, names),
                            data.getSubjects(), data.getExams()));
                }
            }
        }
        if (inputs.isEmpty() && currentSubjects != null && !currentSubjects.isEmpty()) {
            inputs.add(new ProgressService.EnrollmentInput("", currentSubjects, List.of()));
        }
        return inputs;
    }

    private static String nameOf(String id, StudentInfo info, List<String> names) {
        int index = info.getEnrollmentListIds().indexOf(id);
        return names != null && index >= 0 && index < names.size() ? names.get(index) : id;
    }

    private StudyPlanCatalog loadCatalog(StudentInfo studentInfo) {
        try {
            return studentInfo == null ? null : catalogService.loadForStudent(studentInfo).orElse(null);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /**
     * Nové = získané teraz, ale ešte nevidené pri predchádzajúcom spustení aplikácie.
     * Pri úplne prvom spustení sa nič neoznačí (všetko sa berie ako východiskový stav).
     */
    private Set<String> detectNew(StudentInfo studentInfo, List<Achievement> current) {
        String key = "seen." + (studentInfo == null || studentInfo.getIdo() == null ? "default" : studentInfo.getIdo());
        List<String> unlockedKeys = current.stream().filter(Achievement::unlocked).map(Achievement::key).toList();
        if (seenAtStart == null || !key.equals(seenKey)) {
            String stored = preferences.get(key, null);
            seenKey = key;
            seenAtStart = stored == null ? new HashSet<>(unlockedKeys) : new HashSet<>(Arrays.asList(stored.split(",")));
        }
        preferences.put(key, String.join(",", unlockedKeys));
        Set<String> fresh = new HashSet<>(unlockedKeys);
        fresh.removeAll(seenAtStart);
        return fresh;
    }
}
