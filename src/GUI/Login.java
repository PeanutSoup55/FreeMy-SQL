package GUI;

import globalfuncs.creds;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.BoxBlur;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.Optional;

public class Login extends HBox {

    private ComboBox<String> profileComboBox;
    private TextField hostField;
    private TextField portField;
    private TextField nameField;
    private TextField passField;
    private TextField initialsField;
    private CheckBox rememberMeCheck;

    // ── Account auth state ───────────────────────────────────────────
    private TextField accountEmailField;
    private PasswordField accountPasswordField;
    private PasswordField accountConfirmField;
    private VBox confirmGroup;
    private Text accountError;
    private Button accountSubmitBtn;
    private Hyperlink switchModeLink;
    private boolean signUpMode = false;

    private StackPane formStack;
    private VBox accountPane;
    private VBox mysqlPane;

    private static final double FLY_DISTANCE = 900; // comfortably larger than any realistic window height

    public Login(Stage stage) {
        stage.setWidth(1700);
        stage.setHeight(1000);
        stage.show();
        createLoginPage();
        populateProfiles();
    }

    public void createLoginPage() {

        //left
        Pane circleLayer = new Pane();
        circleLayer.setMinWidth(0);
        circleLayer.setMinHeight(0);

        Circle c1 = new Circle(900, Color.web("#7dbba3"));
        Circle c2 = new Circle(450, Color.web("#3e8e75"));
        Circle c3 = new Circle(350, Color.web("#1a3a31"));
        Circle c4 = new Circle(250, Color.web("#091413"));

        BoxBlur blur = new BoxBlur(40, 40, 3);
        c1.setEffect(blur);
        c2.setEffect(blur);
        c3.setEffect(blur);
        c4.setEffect(blur);

        double offset = 100;

        for (Circle c : new Circle[]{c1, c2, c3, c4}) {
            c.centerXProperty().bind(circleLayer.widthProperty().divide(2).add(offset));
            c.centerYProperty().bind(circleLayer.heightProperty().divide(2));
        }

        StackPane labelHolder = new StackPane();
        labelHolder.setPickOnBounds(false);
        labelHolder.layoutXProperty().bind(circleLayer.widthProperty().divide(2).add(offset).subtract(150));
        labelHolder.layoutYProperty().bind(circleLayer.heightProperty().divide(2).subtract(50));
        labelHolder.setPrefSize(300, 100);

        Text logoTxt = new Text("Free My\nSQL");
        logoTxt.setFont(Font.font("System", FontWeight.BOLD, 40));
        logoTxt.setFill(Color.WHITE);
        logoTxt.setTextAlignment(TextAlignment.CENTER);
        StackPane.setAlignment(logoTxt, Pos.CENTER);

        labelHolder.getChildren().add(logoTxt);

        Rectangle circleClip = new Rectangle();
        circleClip.widthProperty().bind(circleLayer.widthProperty().add(20));
        circleClip.heightProperty().bind(circleLayer.heightProperty());
        circleClip.setTranslateX(-40);
        circleClip.setArcHeight(80);
        circleClip.setArcWidth(80);
        circleLayer.setClip(circleClip);

        circleLayer.getChildren().addAll(c1, c2, c3, c4, labelHolder);
        HBox.setHgrow(circleLayer, Priority.ALWAYS);

        //right
        VBox rightSide = new VBox();
        rightSide.setAlignment(Pos.CENTER);
        rightSide.setPadding(new Insets(60, 50, 60, 50));
        rightSide.setMinWidth(0);
        rightSide.setMinHeight(0);
        HBox.setHgrow(rightSide, Priority.ALWAYS);

        accountPane = buildAccountPane();
        mysqlPane   = buildMysqlPane();

        // mysqlPane starts below the fold, waiting to fly in; accountPane starts in place
        mysqlPane.setTranslateY(FLY_DISTANCE);
        mysqlPane.setOpacity(0);
        mysqlPane.setMouseTransparent(true);

        formStack = new StackPane(mysqlPane, accountPane); // mysql behind, account on top initially
        formStack.setAlignment(Pos.CENTER);

        Rectangle stackClip = new Rectangle();
        stackClip.widthProperty().bind(formStack.widthProperty());
        stackClip.heightProperty().bind(formStack.heightProperty());
        formStack.setClip(stackClip);

        VBox.setVgrow(formStack, Priority.ALWAYS);
        rightSide.getChildren().add(formStack);

        this.setMinWidth(0);
        this.setMinHeight(0);
        this.getChildren().addAll(circleLayer, rightSide);

        this.widthProperty().addListener((obs, oldVal, newVal) -> {
            double half = newVal.doubleValue() / 2.0;
            circleLayer.setPrefWidth(half);
            circleLayer.setMaxWidth(half);
            rightSide.setPrefWidth(half);
            rightSide.setMaxWidth(half);
        });
    }

