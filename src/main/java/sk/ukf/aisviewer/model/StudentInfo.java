package sk.ukf.aisviewer.model;

import java.util.ArrayList;
import java.util.List;

public class StudentInfo {

    private String fullName;
    private String ido;
    private String studyProgram;
    private String academicYear;
    private String enrollmentListId;
    private List<String> enrollmentListIds;
    private List<String> enrollmentListNames;
    private String thesisTitle;

    public StudentInfo() {
        this.enrollmentListIds = new ArrayList<>();
        this.enrollmentListNames = new ArrayList<>();
    }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getIdo() { return ido; }
    public void setIdo(String ido) { this.ido = ido; }

    public String getStudyProgram() { return studyProgram; }
    public void setStudyProgram(String studyProgram) { this.studyProgram = studyProgram; }

    public String getAcademicYear() { return academicYear; }
    public void setAcademicYear(String academicYear) { this.academicYear = academicYear; }

    public String getEnrollmentListId() { return enrollmentListId; }
    public void setEnrollmentListId(String enrollmentListId) { this.enrollmentListId = enrollmentListId; }

    public List<String> getEnrollmentListIds() { return enrollmentListIds; }
    public void setEnrollmentListIds(List<String> enrollmentListIds) { this.enrollmentListIds = enrollmentListIds; }

    public List<String> getEnrollmentListNames() { return enrollmentListNames; }
    public void setEnrollmentListNames(List<String> enrollmentListNames) { this.enrollmentListNames = enrollmentListNames; }

    public String getThesisTitle() { return thesisTitle; }
    public void setThesisTitle(String thesisTitle) { this.thesisTitle = thesisTitle; }

    @Override
    public String toString() {
        return fullName + " | " + studyProgram + " (" + academicYear + ")";
    }
}
