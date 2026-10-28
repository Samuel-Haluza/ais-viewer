package sk.ukf.aisviewer.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import sk.ukf.aisviewer.model.*;

import java.io.IOException;
import java.io.Reader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StudyPlanCatalogService {
    private final Gson gson = new GsonBuilder().create();

    public Optional<StudyPlanCatalog> load(DegreeLevel degreeLevel, StudyForm studyForm) {
        String resource = resourceName(degreeLevel, studyForm);
        InputStream stream = getClass().getResourceAsStream(resource);
        if (stream == null) return Optional.empty();
        try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            return Optional.ofNullable(gson.fromJson(reader, StudyPlanCatalog.class));
        } catch (IOException | RuntimeException e) {
            throw new IllegalStateException("Katalóg sa nepodarilo načítať: " + resource, e);
        }
    }

    public Optional<StudyPlanCatalog> loadForStudent(StudentInfo studentInfo) {
        DegreeLevel degreeLevel = resolveDegreeLevel(studentInfo);
        StudyForm studyForm = resolveStudyForm(studentInfo);
        return load(degreeLevel, studyForm);
    }

    public DegreeLevel resolveDegreeLevel(StudentInfo studentInfo) {
        String value = studentProgramValue(studentInfo);
        if (value.contains("ai22m") || value.contains("magister")
                || value.contains("master") || value.contains("ii. st")) {
            return DegreeLevel.MASTER;
        }
        return DegreeLevel.BACHELOR;
    }

    public StudyForm resolveStudyForm(StudentInfo studentInfo) {
        String value = studentProgramValue(studentInfo);
        return value.contains("extern") ? StudyForm.EXTERNAL : StudyForm.FULL_TIME;
    }

    private String studentProgramValue(StudentInfo studentInfo) {
        if (studentInfo == null) {
            return "";
        }
        if (studentInfo.getStudyProgram() != null && !studentInfo.getStudyProgram().isBlank()) {
            return studentInfo.getStudyProgram().toLowerCase(Locale.ROOT);
        }
        if (studentInfo.getEnrollmentListNames() != null
                && !studentInfo.getEnrollmentListNames().isEmpty()) {
            String latest = studentInfo.getEnrollmentListNames().get(0);
            int latestYear = academicYear(latest);
            for (String name : studentInfo.getEnrollmentListNames()) {
                int year = academicYear(name);
                if (year > latestYear) {
                    latest = name;
                    latestYear = year;
                }
            }
            return latest.toLowerCase(Locale.ROOT);
        }
        return "";
    }

    private int academicYear(String value) {
        if (value == null) {
            return -1;
        }
        Matcher matcher = Pattern.compile("(\\d{4})").matcher(value);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : -1;
    }

    public String resourceName(DegreeLevel degreeLevel, StudyForm studyForm) {
        String prefix = degreeLevel == DegreeLevel.BACHELOR ? "ai22b" : "ai22m";
        String suffix = studyForm == StudyForm.FULL_TIME ? "fulltime" : "external";
        return "/study-plans/" + prefix + "-" + suffix + ".json";
    }

    public Optional<CurriculumSubject> findByCode(StudyPlanCatalog catalog, String code) {
        if (catalog == null || code == null) return Optional.empty();
        return catalog.getSubjects().stream()
                .filter(subject -> SubjectCompletionService.normalizeCode(code)
                        .equals(SubjectCompletionService.normalizeCode(subject.getCode())))
                .findFirst();
    }

    public List<CurriculumSubject> byType(StudyPlanCatalog catalog, SubjectType type) {
        if (catalog == null) return List.of();
        return catalog.getSubjects().stream().filter(subject -> subject.getType() == type).toList();
    }
}
