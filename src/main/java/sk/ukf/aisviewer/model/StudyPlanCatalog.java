package sk.ukf.aisviewer.model;

import java.util.ArrayList;
import java.util.List;

public class StudyPlanCatalog {
    private String programCode;
    private DegreeLevel degreeLevel;
    private StudyForm studyForm;
    private int requiredTotalCredits;
    private int requiredStudyCredits;
    private int stateExamCredits;
    private int requiredMandatoryCredits;
    private int requiredCompulsoryElectiveCredits;
    private List<CurriculumSubject> subjects = new ArrayList<>();

    public String getProgramCode() { return programCode; }
    public void setProgramCode(String programCode) { this.programCode = programCode; }
    public DegreeLevel getDegreeLevel() { return degreeLevel; }
    public void setDegreeLevel(DegreeLevel degreeLevel) { this.degreeLevel = degreeLevel; }
    public StudyForm getStudyForm() { return studyForm; }
    public void setStudyForm(StudyForm studyForm) { this.studyForm = studyForm; }
    public int getRequiredTotalCredits() { return requiredTotalCredits; }
    public void setRequiredTotalCredits(int requiredTotalCredits) { this.requiredTotalCredits = requiredTotalCredits; }
    public int getRequiredStudyCredits() { return requiredStudyCredits; }
    public void setRequiredStudyCredits(int requiredStudyCredits) { this.requiredStudyCredits = requiredStudyCredits; }
    public int getStateExamCredits() { return stateExamCredits; }
    public void setStateExamCredits(int stateExamCredits) { this.stateExamCredits = stateExamCredits; }
    public int getRequiredMandatoryCredits() { return requiredMandatoryCredits; }
    public void setRequiredMandatoryCredits(int requiredMandatoryCredits) { this.requiredMandatoryCredits = requiredMandatoryCredits; }
    public int getRequiredCompulsoryElectiveCredits() { return requiredCompulsoryElectiveCredits; }
    public void setRequiredCompulsoryElectiveCredits(int requiredCompulsoryElectiveCredits) { this.requiredCompulsoryElectiveCredits = requiredCompulsoryElectiveCredits; }
    public List<CurriculumSubject> getSubjects() { return subjects; }
    public void setSubjects(List<CurriculumSubject> subjects) {
        this.subjects = subjects == null ? new ArrayList<>() : subjects;
    }
}
