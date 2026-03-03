package sk.ukf.aisviewer.model;

public class Subject {

    private String name;
    private String abbreviation;
    private String credits;
    private String semester;
    private String gradeType;
    private String grade;
    private String teacher;
    private String category;

    public Subject() {}

    public Subject(String name, String abbreviation, String credits,
                   String semester, String gradeType, String grade,
                   String teacher, String category) {
        this.name = name;
        this.abbreviation = abbreviation;
        this.credits = credits;
        this.semester = semester;
        this.gradeType = gradeType;
        this.grade = grade;
        this.teacher = teacher;
        this.category = category;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAbbreviation() { return abbreviation; }
    public void setAbbreviation(String abbreviation) { this.abbreviation = abbreviation; }

    public String getCredits() { return credits; }
    public void setCredits(String credits) { this.credits = credits; }

    public String getSemester() { return semester; }
    public void setSemester(String semester) { this.semester = semester; }

    public String getGradeType() { return gradeType; }
    public void setGradeType(String gradeType) { this.gradeType = gradeType; }

    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }

    public String getTeacher() { return teacher; }
    public void setTeacher(String teacher) { this.teacher = teacher; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public int getCreditsValue() {
        if (credits == null) return 0;
        String c = credits.replace("K", "").replace("k", "").trim();
        try {
            return Integer.parseInt(c);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public double getGradeNumeric() {
        if (grade == null || grade.equals("-")) return 0;
        if (grade.contains("(1)")) return 1.0;
        if (grade.contains("(2)")) return 2.0;
        if (grade.contains("(3)")) return 3.0;
        if (grade.contains("(4)")) return 4.0;
        return 0;
    }

    @Override
    public String toString() {
        return name + " (" + abbreviation + ")";
    }
}
