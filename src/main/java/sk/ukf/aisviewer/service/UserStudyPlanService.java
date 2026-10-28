package sk.ukf.aisviewer.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import sk.ukf.aisviewer.model.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Objects;

public class UserStudyPlanService {
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path planFile;

    public UserStudyPlanService() {
        String appData = System.getenv("APPDATA");
        Path base = appData != null && !appData.isBlank()
                ? Path.of(appData, "AIS Viewer") : Path.of(System.getProperty("user.home"), ".ais-viewer");
        planFile = base.resolve("study-plan.json");
    }

    public UserStudyPlanService(Path planFile) {
        this.planFile = Objects.requireNonNull(planFile, "planFile");
    }

    public UserStudyPlan load() {
        if (!Files.isRegularFile(planFile)) return new UserStudyPlan();
        try {
            UserStudyPlan plan = gson.fromJson(
                    Files.readString(planFile, StandardCharsets.UTF_8), UserStudyPlan.class);
            return plan == null ? new UserStudyPlan() : plan;
        } catch (IOException | JsonSyntaxException e) {
            throw new IllegalStateException("Používateľský plán sa nepodarilo načítať.", e);
        }
    }

    public void save(UserStudyPlan plan) throws IOException {
        Objects.requireNonNull(plan, "plan");
        validateDirections(plan.getPrimaryDirection(), plan.getSecondaryDirection());
        Files.createDirectories(planFile.getParent());
        Path temporary = planFile.resolveSibling("study-plan.json.tmp");
        Files.writeString(temporary, gson.toJson(plan), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        try {
            Files.move(temporary, planFile, StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temporary, planFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public void validateDirections(StudyDirection primary, StudyDirection secondary) {
        if (primary != null && primary == secondary) {
            throw new IllegalArgumentException("Hlavný a vedľajší smer nesmú byť rovnaké.");
        }
    }

    public void setDirections(UserStudyPlan plan, StudyDirection primary,
                              StudyDirection secondary) {
        validateDirections(primary, secondary);
        plan.setPrimaryDirection(primary);
        plan.setSecondaryDirection(secondary);
    }

    public void addSubject(UserStudyPlan plan, String code, int year, Semester semester) {
        if (plan.getPlannedSubjects().stream()
                .anyMatch(item -> item.getSubjectCode().equalsIgnoreCase(code))) {
            return;
        }
        plan.getPlannedSubjects().add(new PlannedSubject(code, year, semester));
    }

    public void addSubject(UserStudyPlan plan, CurriculumSubject subject) {
        Objects.requireNonNull(subject, "subject");
        addSubject(plan, subject.getCode(), subject.getRecommendedYear(),
                subject.getSemester() == Semester.SUMMER ? Semester.SUMMER : Semester.WINTER);
    }

    public void removeSubject(UserStudyPlan plan, String code) {
        plan.getPlannedSubjects().removeIf(item -> item.getSubjectCode().equalsIgnoreCase(code));
    }
}
