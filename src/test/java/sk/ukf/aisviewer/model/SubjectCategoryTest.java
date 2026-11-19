package sk.ukf.aisviewer.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubjectCategoryTest {

    private static Subject subject(String category, String credits) {
        return new Subject("P", "P", credits, "ZS", "S", "A - výborne (1)", "", category);
    }

    @Test
    void displayNamesMatchTextsStoredInCache() {
        // Tieto texty sú uložené v cache.json – nesmú sa zmeniť, inak by stará cache prestala fungovať.
        assertEquals("Povinné predmety", SubjectCategory.MANDATORY.displayName());
        assertEquals("Povinne voliteľné predmety", SubjectCategory.COMPULSORY_ELECTIVE.displayName());
        assertEquals("Výberové predmety", SubjectCategory.ELECTIVE.displayName());
    }

    @Test
    void fromDisplayNameIsExactButIgnoresCaseAndSpaces() {
        assertEquals(Optional.of(SubjectCategory.MANDATORY), SubjectCategory.fromDisplayName(" povinné predmety "));
        assertEquals(Optional.empty(), SubjectCategory.fromDisplayName("Iné"));
        assertEquals(Optional.empty(), SubjectCategory.fromDisplayName(null));
    }

    @Test
    void fromHeadingRecognisesAisHeadingsWithAndWithoutDiacritics() {
        assertEquals(Optional.of(SubjectCategory.COMPULSORY_ELECTIVE),
                SubjectCategory.fromHeading("Povinne voliteľné predmety (min. 66 kreditov)"));
        assertEquals(Optional.of(SubjectCategory.COMPULSORY_ELECTIVE),
                SubjectCategory.fromHeading("POVINNE VOLITELNE PREDMETY"));
        assertEquals(Optional.of(SubjectCategory.ELECTIVE), SubjectCategory.fromHeading("Výberové predmety"));
        assertEquals(Optional.of(SubjectCategory.MANDATORY), SubjectCategory.fromHeading("Povinné predmety"));
        assertEquals(Optional.empty(), SubjectCategory.fromHeading("Rozvrh"));
    }

    @Test
    void filterAndSumCreditsByCategory() {
        List<Subject> subjects = List.of(
                subject("Povinné predmety", "6K"),
                subject("Povinné predmety", "4K"),
                subject("Povinne voliteľné predmety", "3K"),
                subject(null, "5K"));

        assertEquals(10, SubjectCategory.MANDATORY.sumCredits(subjects));
        assertEquals(3, SubjectCategory.COMPULSORY_ELECTIVE.sumCredits(subjects));
        assertEquals(0, SubjectCategory.ELECTIVE.sumCredits(subjects));
        assertTrue(subjects.get(0).isCategory(SubjectCategory.MANDATORY));
        assertFalse(subjects.get(3).isCategory(SubjectCategory.MANDATORY));
    }
}
