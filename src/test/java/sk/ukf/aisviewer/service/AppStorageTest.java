package sk.ukf.aisviewer.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import sk.ukf.aisviewer.model.StudentInfo;
import sk.ukf.aisviewer.model.Subject;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AppStorageTest {

    @Test
    void writeAtomicallyCreatesFolderReplacesContentAndLeavesNoTempFile(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("nested").resolve("data.json");
        AppStorage.writeAtomically(file, "prvý");
        AppStorage.writeAtomically(file, "druhý");

        assertEquals("druhý", Files.readString(file, StandardCharsets.UTF_8));
        assertFalse(Files.exists(dir.resolve("nested").resolve("data.json.tmp")));
    }

    @Test
    void cacheRoundTripThroughSharedStorage(@TempDir Path dir) throws Exception {
        LocalCacheService cache = new LocalCacheService(dir.resolve("cache.json"));
        StudentInfo student = new StudentInfo();
        student.setFullName("Test Študent");
        LocalCacheService.CacheSnapshot snapshot = LocalCacheService.createSnapshot(student);
        snapshot.getEnrollmentData().put("1", LocalCacheService.EnrollmentData.of(
                List.of(new Subject("Programovanie", "PROG", "6K", "ZS", "S", "C - dobre (2)", "", "Povinné predmety")),
                List.of(), List.of()));
        cache.save(snapshot);

        LocalCacheService.CacheSnapshot loaded = cache.load().orElseThrow();
        assertEquals("Test Študent", loaded.getStudentInfo().getFullName());
        assertEquals("C - dobre (2)", loaded.getEnrollmentData().get("1").getSubjects().get(0).getGrade());
    }

    @Test
    void identityKeyMatchesSameSubjectAcrossFormats() {
        Subject shortCode = new Subject("Programovanie", "PROG", "6K", "ZS", "S", "-", "", "");
        Subject fullCode = new Subject("Programovanie", "KI/PROG/22", "6K", "ZS", "S", "-", "", "");
        Subject noCode = new Subject("Programovanie", null, "6K", "ZS", "S", "-", "", "");

        assertEquals("prog", SubjectCompletionService.identityKey(shortCode));
        assertEquals("prog", SubjectCompletionService.identityKey(fullCode));
        assertEquals("programovanie", SubjectCompletionService.identityKey(noCode));
    }
}
