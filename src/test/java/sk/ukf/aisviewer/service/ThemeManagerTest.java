package sk.ukf.aisviewer.service;

import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ThemeManagerTest {
    @Test
    void defaultsToDarkAndPersistsToggledTheme() throws Exception {
        Preferences preferences = Preferences.userRoot()
                .node("ais-viewer-test-" + UUID.randomUUID());
        try {
            ThemeManager manager = new ThemeManager(preferences);
            assertEquals(AppTheme.DARK, manager.getCurrentTheme());
            manager.toggleTheme();
            assertEquals(AppTheme.LIGHT, manager.getCurrentTheme());
            assertEquals("LIGHT", preferences.get("theme", ""));

            ThemeManager reloaded = new ThemeManager(preferences);
            assertEquals(AppTheme.LIGHT, reloaded.getCurrentTheme());
        } finally {
            preferences.removeNode();
        }
    }
}
