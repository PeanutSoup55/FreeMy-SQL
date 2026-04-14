package GUI;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.*;
import globalfuncs.db;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

import java.util.ArrayList;
import java.util.List;

public class SchemasRoot extends BorderPane{
    public static List<String> schemas = db.Schemas();
    private HBox selectedTab;

    public SchemasRoot(){
        createSide();
        createTables();
    }

    private void createSide(){
        VBox vBox = new VBox();
        vBox.setStyle("-fx-background-radius: 15;" +
                "-fx-background-color: #FFFFFF;" +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 12, 0, 0, 0);");
        BorderPane.setMargin(vBox, new Insets(10));

        List <HBox> schemaTabs = new ArrayList<>();
        for (String schema : schemas){
            schemaTabs.add(generateTab(schema));
        }
        vBox.getChildren().addAll(schemaTabs);

        setLeft(vBox);
    }

    private HBox generateTab(String schema){
        HBox hbox = new HBox();
        hbox.setAlignment(Pos.CENTER);
        hbox.setPadding(new Insets(10, 10, 10, 10));
        hbox.setMinWidth(100);
        hbox.setPrefHeight(15);

        Text text = new Text(schema);
        text.setFont(Font.font("System", 13));
        text.setTextAlignment(TextAlignment.CENTER);
        text.setStyle("-fx-font-weight: bold;");

        if (selectedTab == null) {
            applySelectedStyle(hbox, text);
            selectedTab = hbox;
        } else {
            applyDefaultStyle(hbox, text);
        }
        hbox.setOnMouseClicked(e ->{
            if (selectedTab != null){
                Text prevLabel = (Text) selectedTab.getChildren().getFirst();
                applyDefaultStyle(selectedTab, prevLabel);
            }
            applySelectedStyle(hbox, text);
            selectedTab = hbox;

        });

        hbox.getChildren().add(text);
        return hbox;
    }

    private void applySelectedStyle(HBox hbox, Text label) {
        hbox.setBackground(new Background(new BackgroundFill(Color.web("#2E5A47"), new CornerRadii(8), Insets.EMPTY)));
        label.setFill(Color.WHITE);
    }
    private void applyDefaultStyle(HBox hbox, Text label) {
        hbox.setBackground(null);
        label.setFill(Color.web("#4A4A4A"));
    }

    public GridPane createTables(){
        GridPane gridPane = new GridPane();
        return gridPane;
    }

}
