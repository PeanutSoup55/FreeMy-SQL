package GUI.Schemas;

import GUI.Schemas.SchemasRoot;
import Objects.Field;
import Objects.Table;
import globalfuncs.db;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
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
    private final BorderPane hostPane;
    private final String originalTableName;

    private final TextField tableNameField;
    private final TextField pkNameField;
    private final ComboBox<String> pkTypeBox;
    private final VBox fieldsContainer;
    private final List<FieldEntry> fieldEntries = new ArrayList<>();
    final ObservableList<String> availablePKs = FXCollections.observableArrayList();

    public TableEdit(SchemasRoot root, String schemaName, Table table, List<String> schemaPKList) {
        this(root, root, schemaName, table, schemaPKList);
    }

    public TableEdit(SchemasRoot root, BorderPane hostPane, String schemaName, Table table, List<String> schemaPKList) {
        this.root = root;
        this.hostPane = hostPane;
        this.schemaName = schemaName;
        this.originalTableName = table.getName();

        availablePKs.setAll(schemaPKList);

        setSpacing(10);
        setPadding(new Insets(14));
        setStyle("-fx-background-color: #1C2333;");
        setPrefWidth(400);
        setMaxWidth(400);

        Button backBtn = new Button("← Back");
        backBtn.setStyle("-fx-background-color: rgba(255,255,255,0.12); -fx-text-fill: white;" +
                "-fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 12;" +
                "-fx-background-radius: 8; -fx-padding: 8 16;" +
                "-fx-border-color: rgba(255,255,255,0.28); -fx-border-radius: 8; -fx-border-width: 1;");
        backBtn.setOnAction(e -> {
            hostPane.setRight(null);
        });

        Text title = new Text("Edit Table: " + table.getName());
        title.setFont(Font.font("System", FontWeight.BOLD, 15));
        title.setFill(Color.web("#EDEFF4"));
        title.setWrappingWidth(370);

        VBox header = new VBox(4, backBtn, title);

        tableNameField = new TextField(table.getName());
        tableNameField.setPromptText("Table Name...");
        tableNameField.setMaxWidth(Double.MAX_VALUE);
        tableNameField.setStyle("-fx-background-color: #1F2330; -fx-border-color: #343B4D;" +
                "-fx-border-radius: 6; -fx-background-radius: 6; -fx-text-fill: #EDEFF4;" +
                "-fx-font-size: 13; -fx-padding: 8 10;");

        Field pkField = table.getFields().stream()
                .filter(Field::isPrimary)
                .findFirst()
                .orElse(null);

        // --- PK card ---
        pkNameField = new TextField(pkField != null ? pkField.getName() : "");
        pkNameField.setPromptText("Primary Key...");
        pkNameField.setDisable(true);
        pkNameField.setMaxWidth(Double.MAX_VALUE);
        pkNameField.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;" +
                "-fx-text-fill: #B7BDCC; -fx-font-size: 12; -fx-padding: 0;");

        Label pkBadge = new Label("PK");
        pkBadge.setStyle("-fx-background-color: #6E9BFF; -fx-text-fill: #14171F;" +
                "-fx-background-radius: 3; -fx-font-size: 9; -fx-font-weight: bold;" +
                "-fx-padding: 2 6;");

        pkTypeBox = SchemasAdd.greenCombo(FXCollections.observableArrayList(SchemasAdd.SQL_TYPES));
        pkTypeBox.setValue(pkField != null ? normalizeType(pkField.getType()) : "INT");
        pkTypeBox.setPrefWidth(130);
        pkTypeBox.setDisable(true);
        pkTypeBox.setStyle("-fx-background-color: #2A2F3F; -fx-text-fill: #8B92A6;" +
                "-fx-font-size: 11; -fx-background-radius: 5;");

        HBox pkNameRow = new HBox(6, pkNameField, pkBadge);
        pkNameRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(pkNameField, Priority.ALWAYS);

        VBox pkCard = new VBox(6, pkNameRow, pkTypeBox);
        pkCard.setPadding(new Insets(10, 12, 10, 12));
        pkCard.setStyle("-fx-background-color: #1B1E29; -fx-background-radius: 7;" +
                "-fx-border-color: #262B3A; -fx-border-radius: 7; -fx-border-width: 1;");

        // --- Field list ---
        fieldsContainer = new VBox(8);

        for (Field f : table.getFields()) {
            if (!f.isPrimary()) {
                addFieldEntry(f);
            }
        }

        VBox fieldsBlock = new VBox(8, pkCard, fieldsContainer);

        Button addFieldBtn = new Button("+ Add Field");
        addFieldBtn.setMaxWidth(Double.MAX_VALUE);
        addFieldBtn.setStyle("-fx-background-color: #232838; -fx-text-fill: #6E9BFF;" +
                "-fx-font-size: 12; -fx-font-weight: bold; -fx-background-radius: 6;" +
                "-fx-border-color: #343B4D; -fx-border-radius: 6; -fx-border-width: 1;" +
                "-fx-cursor: hand; -fx-padding: 9 0;");
        addFieldBtn.setOnAction(e -> addFieldEntry(null));

        VBox card = new VBox(10, fieldsBlock, addFieldBtn);
        card.setPadding(new Insets(2, 0, 0, 0));

        ScrollPane scroll = new ScrollPane(card);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        Button saveBtn = new Button("Save Changes");
        saveBtn.setMaxWidth(Double.MAX_VALUE);
        saveBtn.setStyle("-fx-background-color: #6E9BFF; -fx-text-fill: #14171F;" +
                "-fx-font-size: 13; -fx-font-weight: bold; -fx-background-radius: 6;" +
                "-fx-cursor: hand; -fx-padding: 10 0;");
        saveBtn.setOnAction(e -> saveChanges());

        getChildren().addAll(header, tableNameField, scroll, saveBtn);
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

    static class FieldEntry extends VBox {

        private final TextField nameField;
        private final ComboBox<String> typeBox;
        private final ComboBox<String> refBox;
        private final String originalName;

        FieldEntry(TableEdit parent, Field prefill) {
            setSpacing(6);
            setPadding(new Insets(10, 12, 10, 12));
            setStyle("-fx-background-color: #1B1E29; -fx-background-radius: 7;" +
                    "-fx-border-color: #262B3A; -fx-border-radius: 7; -fx-border-width: 1;");

            this.originalName = prefill != null ? prefill.getName() : null;

            nameField = new TextField(prefill != null ? prefill.getName() : "");
            nameField.setPromptText("Field Name...");
            nameField.setMaxWidth(Double.MAX_VALUE);
            nameField.setStyle("-fx-background-color: transparent; -fx-border-color: transparent;" +
                    "-fx-text-fill: #EDEFF4; -fx-font-size: 13; -fx-padding: 0;");

            typeBox = SchemasAdd.greenCombo(FXCollections.observableArrayList(SchemasAdd.SQL_TYPES));
            typeBox.setPromptText("Type");
            typeBox.setMaxWidth(Double.MAX_VALUE);
            typeBox.setStyle("-fx-background-color: #2A2F3F; -fx-text-fill: #EDEFF4;" +
                    "-fx-font-size: 11; -fx-background-radius: 5;");
            if (prefill != null && prefill.getType() != null) {
                typeBox.setValue(normalizeType(prefill.getType()));
            }

            refBox = SchemasAdd.greenCombo(parent.availablePKs);
            refBox.setPromptText("No reference");
            refBox.setMaxWidth(Double.MAX_VALUE);
            refBox.setStyle("-fx-background-color: #2A2F3F; -fx-text-fill: #8B92A6;" +
                    "-fx-font-size: 11; -fx-background-radius: 5;");
            if (prefill != null && prefill.getReference() != null && !prefill.getReference().isEmpty()) {
                refBox.setValue(prefill.getReference());
                refBox.setStyle("-fx-background-color: #2A2F3F; -fx-text-fill: #EDEFF4;" +
                        "-fx-font-size: 11; -fx-background-radius: 5;");
            }

            Button removeBtn = new Button("Remove");
            removeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #6B7286;" +
                    "-fx-cursor: hand; -fx-font-size: 10; -fx-padding: 4 0 0 0;" +
                    "-fx-underline: true;");
            removeBtn.setOnAction(e -> parent.removeField(this));

            HBox typeRefRow = new HBox(6, typeBox, refBox);
            HBox.setHgrow(typeBox, Priority.ALWAYS);
            HBox.setHgrow(refBox, Priority.ALWAYS);

            HBox bottomRow = new HBox(removeBtn);
            bottomRow.setAlignment(Pos.CENTER_RIGHT);

            getChildren().addAll(nameField, typeRefRow, bottomRow);
        }

        Field buildField() {
            String name = nameField.getText().trim();
            String type = typeBox.getValue();
            if (name.isEmpty()) { SchemasAdd.warn("Field name cannot be empty."); return null; }
            if (type == null) { SchemasAdd.warn("Select a type for field: " + name); return null; }
            Field f = new Field(refBox.getValue(), false, type, name);
            if (originalName != null) f.setOldName(originalName); // ADD THIS
            return f;
        }
    }
}