package sk.ukf.aisviewer.service;

import sk.ukf.aisviewer.AppLog;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import sk.ukf.aisviewer.model.Exam;
import sk.ukf.aisviewer.model.ScheduleEntry;
import sk.ukf.aisviewer.model.StudentInfo;
import sk.ukf.aisviewer.model.Subject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class LocalCacheService {
    private static final Logger LOG = AppLog.get(LocalCacheService.class);


    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path cacheFile;

    public LocalCacheService() {
        this(AppStorage.file("cache.json"));
    }

    /** Pre testy – cache v zadanom súbore. */
    public LocalCacheService(Path cacheFile) {
        this.cacheFile = cacheFile;
    }

    public Optional<CacheSnapshot> load() {
        if (!Files.isRegularFile(cacheFile)) {
            return Optional.empty();
        }

        try {
            String json = Files.readString(cacheFile, StandardCharsets.UTF_8);
            CacheSnapshot snapshot = gson.fromJson(json, CacheSnapshot.class);
            if (snapshot == null || snapshot.enrollmentData == null) {
                return Optional.empty();
            }
            return Optional.of(snapshot);
        } catch (IOException | JsonSyntaxException e) {
            LOG.log(Level.WARNING, "Cache sa nepodarilo načítať: " + e.getMessage(), e);
            return Optional.empty();
        } catch (RuntimeException e) {
            LOG.log(Level.WARNING, "Neočakávaná chyba pri načítaní cache.", e);
            return Optional.empty();
        }
    }

    public void save(CacheSnapshot snapshot) throws IOException {
        AppStorage.writeAtomically(cacheFile, gson.toJson(snapshot));
    }

    public static CacheSnapshot createSnapshot(StudentInfo studentInfo) {
        CacheSnapshot snapshot = new CacheSnapshot();
        snapshot.studentInfo = studentInfo;
        snapshot.updatedAt = LocalDateTime.now().format(TIMESTAMP_FORMAT);
        return snapshot;
    }

    public static class CacheSnapshot {
        private StudentInfo studentInfo;
        private String updatedAt;
        private Map<String, EnrollmentData> enrollmentData = new HashMap<>();

        public StudentInfo getStudentInfo() {
            return studentInfo;
        }

        public void setStudentInfo(StudentInfo studentInfo) {
            this.studentInfo = studentInfo;
        }

        public String getUpdatedAt() {
            return updatedAt;
        }

        public void setUpdatedAt(String updatedAt) {
            this.updatedAt = updatedAt;
        }

        public Map<String, EnrollmentData> getEnrollmentData() {
            return enrollmentData;
        }
    }

    public static class EnrollmentData {
        private List<Subject> subjects = new ArrayList<>();
        private List<Exam> exams = new ArrayList<>();
        private List<ScheduleEntry> scheduleEntries = new ArrayList<>();

        public List<Subject> getSubjects() {
            return subjects;
        }

        public List<Exam> getExams() {
            return exams;
        }

        public List<ScheduleEntry> getScheduleEntries() {
            return scheduleEntries;
        }

        public static EnrollmentData of(List<Subject> subjects, List<Exam> exams,
                                        List<ScheduleEntry> scheduleEntries) {
            EnrollmentData data = new EnrollmentData();
            data.subjects = new ArrayList<>(subjects);
            data.exams = new ArrayList<>(exams);
            data.scheduleEntries = new ArrayList<>(scheduleEntries);
            return data;
        }
    }
}
