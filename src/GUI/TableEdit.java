package GUI;

import Objects.Field;
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

public class TableEdit extends VBox {

    private final SchemasRoot root;
    private final String schemaName;
    private final String originalTableName;

    private final TextField tableNameField;
    private final TextField pkNameField;
    private final ComboBox<String> pkTypeBox;
    private final VBox fieldsContainer;
    private final List<FieldEntry> fieldEntries = new ArrayList<>();
    final ObservableList<String> availablePKs = FXCollections.observableArrayList();

    public TableEdit(SchemasRoot root, String schemaName, Table table, List<String> schemaPKList) {
        this.root = root;
        this.schemaName = schemaName;
        this.originalTableName = table.getName();

        availablePKs.setAll(schemaPKList);

        setSpacing(20);
        setPadding(new Insets(28, 32, 28, 32));
        setStyle("-fx-background-color: #F2F4F2;");

        Button backBtn = new Button("← Back");
        backBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #2E5A47;" +
                "-fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 13;" +
                "-fx-border-color: transparent;");
        backBtn.setOnAction(e -> root.createTables());

        Text title = new Text("Edit Table  —  " + table.getName());
        title.setFont(Font.font("System", FontWeight.BOLD, 20));
        title.setFill(Color.web("#1E3D30"));

        HBox header = new HBox(14, backBtn, title);
        header.setAlignment(Pos.CENTER_LEFT);

        tableNameField = new TextField(table.getName());
        tableNameField.setPromptText("Table Name...");
        tableNameField.setMaxWidth(380);
        tableNameField.setStyle(SchemasAdd.fieldStyle());

        Field pkField = table.getFields().stream()
                .filter(Field::isPrimary)
                .findFirst()
                .orElse(null);

        pkNameField = new TextField(pkField != null ? pkField.getName() : "");
        pkNameField.setPromptText("Primary Key...");
        pkNameField.setDisable(true);
        pkNameField.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;" +
                "-fx-font-size: 13; -fx-padding: 12 16; -fx-opacity: 0.6;");
        HBox.setHgrow(pkNameField, Priority.ALWAYS);

        Label pkBadge = new Label("PK");
        pkBadge.setStyle("-fx-background-color: #2E5A47; -fx-text-fill: white;" +
                "-fx-background-radius: 4; -fx-font-size: 10; -fx-font-weight: bold;" +
                "-fx-padding: 2 6;");

        pkTypeBox = SchemasAdd.greenCombo(FXCollections.observableArrayList(SchemasAdd.SQL_TYPES));
        pkTypeBox.setValue(pkField != null ? normalizeType(pkField.getType()) : "INT");
        pkTypeBox.setPrefWidth(165);
        pkTypeBox.setDisable(true);
        pkTypeBox.setStyle(pkTypeBox.getStyle() + " -fx-opacity: 0.6;");

        HBox pkRow = new HBox(8, pkNameField, pkBadge, pkTypeBox);
        pkRow.setAlignment(Pos.CENTER_LEFT);
        pkRow.setPadding(new Insets(0, 14, 0, 0));
        pkRow.setStyle("-fx-border-color: #EEEEEE; -fx-border-width: 0 0 1 0;");

        fieldsContainer = new VBox(0);

        for (Field f : table.getFields()) {
            if (!f.isPrimary()) {
                addFieldEntry(f);
            }
        }

        Button addFieldBtn = SchemasAdd.filledBtn("Add Field");
        addFieldBtn.setOnAction(e -> addFieldEntry(null));
        HBox addRow = new HBox(addFieldBtn);
        addRow.setAlignment(Pos.CENTER);
        addRow.setPadding(new Insets(14, 0, 14, 0));

        VBox card = new VBox(pkRow, fieldsContainer, addRow);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12;" +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 14, 0, 0, 3);");

        ScrollPane scroll = new ScrollPane(card);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        Button saveBtn = SchemasAdd.filledBtn("Save Changes");
        saveBtn.setOnAction(e -> saveChanges());

        HBox bottomRow = new HBox(saveBtn);
        bottomRow.setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(header, tableNameField, scroll, bottomRow);
    }

    private void addFieldEntry(Field prefill) {
        FieldEntry fe = new FieldEntry(this, prefill);
        fieldEntries.add(fe);
        fieldsContainer.getChildren().add(fe);
    }

    void removeField(FieldEntry fe) {
        fieldEntries.remove(fe);
        fieldsContainer.getChildren().remove(fe);
    }

    private void saveChanges() {
        String newName = tableNameField.getText().trim();
        String pkName = pkNameField.getText().trim();

        if (newName.isEmpty()) { SchemasAdd.warn("Table name cannot be empty."); return; }
        if (pkName.isEmpty()) { SchemasAdd.warn("Primary key name cannot be empty."); return; }
        if (pkTypeBox.getValue() == null) { SchemasAdd.warn("Select a type for the primary key."); return; }

        Table updated = new Table(newName);
        updated.addField(new Field(null, true, pkTypeBox.getValue(), pkName));

        for (FieldEntry fe : fieldEntries) {
            Field f = fe.buildField();
            if (f == null) return;
            updated.addField(f);
        }

        db.EditTable(schemaName, originalTableName, updated);
        root.createTables();
    }

    private static String normalizeType(String type) {
        if (type == null) return "INT";
        String upper = type.toUpperCase();

        for (String t : SchemasAdd.SQL_TYPES) {
            if (t.equalsIgnoreCase(upper)) return t;
        }

        String baseUpper = upper.contains("(") ? upper.substring(0, upper.indexOf('(')) : upper;
        for (String t : SchemasAdd.SQL_TYPES) {
            String tBase = t.contains("(") ? t.substring(0, t.indexOf('(')).toUpperCase() : t.toUpperCase();
            if (tBase.equals(baseUpper)) return t;
        }

        return upper;
    }

    static class FieldEntry extends HBox {

        private final TextField nameField;
        private final ComboBox<String> typeBox;
        private final ComboBox<String> refBox;

        FieldEntry(TableEdit parent, Field prefill) {
            setAlignment(Pos.CENTER_LEFT);
            setStyle("-fx-border-color: #EEEEEE; -fx-border-width: 0 0 1 0;");
            setPadding(new Insets(0, 14, 0, 0));

            nameField = new TextField(prefill != null ? prefill.getName() : "");
            nameField.setPromptText("Field Name...");
            nameField.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;" +
                    "-fx-font-size: 13; -fx-padding: 12 16;");
            HBox.setHgrow(nameField, Priority.ALWAYS);

            typeBox = SchemasAdd.greenCombo(FXCollections.observableArrayList(SchemasAdd.SQL_TYPES));
            typeBox.setPromptText("Select Type");
            typeBox.setPrefWidth(165);
            if (prefill != null && prefill.getType() != null) {
                typeBox.setValue(normalizeType(prefill.getType()));
            }

            refBox = SchemasAdd.greenCombo(parent.availablePKs);
            refBox.setPromptText("Reference To");
            refBox.setPrefWidth(180);
            if (prefill != null && prefill.getReference() != null && !prefill.getReference().isEmpty()) {
                refBox.setValue(prefill.getReference());
            }

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
            if (type == null) { SchemasAdd.warn("Select a type for field: " + name); return null; }
            return new Field(refBox.getValue(), false, type, name);
        }
    }
}