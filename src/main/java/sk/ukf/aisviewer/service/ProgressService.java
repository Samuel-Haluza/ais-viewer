package sk.ukf.aisviewer.service;

import sk.ukf.aisviewer.model.CurriculumSubject;
import sk.ukf.aisviewer.model.Exam;
import sk.ukf.aisviewer.model.StudyDirection;
import sk.ukf.aisviewer.model.StudyPlanCatalog;
import sk.ukf.aisviewer.model.Subject;
import sk.ukf.aisviewer.model.SubjectCategory;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Záložka „Môj pokrok“: XP, hodnosti, achievementy a časová os semestrov.
 * Čistá logika bez JavaFX – vstupom sú zápisné listy (od najstaršieho) s predmetmi a skúškami.
 */
public final class ProgressService {

    /** XP za 1 kredit podľa známky: A 30, B 25, C 20, D 15, E 10, ABS 15. */
    private static final int XP_PER_CREDIT_UNIT = 10;
    private static final String[] RANK_NAMES = {"Nováčik", "Učeň", "Zvedavec", "Objaviteľ", "Bádateľ",
            "Skúsený", "Pokročilý", "Expert", "Majster", "Veterán", "Legenda", "Absolvent"};
    /** Hranice hodností ako podiel z „ideálneho“ XP pri ukončení (požadované kredity × priemerný násobok 2). */
    private static final double[] RANK_FACTORS = {0, .05, .12, .2, .3, .4, .5, .6, .7, .8, .9, 1.0};
    private static final DateTimeFormatter AIS_DATE = DateTimeFormatter.ofPattern("d.M.uuuu");

    public enum Category {
        STUDY("Štúdium"), GRADES("Známky"), EXAMS("Skúšky"), DIRECTIONS("Smery");

        private final String label;

