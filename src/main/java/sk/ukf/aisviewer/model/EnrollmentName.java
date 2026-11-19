package sk.ukf.aisviewer.model;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Čítanie údajov z názvu zápisného listu v tvare z AIS2, napr. {@code "2025/2026 (2AI22b)"}:
 * <ul>
 *   <li>akademický rok – "2025/2026", začiatočný rok – 2025</li>
 *   <li>kód programu – "2AI22b", ročník – 2, stupeň – 'b' (bakalársky) / 'm' (magisterský)</li>
 * </ul>
 */
public final class EnrollmentName {

    private static final Pattern YEAR = Pattern.compile("(\\d{4})");
    private static final Pattern LEADING_NUMBER = Pattern.compile("^(\\d+)");

    private EnrollmentName() {
    }

    /** Prvý štvorciferný rok v názve ("2025/2026 (2AI22b)" → 2025), inak -1. */
    public static int startYear(String name) {
        if (name == null) return -1;
        Matcher matcher = YEAR.matcher(name);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : -1;
    }

    /** Text pred zátvorkou ("2025/2026 (2AI22b)" → "2025/2026"); bez zátvorky celý názov. */
    public static String academicYear(String name) {
        if (name == null) return "";
        int openingBracket = name.lastIndexOf('(');
        return (openingBracket > 0 ? name.substring(0, openingBracket) : name).trim();
    }

    /** Obsah poslednej zátvorky ("2025/2026 (2AI22b)" → "2AI22b"), inak prázdny reťazec. */
    public static String programCode(String name) {
        if (name == null) return "";
        int openingBracket = name.lastIndexOf('(');
        int closingBracket = name.lastIndexOf(')');
        if (openingBracket >= 0 && closingBracket > openingBracket) {
            return name.substring(openingBracket + 1, closingBracket).trim();
        }
        return "";
    }

    /** Ročník z kódu programu ("2AI22b" → 2), inak -1. */
    public static int studyYear(String programCode) {
        if (programCode == null) return -1;
        Matcher matcher = LEADING_NUMBER.matcher(programCode.trim());
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : -1;
    }

    /** Stupeň štúdia podľa posledného písmena kódu: 'b', 'm', alebo 0 ak sa nedá určiť. */
    public static char degreeLetter(String name) {
        String code = programCode(name);
        return code.isEmpty() ? 0 : Character.toLowerCase(code.charAt(code.length() - 1));
    }

    /** Názov s najvyšším začiatočným rokom (pri zhode prvý v zozname), alebo null. */
    public static String latest(List<String> names) {
        if (names == null || names.isEmpty()) return null;
        String latest = names.get(0);
        int latestYear = startYear(latest);
        for (String name : names) {
            int year = startYear(name);
            if (year > latestYear) {
                latest = name;
                latestYear = year;
            }
        }
        return latest;
    }

    /** Pomocná metóda pre porovnávanie bez ohľadu na veľkosť písmen. */
    public static String lower(String name) {
        return name == null ? "" : name.toLowerCase(Locale.ROOT);
    }
}
