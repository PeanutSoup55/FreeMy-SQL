package GUI.Schemas;

import GUI.Schemas.SchemasRoot;
import Objects.Field;
import Objects.Schema;
import Objects.Table;
import globalfuncs.db;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.List;

public class SchemasAdd extends VBox {

    private final SchemasRoot root;
    private final TextField   schemaNameField;
    private final VBox        tablesContainer;
    private final List<TableEntry> tableEntries = new ArrayList<>();
    final ObservableList<String> availablePKs = FXCollections.observableArrayList();

    static final List<String> SQL_TYPES = List.of(
            "INT", "BIGINT", "SMALLINT", "TINYINT",
            "VARCHAR(50)", "VARCHAR(100)", "VARCHAR(255)",
            "TEXT", "LONGTEXT",
            "BOOLEAN",
            "DATE", "DATETIME", "TIMESTAMP",
            "FLOAT", "DOUBLE", "DECIMAL(10,2)"
    );

    public SchemasAdd(SchemasRoot root) {
        this.root = root;
        setSpacing(20);
        setPadding(new Insets(28, 32, 28, 32));
        setStyle("-fx-background-color: #F2F4F2;");

        // Header row
        Button backBtn = new Button("← Back");
        backBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #2E5A47;" +
                "-fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 13;" +
                "-fx-border-color: transparent;");
        backBtn.setOnAction(e -> root.createTables());

        Text title = new Text("Create New Schema");
        title.setFont(Font.font("System", FontWeight.BOLD, 20));
        title.setFill(Color.web("#1E3D30"));

        HBox header = new HBox(14, backBtn, title);
        header.setAlignment(Pos.CENTER_LEFT);

        schemaNameField = new TextField();
        schemaNameField.setPromptText("Schema Name...");
        schemaNameField.setMaxWidth(380);
        schemaNameField.setStyle(fieldStyle());
        tablesContainer = new VBox(16);
        tablesContainer.setPadding(new Insets(0, 0, 4, 0));

        ScrollPane scroll = new ScrollPane(tablesContainer);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        Button addTableBtn = outlineBtn("+ Add Table");
        addTableBtn.setOnAction(e -> addTableEntry());

        Button saveBtn = filledBtn("Save Schema");
        saveBtn.setOnAction(e -> saveSchema());