        Category(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    /** Jeden zápisný list: názov z AIS, predmety a termíny skúšok. */
    public record EnrollmentInput(String name, List<Subject> subjects, List<Exam> exams) {
    }

    public record Rank(int index, String name, int minXp) {
        public int number() {
            return index + 1;
        }
    }

    /**
     * @param tierCount    1 = jednoduchý achievement, 3 = stupne 🥉🥈🥇
     * @param achievedTier koľko stupňov je splnených (0 = zamknutý)
     * @param progress     0..1 – priebeh k ďalšiemu stupňu (1 = všetko splnené)
     * @param achievedOn   približný dátum získania (dátum skúšky), môže byť null
     */
    public record Achievement(String id, Category category, String icon, String title, String description,
                              int tierCount, int achievedTier, double progress, String progressText,
                              LocalDate achievedOn) {
        public boolean unlocked() {
            return achievedTier > 0;
        }

        public boolean maxed() {
            return achievedTier >= tierCount;
        }

        /** Kľúč na zistenie, či je achievement (alebo jeho stupeň) nový od minula. */
        public String key() {
            return id + ":" + achievedTier;
        }
    }

    /** Jeden semester na časovej osi, napr. „ZS 2024/2025“. */
    public record SemesterStat(String label, int credits, String average, int completed, int total) {
    }

    /**
     * @param average celkový vážený priemer (0 = zatiaľ bez známok)
     */
    public record Report(int xp, Rank rank, Rank nextRank, int xpToNext, double rankProgress,
                         Map<String, Integer> gradeCounts, double average, int firstTryCount, int examSubjectCount,
                         List<Achievement> achievements, List<SemesterStat> timeline) {
        public long unlockedCount() {
            return achievements.stream().filter(Achievement::unlocked).count();
        }

        /** Počet achievementov so stupňami, ktoré majú presne daný stupeň (1 = 🥉, 2 = 🥈, 3 = 🥇). */
        public long tierCount(int tier) {
            return achievements.stream().filter(a -> a.tierCount() > 1 && a.achievedTier() == tier).count();
        }

        /** Splnené jednoduché achievementy (bez stupňov). */
        public long singlesUnlocked() {
            return achievements.stream().filter(a -> a.tierCount() == 1 && a.unlocked()).count();
        }

        /** Naposledy získaný achievement (podľa dátumu), ak nejaký je. */
        public java.util.Optional<Achievement> latestAchievement() {
            return achievements.stream().filter(a -> a.unlocked() && a.achievedOn() != null)
                    .max(Comparator.comparing(Achievement::achievedOn));
        }
    }

    /** Splnený predmet + zápisný list, v ktorom bol splnený, a približný dátum. */
    private record Done(Subject subject, int enrollment, LocalDate date) {
    }

    public Report compute(List<EnrollmentInput> enrollments, int requiredCredits,
                          StudyPlanCatalog catalog, LocalDate today) {
        int required = requiredCredits > 0 ? requiredCredits : 180;
        List<EnrollmentInput> inputs = enrollments == null ? List.of() : enrollments;
        List<Done> done = completedSubjects(inputs, today);
        int credits = done.stream().mapToInt(d -> d.subject().getCreditsValue()).sum();

        int xp = done.stream().mapToInt(d -> xpFor(d.subject())).sum();
        List<Rank> ranks = ranks(required);
        Rank rank = ranks.get(0);
        for (Rank candidate : ranks) {
            boolean last = candidate.index() == ranks.size() - 1;
            if (xp >= candidate.minXp() && (!last || credits >= required)) rank = candidate;
        }
        Rank next = rank.index() + 1 < ranks.size() ? ranks.get(rank.index() + 1) : null;
        int xpToNext = next == null ? 0 : Math.max(0, next.minXp() - xp);
        double rankProgress = next == null ? 1.0
                : clamp((double) (xp - rank.minXp()) / Math.max(1, next.minXp() - rank.minXp()));

        Map<String, Integer> gradeCounts = new LinkedHashMap<>();
        for (String letter : new String[]{"A", "B", "C", "D", "E", "ABS"}) gradeCounts.put(letter, 0);
        done.forEach(d -> gradeCounts.merge(letter(d.subject()), 1, Integer::sum));

        Map<String, Integer> terms = examTermsPerSubject(inputs, today);
        List<Done> withExam = done.stream().filter(d -> terms.containsKey(key(d.subject().getName()))).toList();
        int firstTry = (int) withExam.stream().filter(d -> terms.get(key(d.subject().getName())) == 1).count();

        List<Achievement> achievements = new ArrayList<>();
        studyAchievements(achievements, inputs, done, credits, required, catalog);
        gradeAchievements(achievements, inputs, done);
        examAchievements(achievements, inputs, done, terms, firstTry, today);
        directionAchievements(achievements, done, catalog);

        double average = weightedAverage(done.stream().map(Done::subject).toList());
        return new Report(xp, rank, next, xpToNext, rankProgress, gradeCounts, average, firstTry, withExam.size(),
                achievements, timeline(inputs));
    }

    // ---------------------------------------------------------------- XP a hodnosti

    static int xpFor(Subject subject) {
        double multiplier = switch (letter(subject)) {
            case "A" -> 3.0;
            case "B" -> 2.5;
            case "C" -> 2.0;
            case "D" -> 1.5;
            case "E" -> 1.0;
            default -> 1.5; // ABS – absolvované bez známky
        };
        return (int) Math.round(subject.getCreditsValue() * multiplier * XP_PER_CREDIT_UNIT);
    }

    public static List<Rank> ranks(int requiredCredits) {
        int ideal = requiredCredits * 2 * XP_PER_CREDIT_UNIT;
        List<Rank> ranks = new ArrayList<>();
        for (int i = 0; i < RANK_NAMES.length; i++) {
            ranks.add(new Rank(i, RANK_NAMES[i], (int) Math.round(ideal * RANK_FACTORS[i])));
        }
        return ranks;
    }

    // ---------------------------------------------------------------- achievementy: štúdium

    private void studyAchievements(List<Achievement> out, List<EnrollmentInput> inputs, List<Done> done,
                                   int credits, int required, StudyPlanCatalog catalog) {
        out.add(single("first-step", Category.STUDY, "👣", "Prvý krok", "Splň svoj prvý predmet",
                !done.isEmpty(), done.isEmpty() ? 0 : 1, done.isEmpty() ? "0 / 1" : "✓",
                done.stream().map(Done::date).filter(Objects::nonNull).min(Comparator.naturalOrder()).orElse(null)));

        boolean master = required < 180;
        String[] yearTitles = master ? new String[]{"1. ročník Mgr.", "2. ročník Mgr."}
                : new String[]{"Prvák", "Druhák", "Tretiak"};
        for (int year = 0; year < yearTitles.length; year++) {
            EnrollmentInput input = year < inputs.size() ? inputs.get(year) : null;
            int total = input == null ? 0 : (int) input.subjects().stream().filter(Objects::nonNull).count();
            final int index = year;
            List<Done> inYear = done.stream().filter(d -> d.enrollment() == index).toList();
            boolean complete = total > 0 && inYear.size() >= total;
            out.add(single("year-" + (year + 1), Category.STUDY, "📚", yearTitles[year],
                    "Celý " + (year + 1) + ". ročník bez dlhu", complete,
                    total == 0 ? 0 : (double) inYear.size() / total,
                    total == 0 ? "zatiaľ nezačatý" : inYear.size() + " / " + total + " predmetov",
                    complete ? latest(inYear) : null));
        }

        List<Done> byDate = sortedByDate(done);
        out.add(creditThreshold("half-way", "🛤", "Polovica cesty", required / 2, credits, byDate));
        out.add(creditThreshold("final-stretch", "🏁", "Cieľová rovinka", required - 30, credits, byDate));
        out.add(creditThreshold("graduate", "🎓", "Absolvent", required, credits, byDate));

        if (catalog != null && catalog.getRequiredMandatoryCredits() > 0) {
            int mandatory = SubjectCategory.MANDATORY.sumCredits(done.stream().map(Done::subject).toList());
            int need = catalog.getRequiredMandatoryCredits();
            out.add(single("mandatory", Category.STUDY, "📘", "Povinná jazda",
                    "Získaj všetkých " + need + " kreditov z povinných predmetov", mandatory >= need,
                    (double) mandatory / need, mandatory + " / " + need + " kr.", null));
        }
        if (catalog != null && catalog.getRequiredCompulsoryElectiveCredits() > 0) {
            int pvp = SubjectCategory.COMPULSORY_ELECTIVE.sumCredits(done.stream().map(Done::subject).toList());
            int need = catalog.getRequiredCompulsoryElectiveCredits();
            out.add(single("pvp", Category.STUDY, "📗", "PVP majster",
                    "Získaj " + need + " kreditov z povinne voliteľných predmetov", pvp >= need,
                    (double) pvp / need, pvp + " / " + need + " kr.", null));
        }
    }

    private Achievement creditThreshold(String id, String icon, String title, int threshold,
                                        int credits, List<Done> byDate) {
        LocalDate reachedOn = null;
        int sum = 0;
        for (Done d : byDate) {
            sum += d.subject().getCreditsValue();
            if (sum >= threshold) {
                reachedOn = d.date();
                break;
            }
        }
        return single(id, Category.STUDY, icon, title, "Získaj " + threshold + " kreditov",
                credits >= threshold, (double) credits / threshold,
                credits >= threshold ? "✓ " + threshold + " kr." : credits + " / " + threshold + " kr.",
                credits >= threshold ? reachedOn : null);
    }

    // ---------------------------------------------------------------- achievementy: známky

    private void gradeAchievements(List<Achievement> out, List<EnrollmentInput> inputs, List<Done> done) {
        List<Done> aGrades = sortedByDate(done.stream().filter(d -> "A".equals(letter(d.subject()))).toList());
        out.add(tiered("collector-a", Category.GRADES, "⭐", "Zberateľ A", "Získaj 1 / 5 / 10 známok A",
                aGrades.size(), new int[]{1, 5, 10}, aGrades));

        // čistý semester: aspoň 2 známky (nie ABS) a všetky A alebo B
        String bestSemester = null;
        double bestRatio = 0;
        boolean clean = false;
        for (int e = 0; e < inputs.size(); e++) {
            for (String semester : new String[]{"ZS", "LS"}) {
                final int index = e;
                List<String> letters = done.stream()
                        .filter(d -> d.enrollment() == index && semester.equalsIgnoreCase(trim(d.subject().getSemester())))
                        .map(d -> letter(d.subject())).filter(l -> !"ABS".equals(l)).toList();
                if (letters.size() < 2) continue;
                long good = letters.stream().filter(l -> "A".equals(l) || "B".equals(l)).count();
                double ratio = (double) good / letters.size();
                if (ratio >= bestRatio) {
                    bestRatio = ratio;
                    bestSemester = good + " / " + letters.size() + " za A/B (" + semester + " "
                            + academicYear(inputs.get(e).name()) + ")";
                }
                clean |= good == letters.size();
            }
        }
        out.add(single("clean-semester", Category.GRADES, "✨", "Čistý semester",
                "Semester, v ktorom máš len známky A a B", clean, bestRatio,
                bestSemester == null ? "zatiaľ bez známok" : "najlepší: " + bestSemester, null));

        // ročník bez E / FX (aspoň 3 známky)
        String noE = null;
        for (int e = 0; e < inputs.size() && noE == null; e++) {
            final int index = e;
            List<String> letters = done.stream().filter(d -> d.enrollment() == index)
                    .map(d -> letter(d.subject())).filter(l -> !"ABS".equals(l)).toList();
            if (letters.size() >= 3 && letters.stream().noneMatch("E"::equals)) noE = (e + 1) + ". ročník";
        }
        out.add(single("no-e", Category.GRADES, "🛡", "Bez E-čka", "Celý ročník bez známky E",
                noE != null, noE != null ? 1 : 0, noE != null ? "✓ " + noE : "zatiaľ nie", null));

        double average = weightedAverage(done.stream().map(Done::subject).toList());
        out.add(single("honors", Category.GRADES, "🏆", "Vyznamenanie", "Celkový vážený priemer do 1,50",
                average > 0 && average <= 1.5, average <= 0 ? 0 : clamp((3.0 - average) / 1.5),
                average <= 0 ? "bez známok" : String.format(Locale.ROOT, "%.2f (cieľ 1,50)", average)
                        .replace('.', ','), null));

        // lepší priemer ako v predchádzajúcom ročníku
        List<Double> yearly = new ArrayList<>();
        for (int e = 0; e < inputs.size(); e++) {
            final int index = e;
            yearly.add(weightedAverage(done.stream().filter(d -> d.enrollment() == index).map(Done::subject).toList()));
        }
        boolean rising = false;
        String risingText = "potrebné aspoň 2 ročníky so známkami";
        for (int i = 1; i < yearly.size(); i++) {
            if (yearly.get(i) > 0 && yearly.get(i - 1) > 0) {
                risingText = format(yearly.get(i - 1)) + " → " + format(yearly.get(i));
                if (yearly.get(i) < yearly.get(i - 1)) rising = true;
            }
        }
        out.add(single("rising", Category.GRADES, "📈", "Na vzostupe",
                "Lepší priemer ako v predchádzajúcom ročníku", rising, rising ? 1 : 0, risingText, null));
    }

    // ---------------------------------------------------------------- achievementy: skúšky

    private void examAchievements(List<Achievement> out, List<EnrollmentInput> inputs, List<Done> done,
                                  Map<String, Integer> terms, int firstTry, LocalDate today) {
        List<Done> firstTrySubjects = sortedByDate(done.stream()
                .filter(d -> terms.getOrDefault(key(d.subject().getName()), 0) == 1).toList());
        out.add(tiered("first-try", Category.EXAMS, "🎯", "Na prvý šup",
                "Predmety so skúškou zvládnuté na prvý termín (5 / 10 / 20)",
                firstTry, new int[]{5, 10, 20}, firstTrySubjects));

        String persistent = done.stream()
                .filter(d -> terms.getOrDefault(key(d.subject().getName()), 0) >= 3)
                .map(d -> d.subject().getName()).findFirst().orElse(null);
        out.add(single("never-give-up", Category.EXAMS, "💪", "Nevzdal som to",
                "Predmet úspešne ukončený až na 3. termín", persistent != null, persistent != null ? 1 : 0,
                persistent != null ? "✓ " + persistent : "zatiaľ nie", null));

        List<LocalDate> examDates = new ArrayList<>();
        List<Exam> pastExams = new ArrayList<>();
        for (EnrollmentInput input : inputs) {
            for (Exam exam : safe(input.exams())) {
                LocalDate date = parseDate(exam.getDate());
                if (date != null && !date.isAfter(today)) {
                    examDates.add(date);
                    pastExams.add(exam);
                }
            }
        }
        examDates.sort(Comparator.naturalOrder());
        int bestWeek = 0;
        LocalDate marathonDate = null;
        for (int i = 0; i < examDates.size(); i++) {
            int count = 0;
            for (int j = i; j < examDates.size() && !examDates.get(j).isAfter(examDates.get(i).plusDays(6)); j++) {
                count++;
                if (count >= 3 && marathonDate == null) marathonDate = examDates.get(j);
            }
            bestWeek = Math.max(bestWeek, count);
        }
        out.add(single("marathon", Category.EXAMS, "🏃", "Skúškový maratón", "3 skúšky počas 7 dní",
                bestWeek >= 3, Math.min(1.0, bestWeek / 3.0), "najviac " + bestWeek + " za týždeň", marathonDate));

        List<LocalDate> early = pastExams.stream()
                .filter(exam -> exam.getTime() != null && !exam.getTime().isBlank()
                        && exam.getTime().trim().compareTo("08:00") <= 0 && exam.getTime().trim().length() == 5)
                .map(exam -> parseDate(exam.getDate())).sorted().toList();
        out.add(tieredByDates("early-bird", Category.EXAMS, "🌅", "Ranné vtáča",
                "Skúšky o 8:00 alebo skôr (1 / 5 / 10)", early, new int[]{1, 5, 10}));
    }

    // ---------------------------------------------------------------- achievementy: smery

    private void directionAchievements(List<Achievement> out, List<Done> done, StudyPlanCatalog catalog) {
        if (catalog == null || catalog.getSubjects() == null) return;
        Map<StudyDirection, List<Done>> byDirection = new EnumMap<>(StudyDirection.class);
        for (CurriculumSubject curriculum : catalog.getSubjects()) {
            for (StudyDirection direction : curriculum.getDirections()) byDirection.putIfAbsent(direction, new ArrayList<>());
        }
        for (Done d : done) {
            for (CurriculumSubject curriculum : catalog.getSubjects()) {
                if (CompletedSubjectsService.codesEquivalent(d.subject().getAbbreviation(), curriculum)) {
                    curriculum.getDirections().forEach(direction -> byDirection.get(direction).add(d));
                    break;
                }
            }
        }
        int directionsWithSubject = 0;
        for (Map.Entry<StudyDirection, List<Done>> entry : byDirection.entrySet()) {
            List<Done> subjects = sortedByDate(entry.getValue());
            if (!subjects.isEmpty()) directionsWithSubject++;
            out.add(tiered("direction-" + entry.getKey().name().toLowerCase(Locale.ROOT), Category.DIRECTIONS,
                    directionIcon(entry.getKey()), entry.getKey().getSlovakName(),
                    "Splnené predmety zo smeru (2 / 4 / 6)", subjects.size(), new int[]{2, 4, 6}, subjects));
        }
        out.add(single("renaissance", Category.DIRECTIONS, "🌈", "Renesančný študent",
                "Aspoň jeden predmet z 5 rôznych smerov", directionsWithSubject >= 5,
                Math.min(1.0, directionsWithSubject / 5.0), directionsWithSubject + " / 5 smerov", null));
    }

    private static String directionIcon(StudyDirection direction) {
        return switch (direction) {
            case ALGORITHMS -> "🔢";
            case SOFTWARE_DEVELOPMENT -> "💻";
            case DATA_AND_AI -> "📊";
            case WEB_DEVELOPMENT -> "🌐";
            case GAME_AND_GRAPHICS -> "🎮";
            case COMPUTER_SYSTEMS -> "🖥";
            case INTELLIGENT_TECHNOLOGIES -> "🤖";
            case MATHEMATICAL_FOUNDATIONS -> "📐";
            case GENERAL_SKILLS -> "🗣";
        };
    }

    // ---------------------------------------------------------------- časová os

    private List<SemesterStat> timeline(List<EnrollmentInput> inputs) {
        List<SemesterStat> result = new ArrayList<>();
        for (EnrollmentInput input : inputs) {
            for (String semester : new String[]{"ZS", "LS"}) {
                List<Subject> subjects = safe(input.subjects()).stream().filter(Objects::nonNull)
                        .filter(s -> semester.equalsIgnoreCase(trim(s.getSemester()))).toList();
                if (subjects.isEmpty()) continue;
                List<Subject> completed = subjects.stream().filter(SubjectCompletionService::isCompleted).toList();
                double average = weightedAverage(completed);
                result.add(new SemesterStat(semester + " " + academicYear(input.name()),
                        completed.stream().mapToInt(Subject::getCreditsValue).sum(),
                        average > 0 ? format(average) : "-", completed.size(), subjects.size()));
            }
        }
        return result;
    }

    // ---------------------------------------------------------------- pomocné

    private List<Done> completedSubjects(List<EnrollmentInput> inputs, LocalDate today) {
        Map<String, Done> unique = new LinkedHashMap<>();
        for (int e = 0; e < inputs.size(); e++) {
            EnrollmentInput input = inputs.get(e);
            Map<String, LocalDate> examDates = new HashMap<>();
            LocalDate lastInEnrollment = null;
            for (Exam exam : safe(input.exams())) {
                LocalDate date = parseDate(exam.getDate());
                if (date == null || date.isAfter(today)) continue;
                examDates.merge(key(exam.getSubjectName()), date, (a, b) -> a.isAfter(b) ? a : b);
                if (lastInEnrollment == null || date.isAfter(lastInEnrollment)) lastInEnrollment = date;
            }
            for (Subject subject : safe(input.subjects())) {
                if (subject == null || !SubjectCompletionService.isCompleted(subject)) continue;
                // bez skúšky (napr. ABS) = približne koniec skúškového obdobia ročníka
                LocalDate date = examDates.getOrDefault(key(subject.getName()), lastInEnrollment);
                unique.putIfAbsent(SubjectCompletionService.identityKey(subject), new Done(subject, e, date));
            }
        }
        return new ArrayList<>(unique.values());
    }

    private static Map<String, Integer> examTermsPerSubject(List<EnrollmentInput> inputs, LocalDate today) {
        Map<String, Integer> terms = new HashMap<>();
        for (EnrollmentInput input : inputs) {
            for (Exam exam : safe(input.exams())) {
                LocalDate date = parseDate(exam.getDate());
                if (date != null && !date.isAfter(today)) terms.merge(key(exam.getSubjectName()), 1, Integer::sum);
            }
        }
        return terms;
    }

    private static Achievement single(String id, Category category, String icon, String title, String description,
                                      boolean unlocked, double progress, String text, LocalDate date) {
        return new Achievement(id, category, icon, title, description, 1, unlocked ? 1 : 0,
                unlocked ? 1.0 : clamp(progress), text, unlocked ? date : null);
    }

    /** Achievement so stupňami – dátum získania = dátum, kedy počet dosiahol najvyšší splnený stupeň. */
    private static Achievement tiered(String id, Category category, String icon, String title, String description,
                                      int value, int[] tiers, List<Done> sortedEvents) {
        List<LocalDate> dates = sortedEvents.stream().map(Done::date).toList();
        return tieredByDates(id, category, icon, title, description, dates, tiers, value);
    }

    private static Achievement tieredByDates(String id, Category category, String icon, String title,
                                             String description, List<LocalDate> sortedDates, int[] tiers) {
        return tieredByDates(id, category, icon, title, description, sortedDates, tiers, sortedDates.size());
    }

    private static Achievement tieredByDates(String id, Category category, String icon, String title,
                                             String description, List<LocalDate> sortedDates, int[] tiers, int value) {
        int achieved = 0;
        while (achieved < tiers.length && value >= tiers[achieved]) achieved++;
        double progress = achieved >= tiers.length ? 1.0 : clamp((double) value / tiers[achieved]);
        String text = achieved >= tiers.length ? value + " ✓ všetky stupne"
                : value + " / " + tiers[achieved];
        LocalDate date = achieved > 0 && tiers[achieved - 1] - 1 < sortedDates.size()
                ? sortedDates.get(tiers[achieved - 1] - 1) : null;
        return new Achievement(id, category, icon, title, description, tiers.length, achieved, progress, text, date);
    }

    private static List<Done> sortedByDate(List<Done> items) {
        List<Done> sorted = new ArrayList<>(items);
        sorted.sort(Comparator.comparing(Done::date, Comparator.nullsLast(Comparator.naturalOrder())));
        return sorted;
    }

    private static LocalDate latest(List<Done> items) {
        return items.stream().map(Done::date).filter(Objects::nonNull).max(Comparator.naturalOrder()).orElse(null);
    }

    /** "A", "B" … "E", "FX" alebo "ABS" (absolvované bez známky). */
    static String letter(Subject subject) {
        double numeric = subject.getGradeNumeric();
        if (numeric <= 0) return "ABS";
        if (numeric <= 1.0) return "A";
        if (numeric <= 1.5) return "B";
        if (numeric <= 2.0) return "C";
        if (numeric <= 2.5) return "D";
        if (numeric <= 3.0) return "E";
        return "FX";
    }

    private static double weightedAverage(List<Subject> subjects) {
        double sum = 0;
        int weight = 0;
        for (Subject subject : subjects) {
            double grade = subject.getGradeNumeric();
            int credits = subject.getCreditsValue();
            if (SubjectCompletionService.isCompleted(subject) && grade > 0 && credits > 0) {
                sum += grade * credits;
                weight += credits;
            }
        }
        return weight > 0 ? sum / weight : 0;
    }

    private static String academicYear(String enrollmentName) {
        String year = sk.ukf.aisviewer.model.EnrollmentName.academicYear(enrollmentName);
        return year.isBlank() ? "" : year;
    }

    static LocalDate parseDate(String text) {
        if (text == null || text.isBlank()) return null;
        try {
            return LocalDate.parse(text.trim(), AIS_DATE);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static String key(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }

    private static String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value).replace('.', ',');
    }

    private static double clamp(double value) {
        return Math.max(0, Math.min(1, value));
    }

    private static <T> List<T> safe(List<T> list) {
        return list == null ? List.of() : list;
    }
}
