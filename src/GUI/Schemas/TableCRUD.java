package GUI.Schemas;

import Objects.Field;
import Objects.Table;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
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
        SplitPane split = new SplitPane();
        split.setOrientation(javafx.geometry.Orientation.HORIZONTAL);
        split.getItems().addAll(buildDataSection(), buildFormSection());
        split.setDividerPositions(0.62);
        VBox.setVgrow(split, Priority.ALWAYS);
        return split;
    }

    private VBox buildDataSection(){
        Label sectionTitle = new Label("Existing Data");
        sectionTitle.setFont(Font.font("System", FontWeight.BOLD, 14));
        sectionTitle.setTextFill(Color.web("#1E3D30"));

        Button refreshBtn = new Button("↻");
        refreshBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #2E5A47;" +
                "-fx-font-size: 16; -fx-cursor: hand;");
        refreshBtn.setOnAction(e -> loadData());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox topBar = new HBox(10, sectionTitle, spacer, refreshBtn);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(14, 16, 10, 16));
        topBar.setStyle("-fx-background-color: white;" +
                "-fx-border-color: #EEEEEE; -fx-border-width: 0 0 1 0;");

        dataTable.setStyle("-fx-background-color: white; -fx-border-color: transparent;");
        dataTable.setFixedCellSize(36);
        dataTable.setPlaceholder(styledPlaceholder("No rows yet — insert one below."));

        dataTable.getSelectionModel().selectedItemProperty().addListener((obs, old, row) -> { if (row != null) populateFormForEdit(row); });

        ScrollPane tableScroll = new ScrollPane(dataTable);
        tableScroll.setFitToHeight(true);
        tableScroll.setFitToWidth(false);
        tableScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        tableScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        tableScroll.setStyle("-fx-background-color: white; -fx-background: white;");

        dataTable.prefHeightProperty().bind(tableScroll.heightProperty());

        VBox.setVgrow(tableScroll, Priority.ALWAYS);

        VBox section = new VBox(0, topBar, tableScroll);
        VBox.setVgrow(section, Priority.ALWAYS);
        section.setStyle("-fx-background-color: white;");
        return section;
    }
    private VBox buildFormSection(){

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