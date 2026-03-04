package sk.ukf.aisviewer.model;

/**
 * Represents a single entry in the weekly schedule (timetable).
 */
public class ScheduleEntry {

    private String day;          // "Pondelok", "Utorok", ...
    private int dayIndex;        // 0=Po, 1=Ut, 2=St, 3=Št, 4=Pi
    private String timeFrom;     // "08:00"
    private String timeTo;       // "09:30"
    private String subjectName;
    private String subjectCode;
    private String room;
    private String teacher;
    private String type;         // "P" (prednáška), "C" (cvičenie), "S" (seminár)

    public ScheduleEntry() {}

    public ScheduleEntry(String day, int dayIndex, String timeFrom, String timeTo,
                         String subjectName, String subjectCode, String room,
                         String teacher, String type) {
        this.day = day;
        this.dayIndex = dayIndex;
        this.timeFrom = timeFrom;
        this.timeTo = timeTo;
        this.subjectName = subjectName;
        this.subjectCode = subjectCode;
        this.room = room;
        this.teacher = teacher;
        this.type = type;
    }

    public String getDay() { return day; }
    public void setDay(String day) { this.day = day; }

    public int getDayIndex() { return dayIndex; }
    public void setDayIndex(int dayIndex) { this.dayIndex = dayIndex; }

    public String getTimeFrom() { return timeFrom; }
    public void setTimeFrom(String timeFrom) { this.timeFrom = timeFrom; }

    public String getTimeTo() { return timeTo; }
    public void setTimeTo(String timeTo) { this.timeTo = timeTo; }

    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }

    public String getSubjectCode() { return subjectCode; }
    public void setSubjectCode(String subjectCode) { this.subjectCode = subjectCode; }

    public String getRoom() { return room; }
    public void setRoom(String room) { this.room = room; }

    public String getTeacher() { return teacher; }
    public void setTeacher(String teacher) { this.teacher = teacher; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    /**
     * Returns the start hour (e.g. "08:00" -> 8).
     */
    public int getStartHour() {
        try {
            return Integer.parseInt(timeFrom.split(":")[0]);
        } catch (Exception e) {
            return 8;
        }
    }

    /**
     * Returns the start minute.
     */
    public int getStartMinute() {
        try {
            return Integer.parseInt(timeFrom.split(":")[1]);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Returns the end hour.
     */
    public int getEndHour() {
        try {
            return Integer.parseInt(timeTo.split(":")[0]);
        } catch (Exception e) {
            return getStartHour() + 1;
        }
    }

    /**
     * Returns the end minute.
     */
    public int getEndMinute() {
        try {
            return Integer.parseInt(timeTo.split(":")[1]);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Duration in minutes.
     */
    public int getDurationMinutes() {
        return (getEndHour() * 60 + getEndMinute()) - (getStartHour() * 60 + getStartMinute());
    }

    @Override
    public String toString() {
        return day + " " + timeFrom + "-" + timeTo + " " + subjectName + " (" + room + ")";
    }
}

