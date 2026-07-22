package GUI.Schemas;

import GUI.Schemas.SchemasRoot;
import GUI.Settings.Theme;
import Objects.Field;
import Objects.Schema;
import Objects.Table;
import globalfuncs.db;
import javafx.application.Platform;
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

public class SchemasAdd extends BorderPane {

    private final SchemasRoot root;
    private final Runnable onDone;
    private final TextField   schemaNameField;
    private final VBox        tablesContainer;
    private final Label       tableCountLabel;
    private final Button      saveBtn;
    private final List<TableEntry> tableEntries = new ArrayList<>();
    final ObservableList<String> availablePKs = FXCollections.observableArrayList();

    // --- elements that need re-styling on theme change ---
    private final BorderPane topBar;
    private final Text       title;
    private final Button     backBtn;
    private final VBox       nameSection;
    private final Label      nameLabel;
    private final Button     addTableBtn;
    private final HBox       saveBar;

    static final List<String> SQL_TYPES = List.of(
            "INT", "BIGINT", "SMALLINT", "TINYINT",
            "VARCHAR(50)", "VARCHAR(100)", "VARCHAR(255)",
            "TEXT", "LONGTEXT",
            "BOOLEAN",
            "DATE", "DATETIME", "TIMESTAMP",
            "FLOAT", "DOUBLE", "DECIMAL(10,2)"
    );

    public SchemasAdd(SchemasRoot root, Runnable onDone) {
        this.root = root;
        this.onDone = onDone;

        // ── Header: dark topbar, matches SchemasEdit / LoginGen ────────
        backBtn = new Button("← Back");
        backBtn.setOnAction(e -> onDone.run());

        title = new Text("Create New Schema");
        title.setFont(Font.font("System", FontWeight.BOLD, 20));

        HBox leftBox = new HBox(backBtn);
        leftBox.setAlignment(Pos.CENTER_LEFT);

        HBox centerBox = new HBox(title);
        centerBox.setAlignment(Pos.CENTER);

        HBox rightSpacer = new HBox();
        rightSpacer.prefWidthProperty().bind(leftBox.widthProperty());

        topBar = new BorderPane();
        topBar.setPadding(new Insets(18, 24, 18, 24));
        topBar.setLeft(leftBox);
        topBar.setCenter(centerBox);
        topBar.setRight(rightSpacer);

        // ── Schema name row ──────────────────────────────────────────
        nameLabel = new Label("Schema Name");
        nameLabel.setFont(Font.font("System", FontWeight.BOLD, 12));

        schemaNameField = new TextField();
        schemaNameField.setPromptText("e.g. inventory_db");
        schemaNameField.setMaxWidth(380);

        nameSection = new VBox(6, nameLabel, schemaNameField);
        nameSection.setPadding(new Insets(18, 24, 18, 24));

        VBox header = new VBox(0, topBar, nameSection);

        // ── Tables section ───────────────────────────────────────────
        tableCountLabel = new Label();
        tableCountLabel.setFont(Font.font("System", FontWeight.BOLD, 13));

        addTableBtn = outlineBtn("+ Add Table");
        addTableBtn.setOnAction(e -> addTableEntry());

        Region tablesSpacer = new Region();
        HBox.setHgrow(tablesSpacer, Priority.ALWAYS);

        HBox tablesToolbar = new HBox(10, tableCountLabel, tablesSpacer, addTableBtn);
        tablesToolbar.setAlignment(Pos.CENTER_LEFT);
        tablesToolbar.setPadding(new Insets(20, 24, 10, 24));

        tablesContainer = new VBox(16);
        tablesContainer.setPadding(new Insets(0, 24, 24, 24));

        VBox body = new VBox(0, tablesToolbar, tablesContainer);

        ScrollPane scroll = new ScrollPane(body);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;" +
                "-fx-border-color: transparent;");

        // ── Bottom save bar ──────────────────────────────────────────
        saveBtn = filledBtn("Save Schema");
        saveBtn.setOnAction(e -> saveSchema());
        saveBtn.setDisable(true);
        schemaNameField.textProperty().addListener((obs, o, n) ->
                saveBtn.setDisable(n.trim().isEmpty()));

        saveBar = new HBox(saveBtn);
        saveBar.setAlignment(Pos.CENTER_RIGHT);
        saveBar.setPadding(new Insets(14, 24, 14, 24));

        setTop(header);
        setCenter(scroll);
        setBottom(saveBar);

        addTableEntry();
        refreshTableCount();

