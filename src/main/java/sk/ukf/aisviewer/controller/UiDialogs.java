package sk.ukf.aisviewer.controller;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.util.Optional;

final class UiDialogs {

    private UiDialogs() {
    }

    static boolean showFirstLoginNotice(Window owner) {
        return showNotice(owner, "Prvé načítanie údajov",
                "Prvé načítanie údajov môže chvíľu trvať.\n"
                        + "Aplikácia sa teraz prihlási do AIS a načíta vaše údaje.\n"
                        + "Počas načítavania aplikáciu nezatvárajte.",
                "OK", null);
    }

    static boolean showRefreshNotice(Window owner) {
        return showNotice(owner, "Obnovenie údajov",
                "Obnovenie údajov môže chvíľu trvať.\n"
                        + "Aplikácia sa pripojí k AIS a znovu načíta aktuálne údaje.\n\n"
                        + "Z bezpečnostných dôvodov nebude heslo uložené do cache.\n"
                        + "Ak ste aplikáciu po poslednom prihlásení reštartovali,\n"
                        + "bude potrebné heslo zadať znova.",
                "OK / Pokračovať", "Zrušiť");
    }

    static void showInvalidPassword(Window owner) {
        showNotice(owner, "Prihlásenie sa nepodarilo",
                "Zadané heslo nie je správne.\n"
                        + "Skontrolujte heslo a skúste to znova.",
                "OK", null);
    }

    static Optional<String> showPasswordPrompt(Window owner) {
        Stage stage = createStage(owner, "Obnovenie údajov");
        VBox card = createCard("Obnovenie údajov");
        Label explanation = new Label(
                "Na pripojenie do AIS je potrebné zadať heslo.\n"
                        + "Heslo sa uloží iba do pamäte počas tejto relácie\n"
                        + "a nebude zapísané do cache.");
        explanation.getStyleClass().add("info-label");
        explanation.setWrapText(true);

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Heslo do AIS");
        passwordField.getStyleClass().add("login-field");

        Button continueButton = styledButton("Obnoviť", "login-button");
        Button cancelButton = styledButton("Zrušiť", "dialog-secondary-button");
        HBox buttons = new HBox(10, cancelButton, continueButton);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        card.getChildren().addAll(explanation, passwordField, buttons);
        stage.setScene(styledScene(card, 430, 300));

        final String[] result = {null};
        continueButton.setOnAction(event -> {
            if (!passwordField.getText().isBlank()) {
                result[0] = passwordField.getText();
                stage.close();
            }
        });
        cancelButton.setOnAction(event -> stage.close());
        stage.setOnShown(event -> passwordField.requestFocus());
        stage.showAndWait();
        return Optional.ofNullable(result[0]);
    }

    private static boolean showNotice(Window owner, String title, String message,
                                      String confirmText, String cancelText) {
        Stage stage = createStage(owner, title);
        VBox card = createCard(title);
        Label messageLabel = new Label(message);
        messageLabel.getStyleClass().add("info-label");
        messageLabel.setWrapText(true);

        Button confirmButton = styledButton(confirmText, "login-button");
        HBox buttons = new HBox(confirmButton);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        if (cancelText != null) {
            Button cancelButton = styledButton(cancelText, "dialog-secondary-button");
            cancelButton.setOnAction(event -> stage.close());
            buttons.getChildren().add(0, cancelButton);
        }

        card.getChildren().addAll(messageLabel, buttons);
        stage.setScene(styledScene(card, cancelText == null ? 430 : 500, 285));
        final boolean[] confirmed = {false};
        confirmButton.setOnAction(event -> {
            confirmed[0] = true;
            stage.close();
        });
        stage.showAndWait();
        return confirmed[0];
    }

    private static Stage createStage(Window owner, String title) {
        Stage stage = new Stage();
        stage.setTitle(title);
        stage.initOwner(owner);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setResizable(false);
        return stage;
    }

    private static VBox createCard(String title) {
        VBox card = new VBox(16);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(24, 28, 26, 28));
        card.getStyleClass().addAll("login-root", "login-card");
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("login-card-title");
        card.getChildren().add(titleLabel);
        return card;
    }

    private static Button styledButton(String text, String styleClass) {
        Button button = new Button(text);
        button.getStyleClass().add(styleClass);
        button.setMinWidth(100);
        return button;
    }

    private static Scene styledScene(VBox root, double width, double height) {
        Scene scene = new Scene(root, width, height);
        String stylesheet = UiDialogs.class.getResource("/sk/ukf/aisviewer/styles.css")
                .toExternalForm();
        scene.getStylesheets().add(stylesheet);
        return scene;
    }
}
