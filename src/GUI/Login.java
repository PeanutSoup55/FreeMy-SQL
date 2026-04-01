package GUI;

import globalfuncs.creds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
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

public class Login extends HBox {

    public Login(Stage stage) {
        stage.setWidth(1400);
        stage.setHeight(900);
        stage.show();
        createLoginPage();
    }

    public void createLoginPage() {

        // ── LEFT SIDE ──────────────────────────────────────────────────────────
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

        double offset = 150;

        for (Circle c : new Circle[]{c1, c2, c3, c4}) {
            c.centerXProperty().bind(circleLayer.widthProperty().divide(2).add(offset));
            c.centerYProperty().bind(circleLayer.heightProperty().divide(2));
        }

        // StackPane to perfectly centre the label — fills the whole panel,
        // then shifts its own centre by the same offset as the circles
        StackPane labelHolder = new StackPane();
        labelHolder.setPickOnBounds(false); // don't block mouse events
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
        clip.setTranslateX(-20);
        clip.setArcHeight(80);
        clip.setArcWidth(80);
        circleLayer.setClip(clip);

        circleLayer.getChildren().addAll(c1, c2, c3, c4, labelHolder);
        HBox.setHgrow(circleLayer, Priority.ALWAYS);

        // ── RIGHT SIDE ─────────────────────────────────────────────────────────
        VBox rightSide = new VBox(20);
        rightSide.setAlignment(Pos.CENTER);
        rightSide.setPadding(new Insets(60, 50, 60, 50));

        rightSide.setMinWidth(0);
        rightSide.setMinHeight(0);
        HBox.setHgrow(rightSide, Priority.ALWAYS);

        Text title = new Text("GUI.Login with local Credentials");
        title.setFont(Font.font("System", FontWeight.BOLD, 20));
        title.setFill(Color.web("#1a1a1a"));

        VBox urlGroup  = createFieldGroup("MySQL URL");
        TextField urlField = (TextField) urlGroup.getChildren().get(1);
        VBox nameGroup = createFieldGroup("MySQL Name");
        TextField nameField = (TextField) nameGroup.getChildren().get(1);
        VBox passGroup = createFieldGroup("MySQL Password");
        TextField passField = (TextField) passGroup.getChildren().get(1);

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
        connectBtn.setOnAction(e -> enterCreds(urlField.getText(), nameField.getText(), passField.getText()));

        HBox btnRow = new HBox(connectBtn);
        btnRow.setAlignment(Pos.CENTER);
        btnRow.setMaxWidth(300);

        rightSide.getChildren().addAll(title, urlGroup, nameGroup, passGroup, btnRow);

        // ── ASSEMBLE ───────────────────────────────────────────────────────────
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
    private void enterCreds(String url, String user, String password){
        creds.user = user;
        creds.pass = password;
        creds.url = url;
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
        field.setStyle(
                "-fx-background-color: white; -fx-border-color: #d1d1d1;" +
                        "-fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 10 12; -fx-font-size: 13px;"
        );

        VBox group = new VBox(5, label, field);
        group.setMaxWidth(300);
        return group;
    }
}