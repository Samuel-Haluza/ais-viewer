package sk.ukf.aisviewer.model;

public enum StudyDirection {
    ALGORITHMS("Algoritmizácia"),
    SOFTWARE_DEVELOPMENT("Vývoj softvéru"),
    DATA_AND_AI("Dáta a umelá inteligencia"),
    WEB_DEVELOPMENT("Tvorba webu"),
    GAME_AND_GRAPHICS("Vývoj hier a grafika"),
    COMPUTER_SYSTEMS("Počítačové systémy"),
    INTELLIGENT_TECHNOLOGIES("Inteligentné technológie"),
    MATHEMATICAL_FOUNDATIONS("Matematické princípy informatiky"),
    GENERAL_SKILLS("Všeobecné zručnosti");

    private final String slovakName;

    StudyDirection(String slovakName) {
        this.slovakName = slovakName;
    }

    public String getSlovakName() {
        return slovakName;
    }

    @Override
    public String toString() {
        return slovakName;
    }
}
