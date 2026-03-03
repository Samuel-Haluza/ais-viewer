package sk.ukf.aisviewer.model;

public class Exam {

    private String subjectName;
    private String subjectAbbreviation;
    private String date;
    private String time;
    private String room;
    private String teacher;
    private String capacity;
    private String enrolled;
    private String status;
    private String termId;

    public Exam() {}

    public Exam(String subjectName, String subjectAbbreviation, String date,
                String time, String room, String teacher,
                String capacity, String enrolled, String status) {
        this.subjectName = subjectName;
        this.subjectAbbreviation = subjectAbbreviation;
        this.date = date;
        this.time = time;
        this.room = room;
        this.teacher = teacher;
        this.capacity = capacity;
        this.enrolled = enrolled;
        this.status = status;
    }

    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }

    public String getSubjectAbbreviation() { return subjectAbbreviation; }
    public void setSubjectAbbreviation(String subjectAbbreviation) { this.subjectAbbreviation = subjectAbbreviation; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public String getRoom() { return room; }
    public void setRoom(String room) { this.room = room; }

    public String getTeacher() { return teacher; }
    public void setTeacher(String teacher) { this.teacher = teacher; }

    public String getCapacity() { return capacity; }
    public void setCapacity(String capacity) { this.capacity = capacity; }

    public String getEnrolled() { return enrolled; }
    public void setEnrolled(String enrolled) { this.enrolled = enrolled; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getTermId() { return termId; }
    public void setTermId(String termId) { this.termId = termId; }

    @Override
    public String toString() {
        return subjectName + " – " + date + " " + time;
    }
}
