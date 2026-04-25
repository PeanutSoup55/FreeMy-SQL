package GUI.Schemas;

import Objects.Field;
import Objects.Table;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import javax.swing.text.TableView;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TableCRUD extends VBox {

    private final SchemasRoot root;
    private final String schemaName;
    private final Table table;

    private final TableView<String[]> dataTable = new TableView<>();
    private final VBox formContainer = new VBox(12);
    private Map<String, Control> formControls = new LinkedHashMap<>();
    private final Label modeLabel = new Label("Insert New Row");
    private final Button saveBtn = filledBtn("Insert Row");
    private final Button clearBtn = outlineBtn("Clear");
    private final Label statusLabel = new Label();

    private List<String> columnNames = new ArrayList<>();
    private List<String[]> rows = new ArrayList<>();
    private String[] selectedRow = null;
    private Field pkField = null;
    private int pkColIndex = -1;

    public TableCRUD(SchemasRoot root, String schemaName, Table table){
        this.root = root;
        this.schemaName = schemaName;
        this.table = table;

        for (Field f : table.getFields()) {
            if (f.isPrimary()) { pkField = f; break; }
        }

        setSpacing(0);
        setStyle("-fx-background-color: #F2F4F2;");
        getChildren().addAll(buildHeader(), buildSplit());
        loadData();
    }

    private HBox buildHeader() {
        Button backBtn = new Button("← Back");
        backBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #2E5A47;" +
                "-fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 13;");
        backBtn.setOnAction(e -> root.createTables());

        Text title = new Text(table.getName());
        title.setFont(Font.font("System", FontWeight.BOLD, 20));
        title.setFill(Color.web("#1E3D30"));

        Label badge = new Label(schemaName);
        badge.setStyle("-fx-background-color: #E9F5E8; -fx-text-fill: #2E5A47;" +
                "-fx-background-radius: 6; -fx-padding: 4 10;" +
                "-fx-font-weight: bold; -fx-font-size: 11;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(14, backBtn, title, badge, spacer);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(18, 24, 14, 24));
        header.setStyle("-fx-background-color: white;" +
                "-fx-border-color: #EEEEEE; -fx-border-width: 0 0 1 0;");
        return header;

    }

    private SplitPane buildSplit() {

    }

    private void loadData(){

    }

    private static Button filledBtn(String label) {
        Button b = new Button(label);
        b.setStyle("-fx-background-color: #2E5A47; -fx-text-fill: white;" +
                "-fx-background-radius: 8; -fx-font-weight: bold;" +
                "-fx-cursor: hand; -fx-padding: 10 28; -fx-font-size: 13;");
        return b;
    }

    private static Button outlineBtn(String label) {
        Button b = new Button(label);
        b.setStyle("-fx-background-color: white; -fx-text-fill: #2E5A47;" +
                "-fx-border-color: #2E5A47; -fx-border-radius: 8;" +
                "-fx-background-radius: 8; -fx-font-weight: bold;" +
                "-fx-cursor: hand; -fx-padding: 10 28; -fx-font-size: 13;");
        return b;
    }
}