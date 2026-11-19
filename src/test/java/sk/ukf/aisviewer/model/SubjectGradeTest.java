package sk.ukf.aisviewer.model;

import org.junit.jupiter.api.Test;
import sk.ukf.aisviewer.service.SubjectCompletionService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Formáty známok sú skopírované zo skutočného výstupu AIS2 UKF. */
class SubjectGradeTest {

    private static Subject withGrade(String grade) {
        return new Subject("Predmet", "KOD", "6K", "ZS", "S", grade, "", "Povinné predmety");
    }

    @Test
    void numericValueOfAllAisGrades() {
        assertEquals(1.0, withGrade("A - výborne (1)").getGradeNumeric());
        assertEquals(1.5, withGrade("B - veľmi dobre (1,5)").getGradeNumeric());
        assertEquals(2.0, withGrade("C - dobre (2)").getGradeNumeric());
        assertEquals(2.5, withGrade("D - uspokojivo (2,5)").getGradeNumeric());
        assertEquals(3.0, withGrade("E - dostatočne (3)").getGradeNumeric());
        assertEquals(4.0, withGrade("FX - nedostatočne (4)").getGradeNumeric());
    }

    @Test
    void numericValueFromLetterWhenNumberIsMissing() {
        assertEquals(1.5, withGrade("B").getGradeNumeric());
        assertEquals(4.0, withGrade("FX").getGradeNumeric());
    }

    @Test
    void nonNumericGradesReturnZero() {
        assertEquals(0, withGrade("ABS - absolvoval/a").getGradeNumeric());
        assertEquals(0, withGrade("-").getGradeNumeric());
        assertEquals(0, withGrade("").getGradeNumeric());
        assertEquals(0, withGrade(null).getGradeNumeric());
    }

    @Test
    void passedGradesAreCompleted() {
        assertTrue(SubjectCompletionService.isCompleted(withGrade("A - výborne (1)")));
        assertTrue(SubjectCompletionService.isCompleted(withGrade("B - veľmi dobre (1,5)")));
        assertTrue(SubjectCompletionService.isCompleted(withGrade("D - uspokojivo (2,5)")));
        assertTrue(SubjectCompletionService.isCompleted(withGrade("E - dostatočne (3)")));
        assertTrue(SubjectCompletionService.isCompleted(withGrade("ABS - absolvoval/a")));
    }

    @Test
    void failedOrMissingGradesAreNotCompleted() {
        assertFalse(SubjectCompletionService.isCompleted(withGrade("FX - nedostatočne (4)")));
        assertFalse(SubjectCompletionService.isCompleted(withGrade("FX")));
        assertFalse(SubjectCompletionService.isCompleted(withGrade("NEABS - neabsolvoval/a")));
        assertFalse(SubjectCompletionService.isCompleted(withGrade("neabsolvoval")));
        assertFalse(SubjectCompletionService.isCompleted(withGrade("-")));
        assertFalse(SubjectCompletionService.isCompleted(withGrade(null)));
    }
}
