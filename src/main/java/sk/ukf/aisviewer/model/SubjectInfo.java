package sk.ukf.aisviewer.model;

import java.util.ArrayList;
import java.util.List;

public class SubjectInfo {
    private String code;
    private String name;
    private String teachingForm;
    private String teachingExtent;
    private String studyMethod;
    private String completionRequirements;
    private String learningOutcomes;
    private String syllabus;
    private List<String> recommendedLiterature = new ArrayList<>();
    private String language;
    private List<String> teachers = new ArrayList<>();
    private String lastChangeDate;

    public String getCode() { return code; }
    public String getName() { return name; }
    public String getTeachingForm() { return teachingForm; }
    public String getTeachingExtent() { return teachingExtent; }
    public String getStudyMethod() { return studyMethod; }
    public String getCompletionRequirements() { return completionRequirements; }
    public String getLearningOutcomes() { return learningOutcomes; }
    public String getSyllabus() { return syllabus; }
    public List<String> getRecommendedLiterature() { return recommendedLiterature; }
    public String getLanguage() { return language; }
    public List<String> getTeachers() { return teachers; }
    public String getLastChangeDate() { return lastChangeDate; }
}
