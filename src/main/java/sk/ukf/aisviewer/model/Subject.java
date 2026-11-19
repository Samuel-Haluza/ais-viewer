package sk.ukf.aisviewer.model;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Subject {

    /** Číslo v zátvorke, napr. "B - veľmi dobre (1,5)" → 1,5. */
    private static final Pattern GRADE_NUMBER = Pattern.compile("\\((\\d+(?:[.,]\\d+)?)\\)");
    /** Písmeno na začiatku, napr. "FX - nedostatočne" → FX. "ABS" sem nepatrí. */
    private static final Pattern GRADE_LETTER = Pattern.compile("^(FX|[A-E])(?![A-Z])");

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

    /** Kategória ako enum (text v poli {@code category} ostáva kvôli kompatibilite s cache). */
    public boolean isCategory(SubjectCategory expected) {
        return SubjectCategory.fromDisplayName(category).orElse(null) == expected;
    }

    public int getCreditsValue() {
        if (credits == null) return 0;
        String c = credits.replace("K", "").replace("k", "").trim();
        try {
            return Integer.parseInt(c);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Číselná hodnota známky podľa stupnice UKF:
     * A=1, B=1,5, C=2, D=2,5, E=3, FX=4. Pre "ABS", "-" a neznáme hodnoty vráti 0.
     */
    public double getGradeNumeric() {
        if (grade == null) return 0;
        String value = grade.trim().toUpperCase(Locale.ROOT);
        if (value.isEmpty() || value.equals("-")) return 0;

        Matcher number = GRADE_NUMBER.matcher(value);
        if (number.find()) {
            return Double.parseDouble(number.group(1).replace(',', '.'));
        }

        Matcher letter = GRADE_LETTER.matcher(value);
        if (letter.find()) {
            return switch (letter.group(1)) {
                case "A" -> 1.0;
                case "B" -> 1.5;
                case "C" -> 2.0;
                case "D" -> 2.5;
                case "E" -> 3.0;
                default -> 4.0; // FX
            };
        }
        return 0;
    }

    /** FX = neúspešne ukončený predmet. */
    public boolean isFailingGrade() {
        if (grade == null) return false;
        String value = grade.trim().toUpperCase(Locale.ROOT);
        return value.startsWith("FX") || getGradeNumeric() >= 4.0;
    }

    @Override
    public String toString() {
        return name + " (" + abbreviation + ")";
    }
}
