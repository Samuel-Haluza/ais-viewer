package sk.ukf.aisviewer.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import sk.ukf.aisviewer.model.Exam;
import sk.ukf.aisviewer.model.ScheduleEntry;
import sk.ukf.aisviewer.model.StudentInfo;
import sk.ukf.aisviewer.model.Subject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class LocalCacheService {

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path cacheFile;

    public LocalCacheService() {
        String appData = System.getenv("APPDATA");
        Path baseDirectory = appData != null && !appData.isBlank()
                ? Path.of(appData)
                : Path.of(System.getProperty("user.home"), ".ais-viewer");
        Path applicationDirectory = appData != null && !appData.isBlank()
                ? baseDirectory.resolve("AIS Viewer")
                : baseDirectory;
        cacheFile = applicationDirectory.resolve("cache.json");
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
            System.out.println("[CACHE] Cache sa nepodarilo načítať: " + e.getMessage());
            return Optional.empty();
        }
    }

    public void save(CacheSnapshot snapshot) throws IOException {
        Files.createDirectories(cacheFile.getParent());
        Path temporaryFile = cacheFile.resolveSibling("cache.json.tmp");
        String json = gson.toJson(snapshot);
        Files.writeString(temporaryFile, json, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        try {
            Files.move(temporaryFile, cacheFile,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temporaryFile, cacheFile, StandardCopyOption.REPLACE_EXISTING);
        }
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
