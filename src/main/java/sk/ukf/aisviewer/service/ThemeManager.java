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
        scenes.put(scene, Boolean.TRUE);
        applyRootTheme(scene.getRoot());
        updateThemeStylesheet(scene);
    }

    public void applyTheme(DialogPane dialogPane) {
        if (dialogPane == null) {
            return;
        }
        applyRootTheme(dialogPane);
        addBaseStylesheet(dialogPane.getStylesheets());
        updateThemeStylesheet(dialogPane.getStylesheets());
        dialogPane.sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (newScene != null) {
                applyTheme(newScene);
            }
        });
    }

    private void applyRootTheme(Parent root) {
        root.getStyleClass().removeAll("theme-dark", "theme-light");
        root.getStyleClass().add(currentTheme == AppTheme.DARK
                ? "theme-dark" : "theme-light");
    }

    private void updateThemeStylesheet(Scene scene) {
        updateThemeStylesheet(scene.getStylesheets());
    }

    private void updateThemeStylesheet(java.util.List<String> stylesheets) {
        URL lightUrl = ThemeManager.class.getResource("/sk/ukf/aisviewer/theme-light.css");
        if (lightUrl == null) {
            return;
        }
        String lightStylesheet = lightUrl.toExternalForm();
        stylesheets.remove(lightStylesheet);
        if (currentTheme == AppTheme.LIGHT) {
            stylesheets.add(lightStylesheet);
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
