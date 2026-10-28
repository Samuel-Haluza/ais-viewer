package sk.ukf.aisviewer.model;

public class Recommendation {
    private CurriculumSubject subject;
    private RecommendationReason reason;
    private PrerequisiteStatus prerequisiteStatus;

    public Recommendation(CurriculumSubject subject, RecommendationReason reason,
                           PrerequisiteStatus prerequisiteStatus) {
        this.subject = subject;
        this.reason = reason;
        this.prerequisiteStatus = prerequisiteStatus;
    }

    public CurriculumSubject getSubject() { return subject; }
    public RecommendationReason getReason() { return reason; }
    public PrerequisiteStatus getPrerequisiteStatus() { return prerequisiteStatus; }
    public boolean isPrerequisitesMet() {
        return prerequisiteStatus == PrerequisiteStatus.SATISFIED;
    }
}