    // ── Account sign in / sign up pane ───────────────────────────────
    private VBox buildAccountPane() {
        Text title = new Text("Sign in to your account");
        title.setFont(Font.font("System", FontWeight.BOLD, 20));
        title.setFill(Color.web("#1a1a1a"));

        VBox emailGroup = createFieldGroup("Email", "you@example.com", false);
        accountEmailField = (TextField) emailGroup.getChildren().get(1);

        VBox passwordGroup = createFieldGroup("Password", "Password", true);
        accountPasswordField = (PasswordField) passwordGroup.getChildren().get(1);

        confirmGroup = createFieldGroup("Confirm Password", "Confirm password", true);
        accountConfirmField = (PasswordField) confirmGroup.getChildren().get(1);
        confirmGroup.setVisible(false);
        confirmGroup.setManaged(false);

        accountError = new Text();
        accountError.setFont(Font.font("System", 12));
        accountError.setFill(Color.web("#c0392b"));
        accountError.setWrappingWidth(300);

        accountSubmitBtn = new Button("Sign In");
        accountSubmitBtn.setPrefWidth(160);
        styleAsPrimary(accountSubmitBtn);
        accountSubmitBtn.setOnAction(e -> handleAccountSubmit());

        accountPasswordField.setOnAction(e -> accountSubmitBtn.fire());
        accountConfirmField.setOnAction(e -> accountSubmitBtn.fire());

        HBox btnRow = new HBox(accountSubmitBtn);
        btnRow.setAlignment(Pos.CENTER);
        btnRow.setMaxWidth(300);

        switchModeLink = new Hyperlink("Need an account? Sign Up");
        switchModeLink.setFont(Font.font("System", 12));
        switchModeLink.setOnAction(e -> toggleAuthMode());

        HBox linkRow = new HBox(switchModeLink);
        linkRow.setAlignment(Pos.CENTER);
        linkRow.setMaxWidth(300);

        VBox pane = new VBox(18, title, emailGroup, passwordGroup, confirmGroup, accountError, btnRow, linkRow);
        pane.setAlignment(Pos.CENTER);
        pane.setMaxWidth(300);
        return pane;
    }

    private void toggleAuthMode() {
        signUpMode = !signUpMode;
        accountError.setText("");
        confirmGroup.setVisible(signUpMode);
        confirmGroup.setManaged(signUpMode);
        accountSubmitBtn.setText(signUpMode ? "Create Account" : "Sign In");
        switchModeLink.setText(signUpMode
                ? "Already have an account? Sign In"
                : "Need an account? Sign Up");
    }

    private void handleAccountSubmit() {
        String email = accountEmailField.getText().trim();
        String password = accountPasswordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            accountError.setText("Please fill in all fields.");
            return;
        }
        if (signUpMode && !password.equals(accountConfirmField.getText())) {
            accountError.setText("Passwords do not match.");
            return;
        }

        accountSubmitBtn.setDisable(true);
        accountError.setText("");

        // ─────────────────────────────────────────────────────────────
        // TODO: replace this stub with a real call to your licensing
        // backend once it exists (e.g. Supabase auth + subscription check).
        // Suggested shape:
        //
        //   AuthClient.authenticate(email, password, signUpMode)
        //       .thenAccept(result -> Platform.runLater(() -> {
        //           accountSubmitBtn.setDisable(false);
        //           if (result.success()) {
        //               LicenseStore.save(result.token(), result.expiresAt());
        //               playFormTransition();
        //           } else {
        //               accountError.setText(result.message());
        //           }
        //       }));
        //
        // For now, every attempt succeeds so the UI/animation can be tested.
        boolean success = true;
        // ─────────────────────────────────────────────────────────────

