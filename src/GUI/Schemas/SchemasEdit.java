package GUI.Schemas;

import GUI.Root;
import GUI.Settings.Theme;
import Objects.Schema;
import Objects.Table;
import Objects.Field;
import globalfuncs.db;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.util.*;
import java.util.prefs.Preferences;

public class SchemasEdit extends BorderPane {

    private static final String RED = "#E88C8C";

    private static final Preferences ORDER_PREFS =
            Preferences.userRoot().node("Free_My_SQL/table_order");

    private final Root appRoot;
    private final SchemasRoot root;
    private final Runnable onDone;
    private String schemaName;

    private final Text titleText;
    private final FlowPane tablesGrid = new FlowPane(16, 16);
    private final List<String> tableOrder = new ArrayList<>();

    // --- elements that need re-styling on theme change ---
    private StackPane topBar;
    private HBox renameRow;
    private VBox header;
    private TextField nameField;
    private Button backBtn;
    private Button addTableBtn;
    private Button renameBtn;
    private Button deleteSchemaBtn;

    public SchemasEdit(Root appRoot, SchemasRoot root, String schemaName, Runnable onDone) {
        this.appRoot = appRoot;
        this.root = root;
        this.schemaName = schemaName;
        this.onDone = onDone;

        // ── Back button ──────────────────
        backBtn = new Button("← Back");
        backBtn.setOnAction(e -> onDone.run());

        addTableBtn = new Button("+ Add Table");
        addTableBtn.setOnAction(e -> addNewTable());

        deleteSchemaBtn = new Button("Delete Schema");
        deleteSchemaBtn.setOnAction(e -> confirmDeleteSchema());

        titleText = new Text("Edit Schema: " + schemaName);
        titleText.setFont(Font.font("System", FontWeight.BOLD, 20));

        HBox centerBox = new HBox(titleText);
        centerBox.setAlignment(Pos.CENTER);
        centerBox.setMaxWidth(Region.USE_PREF_SIZE);

        HBox leftBox = new HBox(backBtn);
        leftBox.setAlignment(Pos.CENTER_LEFT);
        leftBox.setMaxWidth(Region.USE_PREF_SIZE);

        HBox rightBox = new HBox(8, addTableBtn, deleteSchemaBtn);
        rightBox.setAlignment(Pos.CENTER_RIGHT);
        rightBox.setMaxWidth(Region.USE_PREF_SIZE);

        topBar = new StackPane();
        topBar.setPadding(new Insets(18, 24, 18, 24));
        StackPane.setAlignment(centerBox, Pos.CENTER);
        StackPane.setAlignment(leftBox, Pos.CENTER_LEFT);
        StackPane.setAlignment(rightBox, Pos.CENTER_RIGHT);
        topBar.getChildren().addAll(centerBox, leftBox, rightBox);

        nameField = new TextField(schemaName);
        nameField.setPromptText("Schema name...");
        nameField.setPrefWidth(220);

        renameBtn = new Button("Rename");
        renameBtn.setOnAction(e -> renameSchema(nameField.getText().trim()));

        renameRow = new HBox(8, nameField, renameBtn);
        renameRow.setAlignment(Pos.CENTER_LEFT);
        renameRow.setPadding(new Insets(14, 20, 16, 20));

        header = new VBox(0, topBar, renameRow);

        tablesGrid.setPadding(new Insets(0));

        VBox body = new VBox(tablesGrid);
        body.setPadding(new Insets(20));

        ScrollPane scroll = new ScrollPane(body);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-padding: 0;");

        setTop(header);
        setCenter(scroll);

        loadTableOrder();
        rebuildTableList();

        applyTheme();
        Theme.registerThemeListener(this, this::applyTheme);
    }

