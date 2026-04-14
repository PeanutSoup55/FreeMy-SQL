package GUI;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.*;
import globalfuncs.db;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SchemasRoot extends BorderPane{
    public static List<String> schemas = db.Schemas();
    private HBox selectedTab;

    public SchemasRoot(){
        createSide();
        createTables();
    }

    private void createSide(){
        VBox vBox = new VBox();
        vBox.setPadding(new Insets(10));
        vBox.setStyle("-fx-background-radius: 15;" +
                "-fx-background-color: #FFFFFF;" +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 12, 0, 0, 0);");
        BorderPane.setMargin(vBox, new Insets(10));

        Text top = new Text("Schemas");
        top.setStyle("-fx-font-weight: 600;" +
                "-fx-padding: 10;");
        top.setTextAlignment(TextAlignment.CENTER);

        Separator sep = new Separator();
        sep.setPadding(new Insets(5, 0, 5, 0));


        List <HBox> schemaTabs = new ArrayList<>();
        for (String schema : schemas){
            schemaTabs.add(generateTab(schema));
        }
        vBox.getChildren().addAll(top, sep);
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
            createTables();
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

    public void createTables() {
        FlowPane flow = new FlowPane();
        flow.setHgap(40);
        flow.setVgap(40);
        flow.setPadding(new Insets(20));

        String selectedSchema = selectedTab != null
                ? ((Text) selectedTab.getChildren().getFirst()).getText()
                : (schemas.isEmpty() ? "" : schemas.get(0));

        Map<String, List<String[]>> tableMap = db.GetTablesInSchema(selectedSchema);

        for (Map.Entry<String, List<String[]>> entry : tableMap.entrySet()) {
            VBox card = buildCard(entry.getKey(), entry.getValue());
            flow.getChildren().add(card);
        }

        ScrollPane scroll = new ScrollPane(flow);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(false);
        scroll.setStyle("-fx-background-color: transparent;");
        setCenter(scroll);
    }
    public VBox buildCard(String tableName, List<String[]> columns){
        VBox card = new VBox();
        card.setStyle("-fx-background-radius: 15;" +
                "-fx-background-color: #FFFFFF;" +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 12, 0, 0, 0);");
        card.setMinWidth(160);
        card.setPrefWidth(Region.USE_COMPUTED_SIZE);
        card.setMaxWidth(Region.USE_COMPUTED_SIZE);

        HBox header = new HBox();
        header.setPadding(new Insets(8, 12, 8, 12));
        header.setStyle("-fx-background-color: #2E5A47; -fx-background-radius: 10 10 0 0;");
        header.setAlignment(Pos.CENTER_LEFT);

        Text title = new Text(tableName);
        title.setFill(Color.WHITE);
        title.setFont(Font.font("System", FontWeight.BOLD, 13));
        header.getChildren().add(title);
        card.getChildren().add(header);

        for (String[] col : columns) {
            String colName = col[0];
            String dataType = col[1];
            String keyType  = col[2];

            String prefix = "";
            if ("PRI".equals(keyType))      prefix = "PK ";
            else if ("MUL".equals(keyType)) prefix = "FK ";

            HBox row = new HBox(6);
            row.setPadding(new Insets(5, 12, 5, 12));
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-border-color: #EEEEEE; -fx-border-width: 0 0 1 0;");

            Text colText = new Text(prefix + dataType + " " + colName);
            colText.setFont(Font.font("System", 12));
            colText.setFill(Color.web("#333333"));

            row.getChildren().add(colText);
            card.getChildren().add(row);
        }

        return card;
    }

}
