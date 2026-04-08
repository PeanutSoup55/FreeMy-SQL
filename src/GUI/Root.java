package GUI;

import globalfuncs.creds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.scene.shape.Rectangle;

import java.awt.*;

public class Root extends BorderPane {
    private HBox selectedTab;

    public Root(){
        createSide();
        SchemasRoot sRoot = new SchemasRoot();
        setCenter(sRoot);
    }

    public void createSide(){
        VBox vBox = new VBox();
        vBox.setStyle("-fx-background-radius: 15;" +
                "-fx-background-color: #FFFFFF;" +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 12, 0, 0, 0);");
        BorderPane.setMargin(vBox, new Insets(10));
        HBox tophbox = createTopHBox(creds.getInitials(), creds.getUser(), creds.getUrl());
        HBox searchBox = createSearch("./assets/search.png", "Search...");
        HBox schemas = createMenuItems("./assets/schema.png", "Schemas", true);
        HBox query = createMenuItems("./assets/query.png", "Query", false);
        HBox dashboard = createMenuItems("./assets/dashboard.png", "Dashboard", false);
        HBox credentials = createMenuItems("./assets/creds.png", "Credentials", false);
        HBox logs = createMenuItems("./assets/logs.png", "Logs", false);

        vBox.getChildren().addAll(tophbox, searchBox, schemas, dashboard, query, credentials, logs);
        setLeft(vBox);
    }
    public void isSelected(){

    }
    public HBox createSearch(String icon, String search){
        Text label = new Text(search);
        label.setFont(Font.font("System", 13));
        label.setTextAlignment(TextAlignment.RIGHT);
        label.setStyle("-fx-font-weight: bold;");

        Image image = new Image(icon);
        ImageView imageView = new ImageView(image);
        imageView.setFitHeight(22);
        imageView.setFitWidth(22);

        HBox hbox = new HBox(20, imageView, label);
        hbox.setAlignment(Pos.CENTER_LEFT);
        hbox.setPadding(new Insets(20, 20, 20, 20));
        hbox.setMinWidth(200);
        hbox.setPrefHeight(30);


        label.setFill(Color.web("#4A4A4A"));
        hbox.setBackground(new Background(new BackgroundFill(Color.web("#E9F5E8"), new CornerRadii(8), Insets.EMPTY)));

        VBox.setMargin(hbox, new Insets(5, 10, 5, 10));
        return hbox;
    }
    public HBox createTopHBox(String initials, String user, String url){
        Text topLabel = new Text(user);
        topLabel.setStyle("-fx-font-weight: bold; -fx-fill: #333;");

        Text bottomLabel = new Text(url);
        bottomLabel.setStyle("-fx-fill: gray; -fx-font-size: 10px;");

        VBox vBox = new VBox(10, topLabel, bottomLabel);
        vBox.setAlignment(Pos.CENTER_LEFT);

        Text initialLabel = new Text(initials);
        initialLabel.setStyle("-fx-font-weight: bold; " +
                "-fx-fill: white;" +
                "-fx-font-size: 25;");

        Rectangle rec = new Rectangle(50, 50);
        rec.setArcWidth(10);
        rec.setArcHeight(10);
        rec.setFill(Color.web("#285A48"));

        StackPane stack = new StackPane(rec, initialLabel);

        HBox hbox = new HBox(20, stack, vBox);
        hbox.setAlignment(Pos.CENTER_LEFT);
        hbox.setPadding(new Insets(10));

        return hbox;
    }
    public HBox createMenuItems(String icon, String labelText, boolean isSelected){
        Text label = new Text(labelText);
        label.setFont(Font.font("System", 13));
        label.setTextAlignment(TextAlignment.RIGHT);
        label.setStyle("-fx-font-weight: bold;");

        Image image = new Image(icon);
        ImageView imageView = new ImageView(image);
        imageView.setFitHeight(22);
        imageView.setFitWidth(22);

        HBox hbox = new HBox(20, imageView, label);
        hbox.setAlignment(Pos.CENTER_LEFT);
        hbox.setPadding(new Insets(20, 20, 20, 20));
        hbox.setMinWidth(200);
        hbox.setPrefHeight(30);

        if (isSelected) {
            applySelectedStyle(hbox, label);
            selectedTab = hbox;
        } else {
            applyDefaultStyle(hbox, label);
        }
        hbox.setOnMouseClicked(e ->{
            if (selectedTab != null){
                Text prevLabel = (Text) selectedTab.getChildren().get(1);
                applyDefaultStyle(selectedTab, prevLabel);
            }
            applySelectedStyle(hbox, label);
            selectedTab = hbox;

            switchCenterContent(labelText);
        });

        VBox.setMargin(hbox, new Insets(5, 10, 5, 10));
        return hbox;
    }
    private void applySelectedStyle(HBox hbox, Text label) {
        hbox.setBackground(new Background(new BackgroundFill(
                Color.web("#2E5A47"), new CornerRadii(8), Insets.EMPTY)));
        label.setFill(Color.WHITE);
    }

    private void applyDefaultStyle(HBox hbox, Text label) {
        hbox.setBackground(null);
        label.setFill(Color.web("#4A4A4A"));
    }

    private void switchCenterContent(String menuTitle) {
        switch (menuTitle) {
            case "Schemas": setCenter(new SchemasRoot()); break;
            case "Query": setCenter(new StackPane(new Text("Query View"))); break;
            case "Dashboard": setCenter(new StackPane(new Text("Dashboard View"))); break;
        }
    }
}
