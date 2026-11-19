package sk.ukf.aisviewer.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

/** Spoločné miesto pre lokálne súbory aplikácie (cache, študijný plán). */
public final class AppStorage {

    private AppStorage() {
    }

    /** Windows: %APPDATA%\AIS Viewer, inak ~/.ais-viewer */
    public static Path dataDirectory() {
        String appData = System.getenv("APPDATA");
        return appData != null && !appData.isBlank()
                ? Path.of(appData, "AIS Viewer")
                : Path.of(System.getProperty("user.home"), ".ais-viewer");
    }

    /** Súbor v priečinku aplikácie, napr. {@code file("cache.json")}. */
    public static Path file(String name) {
        return dataDirectory().resolve(name);
    }

    /**
     * Zapíše text bezpečne: najprv do dočasného súboru a potom ho premenuje.
     * Pri páde počas zápisu tak pôvodný súbor ostane nepoškodený.
     */
    public static void writeAtomically(Path target, String content) throws IOException {
        Files.createDirectories(target.toAbsolutePath().getParent());
        Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
        Files.writeString(temporary, content, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        try {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
