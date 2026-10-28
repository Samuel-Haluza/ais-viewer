package sk.ukf.aisviewer.model;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class CurriculumSubject {
    private String code;
    private String name;
    private int credits;
    private DegreeLevel degreeLevel;
    private StudyForm studyForm;
    private SubjectType type;
    private int recommendedYear;
    private Semester semester;
    private List<String> prerequisiteCodes = new ArrayList<>();
    private List<String> equivalentCodes = new ArrayList<>();
    private Set<StudyDirection> directions = new LinkedHashSet<>();

    public CurriculumSubject() {
    }

    public CurriculumSubject(String code, String name, int credits,
                             DegreeLevel degreeLevel, StudyForm studyForm,
                             SubjectType type, int recommendedYear, Semester semester) {
        this.code = code;
        this.name = name;
        this.credits = credits;
        this.degreeLevel = degreeLevel;
        this.studyForm = studyForm;
        this.type = type;
        this.recommendedYear = recommendedYear;
        this.semester = semester;
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getCredits() { return credits; }
    public void setCredits(int credits) { this.credits = credits; }
    public DegreeLevel getDegreeLevel() { return degreeLevel; }
    public void setDegreeLevel(DegreeLevel degreeLevel) { this.degreeLevel = degreeLevel; }
    public StudyForm getStudyForm() { return studyForm; }
    public void setStudyForm(StudyForm studyForm) { this.studyForm = studyForm; }
    public SubjectType getType() { return type; }
    public void setType(SubjectType type) { this.type = type; }
    public int getRecommendedYear() { return recommendedYear; }
    public void setRecommendedYear(int recommendedYear) { this.recommendedYear = recommendedYear; }
    public Semester getSemester() { return semester; }
    public void setSemester(Semester semester) { this.semester = semester; }
    public List<String> getPrerequisiteCodes() { return prerequisiteCodes; }
    public void setPrerequisiteCodes(List<String> prerequisiteCodes) {
        this.prerequisiteCodes = prerequisiteCodes == null ? new ArrayList<>() : prerequisiteCodes;
    }
    public List<String> getEquivalentCodes() { return equivalentCodes; }
    public void setEquivalentCodes(List<String> equivalentCodes) {
        this.equivalentCodes = equivalentCodes == null ? new ArrayList<>() : equivalentCodes;
    }
    public Set<StudyDirection> getDirections() { return directions; }
    public void setDirections(Set<StudyDirection> directions) {
        this.directions = directions == null ? new LinkedHashSet<>() : directions;
    }
}
