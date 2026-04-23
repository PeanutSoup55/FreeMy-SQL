package GUI;

import globalfuncs.creds;
import globalfuncs.db;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.sql.SQLException;

public class Creds extends VBox {

    private static final String GREEN = "#2F5230";
    private static final String GREEN_HOVER = "#3d6b40";
    private static final String BG_CARD = "#FFFFFF";
    private static final String BG_PAGE = "#EBEBEB";
    private static final String TEXT_MAIN = "#1a1a1a";
    private static final String TEXT_MUTED = "#6b7280";
    private static final String BORDER = "#e0e0e0";

    private Label userStatus;
    private Label passStatus;
    private Label urlStatus;
    private Label initialsStatus;

    public Creds() {
        setSpacing(12);
        setPadding(new Insets(24));
        setStyle("-fx-background-color: " + BG_PAGE + ";");

        Label pageTitle = new Label("Credentials");
        pageTitle.setStyle("-fx-font-size: 20px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: " + TEXT_MAIN + ";" +
                        "-fx-padding: 0 0 8 0;"
        );

        getChildren().addAll(pageTitle, UserBox(), PassBox(), URLBox(), InitialsBox());
    }

    public VBox UserBox() {
        Label userLabel = new Label(nullSafe(creds.getUser(), "—"));
        styleValueLabel(userLabel);

        userStatus = new Label();
        userStatus.setVisible(false);

        TextField changeUser = new TextField();
        styleTextField(changeUser, "New username");
        changeUser.textProperty().addListener((obs, o, n) -> userStatus.setVisible(false));

        Button enter = styledButton("Save");
        enter.setOnAction(e -> {
            String val = sanitize(changeUser.getText());
            if (val == null) { showStatus(userStatus, "Username cannot be empty.", true); return; }
            String prev = creds.getUser();
            creds.setUser(val);
            if (testConnection()) {
                userLabel.setText(val);
                changeUser.clear();
                showStatus(userStatus, "Connected as " + val + ".", false);
            } else {
                creds.setUser(prev);
                showStatus(userStatus, "Connection failed — reverted.", true);
            }
        });

        return buildCard("User", "./assets/user.png", userLabel, changeUser, enter, userStatus);
    }

    public VBox PassBox() {
        Label passLabel = new Label(mask(creds.getPass()));
        styleValueLabel(passLabel);

        passStatus = new Label();
        passStatus.setVisible(false);

        PasswordField changePass = new PasswordField();
        styleTextField(changePass, "New password");
        changePass.textProperty().addListener((obs, o, n) -> passStatus.setVisible(false));

        Button enter = styledButton("Save");
        enter.setOnAction(e -> {
            String val = changePass.getText();
            if (val == null || val.isEmpty()) { showStatus(passStatus, "Password cannot be empty.", true); return; }
            String prev = creds.getPass();
            creds.setPass(val);
            if (testConnection()) {
                passLabel.setText(mask(val));
                changePass.clear();
                showStatus(passStatus, "Password updated.", false);
            } else {
                creds.setPass(prev);
                showStatus(passStatus, "Connection failed — reverted.", true);
            }
        });

        return buildCard("Password", "./assets/lock.png", passLabel, changePass, enter, passStatus);
    }

    public VBox URLBox() {
        Label urlLabel = new Label(nullSafe(creds.getUrl(), "—"));
        styleValueLabel(urlLabel);
        urlLabel.setWrapText(true);

        urlStatus = new Label();
        urlStatus.setVisible(false);

        TextField changeUrl = new TextField();
        styleTextField(changeUrl, "jdbc:mysql://host:3306/db");
        changeUrl.textProperty().addListener((obs, o, n) -> urlStatus.setVisible(false));

        Button enter = styledButton("Save");
        enter.setOnAction(e -> {
            String val = sanitize(changeUrl.getText());
            if (val == null) { showStatus(urlStatus, "URL cannot be empty.", true); return; }
            if (!isValidJdbcUrl(val)) { showStatus(urlStatus, "Must start with jdbc:mysql://", true); return; }
            String prev = creds.getUrl();
            creds.setUrl(val);
            if (testConnection()) {
                urlLabel.setText(val);
                changeUrl.clear();
                showStatus(urlStatus, "Connected to new host.", false);
            } else {
                creds.setUrl(prev);
                showStatus(urlStatus, "Connection failed — reverted.", true);
            }
        });

        return buildCard("Host URL", "./assets/link.png", urlLabel, changeUrl, enter, urlStatus);
    }