    private void applyTheme() {
        Platform.runLater(() -> {
            setStyle("-fx-background-color: " + Theme.colour2 + ";");

            topBar.setStyle("-fx-background-color: " + Theme.colourDark + ";" +
                    "-fx-border-color: " + Theme.colourDark + "; -fx-border-width: 0 0 1 0;");
            titleText.setFill(Color.WHITE);

            backBtn.setStyle("-fx-background-color: rgba(255,255,255,0.12); -fx-text-fill: white;" +
                    "-fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 12;" +
                    "-fx-background-radius: 8; -fx-padding: 8 16;" +
                    "-fx-border-color: rgba(255,255,255,0.28); -fx-border-radius: 8; -fx-border-width: 1;");

            addTableBtn.setStyle("-fx-background-color: rgba(255,255,255,0.12); -fx-text-fill: white;" +
                    "-fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 12;" +
                    "-fx-background-radius: 8; -fx-padding: 8 14;" +
                    "-fx-border-color: rgba(255,255,255,0.28); -fx-border-radius: 8; -fx-border-width: 1;");

            deleteSchemaBtn.setStyle(
                    "-fx-background-color: transparent; -fx-text-fill: " + RED + ";" +
                            "-fx-font-size: 12; -fx-font-weight: bold; -fx-cursor: hand;" +
                            "-fx-border-color: " + RED + "; -fx-border-radius: 6; -fx-padding: 7 12;"
            );

            renameRow.setStyle("-fx-background-color: " + Theme.colour1 + ";");
            header.setStyle("-fx-border-color: " + Theme.colour3 + "; -fx-border-width: 0 0 1 0;");

            nameField.setStyle("-fx-background-color: " + Theme.colour1 + "; -fx-border-color: " + Theme.colour3 + ";" +
                    "-fx-border-radius: 6; -fx-background-radius: 6; -fx-padding: 8 10; -fx-font-size: 13;" +
                    "-fx-text-fill: " + Theme.colour6 + ";");

            renameBtn.setStyle("-fx-background-color: " + Theme.colourDark + "; -fx-text-fill: white;" +
                    "-fx-font-size: 12; -fx-font-weight: bold; -fx-background-radius: 6;" +
                    "-fx-cursor: hand; -fx-padding: 8 14;");
        });
    }

    private void renameSchema(String newName) {
        if (newName.isEmpty() || newName.equals(schemaName)) return;
        try {
            db.renameSchema(schemaName, newName);
        } catch (Exception ex) {
            SchemasAdd.warn("Could not rename schema: " + ex.getMessage());
            return;
        }
        String orderVal = ORDER_PREFS.get(schemaName, null);
        if (orderVal != null) {
            ORDER_PREFS.put(newName, orderVal);
            ORDER_PREFS.remove(schemaName);
        }
        if (SchemasRoot.isRemoteLinked(schemaName)) {   // NEW
            SchemasRoot.markRemoteLinked(newName);
            SchemasRoot.clearRemoteLink(schemaName);
        }
        schemaName = newName;
        titleText.setText("Edit Schema — " + schemaName);
        rebuildTableList();
    }

    private void confirmDeleteSchema() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Delete Schema");
        dialog.setHeaderText(null);

        ButtonType deleteButtonType = new ButtonType("Delete", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(deleteButtonType, ButtonType.CANCEL);

        Label warning = new Label("This will permanently delete '" + schemaName + "' and all its tables.");
        Label instruction = new Label("Type '" + schemaName + "' to confirm:");
        TextField input = new TextField();
        VBox content = new VBox(10, warning, instruction, input);
        content.setPadding(new Insets(12));
        dialog.getDialogPane().setContent(content);

        Node deleteButton = dialog.getDialogPane().lookupButton(deleteButtonType);
        deleteButton.setDisable(true);
        input.textProperty().addListener((obs, oldV, newV) ->
                deleteButton.setDisable(!newV.equals(schemaName)));

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == deleteButtonType) {
            db.deleteSchema(new Schema(schemaName));
            ORDER_PREFS.remove(schemaName);
            SchemasRoot.clearRemoteLink(schemaName);   // NEW
            onDone.run();
        }
    }

    private void loadTableOrder() {
        tableOrder.clear();
        String saved = ORDER_PREFS.get(schemaName, null);
        if (saved != null && !saved.isEmpty()) {
            tableOrder.addAll(Arrays.asList(saved.split("\\|")));
        }
    }

    private void saveTableOrder() {
        ORDER_PREFS.put(schemaName, String.join("|", tableOrder));
    }

    private void rebuildTableList() {
        root.setSelectedSchema(schemaName, false);
        tablesGrid.getChildren().clear();
        Schema full = root.getTablesFor(schemaName, false);
        List<Table> tables = new ArrayList<>(full.getTables());

        List<String> liveNames = tables.stream().map(Table::getName).toList();
        tableOrder.retainAll(liveNames);
        for (String name : liveNames) {
            if (!tableOrder.contains(name)) tableOrder.add(name);
        }
        saveTableOrder();

        tables.sort(Comparator.comparingInt(t -> tableOrder.indexOf(t.getName())));

        for (Table table : tables) {
            VBox card = root.buildCard(
                    table,
                    this,
                    () -> appRoot.setCenter(root),
                    this::rebuildTableList
            );
            tablesGrid.getChildren().add(card);
        }
    }

    private void addNewTable() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("New Table");
        dialog.setHeaderText(null);
        dialog.setContentText("Table name:");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(name -> {
            String trimmed = name.trim();
            if (trimmed.isEmpty()) return;
            Table blank = new Table(trimmed);
            blank.addField(new Field(null, true, "INT", "id"));
            db.CreateTable(schemaName, blank);
            tableOrder.add(trimmed);
            saveTableOrder();
            rebuildTableList();
        });
    }
}