package sk.ukf.aisviewer;

import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;
import sk.ukf.aisviewer.controller.MainController;
import sk.ukf.aisviewer.service.LocalCacheService;
import sk.ukf.aisviewer.service.ThemeManager;

import java.io.IOException;
import java.net.URL;
import java.util.List;

public class App extends Application {
    private static final Logger LOG = AppLog.get(App.class);


    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws IOException {
        primaryStage = stage;
        stage.setTitle("AIS Viewer – UKF Nitra");

        URL iconUrl = getClass().getResource("ais-icon.png");
        if (iconUrl != null) {
            stage.getIcons().add(new Image(iconUrl.toExternalForm()));
        }

        LOG.info("Začínam načítanie cache.");
        try {
            LocalCacheService.CacheSnapshot cachedSnapshot =
                    new LocalCacheService().load().orElse(null);
            if (cachedSnapshot != null && cachedSnapshot.getStudentInfo() != null
                    && cachedSnapshot.getEnrollmentData() != null
                    && !cachedSnapshot.getEnrollmentData().isEmpty()) {
                LOG.info("Platná cache nájdená, otváram hlavné okno.");
                MainController.setAisClient(null);
                showMainScreen();
            } else {
                LOG.info("Použiteľná cache nebola nájdená, otváram login.");
                showLoginScreen();
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Chyba pri otváraní UI s cache, prechádzam na login.", e);
            showLoginScreen();
        }
    }

    public static void showLoginScreen() throws IOException {
        FXMLLoader loader = new FXMLLoader(App.class.getResource("login.fxml"));
        double[] size = fitToScreen(600, 600);
        Scene scene = new Scene(loader.load(), size[0], size[1]);
        ThemeManager.getInstance().applyTheme(scene);
        primaryStage.setScene(scene);
        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    public static void showMainScreen() throws IOException {
        FXMLLoader loader = new FXMLLoader(App.class.getResource("main.fxml"));
        double[] size = fitToScreen(1280, 800);
        Scene scene = new Scene(loader.load(), size[0], size[1]);
        ThemeManager.getInstance().applyTheme(scene);
        primaryStage.setScene(scene);
        primaryStage.setResizable(true);
        primaryStage.setMinWidth(Math.min(800, size[0]));
        primaryStage.setMinHeight(Math.min(560, size[1]));
        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    /**
     * Veľkosť obsahu okna tak, aby sa celé okno (aj s titulkom a tlačidlami zmenšiť/zavrieť)
     * zmestilo na obrazovku. Pri zväčšení Windows 150 % má Full HD obrazovka len ~1280 × 690
     * logických bodov – pevných 800 bodov na výšku by vytlačilo titulok okna mimo obrazovky.
     */
    public static double[] fitToScreen(double preferredWidth, double preferredHeight) {
        Rectangle2D screen = screenBounds(primaryStage);
        double titleBar = 40; // titulok okna a rámik
        double width = Math.min(preferredWidth, screen.getWidth() - 24);
        double height = Math.min(preferredHeight, screen.getHeight() - titleBar - 16);
        return new double[]{width, height};
    }

    /**
     * Pracovná plocha obrazovky, na ktorej je okno (podľa jeho stredu). Pri viacerých monitoroch
     * s rôznym zväčšením sa tak veľkosť počíta pre ten správny. Ak okno ešte nie je zobrazené,
     * použije sa hlavná obrazovka.
     */
    public static Rectangle2D screenBounds(Window window) {
        if (window != null && window.isShowing() && !Double.isNaN(window.getX())) {
            double centerX = window.getX() + window.getWidth() / 2;
            double centerY = window.getY() + window.getHeight() / 2;
            List<Screen> screens = Screen.getScreensForRectangle(centerX, centerY, 1, 1);
            if (!screens.isEmpty()) {
                return screens.get(0).getVisualBounds();
            }
        }
        return Screen.getPrimary().getVisualBounds();
    }

    /** Okno, ktoré má práve fokus (napr. otvorený dialóg), inak hlavné okno aplikácie. */
    public static Window activeWindow() {
        return Window.getWindows().stream()
                .filter(Window::isFocused)
                .findFirst()
                .orElse(primaryStage);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
