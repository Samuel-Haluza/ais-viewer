package sk.ukf.aisviewer.model;

public class PlannedSubject {
    private String subjectCode;
    private int plannedYear;
    private Semester plannedSemester;

    public PlannedSubject() {
    }

    public PlannedSubject(String subjectCode, int plannedYear, Semester plannedSemester) {
        this.subjectCode = subjectCode;
        this.plannedYear = plannedYear;
        this.plannedSemester = plannedSemester;
    }

    public String getSubjectCode() { return subjectCode; }
    public void setSubjectCode(String subjectCode) { this.subjectCode = subjectCode; }
    public int getPlannedYear() { return plannedYear; }
    public void setPlannedYear(int plannedYear) { this.plannedYear = plannedYear; }
    public Semester getPlannedSemester() { return plannedSemester; }
    public void setPlannedSemester(Semester plannedSemester) { this.plannedSemester = plannedSemester; }
}
