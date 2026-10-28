package sk.ukf.aisviewer.model;

public enum RecommendationReason {
    MANDATORY("Povinný predmet"),
    BOTH_DIRECTIONS("Odporúčaný pre oba zvolené smery"),
    PRIMARY_DIRECTION("Odporúčaný pre hlavný smer"),
    SECONDARY_DIRECTION("Odporúčaný pre vedľajší smer"),
    OTHER("Ostatný dostupný predmet");

    private final String displayName;

    RecommendationReason(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
