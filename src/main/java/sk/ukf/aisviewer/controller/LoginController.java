package sk.ukf.aisviewer.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import sk.ukf.aisviewer.App;
import sk.ukf.aisviewer.service.AisClient;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Label statusLabel;

    @FXML
    public void initialize() {
        statusLabel.setVisible(false);
        passwordField.setOnAction(event -> handleLogin());
        usernameField.setOnAction(event -> passwordField.requestFocus());
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showError("Zadajte meno a heslo.");
            return;
        }

        loginButton.setDisable(true);
        usernameField.setDisable(true);
        passwordField.setDisable(true);
        showInfo("Prihlasovanie (spúšťam prehliadač)...");

        Thread loginThread = new Thread(() -> {
            try {
                AisClient client = new AisClient();
                boolean success = client.login(username, password);

                Platform.runLater(() -> {
                    if (success) {
                        try {
                            MainController.setAisClient(client);
                            App.showMainScreen();
                        } catch (Exception e) {
                            showError("Chyba pri otváraní hlavného okna: " + e.getMessage());
                            enableControls();
                        }
                    } else {
                        showError("Nesprávne meno alebo heslo.");
                        enableControls();
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showError("Chyba: " + e.getMessage());
                    enableControls();
                });
            }
        });
        loginThread.setDaemon(true);
        loginThread.start();
    }

    private void showError(String message) {
        statusLabel.setText(message);
        statusLabel.getStyleClass().removeAll("info-label");
        statusLabel.getStyleClass().add("error-label");
        statusLabel.setVisible(true);
    }

    private void showInfo(String message) {
        statusLabel.setText(message);
        statusLabel.getStyleClass().removeAll("error-label");
        statusLabel.getStyleClass().add("info-label");
        statusLabel.setVisible(true);
    }

    private void enableControls() {
        loginButton.setDisable(false);
        usernameField.setDisable(false);
        passwordField.setDisable(false);
    }
}