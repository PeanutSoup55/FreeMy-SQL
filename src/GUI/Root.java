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

import java.awt.*;

public class Root extends BorderPane {

    public Root(){
        createSide();
        SchemasRoot sRoot = new SchemasRoot();
        setCenter(sRoot);
    }

    public void createSide(){
        VBox vBox = new VBox();
        HBox tophbox = new HBox();
        VBox topInnerHBox = new VBox();

        HBox schemas = createMenuItems("./assets/schema.png", "Schemas", true);
        HBox query = createMenuItems("./assets/query.png", "Query", false);
        HBox dashboard = createMenuItems("./assets/dashboard.png", "Dashboard", false);
        HBox credentials = createMenuItems("./assets/creds.png", "Credentials", false);
        HBox logs = createMenuItems("./assets/logs.png", "Logs", false);

        vBox.getChildren().addAll(schemas, dashboard, query, credentials, logs);
        setLeft(vBox);

    }
    public HBox createMenuItems(String icon, String labelText, boolean isSelected){
        Text label = new Text(labelText);
        label.setFont(Font.font("System", 13));
        label.setTextAlignment(TextAlignment.RIGHT);

        Image image = new Image(icon);
        ImageView imageView = new ImageView(image);
        imageView.setFitHeight(22);
        imageView.setFitWidth(22);

        HBox hbox = new HBox(20, imageView, label);
        hbox.setAlignment(Pos.CENTER_LEFT);
        hbox.setPadding(new Insets(20, 20, 20, 20));
        hbox.setMinWidth(200);
        hbox.setPrefHeight(30);

        if (isSelected){
            hbox.setBackground(new Background(new BackgroundFill(
                    Color.web("#2E5A47"), new CornerRadii(8), Insets.EMPTY)));
            label.setFill(Color.WHITE);
        }else {
            label.setFill(Color.web("#4A4A4A"));
            hbox.setBackground(null);
        }

        VBox.setMargin(hbox, new Insets(5, 10, 5, 10));
        return hbox;
    }
}
