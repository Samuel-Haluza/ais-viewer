package sk.ukf.aisviewer.service;

import org.junit.jupiter.api.Test;
import sk.ukf.aisviewer.model.Subject;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GradeChangesTest {

    private static Subject subject(String code, String grade) {
        return new Subject("Predmet " + code, code, "6K", "ZS", "S", grade, "", "Povinné predmety");
    }

    @Test
    void detectsNewAndChangedGrades() {
        List<Subject> before = List.of(subject("OS", "-"), subject("PS", "E - dostatočne (3)"), subject("UI", "-"));
        List<Subject> after = List.of(subject("OS", "B - veľmi dobre (1,5)"),
                subject("PS", "D - uspokojivo (2,5)"), subject("UI", "-"));

        List<GradeChanges.Change> changes = GradeChanges.detect(before, after);

        assertEquals(2, changes.size());
        assertTrue(changes.get(0).isNew());
        assertEquals("B - veľmi dobre (1,5)", changes.get(0).newGrade());
        assertFalse(changes.get(1).isNew());
        assertEquals("E - dostatočne (3)", changes.get(1).oldGrade());
    }

    @Test
    void firstLoadAndUnchangedGradesGiveNoNotification() {
        List<Subject> subjects = List.of(subject("OS", "B - veľmi dobre (1,5)"));
        assertTrue(GradeChanges.detect(null, subjects).isEmpty());
        assertTrue(GradeChanges.detect(subjects, List.of(subject("OS", " B - veľmi dobre (1,5) "))).isEmpty());
    }

    @Test
    void newSubjectWithGradeCountsAsNew() {
        List<GradeChanges.Change> changes = GradeChanges.detect(List.of(), List.of(subject("NV", "A - výborne (1)")));
        assertEquals(1, changes.size());
        assertTrue(changes.get(0).isNew());
    }
}
