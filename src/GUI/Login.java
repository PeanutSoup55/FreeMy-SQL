package GUI;

import globalfuncs.creds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.effect.BoxBlur;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import java.util.Optional;

public class Login extends HBox {

    private ComboBox<String> profileComboBox;
    private TextField urlField;
    private TextField nameField;
    private TextField passField;
    private TextField initialsField;
    private CheckBox rememberMeCheck;

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

        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(circleLayer.widthProperty().add(20));
        clip.heightProperty().bind(circleLayer.heightProperty());
        clip.setTranslateX(-40);
        clip.setArcHeight(80);
        clip.setArcWidth(80);
        circleLayer.setClip(clip);

        circleLayer.getChildren().addAll(c1, c2, c3, c4, labelHolder);
        HBox.setHgrow(circleLayer, Priority.ALWAYS);

        //right
        VBox rightSide = new VBox(20);
        rightSide.setAlignment(Pos.CENTER);
        rightSide.setPadding(new Insets(60, 50, 60, 50));

        rightSide.setMinWidth(0);
        rightSide.setMinHeight(0);
        HBox.setHgrow(rightSide, Priority.ALWAYS);

        Text title = new Text("GUI.Login with local Credentials");
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

        // Add Button Action Logic
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
                    // Lock user onto newly generated selection instantly and clear input boxes
                    profileComboBox.setValue(cleanName);
                    urlField.clear();
                    nameField.clear();
                    passField.clear();
                    initialsField.clear();
                    urlField.requestFocus();
                }
            });
        });

        HBox profileRow = new HBox(10, profileComboBox, addProfileBtn);
        profileRow.setMaxWidth(300);
        profileRow.setAlignment(Pos.CENTER_LEFT);
        profileGroup.getChildren().addAll(profileLabel, profileRow);

        VBox urlGroup  = createFieldGroup("MySQL URL");
        urlField = (TextField) urlGroup.getChildren().get(1);
        VBox nameGroup = createFieldGroup("MySQL Name");
        nameField = (TextField) nameGroup.getChildren().get(1);
        VBox passGroup = createFieldGroup("MySQL Password");
        passField = (TextField) passGroup.getChildren().get(1);
        VBox initialsGroup = createFieldGroup("User Initials");
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
                urlField.setText(details[0]);
                nameField.setText(details[1]);
                initialsField.setText(details[2]);
            }
        });

        Button connectBtn = new Button("Connect");
        connectBtn.setPrefWidth(160);
        connectBtn.setStyle(
                "-fx-background-color: #262626; -fx-text-fill: white; -fx-font-size: 13px;" +
                        "-fx-background-radius: 10; -fx-border-radius: 10; -fx-padding: 10 40; -fx-cursor: hand;"
        );
        connectBtn.setOnMouseEntered(e -> connectBtn.setStyle(
                "-fx-background-color: #404040; -fx-text-fill: white; -fx-font-size: 13px;" +
                        "-fx-background-radius: 10; -fx-border-radius: 10; -fx-padding: 10 40; -fx-cursor: hand;"
        ));
        connectBtn.setOnMouseExited(e -> connectBtn.setStyle(
                "-fx-background-color: #262626; -fx-text-fill: white; -fx-font-size: 13px;" +
                        "-fx-background-radius: 10; -fx-border-radius: 10; -fx-padding: 10 40; -fx-cursor: hand;"
        ));

        connectBtn.setOnAction(e -> {
            String activeProfile = profileComboBox.getValue();
            if (activeProfile == null || activeProfile.trim().isEmpty()) {
                activeProfile = "Default Profile";
            }
            creds.setRememberMe(rememberMeCheck.isSelected());
            if (rememberMeCheck.isSelected()) {
                creds.saveProfile(activeProfile, urlField.getText(), nameField.getText(), initialsField.getText());
            } else {
                creds.removeProfile(activeProfile);
            }

            enterCreds(urlField.getText(), nameField.getText(), passField.getText(), initialsField.getText());
            Root root = new Root();
            Scene newScene = new Scene(root, 1700, 1000, Color.web("#F9F9F9"));
            Stage stage = (Stage) connectBtn.getScene().getWindow();
            stage.setScene(newScene);
        });

        HBox btnRow = new HBox(connectBtn);
        btnRow.setAlignment(Pos.CENTER);
        btnRow.setMaxWidth(300);

        rightSide.getChildren().addAll(title, profileGroup, urlGroup, nameGroup, passGroup, initialsGroup, rememberMeCheck, btnRow);

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

    private void populateProfiles() {
        String[] profiles = creds.getAllProfileNames();
        profileComboBox.getItems().setAll(profiles);

        boolean globalRememberState = creds.isRememberMeEnabled();
        rememberMeCheck.setSelected(globalRememberState);

        if (globalRememberState) {
            String lastUsed = creds.getLastUsedProfile();
            profileComboBox.setValue(lastUsed);

            String[] details = creds.loadProfile(lastUsed);
            urlField.setText(details[0]);
            nameField.setText(details[1]);
            initialsField.setText(details[2]);

            javafx.application.Platform.runLater(() -> passField.requestFocus());
        } else {
            profileComboBox.setValue("Default Profile");
        }
    }

    private void enterCreds(String url, String user, String password, String initials){
        creds.user = user;
        creds.pass = password;
        creds.url = url;
        creds.initials = initials;
        creds.Display();
    }

    private VBox createFieldGroup(String labelText) {
        Text label = new Text(labelText);
        label.setFont(Font.font("System", 13));
        label.setFill(Color.web("#1a1a1a"));

        TextField field = new TextField();
        field.setPromptText("Value");
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