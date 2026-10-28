package sk.ukf.aisviewer.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import sk.ukf.aisviewer.model.DegreeLevel;
import sk.ukf.aisviewer.model.SubjectInfo;
import sk.ukf.aisviewer.model.StudyPlanCatalog;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class SubjectInfoService {
    private final Gson gson = new GsonBuilder().create();
    private final List<SubjectInfo> bachelor;
    private final List<SubjectInfo> master;

    public SubjectInfoService() {
        bachelor = load("/subject-info/subject-info-bc.json");
        master = load("/subject-info/subject-info-mgr.json");
    }

    public Optional<SubjectInfo> findByCode(String subjectCode) {
        return find(bachelor, subjectCode).or(() -> find(master, subjectCode));
    }

    public Optional<SubjectInfo> findByCode(StudyPlanCatalog catalog, String subjectCode) {
        if (catalog == null) return Optional.empty();
        return find(catalog.getDegreeLevel() == DegreeLevel.MASTER ? master : bachelor, subjectCode);
    }

    private Optional<SubjectInfo> find(List<SubjectInfo> items, String code) {
        if (code == null || code.isBlank()) return Optional.empty();
        String normalized = code.trim().toUpperCase(Locale.ROOT);
        return items.stream()
                .filter(item -> item.getCode() != null
                        && item.getCode().trim().toUpperCase(Locale.ROOT).equals(normalized))
                .findFirst();
    }

    private List<SubjectInfo> load(String resource) {
        InputStream stream = getClass().getResourceAsStream(resource);
        if (stream == null) throw new IllegalStateException("Detail predmetov nie je dostupný: " + resource);
        try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            SubjectInfo[] result = gson.fromJson(reader, SubjectInfo[].class);
            return result == null ? List.of() : Arrays.asList(result);
        } catch (IOException | RuntimeException e) {
            throw new IllegalStateException("Detail predmetov sa nepodarilo načítať: " + resource, e);
        }
    }
}
