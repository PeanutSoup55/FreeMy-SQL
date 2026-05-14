package GUI;

import globalfuncs.creds;
import globalfuncs.db;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.sql.SQLException;

public class Creds extends VBox {

    private static final String GREEN = "#2F5230";
    private static final String GREEN_HOVER = "#3d6b40";
    private static final String TEXT_MAIN = "#1a1a1a";
    private static final String TEXT_MUTED = "#6b7280";
    private static final String BORDER = "#e0e0e0";

    private ComboBox<String> profileBox;
    private TextField urlField, userField, initialsField;
    private PasswordField passField;
    private Label statusLabel;

    public Creds() {
        setSpacing(14);
        setPadding(new Insets(24));
        setStyle("-fx-background-color: transparent;");

        Label title = new Label("Credentials");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: " + TEXT_MAIN + ";");

        getChildren().addAll(title, buildProfileBar(), buildFormCard(), buildActionRow());
        populateProfiles();
    }

    private HBox buildProfileBar() {
        profileBox = new ComboBox<>();
        profileBox.setPrefHeight(36);
        HBox.setHgrow(profileBox, Priority.ALWAYS);
        profileBox.setMaxWidth(Double.MAX_VALUE);
        profileBox.setOnAction(e -> loadSelectedProfile());

        Button addBtn = new Button("+");
        Button delBtn = new Button("X");
        for (Button b : new Button[]{addBtn, delBtn}) {
            b.setPrefSize(36, 36);
            b.setStyle("-fx-background-color: white; -fx-border-color: " + BORDER + "; -fx-border-radius: 7; -fx-background-radius: 7; -fx-cursor: hand; -fx-font-size: 14px;");
        }
        addBtn.setOnAction(e -> addProfile());
        delBtn.setOnAction(e -> deleteProfile());

        HBox row = new HBox(8, profileBox, addBtn, delBtn);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

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

        VBox card = new VBox(12,
                row("Host URL", urlField),
                row("User", userField),
                row("Password", passField),
                row("Initials", initialsField)
        );
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color: white; -fx-border-color: " + BORDER + "; -fx-border-radius: 10; -fx-background-radius: 10;");
        return card;
    }

    private HBox buildActionRow() {
        statusLabel = new Label();
        statusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + TEXT_MUTED + ";");
        HBox.setHgrow(statusLabel, Priority.ALWAYS);

        Button save = new Button("Test & Save");
        save.setStyle("-fx-background-color: " + GREEN + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-padding: 9 20; -fx-cursor: hand;");
        save.setOnMouseEntered(e -> save.setStyle(save.getStyle().replace(GREEN, GREEN_HOVER)));
        save.setOnMouseExited(e ->  save.setStyle(save.getStyle().replace(GREEN_HOVER, GREEN)));
        save.setOnAction(e -> testAndSave());

        HBox row = new HBox(12, statusLabel, save);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

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
        statusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " + (error ? "#cc3333" : "#2a7a2a") + ";");
    }

    private TextField field(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        styleField(tf);
        return tf;
    }

    private void styleField(TextField tf) {
        tf.setStyle("-fx-background-color: #f9f9f9; -fx-border-color: " + BORDER + "; -fx-border-radius: 7; -fx-background-radius: 7; -fx-padding: 7 10; -fx-font-size: 13px;");
    }

    private HBox row(String label, TextField field) {
        Label lbl = new Label(label);
        lbl.setMinWidth(72);
        lbl.setStyle("-fx-font-size: 12px; -fx-text-fill: " + TEXT_MUTED + ";");
        HBox.setHgrow(field, Priority.ALWAYS);
        HBox row = new HBox(10, lbl, field);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }
}