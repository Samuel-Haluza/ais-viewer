package sk.ukf.aisviewer.controller;

import javafx.beans.binding.Bindings;
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

import java.util.List;
import java.util.Optional;
import sk.ukf.aisviewer.service.GradeChanges;
import sk.ukf.aisviewer.service.ThemeManager;

/**
 * Jednoduché modálne okná aplikácie. Okno sa prispôsobí obsahu (žiadna pevná výška),
 * tlačidlá sú vpravo: sekundárne (Zrušiť) vľavo od primárneho.
 */
final class UiDialogs {

    private static final double TEXT_WIDTH = 380;
    /** Najviac toľko riadkov v okne s novými hodnoteniami, aby sa zmestilo aj pri zväčšení 150 %. */
    static final int MAX_GRADE_CHANGES = 10;

    private UiDialogs() {
    }

    static boolean showFirstLoginNotice(Window owner) {
        return showNotice(owner, "Prvé načítanie údajov",
                "Aplikácia sa prihlási do AIS a načíta tvoje predmety, skúšky a rozvrh "
                        + "zo všetkých ročníkov. Prvé načítanie trvá zvyčajne 1 až 2 minúty.\n\n"
                        + "Počas načítavania aplikáciu nezatváraj.",
                "Pokračovať", "Zrušiť", false);
    }

    static boolean showRefreshNotice(Window owner) {
        return showNotice(owner, "Obnoviť údaje z AIS",
                "Aplikácia sa znova pripojí k AIS a načíta aktuálne údaje. Môže to chvíľu trvať.\n\n"
                        + "Heslo sa neukladá na disk, takže po reštarte aplikácie "
                        + "ho treba zadať znova.",
                "Obnoviť", "Zrušiť", false);
    }

    /** Po obnovení údajov: zoznam nových alebo zmenených hodnotení. */
    static void showGradeChanges(Window owner, List<GradeChanges.Change> changes) {
        StringBuilder text = new StringBuilder();
        // okno nemá posuvník – pri veľa zmenách by tlačidlo Zavrieť skončilo mimo obrazovky
        int shown = Math.min(changes.size(), MAX_GRADE_CHANGES);
        for (GradeChanges.Change change : changes.subList(0, shown)) {
            text.append(change.subjectName()).append(": ").append(change.newGrade());
            text.append(change.isNew() ? "  (nové)" : "  (predtým " + change.oldGrade() + ")");
            text.append(System.lineSeparator());
        }
        if (changes.size() > shown) {
            text.append("…a ďalšie ").append(changes.size() - shown)
                    .append(". Všetky známky nájdeš v záložke Predmety.");
        }
        showNotice(owner, changes.size() == 1 ? "Nové hodnotenie" : "Nové hodnotenia (" + changes.size() + ")",
                text.toString().trim(), "Zavrieť", null, false);
    }

    static void showInvalidPassword(Window owner) {
        showNotice(owner, "Prihlásenie sa nepodarilo",
                "AIS heslo neprijal. Skontroluj ho a skús to znova.",
                "Zadať znova", null, true);
    }

    static void showExportSuccess(Window owner, String message) {
        showNotice(owner, "Export je hotový", message, "Zavrieť", null, false);
    }

    /** @return true, ak chce používateľ export skúsiť znova */
    static boolean showExportError(Window owner, String message) {
        return showNotice(owner, "Export sa nepodaril", message, "Skúsiť znova", "Zavrieť", true);
    }

    /** Chyba pri príprave údajov – opakovanie by nepomohlo, preto len Zavrieť. */
    static void showExportDataError(Window owner, String message) {
        showNotice(owner, "Export sa nepodaril", message, "Zavrieť", null, true);
    }

    static Optional<String> showPasswordPrompt(Window owner) {
        Stage stage = createStage(owner, "Heslo do AIS");
        VBox root = createRoot("Heslo do AIS");
        Label explanation = textLabel(
                "Na obnovenie údajov potrebuje aplikácia tvoje heslo do AIS. "
                        + "Ostane len v pamäti, kým je aplikácia otvorená, na disk sa neukladá.", false);

        PasswordField passwordField = new PasswordField();
        passwordField.setAccessibleText("Heslo do AIS");
        passwordField.getStyleClass().add("login-field");

        Button continueButton = styledButton("Obnoviť", "primary-button");
        continueButton.setDefaultButton(true);
        // rovnaká podmienka ako v akcii nižšie – heslo len z medzier tlačidlo nepovolí
        continueButton.disableProperty().bind(Bindings.createBooleanBinding(
                () -> passwordField.getText() == null || passwordField.getText().isBlank(),
                passwordField.textProperty()));
        Button cancelButton = styledButton("Zrušiť", "secondary-button");
        cancelButton.setCancelButton(true);
        root.getChildren().addAll(explanation, passwordField, buttonRow(cancelButton, continueButton));
        stage.setScene(styledScene(root));

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
                                      String confirmText, String cancelText, boolean error) {
        Stage stage = createStage(owner, title);
        VBox root = createRoot(title);
        Label messageLabel = textLabel(message, error);

        Button confirmButton = styledButton(confirmText, "primary-button");
        confirmButton.setDefaultButton(true);
        HBox buttons = buttonRow(confirmButton);
        if (cancelText != null) {
            Button cancelButton = styledButton(cancelText, "secondary-button");
            cancelButton.setCancelButton(true);
            cancelButton.setOnAction(event -> stage.close());
            buttons.getChildren().add(0, cancelButton);
        }

        root.getChildren().addAll(messageLabel, buttons);
        stage.setScene(styledScene(root));
        final boolean[] confirmed = {false};
        confirmButton.setOnAction(event -> {
            confirmed[0] = true;
            stage.close();
        });
        stage.setOnShown(event -> confirmButton.requestFocus());
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

    private static VBox createRoot(String title) {
        VBox root = new VBox(12);
        root.setAlignment(Pos.TOP_LEFT);
        root.setPadding(new Insets(20, 24, 20, 24));
        root.getStyleClass().add("dialog-root");
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("dialog-title");
        root.getChildren().add(titleLabel);
        return root;
    }

    private static Label textLabel(String text, boolean error) {
        Label label = new Label(text);
        label.getStyleClass().add("dialog-text");
        if (error) label.getStyleClass().add("dialog-text-error");
        label.setWrapText(true);
        label.setPrefWidth(TEXT_WIDTH);
        label.setMinHeight(Label.USE_PREF_SIZE);
        return label;
    }

    private static HBox buttonRow(Button... buttons) {
        HBox row = new HBox(8, buttons);
        row.setAlignment(Pos.CENTER_RIGHT);
        row.setPadding(new Insets(8, 0, 0, 0));
        return row;
    }

    private static Button styledButton(String text, String styleClass) {
        Button button = new Button(text);
        button.getStyleClass().add(styleClass);
        button.setMinWidth(Button.USE_PREF_SIZE);
        return button;
    }

    /** Scéna bez pevnej veľkosti – okno sa prispôsobí obsahu. */
    private static Scene styledScene(VBox root) {
        Scene scene = new Scene(root);
        String stylesheet = UiDialogs.class.getResource("/sk/ukf/aisviewer/styles.css")
                .toExternalForm();
        scene.getStylesheets().add(stylesheet);
        ThemeManager.getInstance().applyTheme(scene);
        return scene;
    }
}
