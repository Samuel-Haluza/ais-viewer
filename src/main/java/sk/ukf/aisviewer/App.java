package sk.ukf.aisviewer;

import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import sk.ukf.aisviewer.controller.MainController;
import sk.ukf.aisviewer.service.LocalCacheService;
import sk.ukf.aisviewer.service.ThemeManager;

import java.io.IOException;
import java.net.URL;

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
        Scene scene = new Scene(loader.load(), 600, 600);
        ThemeManager.getInstance().applyTheme(scene);
        primaryStage.setScene(scene);
        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    public static void showMainScreen() throws IOException {
        FXMLLoader loader = new FXMLLoader(App.class.getResource("main.fxml"));
        Scene scene = new Scene(loader.load(), 1000, 700);
        ThemeManager.getInstance().applyTheme(scene);
        primaryStage.setScene(scene);
        primaryStage.setResizable(true);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);
        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
