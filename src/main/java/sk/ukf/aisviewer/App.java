package sk.ukf.aisviewer;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import sk.ukf.aisviewer.controller.MainController;
import sk.ukf.aisviewer.service.LocalCacheService;

import java.io.IOException;
import java.net.URL;

public class App extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws IOException {
        primaryStage = stage;
        stage.setTitle("AIS Viewer – UKF Nitra");

        URL iconUrl = getClass().getResource("ais-icon.png");
        if (iconUrl != null) {
            stage.getIcons().add(new Image(iconUrl.toExternalForm()));
        }

        System.out.println("[STARTUP] Začínam načítanie cache.");
        try {
            LocalCacheService.CacheSnapshot cachedSnapshot =
                    new LocalCacheService().load().orElse(null);
            if (cachedSnapshot != null && cachedSnapshot.getStudentInfo() != null
                    && cachedSnapshot.getEnrollmentData() != null
                    && !cachedSnapshot.getEnrollmentData().isEmpty()) {
                System.out.println("[STARTUP] Platná cache nájdená, otváram hlavné okno.");
                MainController.setAisClient(null);
                showMainScreen();
            } else {
                System.out.println("[STARTUP] Použiteľná cache nebola nájdená, otváram login.");
                showLoginScreen();
            }
        } catch (Exception e) {
            System.err.println("[STARTUP] Chyba pri otváraní UI s cache, prechádzam na login.");
            e.printStackTrace();
            showLoginScreen();
        }
    }

    public static void showLoginScreen() throws IOException {
        FXMLLoader loader = new FXMLLoader(App.class.getResource("login.fxml"));
        Scene scene = new Scene(loader.load(), 600, 600);
        primaryStage.setScene(scene);
        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    public static void showMainScreen() throws IOException {
        FXMLLoader loader = new FXMLLoader(App.class.getResource("main.fxml"));
        Scene scene = new Scene(loader.load(), 1000, 700);
        primaryStage.setScene(scene);
        primaryStage.setResizable(true);
        primaryStage.setMinWidth(800);
        primaryStage.setMinHeight(600);
        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    public static Stage getPrimaryStage() {
        return primaryStage;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
