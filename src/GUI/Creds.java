package GUI;

import globalfuncs.creds;
import globalfuncs.db;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;

import java.sql.SQLException;

public class Creds extends VBox {

    // --- Palette (matches the rest of the app's light theme) ---
    private static final String BG          = "#F4F5F9";
    private static final String CARD        = "#FFFFFF";
    private static final String FIELD_BG    = "#F7F8FB";
    private static final String BORDER      = "#E1E5EC";
    private static final String ACCENT      = "#1C2333";
    private static final String ACCENT_DARK = "#2F58C4";
    private static final String ACCENT_BG   = "#EAF0FD";
    private static final String TEXT        = "#1C2230";
    private static final String MUTED       = "#6B7280";
    private static final String GREEN       = "#1E9E5A";
    private static final String RED         = "#D9434B";
    private static final String SHADOW      = "dropshadow(gaussian, rgba(28,34,48,0.06), 14, 0, 0, 3)";

    private ComboBox<String> profileBox;
    private TextField urlField, userField, initialsField;
    private PasswordField passField;
    private Label statusLabel;
    private Button save;

    public Creds() {
        setSpacing(20);
        setPadding(new Insets(28));
        setStyle("-fx-background-color: " + BG + ";");

        getChildren().addAll(buildHeader(), buildProfileBar(), buildFormCard(), buildActionRow());
        populateProfiles();
    }

    // -------------------------------------------------------------------------
    // Header
    // -------------------------------------------------------------------------

    private HBox buildHeader() {
        StackPane badge = iconBadge("\uD83D\uDD11", ACCENT_BG, ACCENT, 40);

        Label title = new Label("Credentials");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: " + TEXT + ";");

        Label subtitle = new Label("Manage saved database login profiles.");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: " + MUTED + ";");

        VBox titleBlock = new VBox(3, title, subtitle);
        titleBlock.setAlignment(Pos.CENTER_LEFT);

        HBox header = new HBox(14, badge, titleBlock);
        header.setAlignment(Pos.CENTER_LEFT);
        return header;
    }

    private StackPane iconBadge(String glyph, String bg, String fg, double size) {
        Circle circle = new Circle(size / 2);
        circle.setFill(Color.web(bg));

        Label glyphLabel = new Label(glyph);
        glyphLabel.setFont(Font.font("System", size * 0.46));
        glyphLabel.setTextFill(Color.web(fg));

        StackPane badge = new StackPane(circle, glyphLabel);
        badge.setMinSize(size, size);
        badge.setMaxSize(size, size);
        return badge;
    }

    // -------------------------------------------------------------------------
    // Profile bar
    // -------------------------------------------------------------------------

