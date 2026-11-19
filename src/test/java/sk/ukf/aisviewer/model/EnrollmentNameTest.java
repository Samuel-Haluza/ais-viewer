package sk.ukf.aisviewer.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** Názvy zápisných listov sú zo skutočného AIS2 UKF. */
class EnrollmentNameTest {

    @Test
    void readsPartsOfAisEnrollmentName() {
        String name = "2025/2026 (2AI22b)";
        assertEquals(2025, EnrollmentName.startYear(name));
        assertEquals("2025/2026", EnrollmentName.academicYear(name));
        assertEquals("2AI22b", EnrollmentName.programCode(name));
        assertEquals(2, EnrollmentName.studyYear(EnrollmentName.programCode(name)));
        assertEquals('b', EnrollmentName.degreeLetter(name));
        assertEquals('m', EnrollmentName.degreeLetter("2026/2027 (1AI22m)"));
    }

    @Test
    void handlesMissingParts() {
        assertEquals(-1, EnrollmentName.startYear(null));
        assertEquals("", EnrollmentName.programCode("Aktuálny zápisný list"));
        assertEquals("Aktuálny zápisný list", EnrollmentName.academicYear("Aktuálny zápisný list"));
        assertEquals(-1, EnrollmentName.studyYear("AI22b"));
        assertEquals(0, EnrollmentName.degreeLetter("bez kódu"));
    }

    @Test
    void latestPicksHighestYearRegardlessOfOrder() {
        assertEquals("2026/2027 (3AI22b)", EnrollmentName.latest(List.of(
                "2024/2025 (1AI22b)", "2026/2027 (3AI22b)", "2025/2026 (2AI22b)")));
        assertNull(EnrollmentName.latest(List.of()));
    }
}
