package sk.ukf.aisviewer.service;

/**
 * Pomocný parser – používa sa len na text rozvrhu z page source.
 * Predmety a skúšky sa parsujú priamo v AisClient cez Selenium.
 */
public class AisParser {

    /**
     * Parsuje rozvrh z HTML page source – vráti čistý text.
     */
    public String parseScheduleAsText(String html) {
        if (html == null || html.isBlank()) return "Rozvrh nie je dostupný.";

        // Odstráň HTML tagy a vráť čistý text
        String text = html
                .replaceAll("<script[^>]*>[\\s\\S]*?</script>", "")
                .replaceAll("<style[^>]*>[\\s\\S]*?</style>", "")
                .replaceAll("<[^>]+>", " ")
                .replaceAll("&nbsp;", " ")
                .replaceAll("&amp;", "&")
                .replaceAll("\\s{2,}", " ")
                .trim();

        if (text.length() > 5000) {
            text = text.substring(0, 5000) + "...";
        }

        return text.isBlank() ? "Rozvrh nie je dostupný." : text;
    }
}