    private HBox buildProfileBar() {
        profileBox = new ComboBox<>();
        profileBox.setPrefHeight(38);
        HBox.setHgrow(profileBox, Priority.ALWAYS);
        profileBox.setMaxWidth(Double.MAX_VALUE);
        profileBox.setOnAction(e -> loadSelectedProfile());
        profileBox.setStyle(
                "-fx-background-color: " + FIELD_BG + ";" +
                        "-fx-border-color: " + BORDER + ";" +
                        "-fx-border-radius: 7;" +
                        "-fx-background-radius: 7;"
        );

        Button addBtn = iconButton("+");
        Button delBtn = iconButton("\u2715"); // ✕
        addBtn.setOnAction(e -> addProfile());
        delBtn.setOnAction(e -> deleteProfile());

        HBox row = new HBox(8, profileBox, addBtn, delBtn);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private Button iconButton(String text) {
        Button b = new Button(text);
        b.setPrefSize(38, 38);
        b.setStyle(
                "-fx-background-color: " + ACCENT_BG + ";" +
                        "-fx-text-fill: " + ACCENT + ";" +
                        "-fx-background-radius: 7;" +
                        "-fx-cursor: hand;" +
                        "-fx-font-size: 14px;" +
                        "-fx-font-weight: bold;"
        );
        return b;
    }

    // -------------------------------------------------------------------------
    // Form card
    // -------------------------------------------------------------------------

    private VBox buildFormCard() {
        urlField = field("jdbc:mysql://host:3306/db");
        userField = field("Username");
        passField = new PasswordField();
        passField.setPromptText("Password");
        styleField(passField);
        initialsField = field("e.g. JD");
        initialsField.textProperty().addListener((obs, o, n) -> {
            if (n != null && n.length() > 4) initialsField.setText(n.substring(0, 4));
        });

        Label cardTitle = new Label("Connection Details");
        cardTitle.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: " + TEXT + ";");
        Label cardSubtitle = new Label("Used when no SSH tunnel is active.");
        cardSubtitle.setStyle("-fx-font-size: 11px; -fx-text-fill: " + MUTED + ";");
        VBox cardHeading = new VBox(2, cardTitle, cardSubtitle);

        VBox fields = new VBox(12,
                row("Host URL", urlField),
                row("User", userField),
                row("Password", passField),
                row("Initials", initialsField)
        );

        VBox card = new VBox(16, cardHeading, fields);
        card.setPadding(new Insets(18));
        card.setStyle(
                "-fx-background-color: " + CARD + ";" +
                        "-fx-border-color: " + BORDER + ";" +
                        "-fx-border-radius: 12;" +
                        "-fx-background-radius: 12;" +
                        "-fx-effect: " + SHADOW + ";"
        );
        return card;
    }

    // -------------------------------------------------------------------------
    // Action row
    // -------------------------------------------------------------------------

    private HBox buildActionRow() {
        statusLabel = new Label();
        statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " + MUTED + ";");
        HBox.setHgrow(statusLabel, Priority.ALWAYS);

        save = new Button("Test & Save");
        save.setStyle(primaryStyle(ACCENT));
        save.setOnMouseEntered(e -> save.setStyle(primaryStyle(ACCENT_DARK)));
        save.setOnMouseExited(e -> save.setStyle(primaryStyle(ACCENT)));
        save.setOnAction(e -> testAndSave());

        HBox row = new HBox(12, statusLabel, save);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private String primaryStyle(String bg) {
        return "-fx-background-color: " + bg + ";" +
                "-fx-text-fill: white;" +
                "-fx-font-weight: bold;" +
                "-fx-background-radius: 8;" +
                "-fx-padding: 10 22;" +
                "-fx-cursor: hand;" +
                "-fx-font-size: 13;";
    }

    // -------------------------------------------------------------------------
    // Profile logic (unchanged)
    // -------------------------------------------------------------------------

    private void populateProfiles() {
        String[] names = creds.getAllProfileNames();
        profileBox.getItems().setAll(names);
        String last = creds.getLastUsedProfile();
        if (last != null && !last.isBlank()) {
            profileBox.setValue(last);
        } else if (names.length > 0) {
            profileBox.setValue(names[0]);
        }
        loadSelectedProfile();
    }

    private void loadSelectedProfile() {
        String sel = profileBox.getValue();
        if (sel == null || sel.isBlank()) return;
        String[] d = creds.loadProfile(sel); // [url, user, initials]
        urlField.setText(d[0]);
        userField.setText(d[1]);
        initialsField.setText(d[2]);
        passField.clear();
        statusLabel.setText("");
    }

    private void addProfile() {
        TextInputDialog dlg = new TextInputDialog();
        dlg.setTitle("New Profile");
        dlg.setHeaderText(null);
        dlg.setContentText("Profile name:");
        dlg.showAndWait().ifPresent(name -> {
            String n = name.trim();
            if (n.isEmpty()) return;
            if (!profileBox.getItems().contains(n)) profileBox.getItems().add(n);
            profileBox.setValue(n);
            urlField.clear(); userField.clear(); passField.clear(); initialsField.clear();
        });
    }

    private void deleteProfile() {
        String sel = profileBox.getValue();
        if (sel == null || sel.isBlank()) return;
        new Alert(Alert.AlertType.CONFIRMATION, "Delete \"" + sel + "\"?", ButtonType.YES, ButtonType.CANCEL)
                .showAndWait().ifPresent(b -> {
                    if (b != ButtonType.YES) return;
                    creds.removeProfile(sel);
                    profileBox.getItems().remove(sel);
                    if (!profileBox.getItems().isEmpty()) {
                        profileBox.setValue(profileBox.getItems().get(0));
                        loadSelectedProfile();
                    } else {
                        urlField.clear(); userField.clear(); passField.clear(); initialsField.clear();
                    }
                    status("Profile deleted.", false);
                });
    }

    private void testAndSave() {
        String url = trim(urlField.getText());
        String user = trim(userField.getText());
        String pass = passField.getText();
        String ini = trim(initialsField.getText());
        String prof = profileBox.getValue();

        if (url == null)  { status("URL cannot be empty.", true); return; }
        if (!url.startsWith("jdbc:mysql://")) { status("URL must start with jdbc:mysql://", true); return; }
        if (user == null) { status("Username cannot be empty.", true); return; }
        if (ini == null)  { status("Initials cannot be empty.", true); return; }
        if (!ini.matches("[A-Za-z]{1,4}")) { status("Initials: letters only, max 4.", true); return; }

        String prevUrl = creds.getUrl(), prevUser = creds.getUser(), prevPass = creds.getPass();
        creds.setUrl(url);
        creds.setUser(user);
        if (!pass.isEmpty()) creds.setPass(pass);

        if (connected()) {
            creds.setInitials(ini.toUpperCase());
            initialsField.setText(ini.toUpperCase());
            if (prof != null && !prof.isBlank()) creds.saveProfile(prof, url, user, ini.toUpperCase());
            passField.clear();
            status("Connected and saved.", false);
        } else {
            creds.setUrl(prevUrl); creds.setUser(prevUser); creds.setPass(prevPass);
            status("Connection failed — reverted.", true);
        }
    }

    private boolean connected() {
        try (var c = db.Connect()) { return c != null && !c.isClosed(); }
        catch (SQLException e) { return false; }
    }

    private String trim(String s) {
        if (s == null) return null;
        String t = s.strip();
        return t.isEmpty() ? null : t;
    }

    private void status(String msg, boolean error) {
        statusLabel.setText(msg);
        statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: " + (error ? RED : GREEN) + ";");
    }

    // -------------------------------------------------------------------------
    // Field helpers
    // -------------------------------------------------------------------------

    private TextField field(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        styleField(tf);
        return tf;
    }

    private void styleField(TextField tf) {
        tf.setStyle(
                "-fx-background-color: " + FIELD_BG + ";" +
                        "-fx-text-fill: " + TEXT + ";" +
                        "-fx-prompt-text-fill: #9AA3B2;" +
                        "-fx-border-color: " + BORDER + ";" +
                        "-fx-border-radius: 7;" +
                        "-fx-background-radius: 7;" +
                        "-fx-highlight-fill: " + ACCENT + ";" +
                        "-fx-padding: 8 10;" +
                        "-fx-font-size: 13px;"
        );
    }

    private HBox row(String label, TextField field) {
        Label lbl = new Label(label);
        lbl.setMinWidth(72);
        lbl.setStyle("-fx-font-size: 12px; -fx-text-fill: " + MUTED + ";");
        HBox.setHgrow(field, Priority.ALWAYS);
        HBox row = new HBox(10, lbl, field);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }
}