    public VBox InitialsBox() {
        Label initialsLabel = new Label(nullSafe(creds.getInitials(), "—"));
        styleValueLabel(initialsLabel);

        initialsStatus = new Label();
        initialsStatus.setVisible(false);

        TextField changeInitials = new TextField();
        styleTextField(changeInitials, "e.g. JD");
        changeInitials.textProperty().addListener((obs, o, n) -> {
            initialsStatus.setVisible(false);
            if (n != null && n.length() > 4)
                changeInitials.setText(n.substring(0, 4));
        });

        Button enter = styledButton("Save");
        enter.setOnAction(e -> {
            String val = sanitize(changeInitials.getText());
            if (val == null) { showStatus(initialsStatus, "Initials cannot be empty.", true); return; }
            if (val.length() > 4) { showStatus(initialsStatus, "Max 4 characters.", true); return; }
            if (!val.matches("[A-Za-z]+")) { showStatus(initialsStatus, "Letters only.", true); return; }
            creds.setInitials(val.toUpperCase());
            initialsLabel.setText(val.toUpperCase());
            changeInitials.clear();
            showStatus(initialsStatus, "Initials saved.", false);
        });

        return buildCard("Initials", "./assets/id.png", initialsLabel, changeInitials, enter, initialsStatus);
    }

    private VBox buildCard(String title, String iconPath, Label currentValue, TextField input, Button saveBtn, Label status) {
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        try {
            ImageView iv = new ImageView(new Image(iconPath));
            iv.setFitWidth(16);
            iv.setFitHeight(16);
            header.getChildren().add(iv);
        } catch (Exception ignored) { /* asset missing — skip icon */ }

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 11px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-text-fill: " + TEXT_MUTED + ";" +
                        "-fx-letter-spacing: 0.05em;"
        );
        header.getChildren().add(titleLabel);

        HBox inputRow = new HBox(8);
        inputRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(input, Priority.ALWAYS);
        inputRow.getChildren().addAll(input, saveBtn);

        VBox card = new VBox(8);
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color: " + BG_CARD + ";" +
                        "-fx-background-radius: 10;" +
                        "-fx-border-color: " + BORDER + ";" +
                        "-fx-border-radius: 10;" +
                        "-fx-border-width: 1;"
        );
        card.getChildren().addAll(header, currentValue, inputRow, status);
        return card;
    }

    private Button styledButton(String text) {
        Button btn = new Button(text);
        btn.setStyle("-fx-background-color: " + GREEN + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-font-size: 13px;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 7;" +
                        "-fx-padding: 8 18 8 18;" +
                        "-fx-cursor: hand;"
        );
        btn.setOnMouseEntered(e -> btn.setStyle(btn.getStyle().replace(GREEN, GREEN_HOVER)));
        btn.setOnMouseExited(e -> btn.setStyle(btn.getStyle().replace(GREEN_HOVER, GREEN)));
        return btn;
    }

    private void styleTextField(TextField tf, String prompt) {
        tf.setPromptText(prompt);
        tf.setStyle("-fx-background-color: #f9f9f9;" +
                        "-fx-border-color: " + BORDER + ";" +
                        "-fx-border-radius: 7;" +
                        "-fx-background-radius: 7;" +
                        "-fx-padding: 8 12 8 12;" +
                        "-fx-font-size: 13px;" +
                        "-fx-text-fill: " + TEXT_MAIN + ";"
        );
        tf.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            String base = "-fx-background-color: #f9f9f9;" +
                            "-fx-border-radius: 7;" +
                            "-fx-background-radius: 7;" +
                            "-fx-padding: 8 12 8 12;" +
                            "-fx-font-size: 13px;" +
                            "-fx-text-fill: " + TEXT_MAIN + ";";
            tf.setStyle(base + (isFocused ? "-fx-border-color: " + GREEN + "; -fx-border-width: 1.5;" : "-fx-border-color: " + BORDER + "; -fx-border-width: 1;"));
        });
    }

    private void styleValueLabel(Label label) {
        label.setStyle("-fx-font-size: 13px;" +
                        "-fx-text-fill: " + TEXT_MAIN + ";" +
                        "-fx-font-family: monospace;"
        );
    }

    private boolean testConnection() {
        try (var conn = db.Connect()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    private String sanitize(String raw) {
        if (raw == null) return null;
        String trimmed = raw.strip();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String nullSafe(String value, String fallback) {
        return (value != null && !value.isBlank()) ? value : fallback;
    }

    private String mask(String pass) {
        if (pass == null || pass.isEmpty()) return "—";
        return "•".repeat(Math.min(pass.length(), 12));
    }

    private boolean isValidJdbcUrl(String url) {
        return url != null && url.startsWith("jdbc:mysql://");
    }

    private void showStatus(Label label, String message, boolean isError) {
        label.setText(message);
        label.setStyle(
                "-fx-font-size: 11px;" +
                        "-fx-text-fill: " + (isError ? "#cc3333" : "#2a7a2a") + ";"
        );
        label.setVisible(true);
    }
}