        if (success) {
            accountSubmitBtn.setDisable(false);
            playFormTransition();
        } else {
            accountSubmitBtn.setDisable(false);
            accountError.setText("Invalid email or password.");
        }
    }

    private void playFormTransition() {
        accountPane.setMouseTransparent(true);
        mysqlPane.setMouseTransparent(false);

        double flyDistance = formStack.getHeight() > 0 ? formStack.getHeight() + 60 : FLY_DISTANCE;

        // Pronounced ease-in-out — steeper than EASE_BOTH's gentle default curve
        Interpolator strongEase = Interpolator.SPLINE(0.65, 0, 0.35, 1);

        TranslateTransition accountUp = new TranslateTransition(Duration.millis(750), accountPane);
        accountUp.setToY(-flyDistance);
        accountUp.setInterpolator(strongEase);

        FadeTransition accountFade = new FadeTransition(Duration.millis(750), accountPane);
        accountFade.setToValue(0);
        accountFade.setInterpolator(strongEase);

        TranslateTransition mysqlIn = new TranslateTransition(Duration.millis(750), mysqlPane);
        mysqlIn.setFromY(flyDistance);
        mysqlIn.setToY(0);
        mysqlIn.setInterpolator(strongEase);

        // Fade finishes at 60% of the glide, so it's fully solid while still decelerating into place
        FadeTransition mysqlFade = new FadeTransition(Duration.millis(450), mysqlPane);
        mysqlFade.setToValue(1);
        mysqlFade.setInterpolator(strongEase);

        ParallelTransition transition = new ParallelTransition(accountUp, accountFade, mysqlIn, mysqlFade);
        transition.setOnFinished(e -> {
            accountPane.setVisible(false);
            accountPane.setManaged(false);
        });
        transition.play();
    }

    // ── MySQL local connection pane (unchanged content, extracted) ──
    private VBox buildMysqlPane() {
        VBox rightSide = new VBox(20);
        rightSide.setAlignment(Pos.CENTER);
        rightSide.setMaxWidth(300);

        Text title = new Text("Login with local Credentials");
        title.setFont(Font.font("System", FontWeight.BOLD, 20));
        title.setFill(Color.web("#1a1a1a"));

        VBox profileGroup = new VBox(5);
        profileGroup.setMaxWidth(300);
        Text profileLabel = new Text("Connection Profile");
        profileLabel.setFont(Font.font("System", 13));
        profileLabel.setFill(Color.web("#1a1a1a"));

        profileComboBox = new ComboBox<>();
        profileComboBox.setEditable(false);
        profileComboBox.setPrefHeight(40);
        HBox.setHgrow(profileComboBox, Priority.ALWAYS);
        profileComboBox.setMaxWidth(Double.MAX_VALUE);

        profileComboBox.getStylesheets().add("data:text/css," +
                ".combo-box { -fx-background-color: white; -fx-border-color: #d1d1d1; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 0 5; -fx-font-size: 13px; -fx-focus-color: transparent; -fx-faint-focus-color: transparent; }" +
                ".combo-box .list-cell { -fx-text-fill: #1a1a1a; -fx-background-color: white; -fx-padding: 8 12; }" +
                ".combo-box .list-view .list-cell:hover { -fx-background-color: #f0f0f0; }" +
                ".combo-box .arrow-button { -fx-background-color: transparent; -fx-padding: 0 10 0 0; }" +
                ".combo-box .arrow { -fx-background-color: #707070; -fx-shape: 'M 0 0 L 4 4 L 8 0 Z'; }"
        );

        Button addProfileBtn = new Button("+");
        addProfileBtn.setPrefHeight(40);
        addProfileBtn.setPrefWidth(40);
        addProfileBtn.setStyle(
                "-fx-background-color: white; -fx-text-fill: #1a1a1a; -fx-font-size: 16px; -fx-font-weight: bold;" +
                        "-fx-border-color: #d1d1d1; -fx-border-radius: 8; -fx-background-radius: 8; -fx-cursor: hand;"
        );
        addProfileBtn.setOnMouseEntered(e -> addProfileBtn.setStyle(
                "-fx-background-color: #f0f0f0; -fx-text-fill: #1a1a1a; -fx-font-size: 16px; -fx-font-weight: bold;" +
                        "-fx-border-color: #b5b5b5; -fx-border-radius: 8; -fx-background-radius: 8; -fx-cursor: hand;"
        ));
        addProfileBtn.setOnMouseExited(e -> addProfileBtn.setStyle(
                "-fx-background-color: white; -fx-text-fill: #1a1a1a; -fx-font-size: 16px; -fx-font-weight: bold;" +
                        "-fx-border-color: #d1d1d1; -fx-border-radius: 8; -fx-background-radius: 8; -fx-cursor: hand;"
        ));

        addProfileBtn.setOnAction(e -> {
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("New Connection Profile");
            dialog.setHeaderText("Create a new profile");
            dialog.setContentText("Profile Name:");

            Optional<String> result = dialog.showAndWait();
            result.ifPresent(name -> {
                String cleanName = name.trim();
                if (!cleanName.isEmpty()) {
                    if (!profileComboBox.getItems().contains(cleanName)) {
                        profileComboBox.getItems().add(cleanName);
                    }
                    profileComboBox.setValue(cleanName);
                    hostField.clear();
                    portField.clear();
                    nameField.clear();
                    passField.clear();
                    initialsField.clear();
                    hostField.requestFocus();
                }
            });
        });

        HBox profileRow = new HBox(10, profileComboBox, addProfileBtn);
        profileRow.setMaxWidth(300);
        profileRow.setAlignment(Pos.CENTER_LEFT);
        profileGroup.getChildren().addAll(profileLabel, profileRow);

        VBox hostGroup = createFieldGroup("Host", "localhost", false);
        hostField = (TextField) hostGroup.getChildren().get(1);
        VBox portGroup = createFieldGroup("Port", "3306", false);
        portField = (TextField) portGroup.getChildren().get(1);
        VBox nameGroup = createFieldGroup("MySQL Username", "Value", false);
        nameField = (TextField) nameGroup.getChildren().get(1);
        VBox passGroup = createFieldGroup("MySQL Password", "Value", false);
        passField = (TextField) passGroup.getChildren().get(1);
        VBox initialsGroup = createFieldGroup("User Initials", "Value", false);
        initialsField = (TextField) initialsGroup.getChildren().get(1);

        rememberMeCheck = new CheckBox("Remember details for this profile");
        rememberMeCheck.setFont(Font.font("System", 13));
        rememberMeCheck.setTextFill(Color.web("#1a1a1a"));
        rememberMeCheck.setMaxWidth(300);
        rememberMeCheck.getStylesheets().add("data:text/css," +
                ".check-box { -fx-cursor: hand; }" +
                ".check-box .box { -fx-background-color: white; -fx-border-color: #d1d1d1; -fx-border-radius: 4; -fx-background-radius: 4; }" +
                ".check-box:hover .box { -fx-border-color: #7dbba3; }" +
                ".check-box:selected .box { -fx-background-color: #262626; -fx-border-color: #262626; }" +
                ".check-box:selected .mark { -fx-background-color: white; }"
        );

        profileComboBox.setOnAction(e -> {
            String selectedProfile = profileComboBox.getValue();
            if (selectedProfile != null && !selectedProfile.isEmpty()) {
                String[] details = creds.loadProfile(selectedProfile);
                parseUrlIntoFields(details[0]);
                nameField.setText(details[1]);
                initialsField.setText(details[2]);
            }
        });

        Button connectBtn = new Button("Connect");

        hostField.setOnAction(e -> connectBtn.fire());
        portField.setOnAction(e -> connectBtn.fire());
        nameField.setOnAction(e -> connectBtn.fire());
        passField.setOnAction(e -> connectBtn.fire());
        initialsField.setOnAction(e -> connectBtn.fire());

        connectBtn.setPrefWidth(160);
        styleAsPrimary(connectBtn);

        connectBtn.setOnAction(e -> {
            String host = hostField.getText().trim().isEmpty() ? "localhost" : hostField.getText().trim();
            String port = portField.getText().trim().isEmpty() ? "3306" : portField.getText().trim();
            String builtUrl = "jdbc:mysql://" + host + ":" + port + "/?allowMultiQueries=true&useSSL=false&allowPublicKeyRetrieval=true";

            String activeProfile = profileComboBox.getValue();
            if (activeProfile == null || activeProfile.trim().isEmpty()) {
                activeProfile = "Default Profile";
            }
            creds.setRememberMe(rememberMeCheck.isSelected());
            if (rememberMeCheck.isSelected()) {
                creds.saveProfile(activeProfile, builtUrl, nameField.getText(), initialsField.getText());
            } else {
                creds.removeProfile(activeProfile);
            }

            enterCreds(builtUrl, nameField.getText(), passField.getText(), initialsField.getText());
            Root root = new Root();
            Scene newScene = new Scene(root, 1700, 1000, Color.web("#F9F9F9"));
            Stage stage = (Stage) connectBtn.getScene().getWindow();
            stage.setScene(newScene);
        });

        HBox btnRow = new HBox(connectBtn);
        btnRow.setAlignment(Pos.CENTER);
        btnRow.setMaxWidth(300);

        rightSide.getChildren().addAll(title, profileGroup, hostGroup, portGroup, nameGroup, passGroup, initialsGroup, rememberMeCheck, btnRow);
        return rightSide;
    }

    private void styleAsPrimary(Button btn) {
        btn.setStyle(
                "-fx-background-color: #262626; -fx-text-fill: white; -fx-font-size: 13px;" +
                        "-fx-background-radius: 10; -fx-border-radius: 10; -fx-padding: 10 40; -fx-cursor: hand;"
        );
        btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color: #404040; -fx-text-fill: white; -fx-font-size: 13px;" +
                        "-fx-background-radius: 10; -fx-border-radius: 10; -fx-padding: 10 40; -fx-cursor: hand;"
        ));
        btn.setOnMouseExited(e -> btn.setStyle(
                "-fx-background-color: #262626; -fx-text-fill: white; -fx-font-size: 13px;" +
                        "-fx-background-radius: 10; -fx-border-radius: 10; -fx-padding: 10 40; -fx-cursor: hand;"
        ));
    }

    private void parseUrlIntoFields(String storedUrl) {
        if (storedUrl == null || storedUrl.isEmpty()) {
            hostField.clear();
            portField.clear();
            return;
        }
        try {
            String stripped = storedUrl.replace("jdbc:mysql://", "");
            String hostPort = stripped.split("/")[0];
            String[] parts = hostPort.split(":");
            hostField.setText(parts[0]);
            portField.setText(parts.length > 1 ? parts[1] : "3306");
        } catch (Exception ex) {
            hostField.clear();
            portField.clear();
        }
    }

    private void populateProfiles() {
        String[] profiles = creds.getAllProfileNames();
        profileComboBox.getItems().setAll(profiles);

        boolean globalRememberState = creds.isRememberMeEnabled();
        rememberMeCheck.setSelected(globalRememberState);

        if (globalRememberState) {
            String lastUsed = creds.getLastUsedProfile();
            profileComboBox.setValue(lastUsed);

            String[] details = creds.loadProfile(lastUsed);
            parseUrlIntoFields(details[0]);
            nameField.setText(details[1]);
            initialsField.setText(details[2]);
        } else {
            profileComboBox.setValue("Default Profile");
        }
    }

    private void enterCreds(String url, String user, String password, String initials) {
        creds.user = user;
        creds.pass = password;
        creds.url = url;
        creds.initials = initials;
        creds.Display();
    }

    private VBox createFieldGroup(String labelText, String placeholder, boolean masked) {
        Text label = new Text(labelText);
        label.setFont(Font.font("System", 13));
        label.setFill(Color.web("#1a1a1a"));

        TextField field = masked ? new PasswordField() : new TextField();
        field.setPromptText(placeholder);
        field.setMaxWidth(300);
        field.setPrefHeight(40);
        field.setStyle("-fx-background-color: white; " +
                "-fx-border-color: #d1d1d1;" +
                "-fx-border-radius: 8; " +
                "-fx-background-radius: 8; " +
                "-fx-padding: 10 12; " +
                "-fx-font-size: 13px;"
        );

        VBox group = new VBox(5, label, field);
        group.setMaxWidth(300);
        return group;
    }
}