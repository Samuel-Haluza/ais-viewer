package sk.ukf.aisviewer.service;

import sk.ukf.aisviewer.model.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CurriculumCatalogImportService {
    private static final Pattern VERSION_PATTERN = Pattern.compile("/(\\d+)$");
    private static final Pattern SUBJECT_CODE = Pattern.compile("[A-Z]{1,6}/[A-Za-z0-9]+/\\d+");
    private static final Map<String, Set<StudyDirection>> DIRECTION_NAMES = createDirectionNames();

    public ImportResult importCatalog(Path html, StudyPlanCatalog metadata) {
        Map<String, List<RawSubject>> byName = new LinkedHashMap<>();
        Map<String, RawSubject> byCode = new LinkedHashMap<>();
        List<String> warnings = new ArrayList<>();
        try {
            String source = Files.readString(html, StandardCharsets.UTF_8);
            String category = null;
            Matcher rowMatcher = Pattern.compile("(?is)<tr[^>]*>(.*?)</tr>").matcher(source);
            while (rowMatcher.find()) {
                String row = rowMatcher.group(1);
                String rowText = normalize(stripTags(row));
                Optional<SubjectCategory> heading = SubjectCategory.fromDisplayName(rowText);
                if (heading.isPresent()) {
                    category = heading.get().displayName();
                    continue;
                }
                List<String> cells = cells(row);
                if (cells.size() < 10 || !cells.get(0).matches("^[A-Z]{1,6}/.+/.+")) {
                    continue;
                }
                if (category == null) {
                    warnings.add("Predmet bez kategórie: " + cells.get(0));
                    continue;
                }
                RawSubject raw = parse(cells, category, metadata);
                RawSubject existing = byCode.get(raw.code);
                if (existing == null) {
                    byCode.put(raw.code, raw);
                    byName.computeIfAbsent(raw.name, ignored -> new ArrayList<>()).add(raw);
                } else if (existing.sameCoreProperties(raw)) {
                    existing.semester = mergeSemester(existing.semester, raw.semester);
                } else {
                    warnings.add("Rovnaký kód má rozdielne vlastnosti, automaticky nezlúčený: "
                            + raw.code);
                    byName.computeIfAbsent(raw.name + "|" + raw.code, ignored -> new ArrayList<>())
                            .add(raw);
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("HTML katalóg sa nepodarilo načítať: " + html, e);
        }

        List<CurriculumSubject> subjects = new ArrayList<>();
        for (List<RawSubject> versions : byName.values()) {
            RawSubject current = versions.stream()
                    .max(Comparator.comparingInt(v -> version(v.code))).orElseThrow();
            CurriculumSubject subject = current.toSubject();
            for (RawSubject older : versions) {
                if (!older.code.equals(current.code)) {
                    subject.getEquivalentCodes().add(older.code);
                }
            }
            subject.setDirections(directionsFor(subject.getName(), metadata.getDegreeLevel(), warnings));
            subjects.add(subject);
        }
        subjects.sort(Comparator.comparing(CurriculumSubject::getCode));
        metadata.setSubjects(subjects);
        return new ImportResult(metadata, warnings);
    }

    private RawSubject parse(List<String> cells, String category, StudyPlanCatalog metadata) {
        RawSubject raw = new RawSubject();
        raw.code = cells.get(0);
        raw.name = cells.get(1);
        raw.credits = integer(cells.get(3));
        raw.year = integer(cells.get(6));
        raw.semester = semester(cells.get(7));
        raw.prerequisites = codes(cells.get(9));
        raw.type = type(category);
        raw.degreeLevel = metadata.getDegreeLevel();
        raw.studyForm = metadata.getStudyForm();
        return raw;
    }

    private static List<String> cells(String row) {
        Matcher matcher = Pattern.compile("(?is)<td[^>]*>(.*?)</td>").matcher(row);
        List<String> result = new ArrayList<>();
        while (matcher.find()) {
            result.add(normalize(stripTags(matcher.group(1))));
        }
        return result;
    }

    private static String stripTags(String value) {
        return value.replaceAll("(?is)<[^>]+>", " ");
    }

    private static String normalize(String value) {
        return value == null ? "" : value.replace('\u00a0', ' ')
                .replace("&nbsp;", " ").replace("&amp;", "&")
                .replaceAll("\\s+", " ").trim();
    }

    private static int integer(String value) {
        Matcher matcher = Pattern.compile("\\d+").matcher(value);
        return matcher.find() ? Integer.parseInt(matcher.group()) : 0;
    }

    private static Semester semester(String value) {
        String normalized = normalize(value).toUpperCase(Locale.ROOT);
        if (normalized.contains("Z")) return Semester.WINTER;
        if (normalized.contains("L")) return Semester.SUMMER;
        return Semester.BOTH;
    }

    private static Semester mergeSemester(Semester first, Semester second) {
        if (first == second) return first;
        if ((first == Semester.WINTER && second == Semester.SUMMER)
                || (first == Semester.SUMMER && second == Semester.WINTER)) {
            return Semester.BOTH;
        }
        return first == Semester.BOTH ? first : second;
    }

    private static List<String> codes(String value) {
        List<String> result = new ArrayList<>();
        // Akákoľvek katedra (KI/…, KM/… pre matematiku a pod.), nie len informatika.
        Matcher matcher = SUBJECT_CODE.matcher(value);
        while (matcher.find()) result.add(matcher.group());
        return result;
    }

    private static SubjectType type(String category) {
        return SubjectCategory.fromDisplayName(category)
                .map(SubjectCategory::subjectType)
                .orElse(SubjectType.MANDATORY);
    }

    private static int version(String code) {
        Matcher matcher = VERSION_PATTERN.matcher(code);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;
    }

    public static Set<StudyDirection> directionsFor(String name, DegreeLevel level,
                                                     List<String> warnings) {
        String key = normalizeKey(name);
        Set<StudyDirection> directions = new LinkedHashSet<>();
        for (Map.Entry<String, Set<StudyDirection>> entry : DIRECTION_NAMES.entrySet()) {
            if (entry.getKey().startsWith(level == DegreeLevel.BACHELOR ? "BC|" : "MGR|")
                    && normalizeKey(entry.getKey().substring(entry.getKey().indexOf('|') + 1))
                    .equals(key)) {
                directions.addAll(entry.getValue());
            }
        }
        if (directions.isEmpty() && !name.isBlank()) {
            warnings.add("Nespárovaný názov smeru (" + level + "): " + name);
        }
        return directions;
    }

    private static String normalizeKey(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ").trim();
    }

    private static Map<String, Set<StudyDirection>> createDirectionNames() {
        Map<String, Set<StudyDirection>> map = new LinkedHashMap<>();
        add(map, "BC", StudyDirection.ALGORITHMS, "Programovanie", "Seminár z programovania",
                "Spracovanie údajov v jazyku Python", "Údajové štruktúry a triedy v jazyku Java",
                "Formálne jazyky a automaty", "Výpočtová zložitosť algoritmov", "Návrhové vzory");
        add(map, "MGR", StudyDirection.ALGORITHMS, "Funkcionálne programovanie");
        add(map, "BC", StudyDirection.SOFTWARE_DEVELOPMENT, "Objektové technológie",
                "Úvod do softvérového inžinierstva", "Vývoj mobilných aplikácií",
                "Programovanie aplikácií so senzormi");
        add(map, "MGR", StudyDirection.SOFTWARE_DEVELOPMENT, "Softvérové inžinierstvo",
                "Manažment softvérových projektov", "Vybrané kapitoly z vývoja softvéru II.",
                "Pokročilé testovanie softvéru", "Vývoj aplikácií pre iOS");
        add(map, "BC", StudyDirection.DATA_AND_AI, "Databázové systémy", "Databázové technológie",
                "Počítačová analýza dát", "Umelá inteligencia");
        add(map, "MGR", StudyDirection.DATA_AND_AI, "Úvod do strojového učenia",
                "Úvod do vizualizácie informácií", "Objavovanie znalostí", "Neurónové siete",
                "Technológie spracovania veľkých dát", "Hĺbková analýza dát",
                "Úvod do spracovania prirodzeného jazyka", "Objavovanie znalostí z edukačných dát",
                "Korpusová a počítačová lingvistika");
        add(map, "BC", StudyDirection.WEB_DEVELOPMENT, "Jazyky webu", "Skriptovacie jazyky",
                "Frontendové technológie", "Backendové technológie",
                "Webové aplikácie na platforme Java", "Testovanie softvéru");
        add(map, "BC", StudyDirection.GAME_AND_GRAPHICS, "Počítačová grafika",
                "Aplikovaná počítačová grafika", "Herné vývojové prostredia", "Vývoj 3D aplikácií");
        add(map, "MGR", StudyDirection.GAME_AND_GRAPHICS, "Vybrané kapitoly z vývoja softvéru I.",
                "Virtuálna realita", "Rozšírená realita");
        add(map, "BC", StudyDirection.COMPUTER_SYSTEMS, "Princípy počítačového inžinierstva",
                "Unixové systémy", "Úvod do počítačových systémov", "Počítačové systémy",
                "Operačné systémy", "Počítačové siete", "Koncepty počítačovej bezpečnosti",
                "Aplikácie počítačových sietí");
        add(map, "MGR", StudyDirection.COMPUTER_SYSTEMS, "Informačná bezpečnosť", "Cloudové technológie");
        add(map, "BC", StudyDirection.INTELLIGENT_TECHNOLOGIES, "Modelovanie a simulácia",
                "Programovanie v jazyku C", "Programovanie mikroprocesorových systémov",
                "Programovanie priemyselných riadiacich systémov",
                "Aplikácie priemyselných riadiacich systémov", "Internet vecí");
        add(map, "MGR", StudyDirection.INTELLIGENT_TECHNOLOGIES,
                "Systémy aplikovanej a priemyselnej informatiky", "Inteligentné systémy",
                "Inteligentné systémy informačné technológie v priemysle a praxi",
                "Princípy robotických systémov a priemyselných meraní");
        add(map, "BC", StudyDirection.MATHEMATICAL_FOUNDATIONS, "Kódovanie a zobrazovanie informácií",
                "Matematika pre informatikov 1",
                "Matematické princípy spracovania jazyka a strojového učenia",
                "Matematika pre informatikov 2");
        add(map, "BC", StudyDirection.GENERAL_SKILLS, "Akademické zručnosti", "Akademická angličtina",
                "Odborná prax", "Seminár k bakalárskej práci I.", "Seminár k bakalárskej práci II.",
                "IT projektový manažment");
        add(map, "MGR", StudyDirection.GENERAL_SKILLS, "Dejiny informatiky",
                "Spoločenské, morálne a ekonomické aspekty informatiky",
                "Odborná komunikácia v anglickom jazyku", "Odborná prax",
                "Seminár k diplomovej práci I.", "Seminár k diplomovej práci II.",
                "Metodológia výskumu v informatike");
        return map;
    }

    private static void add(Map<String, Set<StudyDirection>> map, String level, StudyDirection direction,
                            String... names) {
        for (String name : names) {
            map.computeIfAbsent(level + "|" + name, ignored -> new LinkedHashSet<>()).add(direction);
        }
    }

    public record ImportResult(StudyPlanCatalog catalog, List<String> warnings) {}

    private static class RawSubject {
        private String code;
        private String name;
        private int credits;
        private DegreeLevel degreeLevel;
        private StudyForm studyForm;
        private SubjectType type;
        private int year;
        private Semester semester;
        private List<String> prerequisites;

        private CurriculumSubject toSubject() {
            CurriculumSubject subject = new CurriculumSubject(code, name, credits, degreeLevel,
                    studyForm, type, year, semester);
            subject.setPrerequisiteCodes(prerequisites);
            return subject;
        }

        private boolean sameCoreProperties(RawSubject other) {
            return Objects.equals(name, other.name)
                    && credits == other.credits
                    && degreeLevel == other.degreeLevel
                    && studyForm == other.studyForm
                    && type == other.type
                    && year == other.year
                    && Objects.equals(prerequisites, other.prerequisites);
        }
    }
}
