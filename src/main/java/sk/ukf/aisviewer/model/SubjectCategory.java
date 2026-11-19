package sk.ukf.aisviewer.model;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Kategória predmetu tak, ako ju zobrazuje AIS2.
 * Jediné miesto v aplikácii, kde sú tieto texty napísané.
 */
public enum SubjectCategory {
    MANDATORY("Povinné predmety", SubjectType.MANDATORY),
    COMPULSORY_ELECTIVE("Povinne voliteľné predmety", SubjectType.COMPULSORY_ELECTIVE),
    ELECTIVE("Výberové predmety", SubjectType.ELECTIVE);

    private final String displayName;
    private final SubjectType subjectType;

    SubjectCategory(String displayName, SubjectType subjectType) {
        this.displayName = displayName;
        this.subjectType = subjectType;
    }

    /** Text, ktorý sa ukladá do cache a zobrazuje v aplikácii, napr. "Povinné predmety". */
    public String displayName() {
        return displayName;
    }

    /** Zodpovedajúci typ predmetu v študijnom pláne. */
    public SubjectType subjectType() {
        return subjectType;
    }

    /** Predmety tejto kategórie (zachová poradie). */
    public List<Subject> filter(List<Subject> subjects) {
        if (subjects == null) {
            return List.of();
        }
        return subjects.stream().filter(subject -> subject != null && subject.isCategory(this)).toList();
    }

    /** Súčet kreditov predmetov tejto kategórie. */
    public int sumCredits(List<Subject> subjects) {
        return filter(subjects).stream().mapToInt(Subject::getCreditsValue).sum();
    }

    /** Presná zhoda s textom kategórie (bez ohľadu na veľkosť písmen a medzery okolo). */
    public static Optional<SubjectCategory> fromDisplayName(String text) {
        if (text == null) {
            return Optional.empty();
        }
        String value = text.trim();
        for (SubjectCategory category : values()) {
            if (category.displayName.equalsIgnoreCase(value)) {
                return Optional.of(category);
            }
        }
        return Optional.empty();
    }

    /**
     * Rozpozná kategóriu z nadpisu sekcie na stránke AIS (aj bez diakritiky,
     * napr. "Povinne volitelne predmety"). Vráti prázdne, ak to nie je nadpis kategórie.
     */
    public static Optional<SubjectCategory> fromHeading(String heading) {
        if (heading == null) {
            return Optional.empty();
        }
        String text = heading.trim().toLowerCase(Locale.ROOT);
        if (text.contains("povinne voliteľ") || text.contains("povinne volitel")) {
            return Optional.of(COMPULSORY_ELECTIVE);
        }
        if (text.contains("výberové predmety") || text.contains("vyberove predmety")
                || text.contains("voliteľné predmety") || text.contains("volitelne predmety")) {
            return Optional.of(ELECTIVE);
        }
        if (text.contains("povinné") || text.contains("povinne")) {
            return Optional.of(MANDATORY);
        }
        return Optional.empty();
    }

    @Override
    public String toString() {
        return displayName;
    }
}