        applyTheme();
        Theme.registerThemeListener(this, this::applyTheme);
    }

    private void applyTheme() {
        Platform.runLater(() -> {
            setStyle("-fx-background-color: white;");
            topBar.setStyle("-fx-background-color: " + Theme.colourDark + ";" +
                    "-fx-border-color: " + Theme.colourDark + "; -fx-border-width: 0 0 1 0;");
            title.setFill(Color.WHITE);

            backBtn.setStyle("-fx-background-color: rgba(255,255,255,0.12); -fx-text-fill: white;" +
                    "-fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 12;" +
                    "-fx-background-radius: 8; -fx-padding: 8 16;" +
                    "-fx-border-color: rgba(255,255,255,0.28); -fx-border-radius: 8; -fx-border-width: 1;");

            nameSection.setStyle("-fx-background-color: " + Theme.colour1 + ";" +
                    "-fx-border-color: " + Theme.colour3 + "; -fx-border-width: 0 0 1 0;");
            nameLabel.setTextFill(Color.web(Theme.colour6));
            schemaNameField.setStyle(fieldStyle());

            tableCountLabel.setTextFill(Color.BLACK);
            addTableBtn.setStyle(outlineBtnStyle());

            saveBar.setStyle("-fx-background-color: white;");
            saveBtn.setStyle(filledBtnStyle());
        });
    }

    private void addTableEntry() {
        TableEntry entry = new TableEntry(this);
        tableEntries.add(entry);
        tablesContainer.getChildren().add(entry);
        refreshTableCount();
    }

    void removeTable(TableEntry entry) {
        tableEntries.remove(entry);
        tablesContainer.getChildren().remove(entry);
        rebuildPKList();
        refreshTableCount();
    }

    private void refreshTableCount() {
        int n = tableEntries.size();
        tableCountLabel.setText(n + (n == 1 ? " Table" : " Tables"));
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
        root.refreshData();
        onDone.run();
    }

    static String fieldStyle() {
        return "-fx-background-color: " + Theme.colour1 + "; -fx-background-radius: 8;" +
                "-fx-border-color: " + Theme.colour3 + "; -fx-border-radius: 8;" +
                "-fx-text-fill: " + Theme.colour6 + ";" +
                "-fx-padding: 10 14; -fx-font-size: 13;";
    }

    static String filledBtnStyle() {
        return "-fx-background-color: " + Theme.colourDark + "; -fx-text-fill: white;" +
                "-fx-background-radius: 8; -fx-font-weight: bold;" +
                "-fx-cursor: hand; -fx-padding: 10 28; -fx-font-size: 13;";
    }

    static Button filledBtn(String label) {
        Button b = new Button(label);
        b.setStyle(filledBtnStyle());
        return b;
    }

    // ── modular outline, reused by outlineBtn and TableEntry cards ─────
    static String outlineStyle(String radius) {
        return "-fx-border-color: " + Theme.colourDark + "; -fx-border-width: 1;" +
                "-fx-border-radius: " + radius + ";";
    }

    static String outlineBtnStyle() {
        return "-fx-background-color: " + Theme.colour1 + "; -fx-text-fill: " + Theme.colour6 + ";" +
                outlineStyle("8") +
                "-fx-background-radius: 8; -fx-background-insets: 0; -fx-font-weight: bold;" +
                "-fx-cursor: hand; -fx-padding: 10 28; -fx-font-size: 13;";
    }

    static Button outlineBtn(String label) {
        Button b = new Button(label);
        b.setStyle(outlineBtnStyle());
        return b;
    }

    static ComboBox<String> greenCombo(ObservableList<String> items) {
        ComboBox<String> cb = new ComboBox<>(items);
        cb.setStyle("-fx-background-color: " + Theme.colourDark + "; -fx-background-radius: 8;" +
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
        private final Label emptyFieldsLabel;
        private final List<FieldEntry> fieldEntries = new ArrayList<>();

        TableEntry(SchemasAdd parent) {
            this.parent = parent;
            setSpacing(0);
            setStyle("-fx-background-color: " + Theme.colour1 + "; -fx-background-radius: 12;" +
                    outlineStyle("12"));

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
            nameRow.setStyle("-fx-border-color: " + Theme.colour3 + "; -fx-border-width: 0 0 1 0;");

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
            pkRow.setStyle("-fx-border-color: " + Theme.colour3 + "; -fx-border-width: 0 0 1 0;");

            fieldsContainer = new VBox(0);

            emptyFieldsLabel = new Label("No extra fields yet — add columns beyond the primary key.");
            emptyFieldsLabel.setStyle("-fx-text-fill: #A7AEB8; -fx-font-size: 12; -fx-padding: 12 16;");
            fieldsContainer.getChildren().add(emptyFieldsLabel);

            Button addFieldBtn = filledBtn("Add Field");
            addFieldBtn.setOnAction(e -> addFieldEntry());
            HBox addRow = new HBox(addFieldBtn);
            addRow.setAlignment(Pos.CENTER);
            addRow.setPadding(new Insets(14, 0, 14, 0));

            getChildren().addAll(nameRow, pkRow, fieldsContainer, addRow);
        }

        private void addFieldEntry() {
            fieldsContainer.getChildren().remove(emptyFieldsLabel);
            FieldEntry fe = new FieldEntry(this);
            fieldEntries.add(fe);
            fieldsContainer.getChildren().add(fe);
        }

        void removeField(FieldEntry fe) {
            fieldEntries.remove(fe);
            fieldsContainer.getChildren().remove(fe);
            if (fieldEntries.isEmpty()) fieldsContainer.getChildren().add(emptyFieldsLabel);
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
            setStyle("-fx-border-color: " + Theme.colour3 + "; -fx-border-width: 0 0 1 0;");
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