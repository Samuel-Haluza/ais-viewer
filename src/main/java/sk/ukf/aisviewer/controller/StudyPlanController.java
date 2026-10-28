package sk.ukf.aisviewer.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import sk.ukf.aisviewer.service.ThemeManager;
import sk.ukf.aisviewer.model.*;
import sk.ukf.aisviewer.service.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class StudyPlanController {
    @FXML private ComboBox<StudyDirection> primaryDirectionCombo;
    @FXML private ComboBox<StudyDirection> secondaryDirectionCombo;
    @FXML private TableView<Recommendation> mandatoryTable;
    @FXML private TableView<Recommendation> recommendationTable;
    @FXML private TableView<PlannedSubject> plannedTable;
    @FXML private Label statusLabel;
    @FXML private Label creditsLabel;

    private final StudyPlanCatalogService catalogService = new StudyPlanCatalogService();
    private final UserStudyPlanService userPlanService = new UserStudyPlanService();
    private final RecommendationService recommendationService = new RecommendationService();
    private final StudyPlanCreditsService creditsService = new StudyPlanCreditsService();
    private final SubjectInfoService subjectInfoService = new SubjectInfoService();
    private UserStudyPlan plan;
    private StudyPlanCatalog catalog;
    private List<Subject> completedSubjects = List.of();

    @FXML
    public void initialize() {
        primaryDirectionCombo.setItems(FXCollections.observableArrayList(StudyDirection.values()));
        List<StudyDirection> secondary = new ArrayList<>();
        secondary.add(null);
        secondary.addAll(List.of(StudyDirection.values()));
        secondaryDirectionCombo.setItems(FXCollections.observableArrayList(secondary));
        secondaryDirectionCombo.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(StudyDirection item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item == null ? "Bez vedľajšieho smeru" : item.toString());
            }

            private String reasonText(RecommendationReason reason) {
                if (reason == RecommendationReason.PRIMARY_DIRECTION && plan != null
                        && plan.getPrimaryDirection() != null) {
                    return "Odporúčaný pre hlavný smer " + plan.getPrimaryDirection().getSlovakName();
                }
                if (reason == RecommendationReason.SECONDARY_DIRECTION && plan != null
                        && plan.getSecondaryDirection() != null) {
                    return "Odporúčaný pre vedľajší smer " + plan.getSecondaryDirection().getSlovakName();
                }
                return reason.getDisplayName();
            }
        });
        secondaryDirectionCombo.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(StudyDirection item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item == null ? "Bez vedľajšieho smeru" : item.toString());
            }
        });
        configureTables();
        primaryDirectionCombo.setOnAction(event -> directionsChanged());
        secondaryDirectionCombo.setOnAction(event -> directionsChanged());
        plan = userPlanService.load();
    }

    public void update(StudentInfo studentInfo, List<Subject> currentSubjects,
                       LocalCacheService.CacheSnapshot snapshot) {
        DegreeLevel level = catalogService.resolveDegreeLevel(studentInfo);
        StudyForm form = catalogService.resolveStudyForm(studentInfo);
        catalog = catalogService.load(level, form).orElse(null);
        System.out.println("[STUDY-PLAN] degreeLevel=" + level + ", studyForm=" + form
                + ", catalog=" + catalogService.resourceName(level, form)
                + ", subjects=" + (catalog == null ? 0 : catalog.getSubjects().size()));
        completedSubjects = StudyPlanCompletionService.aggregateSubjects(currentSubjects, snapshot);
        printEnrollmentDiagnostics(snapshot, completedSubjects);
        if (catalog == null) {
            statusLabel.setText("Lokálny katalóg pre " + level + " / " + form
                    + " nie je dostupný.");
            clearTables();
            return;
        }
        primaryDirectionCombo.getSelectionModel().select(plan.getPrimaryDirection());
        secondaryDirectionCombo.getSelectionModel().select(plan.getSecondaryDirection());
        refresh();
    }

    @FXML
    private void addSelectedRecommendation() {
        addSelected(recommendationTable);
    }

    @FXML
    private void removeSelectedPlanned() {
        PlannedSubject selected = plannedTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            userPlanService.removeSubject(plan, selected.getSubjectCode());
            saveAndRefresh();
        }
    }

    private void addSelected(TableView<Recommendation> table) {
        Recommendation selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        userPlanService.addSubject(plan, selected.getSubject());
        saveAndRefresh();
    }

    @FXML
    private void showAllPvp() {
        if (catalog == null) return;
        TextField search = new TextField();
        search.setPromptText("Hľadať podľa názvu alebo kódu");
        TableView<Recommendation> table = new TableView<>();
        table.getStyleClass().add("subject-table");
        List<Recommendation> allPvp = new ArrayList<>(recommendationService.otherSubjects(
                catalog, completedSubjects, plan.getPlannedSubjects(),
                plan.getPrimaryDirection(), plan.getSecondaryDirection()));
        table.setItems(FXCollections.observableArrayList(allPvp));
        addDialogColumns(table);
        search.textProperty().addListener((observable, oldValue, newValue) -> {
            String query = newValue == null ? "" : newValue.trim().toLowerCase(Locale.ROOT);
            table.setItems(FXCollections.observableArrayList(allPvp.stream()
                    .filter(item -> query.isBlank()
                            || item.getSubject().getName().toLowerCase(Locale.ROOT).contains(query)
                            || normalize(item.getSubject().getCode()).contains(query))
                    .toList()));
        });
        Button add = new Button("Pridať do plánu");
        add.setOnAction(event -> {
            Recommendation selected = table.getSelectionModel().getSelectedItem();
            if (selected != null) {
                addRecommendation(selected);
                table.getItems().remove(selected);
            }
        });
        VBox content = new VBox(8, search, table, add);
        content.setPrefSize(700, 450);
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Všetky PVP predmety");
        ThemeManager.getInstance().applyTheme(dialog.getDialogPane());
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    private void addRecommendation(Recommendation recommendation) {
        userPlanService.addSubject(plan, recommendation.getSubject());
        saveAndRefresh();
    }

    private void directionsChanged() {
        if (plan == null) return;
        try {
            userPlanService.setDirections(plan, primaryDirectionCombo.getValue(),
                    secondaryDirectionCombo.getValue());
            saveAndRefresh();
        } catch (IllegalArgumentException e) {
            statusLabel.setText(e.getMessage());
        }
    }

    private void saveAndRefresh() {
        try {
            userPlanService.save(plan);
            refresh();
        } catch (IOException | RuntimeException e) {
            statusLabel.setText("Plán sa nepodarilo uložiť: " + e.getMessage());
        }
    }

    private void refresh() {
        List<PlannedSubject> planned = plan.getPlannedSubjects();
        List<Recommendation> mandatory = recommendationService.mandatory(catalog, completedSubjects, planned);
        mandatoryTable.setItems(FXCollections.observableArrayList(mandatory));
        List<Recommendation> recommendations = recommendationService.recommendations(
                catalog, completedSubjects, planned, plan.getPrimaryDirection(),
                plan.getSecondaryDirection());
        logCandidateDecisions(mandatory, recommendations);
        recommendationTable.setItems(FXCollections.observableArrayList(
                recommendations));
        plannedTable.setItems(FXCollections.observableArrayList(planned));
        int plannedCredits = creditsService.plannedCredits(catalog, plan, completedSubjects);
        int acquired = creditsService.acquiredTotalCredits(catalog, completedSubjects);
        creditsLabel.setText("Získané kredity: " + acquired + " / " + catalog.getRequiredTotalCredits()
                + "\nNaplánované: " + plannedCredits
                + "\nPo splnení plánu: " + (acquired + plannedCredits)
                + " / " + catalog.getRequiredTotalCredits());
        statusLabel.setText("");
    }

    private void logCandidateDecisions(List<Recommendation> mandatory,
                                       List<Recommendation> recommendations) {
        Set<String> completedCodes = recommendationService.completedCodes(catalog, completedSubjects);
        List<Recommendation> allPvp = recommendationService.otherSubjects(
                catalog, completedSubjects, plan.getPlannedSubjects(),
                plan.getPrimaryDirection(), plan.getSecondaryDirection());
        Set<String> visible = new HashSet<>();
        mandatory.forEach(item -> visible.add(
                SubjectCompletionService.extractSubjectAbbreviation(item.getSubject().getCode())));
        recommendations.forEach(item -> visible.add(
                SubjectCompletionService.extractSubjectAbbreviation(item.getSubject().getCode())));
        allPvp.forEach(item -> visible.add(
                SubjectCompletionService.extractSubjectAbbreviation(item.getSubject().getCode())));
        catalog.getSubjects().forEach(subject -> {
            String code = SubjectCompletionService.extractSubjectAbbreviation(subject.getCode());
            boolean completed = completedCodes.contains(code);
            boolean displayed = visible.contains(code);
            completedSubjects.stream()
                    .filter(ais -> similarName(ais.getName(), subject.getName()))
                    .forEach(ais -> System.out.println("[STUDY-PLAN][NAME-CANDIDATE] curriculum="
                            + subject.getCode() + ", aisOriginal=[" + ais.getAbbreviation()
                            + "], aisName=[" + ais.getName() + "], codePresent="
                            + (ais.getAbbreviation() != null && !ais.getAbbreviation().isBlank())
                            + ", fallbackByNameUsed=false"));
            completedSubjects.stream()
                    .filter(SubjectCompletionService::isCompleted)
                    .filter(ais -> recommendationService.codesEquivalent(ais.getAbbreviation(), subject))
                    .forEach(ais -> System.out.println("[STUDY-PLAN][MATCH] curriculum="
                            + subject.getCode() + ", aisOriginal=[" + ais.getAbbreviation()
                            + "], aisNormalized=[" + normalize(ais.getAbbreviation())
                            + "], extractedCurriculumAbbreviation=[" + code
                            + "], currentCodeMatch="
                            + normalize(ais.getAbbreviation()).equals(code)
                            + ", equivalentCodeMatch=" + subject.getEquivalentCodes().stream()
                            .map(SubjectCompletionService::extractSubjectAbbreviation)
                            .anyMatch(normalize(ais.getAbbreviation())::equals)
                            + ", " + SubjectCompletionService.decisionDetails(ais)));
            System.out.println("[STUDY-PLAN][DECISION] curriculum code=[" + subject.getCode()
                    + "], name=[" + subject.getName() + "], equivalentCodes="
                    + subject.getEquivalentCodes() + ", type=" + subject.getType()
                    + ", extractedCurriculumAbbreviation=[" + code
                    + "], normalizedCurriculumCode=[" + code + "], completedCodesContains="
                    + completed + ", insertedIntoResult=" + displayed);
        });
    }

    private boolean similarName(String left, String right) {
        if (left == null || right == null) return false;
        String normalizedLeft = left.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
        String normalizedRight = right.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
        return !normalizedLeft.isBlank() && !normalizedRight.isBlank()
                && (normalizedLeft.contains(normalizedRight)
                || normalizedRight.contains(normalizedLeft));
    }

    private void configureTables() {
        addMandatoryColumns();
        addRecommendationColumns();
        TableColumn<PlannedSubject, String> subject = new TableColumn<>("Predmet");
        subject.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                catalogService.findByCode(catalog, data.getValue().getSubjectCode())
                        .map(CurriculumSubject::getName)
                        .orElse("Neznámy predmet")));
        TableColumn<PlannedSubject, String> code = new TableColumn<>("Kód");
        code.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().getSubjectCode()));
        TableColumn<PlannedSubject, String> credits = new TableColumn<>("Kredity");
        credits.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                catalogService.findByCode(catalog, data.getValue().getSubjectCode())
                        .map(subjectItem -> String.valueOf(subjectItem.getCredits()))
                        .orElse("0")));
        TableColumn<PlannedSubject, String> year = new TableColumn<>("Ročník");
        year.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                String.valueOf(data.getValue().getPlannedYear())));
        TableColumn<PlannedSubject, String> semester = new TableColumn<>("Semester");
        semester.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                semesterText(data.getValue().getPlannedSemester())));
        TableColumn<PlannedSubject, Void> info = plannedInfoColumn();
        plannedTable.getColumns().setAll(subject, code, credits, year, semester, info);
    }

    private void addMandatoryColumns() {
        TableColumn<Recommendation, String> subject = new TableColumn<>("Predmet");
        subject.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().getSubject().getName()));
        TableColumn<Recommendation, String> credits = new TableColumn<>("Kredity");
        credits.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                String.valueOf(data.getValue().getSubject().getCredits())));
        TableColumn<Recommendation, String> semester = semesterColumn();
        mandatoryTable.getColumns().setAll(subject, credits, semester, infoColumn());
    }

    private void addRecommendationColumns() {
        TableColumn<Recommendation, String> subject = new TableColumn<>("Predmet");
        subject.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().getSubject().getName() + " (" + data.getValue().getSubject().getCode() + ")"));
        TableColumn<Recommendation, String> credits = new TableColumn<>("Kredity");
        credits.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                String.valueOf(data.getValue().getSubject().getCredits())));
        TableColumn<Recommendation, String> semester = semesterColumn();
        TableColumn<Recommendation, String> reason = new TableColumn<>("Prečo odporúčame");
        reason.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                reasonText(data.getValue().getReason())));
        recommendationTable.getColumns().setAll(subject, credits, semester, reason, infoColumn());
    }

    private TableColumn<Recommendation, String> semesterColumn() {
        TableColumn<Recommendation, String> semester = new TableColumn<>("Odporúčaný semester");
        semester.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().getSubject().getRecommendedYear() + ". ročník, "
                        + semesterText(data.getValue().getSubject().getSemester())));
        return semester;
    }

    private void addDialogColumns(TableView<Recommendation> table) {
        TableColumn<Recommendation, String> subject = new TableColumn<>("Predmet");
        subject.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().getSubject().getName()));
        TableColumn<Recommendation, String> code = new TableColumn<>("Kód");
        code.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().getSubject().getCode()));
        TableColumn<Recommendation, String> credits = new TableColumn<>("Kredity");
        credits.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(
                String.valueOf(data.getValue().getSubject().getCredits())));
        TableColumn<Recommendation, String> semester = semesterColumn();
        table.getColumns().setAll(subject, code, credits, semester, infoColumn());
    }

    private TableColumn<Recommendation, Void> infoColumn() {
        TableColumn<Recommendation, Void> column = new TableColumn<>("Info");
        column.setCellFactory(table -> new TableCell<>() {
            private final Button button = new Button("Info");
            {
                button.getStyleClass().add("dialog-secondary-button");
                button.setOnAction(event -> {
                    Recommendation recommendation = getTableView().getItems().get(getIndex());
                    openSubjectInfo(recommendation.getSubject());
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : button);
            }
        });
        return column;
    }

    private TableColumn<PlannedSubject, Void> plannedInfoColumn() {
        TableColumn<PlannedSubject, Void> column = new TableColumn<>("Info");
        column.setCellFactory(table -> new TableCell<>() {
            private final Button button = new Button("Info");
            {
                button.getStyleClass().add("dialog-secondary-button");
                button.setOnAction(event -> {
                    PlannedSubject planned = getTableView().getItems().get(getIndex());
                    catalogService.findByCode(catalog, planned.getSubjectCode())
                            .ifPresent(StudyPlanController.this::openSubjectInfo);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : button);
            }
        });
        return column;
    }

    private void openSubjectInfo(CurriculumSubject subject) {
        SubjectInfoDialog.show(subject, catalog, subjectInfoService);
    }

    private String semesterText(Semester semester) {
        return switch (semester) {
            case WINTER -> "Zimný";
            case SUMMER -> "Letný";
            case BOTH -> "Zimný alebo letný";
        };
    }

    private String prerequisiteText(Recommendation recommendation) {
        PrerequisiteStatus status = recommendation.getPrerequisiteStatus();
        if (status == PrerequisiteStatus.NO_PREREQUISITE) return "Bez prerekvizity";
        String names = recommendation.getSubject().getPrerequisiteCodes().stream()
                .map(this::prerequisiteName)
                .filter(name -> !name.isBlank())
                .reduce((left, right) -> left + ", " + right)
                .orElse("");
        return (status == PrerequisiteStatus.SATISFIED ? "Splnené" : "Nesplnené")
                + (names.isBlank() ? "" : " – " + names);
    }

    private String prerequisiteName(String code) {
        String normalized = normalize(code);
        return catalog.getSubjects().stream()
                .filter(subject -> normalize(subject.getCode()).equals(normalized))
                .map(CurriculumSubject::getName)
                .findFirst()
                .orElse(code);
    }

    private Set<String> codes(List<Recommendation> recommendations) {
        Set<String> result = new HashSet<>();
        recommendations.forEach(item -> result.add(normalize(item.getSubject().getCode())));
        return result;
    }

    private String normalize(String code) {
        return code == null ? "" : code.trim().toLowerCase(Locale.ROOT);
    }

    private String reasonText(RecommendationReason reason) {
        if (reason == RecommendationReason.PRIMARY_DIRECTION && plan != null
                && plan.getPrimaryDirection() != null) {
            return "Odporúčaný pre hlavný smer "
                    + plan.getPrimaryDirection().getSlovakName();
        }
        if (reason == RecommendationReason.SECONDARY_DIRECTION && plan != null
                && plan.getSecondaryDirection() != null) {
            return "Odporúčaný pre vedľajší smer "
                    + plan.getSecondaryDirection().getSlovakName();
        }
        return reason.getDisplayName();
    }

    private void printEnrollmentDiagnostics(LocalCacheService.CacheSnapshot snapshot,
                                            List<Subject> aggregated) {
        if (snapshot == null || snapshot.getEnrollmentData() == null) return;
        snapshot.getEnrollmentData().forEach((year, data) -> {
            long completed = data.getSubjects().stream()
                    .filter(SubjectCompletionService::isCompleted).count();
            System.out.println("[STUDY-PLAN] enrollment=" + year + ", subjects="
                    + data.getSubjects().size() + ", completed=" + completed);
        });
        Set<String> completedCodes = new HashSet<>();
        aggregated.stream().filter(SubjectCompletionService::isCompleted)
                .map(Subject::getAbbreviation)
                .map(SubjectCompletionService::normalizeCode)
                .forEach(completedCodes::add);
        System.out.println("[STUDY-PLAN] aggregated completed codes=" + completedCodes);
        if (catalog != null) {
            catalog.getSubjects().stream()
                    .filter(subject -> completedCodes.contains(
                            SubjectCompletionService.extractSubjectAbbreviation(subject.getCode()))
                            || subject.getEquivalentCodes().stream()
                            .map(SubjectCompletionService::extractSubjectAbbreviation)
                            .anyMatch(completedCodes::contains))
                    .findFirst()
                    .ifPresent(subject -> {
                        String year = snapshot.getEnrollmentData().entrySet().stream()
                                .filter(entry -> entry.getValue().getSubjects().stream()
                                        .filter(SubjectCompletionService::isCompleted)
                                        .map(Subject::getAbbreviation)
                                        .map(SubjectCompletionService::normalizeCode)
                                        .anyMatch(code -> SubjectCompletionService.extractSubjectAbbreviation(
                                                subject.getCode()).equals(code)
                                                || subject.getEquivalentCodes().stream()
                                                .map(SubjectCompletionService::extractSubjectAbbreviation)
                                                .anyMatch(code::equals)))
                                .map(Map.Entry::getKey)
                                .findFirst()
                                .orElse("neznámy");
                        System.out.println("[STUDY-PLAN] completed catalog subject "
                                + subject.getCode() + " found in enrollment=" + year);
                    });
        }
    }

    private void clearTables() {
        mandatoryTable.getItems().clear();
        recommendationTable.getItems().clear();
        plannedTable.getItems().clear();
        creditsLabel.setText("");
    }
}
