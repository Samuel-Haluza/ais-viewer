package sk.ukf.aisviewer.service;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.DialogPane;

import java.net.URL;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.prefs.Preferences;

public final class ThemeManager {
    private static final String PREFERENCE_KEY = "theme";
    private static final ThemeManager INSTANCE = new ThemeManager(
            Preferences.userNodeForPackage(ThemeManager.class));

    private final Preferences preferences;
    private final Map<Scene, Boolean> scenes =
            Collections.synchronizedMap(new WeakHashMap<>());
    private AppTheme currentTheme;

    public static ThemeManager getInstance() {
        return INSTANCE;
    }

    public ThemeManager() {
        this(Preferences.userNodeForPackage(ThemeManager.class));
    }

    ThemeManager(Preferences preferences) {
        this.preferences = preferences;
        this.currentTheme = readTheme();
    }

    public AppTheme getCurrentTheme() {
        return currentTheme;
    }

    public void setTheme(AppTheme theme) {
        if (theme == null) {
            return;
        }
        currentTheme = theme;
        preferences.put(PREFERENCE_KEY, theme.name());
        synchronized (scenes) {
            scenes.keySet().forEach(this::applyTheme);
        }
    }

    public void toggleTheme() {
        setTheme(currentTheme == AppTheme.DARK ? AppTheme.LIGHT : AppTheme.DARK);
    }

    public void applyTheme(Scene scene) {
        if (scene == null) {
            return;
        }
        boolean firstTime = scenes.put(scene, Boolean.TRUE) == null;
        applyRootTheme(scene.getRoot());
        if (firstTime) {
            // listener len raz na scénu – setTheme volá applyTheme pri každom prepnutí témy
            addAppIcon(scene.getWindow());
            scene.windowProperty().addListener((observable, oldWindow, window) -> addAppIcon(window));
        }
    }

    /**
     * Logo aplikácie v titulku každého okna (hlavné okno, prihlásenie, oznámenia, detail predmetu…).
     * Všetky okná prechádzajú cez applyTheme, takže ho dostanú aj nové dialógy bez ďalšieho kódu.
     */
    private static javafx.scene.image.Image appIcon;

    private static void addAppIcon(javafx.stage.Window window) {
        if (!(window instanceof javafx.stage.Stage stage) || !stage.getIcons().isEmpty()) {
            return;
        }
        if (appIcon == null) {
            URL url = ThemeManager.class.getResource("/sk/ukf/aisviewer/ais-icon.png");
            if (url == null) {
                return;
            }
            appIcon = new javafx.scene.image.Image(url.toExternalForm());
        }
        stage.getIcons().add(appIcon);
    }

    public void applyTheme(DialogPane dialogPane) {
        if (dialogPane == null) {
            return;
        }
        applyRootTheme(dialogPane);
        addBaseStylesheet(dialogPane.getStylesheets());
        // Dialog má scénu (a okno) hneď od vytvorenia – listener nižšie by sa už nespustil
        if (dialogPane.getScene() != null) {
            applyTheme(dialogPane.getScene());
        }
        dialogPane.sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (newScene != null) {
                applyTheme(newScene);
            }
        });
    }

    /**
     * Téma = trieda theme-dark / theme-light na koreni scény. Farby oboch tém sú
     * ako paleta premenných v styles.css, takže sa nič iné meniť nemusí.
     */
    private void applyRootTheme(Parent root) {
        loadFontsOnce();
        root.getStyleClass().removeAll("theme-dark", "theme-light");
        root.getStyleClass().add(currentTheme == AppTheme.DARK
                ? "theme-dark" : "theme-light");
    }

    /**
     * Písma aplikácie (licencia OFL, súbory v resources/fonts). JavaFX načíta polotučné rezy
     * ako samostatné rodiny: "IBM Plex Sans SmBld" a "Source Serif 4 Semibold" (pozri styles.css).
     * Ak sa súbor nenájde, CSS použije záložné systémové písmo.
     */
    private static final String[] FONT_FILES = {
            "IBMPlexSans-Regular.ttf", "IBMPlexSans-SemiBold.ttf", "IBMPlexSans-Bold.ttf",
            "IBMPlexMono-Regular.ttf", "SourceSerif4-Regular.ttf", "SourceSerif4-Semibold.ttf"};
    private static boolean fontsLoaded;

    private static synchronized void loadFontsOnce() {
        if (fontsLoaded) {
            return;
        }
        fontsLoaded = true;
        for (String file : FONT_FILES) {
            URL url = ThemeManager.class.getResource("/sk/ukf/aisviewer/fonts/" + file);
            if (url != null) {
                javafx.scene.text.Font.loadFont(url.toExternalForm(), 13);
            }
        }
    }

    private void addBaseStylesheet(java.util.List<String> stylesheets) {
        URL stylesheetUrl = ThemeManager.class.getResource("/sk/ukf/aisviewer/styles.css");
        if (stylesheetUrl != null && !stylesheets.contains(stylesheetUrl.toExternalForm())) {
            stylesheets.add(stylesheetUrl.toExternalForm());
        }
    }

    private AppTheme readTheme() {
        try {
            return AppTheme.valueOf(preferences.get(PREFERENCE_KEY, AppTheme.DARK.name()));
        } catch (IllegalArgumentException e) {
            return AppTheme.DARK;
        }
    }
}
