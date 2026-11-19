package sk.ukf.aisviewer.controller;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Texty, ktoré UI ukazuje používateľovi (bez spúšťania JavaFX). */
class UiTextTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 19);

    @Test
    void examCountdownUsesSlovakPlurals() {
        assertEquals("dnes", ExamsController.countdownText(TODAY, TODAY));
        assertEquals("zajtra", ExamsController.countdownText(TODAY.plusDays(1), TODAY));
        assertEquals("o 3 dni", ExamsController.countdownText(TODAY.plusDays(3), TODAY));
        assertEquals("o 57 dní", ExamsController.countdownText(LocalDate.of(2027, 1, 15), TODAY));
        assertEquals("včera", ExamsController.countdownText(TODAY.minusDays(1), TODAY));
        assertEquals("pred 10 dňami", ExamsController.countdownText(TODAY.minusDays(10), TODAY));
    }

    @Test
    void parsesAisExamDate() {
        assertEquals(LocalDate.of(2027, 1, 15), ExamsController.parseDate("15.01.2027"));
        assertNull(ExamsController.parseDate("zajtra"));
        assertNull(ExamsController.parseDate(null));
    }

    @Test
    void gradeTypeAndStyleNames() {
        assertEquals("Skúška", SubjectsController.gradeTypeName("S"));
        assertEquals("Priebežné", SubjectsController.gradeTypeName("PH"));
        assertEquals("Absolvovanie", SubjectsController.gradeTypeName("A"));
        assertEquals("grade-b", TableUi.gradeStyleClass("B - veľmi dobre (1,5)"));
        assertEquals("grade-abs", TableUi.gradeStyleClass("ABS - absolvoval/a"));
        assertEquals("grade-fx", TableUi.gradeStyleClass("FX - nedostatočne (4)"));
    }

    @Test
    void zeroYearMeansEndOfStudy() {
        assertEquals("Záver štúdia", StudyPlanController.yearText(0));
        assertEquals("3. ročník", StudyPlanController.yearText(3));
    }
}
