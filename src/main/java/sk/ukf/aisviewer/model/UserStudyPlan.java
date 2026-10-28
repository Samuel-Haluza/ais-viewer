package sk.ukf.aisviewer.model;

import java.util.ArrayList;
import java.util.List;

public class UserStudyPlan {
    private StudyDirection primaryDirection;
    private StudyDirection secondaryDirection;
    private List<PlannedSubject> plannedSubjects = new ArrayList<>();

    public StudyDirection getPrimaryDirection() { return primaryDirection; }
    public void setPrimaryDirection(StudyDirection primaryDirection) { this.primaryDirection = primaryDirection; }
    public StudyDirection getSecondaryDirection() { return secondaryDirection; }
    public void setSecondaryDirection(StudyDirection secondaryDirection) { this.secondaryDirection = secondaryDirection; }
    public List<PlannedSubject> getPlannedSubjects() { return plannedSubjects; }
    public void setPlannedSubjects(List<PlannedSubject> plannedSubjects) {
        this.plannedSubjects = plannedSubjects == null ? new ArrayList<>() : plannedSubjects;
    }
}