        HBox bottomRow = new HBox(12, addTableBtn, saveBtn);
        bottomRow.setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(header, schemaNameField, scroll, bottomRow);
        addTableEntry();
    }

    private void addTableEntry() {
        TableEntry entry = new TableEntry(this);
        tableEntries.add(entry);
        tablesContainer.getChildren().add(entry);
    }

    void removeTable(TableEntry entry) {
        tableEntries.remove(entry);
        tablesContainer.getChildren().remove(entry);
        rebuildPKList();
    }

    void rebuildPKList() {
        availablePKs.clear();
        for (TableEntry te : tableEntries) {
            String tbl = te.tableNameField.getText().trim();
            String pk  = te.pkNameField.getText().trim();
            if (!tbl.isEmpty() && !pk.isEmpty())
                availablePKs.add(tbl + "(" + pk + ")");
        }
    }

    private void saveSchema() {
        String name = schemaNameField.getText().trim();
        if (name.isEmpty()) { warn("Schema name cannot be empty."); return; }

        Schema schema = new Schema(name);
        for (TableEntry te : tableEntries) {
            Table t = te.buildTable();
            if (t == null) return;
            schema.addTable(t);
        }
        db.MakeSchema(schema);
        root.refresh();
    }

    static String fieldStyle() {
        return "-fx-background-color: white; -fx-background-radius: 8;" +
                "-fx-border-color: #E2E6E2; -fx-border-radius: 8;" +
                "-fx-padding: 10 14; -fx-font-size: 13;";
    }

    static Button filledBtn(String label) {
        Button b = new Button(label);
        b.setStyle("-fx-background-color: #2E5A47; -fx-text-fill: white;" +
                "-fx-background-radius: 8; -fx-font-weight: bold;" +
                "-fx-cursor: hand; -fx-padding: 10 28; -fx-font-size: 13;");
        return b;
    }

    static Button outlineBtn(String label) {
        Button b = new Button(label);
        b.setStyle("-fx-background-color: white; -fx-text-fill: #2E5A47;" +
                "-fx-border-color: #2E5A47; -fx-border-radius: 8;" +
                "-fx-background-radius: 8; -fx-font-weight: bold;" +
                "-fx-cursor: hand; -fx-padding: 10 28; -fx-font-size: 13;");
        return b;
    }

    static ComboBox<String> greenCombo(ObservableList<String> items) {
        ComboBox<String> cb = new ComboBox<>(items);
        cb.setStyle("-fx-background-color: #2E5A47; -fx-background-radius: 8;" +
                "-fx-cursor: hand; -fx-padding: 2 4;");
        cb.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                setText(empty || s == null ? "" : s);
                setTextFill(Color.WHITE);
                setStyle("-fx-background-color: transparent;");
            }
        });
        return cb;
    }

    static void warn(String msg) {
        new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK).showAndWait();
    }

    static class TableEntry extends VBox {

        private final SchemasAdd parent;
        final TextField tableNameField;
        final TextField pkNameField;
        private final ComboBox<String> pkTypeBox;
        private final VBox fieldsContainer;
        private final List<FieldEntry> fieldEntries = new ArrayList<>();

        TableEntry(SchemasAdd parent) {
            this.parent = parent;
            setSpacing(0);
            setStyle("-fx-background-color: white; -fx-background-radius: 12;" +
                    "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 14, 0, 0, 3);");

            tableNameField = new TextField();
            tableNameField.setPromptText("Table Name...");
            tableNameField.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;" +
                    "-fx-font-size: 14; -fx-font-weight: bold; -fx-padding: 14 16;");
            tableNameField.textProperty().addListener((obs, o, n) -> parent.rebuildPKList());
            HBox.setHgrow(tableNameField, Priority.ALWAYS);

            Button removeTableBtn = new Button("✕");
            removeTableBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #BBBBBB;" +
                    "-fx-cursor: hand; -fx-font-size: 15; -fx-padding: 10 14;");
            removeTableBtn.setOnAction(e -> parent.removeTable(this));

            HBox nameRow = new HBox(tableNameField, removeTableBtn);
            nameRow.setAlignment(Pos.CENTER_LEFT);
            nameRow.setStyle("-fx-border-color: #EEEEEE; -fx-border-width: 0 0 1 0;");

            pkNameField = new TextField();
            pkNameField.setPromptText("Primary Key...");
            pkNameField.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;" +
                    "-fx-font-size: 13; -fx-padding: 12 16;");
            pkNameField.textProperty().addListener((obs, o, n) -> parent.rebuildPKList());
            HBox.setHgrow(pkNameField, Priority.ALWAYS);

            pkTypeBox = greenCombo(FXCollections.observableArrayList(SQL_TYPES));
            pkTypeBox.setValue("INT");
            pkTypeBox.setPrefWidth(165);

            HBox pkRow = new HBox(pkNameField, pkTypeBox);
            pkRow.setAlignment(Pos.CENTER_LEFT);
            pkRow.setPadding(new Insets(0, 14, 0, 0));
            pkRow.setStyle("-fx-border-color: #EEEEEE; -fx-border-width: 0 0 1 0;");

            fieldsContainer = new VBox(0);

            Button addFieldBtn = filledBtn("Add Field");
            addFieldBtn.setOnAction(e -> addFieldEntry());
            HBox addRow = new HBox(addFieldBtn);
            addRow.setAlignment(Pos.CENTER);
            addRow.setPadding(new Insets(14, 0, 14, 0));

            getChildren().addAll(nameRow, pkRow, fieldsContainer, addRow);
        }

        private void addFieldEntry() {
            FieldEntry fe = new FieldEntry(this);
            fieldEntries.add(fe);
            fieldsContainer.getChildren().add(fe);
        }

        void removeField(FieldEntry fe) {
            fieldEntries.remove(fe);
            fieldsContainer.getChildren().remove(fe);
        }

        Table buildTable() {
            String name   = tableNameField.getText().trim();
            String pkName = pkNameField.getText().trim();
            if (name.isEmpty())   { SchemasAdd.warn("Table name cannot be empty."); return null; }
            if (pkName.isEmpty()) { SchemasAdd.warn("Primary key name required for table: " + name); return null; }
            if (pkTypeBox.getValue() == null) { SchemasAdd.warn("Select a type for the primary key."); return null; }

            Table table = new Table(name);
            table.addField(new Field(null, true, pkTypeBox.getValue(), pkName));

            for (FieldEntry fe : fieldEntries) {
                Field f = fe.buildField();
                if (f == null) return null;
                table.addField(f);
            }
            return table;
        }
    }

    static class FieldEntry extends HBox {

        private final TableEntry parent;
        private final TextField nameField;
        private final ComboBox<String> typeBox;
        private final ComboBox<String> refBox;

        FieldEntry(TableEntry parent) {
            this.parent = parent;
            setAlignment(Pos.CENTER_LEFT);
            setStyle("-fx-border-color: #EEEEEE; -fx-border-width: 0 0 1 0;");
            setPadding(new Insets(0, 14, 0, 0));

            nameField = new TextField();
            nameField.setPromptText("New Field...");
            nameField.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;" +
                    "-fx-font-size: 13; -fx-padding: 12 16;");
            HBox.setHgrow(nameField, Priority.ALWAYS);

            typeBox = greenCombo(FXCollections.observableArrayList(SQL_TYPES));
            typeBox.setPromptText("Select Input");
            typeBox.setPrefWidth(165);

            refBox = greenCombo(parent.parent.availablePKs);
            refBox.setPromptText("Reference To");
            refBox.setPrefWidth(180);

            Button removeBtn = new Button("✕");
            removeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #BBBBBB;" +
                    "-fx-cursor: hand; -fx-font-size: 13; -fx-padding: 8 10;");
            removeBtn.setOnAction(e -> parent.removeField(this));

            getChildren().addAll(nameField, typeBox, refBox, removeBtn);
        }

        Field buildField() {
            String name = nameField.getText().trim();
            String type = typeBox.getValue();
            if (name.isEmpty()) { SchemasAdd.warn("Field name cannot be empty."); return null; }
            if (type == null)   { SchemasAdd.warn("Select a type for field: " + name); return null; }
            String ref = refBox.getValue();
            return new Field(ref, false, type, name);
        }
    }
}