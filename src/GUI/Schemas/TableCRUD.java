package GUI.Schemas;

import Objects.Field;
import Objects.Table;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

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