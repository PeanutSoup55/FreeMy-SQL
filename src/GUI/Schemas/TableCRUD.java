package GUI.Schemas;

import GUI.Schemas.SchemasRoot;
import Objects.Field;
import Objects.Table;
import globalfuncs.dbcrud;
import globalfuncs.db;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import java.util.*;

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
        setStyle("-fx-background-color: #F4F6FA;");
        getChildren().addAll(buildHeader(), buildSplit());
        loadData();
    }

    private StackPane buildHeader() {
        Button backBtn = new Button("← Back");
        backBtn.setStyle("-fx-background-color: rgba(255,255,255,0.12); -fx-text-fill: white;" +
                "-fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 12;" +
                "-fx-background-radius: 8; -fx-padding: 8 16;" +
                "-fx-border-color: rgba(255,255,255,0.28); -fx-border-radius: 8; -fx-border-width: 1;");
        backBtn.setOnAction(e -> root.createTables());

        Text title = new Text(table.getName());
        title.setFont(Font.font("System", FontWeight.BOLD, 20));
        title.setFill(Color.WHITE);

        Label badge = new Label(schemaName);
        badge.setStyle("-fx-background-color: rgba(255,255,255,0.14); -fx-text-fill: white;" +
                "-fx-background-radius: 6; -fx-padding: 4 10;" +
                "-fx-font-weight: bold; -fx-font-size: 11;");

        HBox centerBox = new HBox(10, title, badge);
        centerBox.setAlignment(Pos.CENTER);

        HBox leftBox = new HBox(backBtn);
        leftBox.setAlignment(Pos.CENTER_LEFT);

        StackPane header = new StackPane();
        header.setPadding(new Insets(18, 24, 18, 24));
        header.setStyle("-fx-background-color: #1C2333;" +
                "-fx-border-color: #1C2333; -fx-border-width: 0 0 1 0;");
        StackPane.setAlignment(centerBox, Pos.CENTER);
        StackPane.setAlignment(leftBox, Pos.CENTER_LEFT);
        header.getChildren().addAll(centerBox, leftBox);
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
        sectionTitle.setTextFill(Color.web("#1C2333"));

        Button refreshBtn = new Button("↻");
        refreshBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #1C2333;" +
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
        // Columns proportionally fill available width instead of being fixed-width w/ h-scroll
        dataTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        dataTable.getSelectionModel().selectedItemProperty().addListener((obs, old, row) -> { if (row != null) populateFormForEdit(row); });

        ScrollPane tableScroll = new ScrollPane(dataTable);
        tableScroll.setFitToHeight(true);
        tableScroll.setFitToWidth(true);
        tableScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
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
        modeLabel.setFont(Font.font("System", FontWeight.BOLD, 15));
        modeLabel.setTextFill(Color.web("#1C2333"));

        statusLabel.setFont(Font.font("System", 12));
        statusLabel.setWrapText(true);

        Button newRowBtn = new Button("+ New Row");
        newRowBtn.setStyle("-fx-background-color: #1C2333; -fx-text-fill: white;" +
                "-fx-background-radius: 8; -fx-font-weight: bold;" +
                "-fx-cursor: hand; -fx-padding: 6 14; -fx-font-size: 12;");
        newRowBtn.setOnAction(e -> clearForm());

        Region formHeaderSpacer = new Region();
        HBox.setHgrow(formHeaderSpacer, Priority.ALWAYS);

        HBox formHeader = new HBox(10, modeLabel, formHeaderSpacer, newRowBtn);
        formHeader.setAlignment(Pos.CENTER_LEFT);
        formHeader.setPadding(new Insets(14, 16, 10, 16));
        formHeader.setStyle("-fx-background-color: white;" +
                "-fx-border-color: #EEEEEE; -fx-border-width: 0 0 1 0;");

        formContainer.setPadding(new Insets(14, 16, 14, 16));
        formContainer.setStyle("-fx-background-color: white;");

        ScrollPane formScroll = new ScrollPane(formContainer);
        formScroll.setFitToWidth(true);
        formScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        formScroll.setStyle("-fx-background-color: white; -fx-background: white;");
        VBox.setVgrow(formScroll, Priority.ALWAYS);

        saveBtn.setOnAction(e -> handleSave());
        clearBtn.setOnAction(e -> clearForm());

        HBox btnRow = new HBox(12, saveBtn, clearBtn);
        btnRow.setPadding(new Insets(12, 16, 12, 16));
        btnRow.setAlignment(Pos.CENTER_LEFT);
        btnRow.setStyle("-fx-background-color: white;" +
                "-fx-border-color: #EEEEEE; -fx-border-width: 1 0 0 0;");

        VBox statusBox = new VBox(4, statusLabel);
        statusBox.setPadding(new Insets(0, 16, 0, 16));
        statusBox.setStyle("-fx-background-color: white;");

        VBox section = new VBox(0, formHeader, formScroll, statusBox, btnRow);
        VBox.setVgrow(section, Priority.ALWAYS);
        section.setStyle("-fx-background-color: white;" +
                "-fx-border-color: #EEEEEE; -fx-border-width: 0 0 0 1;");
        return section;
    }

    private void buildFormControls() {
        formContainer.getChildren().clear();
        formControls.clear();

        for (Field field : table.getFields()) {
            if (field.isPrimary()) continue;

            boolean isFk = field.getReference() != null && !field.getReference().isEmpty();

            Label nameLabel = new Label(field.getName());
            nameLabel.setFont(Font.font("System", FontWeight.BOLD, 13));
            nameLabel.setTextFill(isFk ? Color.web("#8B5E3C") : Color.web("#1C2333"));
            nameLabel.setMinWidth(140);
            nameLabel.setPrefWidth(140);

            Label typeLabel = new Label(isFk ? "FK → " + field.getReference() : field.getType());
            typeLabel.setFont(Font.font("System", 11));
            typeLabel.setTextFill(Color.web("#AAAAAA"));

            VBox labelCol = new VBox(2, nameLabel, typeLabel);
            labelCol.setAlignment(Pos.CENTER_LEFT);
            labelCol.setMinWidth(160);

            Control input;
            if (isFk) {
                String ref = field.getReference();
                int paren = ref.indexOf('(');
                String refTbl = ref.substring(0, paren);
                String refCol = ref.substring(paren + 1, ref.length() - 1);

                List<String> vals = dbcrud.GetColumnValues(schemaName, refTbl, refCol);
                ComboBox<String> cb = new ComboBox<>(FXCollections.observableArrayList(vals));
                cb.setPromptText("Select from " + refTbl + "." + refCol + "…");
                cb.setMaxWidth(Double.MAX_VALUE);
                cb.setStyle("-fx-background-color: #1C2333; -fx-background-radius: 8; -fx-cursor: hand;");
                cb.setButtonCell(new ListCell<>() {
                    @Override protected void updateItem(String s, boolean empty) {
                        super.updateItem(s, empty);
                        setText(empty || s == null ? "" : s);
                        setTextFill(Color.WHITE);
                        setStyle("-fx-background-color: transparent;");
                    }
                });
                input = cb;
            } else {
                TextField tf = new TextField();
                tf.setPromptText(field.getType());
                tf.setStyle("-fx-background-color: #F7F9FC;" +
                        "-fx-background-radius: 8;" +
                        "-fx-border-color: #E2E6E2;" +
                        "-fx-border-radius: 8;" +
                        "-fx-padding: 10 14;" +
                        "-fx-font-size: 13;");
                input = tf;
            }
            HBox.setHgrow(input, Priority.ALWAYS);

            HBox row = new HBox(20, labelCol, input);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(7, 0, 7, 0));
            row.setStyle("-fx-border-color: #F0F0F0; -fx-border-width: 0 0 1 0;");

            formControls.put(field.getName(), input);
            formContainer.getChildren().add(row);
        }
    }


    private void loadData(){
        columnNames.clear();
        rows = db.GetTableData(schemaName, table.getName(), columnNames);

        pkColIndex = -1;
        if (pkField != null) {
            for (int i = 0; i < columnNames.size(); i++) {
                if (columnNames.get(i).equals(pkField.getName())) { pkColIndex = i; break; }
            }
        }

        rebuildTableColumns();
        dataTable.setItems(FXCollections.observableArrayList(rows));
        buildFormControls();
        clearForm();
        setStatus(rows.size() + " row" + (rows.size() == 1 ? "" : "s") + " loaded");
    }

    private void rebuildTableColumns() {
        dataTable.getColumns().clear();
        dataTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        for (int i = 0; i < columnNames.size(); i++) {
            final int col = i;
            TableColumn<String[], String> tc = new TableColumn<>(columnNames.get(col));
            tc.setCellValueFactory(data ->
                    new javafx.beans.property.SimpleStringProperty(
                            col < data.getValue().length ? data.getValue()[col] : ""));

            tc.setCellFactory(column -> new TableCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) { setText(null); setGraphic(null); return; }
                    setText(item);
                    setFont(Font.font("Monospace", 12));
                    setTextFill(Color.web("#2C2C2C"));
                    setStyle("-fx-background-color: " +
                            (getIndex() % 2 == 0 ? "#FFFFFF" : "#F7F9FC") +
                            "; -fx-padding: 0 12;" +
                            " -fx-border-color: #EEEEEE; -fx-border-width: 0 0 1 0;");
                }
            });

            boolean isPk = pkField != null && columnNames.get(col).equals(pkField.getName());
            tc.setPrefWidth(isPk ? 60 : 160);
            tc.setMinWidth(isPk ? 40 : 80);

            dataTable.getColumns().add(tc);
        }

        TableColumn<String[], Void> delCol = new TableColumn<>("");
        delCol.setPrefWidth(52);
        delCol.setMinWidth(52);
        delCol.setMaxWidth(52);
        delCol.setResizable(false);
        delCol.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("✕");
            {
                btn.setStyle("-fx-background-color: #FFF0F0; -fx-text-fill: #C0392B;" +
                        "-fx-background-radius: 6; -fx-cursor: hand;" +
                        "-fx-padding: 3 8; -fx-font-weight: bold;");
                btn.setOnAction(e -> deleteRow(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : btn);
                setStyle("-fx-background-color: " +
                        (getIndex() % 2 == 0 ? "#FFFFFF" : "#F7F9FC") + ";");
            }
        });
        dataTable.getColumns().add(delCol);

        if (dataTable.getSkin() != null) {
            applyHeaderStyle();
        } else {
            dataTable.skinProperty().addListener((obs, old, skin) -> {
                if (skin != null) applyHeaderStyle();
            });
        }
    }

    private void applyHeaderStyle() {
        Platform.runLater(() -> {
            dataTable.lookupAll(".column-header-background").forEach(n -> n.setStyle("-fx-background-color: #1C2333;"));
            dataTable.lookupAll(".column-header").forEach(n -> n.setStyle("-fx-background-color: transparent;" +
                    "-fx-border-color: transparent; -fx-size: 38;"));
            dataTable.lookupAll(".column-header > .label").forEach(n -> n.setStyle("-fx-text-fill: white; -fx-font-family: Monospace;" +
                    "-fx-font-size: 12px; -fx-font-weight: bold;" +
                    "-fx-alignment: CENTER-LEFT; -fx-padding: 0 12;"));
            dataTable.lookupAll(".filler").forEach(n -> n.setStyle("-fx-background-color: #1C2333;"));
        });
    }


    private void populateFormForEdit(String[] row) {
        selectedRow = row;
        String pkInfo = pkColIndex >= 0 ? " — " + pkField.getName() + " = " + row[pkColIndex] : "";
        modeLabel.setText("Editing Row" + pkInfo);
        saveBtn.setText("Update Row");
        saveBtn.setStyle(saveBtn.getStyle().replace("#1C2333", "#3D4C66"));

        for (Field field : table.getFields()) {
            if (field.isPrimary()) continue;
            Control ctrl = formControls.get(field.getName());
            if (ctrl == null) continue;

            int idx = columnNames.indexOf(field.getName());
            String val = (idx >= 0 && idx < row.length) ? row[idx] : "";

            if      (ctrl instanceof TextField tf)    tf.setText(val);
            else if (ctrl instanceof ComboBox<?> cb) {
                @SuppressWarnings("unchecked")
                ComboBox<String> scb = (ComboBox<String>) cb;
                scb.setValue(val);
            }
        }
        setStatus("Click Update Row to save changes, or Clear to cancel.");
    }

    private void clearForm() {
        selectedRow = null;
        dataTable.getSelectionModel().clearSelection();
        modeLabel.setText("Insert New Row");
        saveBtn.setText("Insert Row");
        saveBtn.setStyle("-fx-background-color: #1C2333; -fx-text-fill: white;" +
                "-fx-background-radius: 8; -fx-font-weight: bold;" +
                "-fx-cursor: hand; -fx-padding: 10 28; -fx-font-size: 13;");
        formControls.values().forEach(ctrl -> {
            if (ctrl instanceof TextField tf)    tf.clear();
            else if (ctrl instanceof ComboBox<?> cb)  cb.getSelectionModel().clearSelection();
        });
        setStatus("");
    }

    private void handleSave() {
        Map<String, String> values = new LinkedHashMap<>();
        for (Field field : table.getFields()) {
            if (field.isPrimary()) continue;
            Control ctrl = formControls.get(field.getName());
            if (ctrl == null) continue;

            String val = "";
            if (ctrl instanceof TextField tf)   val = tf.getText().trim();
            else if (ctrl instanceof ComboBox<?> cb) val = cb.getValue() != null ? cb.getValue().toString() : "";

            if (val.isEmpty()) { setError("'" + field.getName() + "' cannot be empty."); return; }
            values.put(field.getName(), val);
        }

        boolean ok;
        if (selectedRow == null) {
            ok = dbcrud.InsertRow(schemaName, table.getName(), values);
            if (ok) setStatus("Row inserted successfully.");
        } else {
            String pkVal = pkColIndex >= 0 ? selectedRow[pkColIndex] : null;
            ok = dbcrud.UpdateRow(schemaName, table.getName(), values, pkField.getName(), pkVal);
            if (ok) setStatus("Row updated successfully.");
        }

        if (ok) loadData();
        else setError("Operation failed — check console.");
    }


    private void deleteRow(String[] row) {
        if (pkColIndex < 0 || pkField == null) { setError("No primary key — cannot delete."); return; }
        String pkVal = row[pkColIndex];

        Map<String, List<String[]>> refs =
                dbcrud.GetReferencingRows(schemaName, table.getName(), pkField.getName(), pkVal, 20);

        if (refs.isEmpty()) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setHeaderText(null);
            confirm.setTitle("Delete Row");
            confirm.setContentText("Delete row where " + pkField.getName() + " = " + pkVal + "?");
            confirm.getButtonTypes().setAll(ButtonType.OK, ButtonType.CANCEL);
            styleConfirmDialog(confirm);
            confirm.showAndWait().ifPresent(bt -> {
                if (bt == ButtonType.OK) {
                    boolean ok = dbcrud.DeleteRow(schemaName, table.getName(), pkField.getName(), pkVal);
                    if (ok) { loadData(); setStatus("Row deleted."); }
                    else      setError("Delete failed — check console.");
                }
            });
            return;
        }

        showFKDeleteDialog(pkVal, refs);
    }

    private void showFKDeleteDialog(String pkVal, Map<String, List<String[]>> refs) {
        Dialog<ButtonType> optDialog = new Dialog<>();
        optDialog.setTitle("Referenced Data Found");
        optDialog.setHeaderText(null);

        ButtonType cascadeType  = new ButtonType("Delete Children",  ButtonBar.ButtonData.OTHER);
        ButtonType setNullType  = new ButtonType("Set Children Null", ButtonBar.ButtonData.OTHER);
        ButtonType cancelType   = ButtonType.CANCEL;
        optDialog.getDialogPane().getButtonTypes().addAll(cascadeType, setNullType, cancelType);

        int totalRefs = refs.values().stream().mapToInt(l -> l.size() - 1).sum();

        Label banner = new Label("Foreign Key Conflict");
        banner.setTextFill(Color.WHITE);
        banner.setFont(Font.font("System", FontWeight.BOLD, 14));

        HBox bannerBox = new HBox(banner);
        bannerBox.setPadding(new Insets(12, 16, 12, 16));
        bannerBox.setStyle("-fx-background-color: #8A3A3A; -fx-background-radius: 8 8 0 0;");

        Label summary = new Label(
                "Deleting " + pkField.getName() + " = " + pkVal + " will affect " +
                        totalRefs + " child row" + (totalRefs == 1 ? "" : "s") +
                        " across " + refs.size() + " table" + (refs.size() == 1 ? "" : "s") + ".\n" +
                        "Choose how to handle the children:");
        summary.setWrapText(true);
        summary.setStyle("-fx-text-fill: #333; -fx-font-size: 13;");

        VBox tables = new VBox(10);

        for (Map.Entry<String, List<String[]>> entry : refs.entrySet()) {
            String key = entry.getKey();
            List<String[]> data = entry.getValue();
            int dataRows = data.size() - 1;

            String[] headers = data.get(0);

            Label tableLabel = new Label(key + "  (" + dataRows + " row" + (dataRows == 1 ? "" : "s") +
                    (dataRows >= 20 ? " — showing first 20" : "") + ")");
            tableLabel.setFont(Font.font("System", FontWeight.BOLD, 12));
            tableLabel.setTextFill(Color.web("#8B5E3C"));


            GridPane grid = new GridPane();
            grid.setHgap(16); grid.setVgap(4);
            grid.setPadding(new Insets(6, 10, 6, 10));
            grid.setStyle("-fx-background-color: #FDF6EE; -fx-background-radius: 6;");

            for (int c = 0; c < headers.length; c++) {
                Label h = new Label(headers[c]);
                h.setFont(Font.font("Monospace", FontWeight.BOLD, 10));
                h.setTextFill(Color.web("#888888"));
                grid.add(h, c, 0);
            }
            for (int r = 1; r < data.size(); r++) {
                String[] dRow = data.get(r);
                for (int c = 0; c < dRow.length; c++) {
                    Label cell = new Label(dRow[c]);
                    cell.setFont(Font.font("Monospace", 10));
                    cell.setTextFill(Color.web("#333333"));
                    grid.add(cell, c, r);
                }
            }

            tables.getChildren().addAll(tableLabel, grid);
        }

        ScrollPane previewScroll = new ScrollPane(tables);
        previewScroll.setFitToWidth(true);
        previewScroll.setPrefHeight(Math.min(300, 80 + totalRefs * 22));
        previewScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        VBox content = new VBox(12, summary, new Separator(), previewScroll);
        content.setPadding(new Insets(14));

        VBox wrapper = new VBox(bannerBox, content);
        wrapper.setStyle("-fx-background-color: white; -fx-background-radius: 8;");
        optDialog.getDialogPane().setContent(wrapper);
        optDialog.getDialogPane().setPrefWidth(560);

        Platform.runLater(() -> {
            optDialog.getDialogPane().lookupButton(cascadeType).setStyle("-fx-background-color: #C0392B; -fx-text-fill: white;" +
                    "-fx-background-radius: 6; -fx-font-weight: bold; -fx-cursor: hand;");
            optDialog.getDialogPane().lookupButton(setNullType).setStyle("-fx-background-color: #3D4C66; -fx-text-fill: white;" +
                    "-fx-background-radius: 6; -fx-font-weight: bold; -fx-cursor: hand;");
            optDialog.getDialogPane().lookupButton(cancelType).setStyle("-fx-background-color: transparent; -fx-text-fill: #555;" +
                    "-fx-font-weight: bold; -fx-cursor: hand;");
        });

        optDialog.showAndWait().ifPresent(bt -> {
            if (bt == cascadeType)  confirmAndExecute(pkVal, true);
            else if (bt == setNullType) confirmAndExecute(pkVal, false);
        });
    }

    private void confirmAndExecute(String pkVal, boolean cascade) {
        String action = cascade ? "permanently delete ALL child rows" : "set child FK columns to NULL";

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle(cascade ? "Confirm Cascade Delete" : "Confirm Set Null");
        confirm.setHeaderText(null);
        confirm.setContentText(
                "This will " + action + " before deleting the row where\n" +
                        pkField.getName() + " = " + pkVal + ".\n\nThis cannot be undone. Continue?");

        ButtonType yesBtn = new ButtonType("Yes, proceed", ButtonBar.ButtonData.OK_DONE);
        confirm.getButtonTypes().setAll(yesBtn, ButtonType.CANCEL);
        styleConfirmDialog(confirm);

        Platform.runLater(() -> confirm.getDialogPane().lookupButton(yesBtn).setStyle(
                "-fx-background-color: " + (cascade ? "#C0392B" : "#3D4C66") +
                        "; -fx-text-fill: white; -fx-background-radius: 6;" +
                        " -fx-font-weight: bold; -fx-cursor: hand;"));

        confirm.showAndWait().ifPresent(bt -> {
            if (bt == yesBtn) {
                boolean ok = cascade
                        ? dbcrud.DeleteRowCascade(schemaName, table.getName(), pkField.getName(), pkVal)
                        : dbcrud.DeleteRowSetNull(schemaName, table.getName(), pkField.getName(), pkVal);
                if (ok) {
                    loadData();
                    setStatus(cascade ? "Row and all children deleted." : "Row deleted, children set to NULL.");
                } else {
                    setError("Operation failed — check console.");
                }
            }
        });
    }

    private static void styleConfirmDialog(Alert alert) {
        alert.getDialogPane().setStyle("-fx-background-color: white; -fx-background-radius: 8;");
        Platform.runLater(() -> alert.getDialogPane().lookupButton(ButtonType.CANCEL).setStyle(
                "-fx-background-color: transparent; " +
                        "-fx-text-fill: #555; " +
                        "-fx-font-weight: bold;"));
    }

    private void setStatus(String msg) {
        statusLabel.setText(msg);
        statusLabel.setTextFill(Color.web("#1C2333"));
    }

    private void setError(String msg) {
        statusLabel.setText(msg);
        statusLabel.setTextFill(Color.web("#C0392B"));
    }

    private static Label styledPlaceholder(String msg) {
        Label l = new Label(msg);
        l.setTextFill(Color.web("#AAAAAA"));
        l.setFont(Font.font("System", 13));
        return l;
    }


    private static Button filledBtn(String label) {
        Button b = new Button(label);
        b.setStyle("-fx-background-color: #1C2333; -fx-text-fill: white;" +
                "-fx-background-radius: 8; -fx-font-weight: bold;" +
                "-fx-cursor: hand; -fx-padding: 10 28; -fx-font-size: 13;");
        return b;
    }

    private static Button outlineBtn(String label) {
        Button b = new Button(label);
        b.setStyle("-fx-background-color: white; -fx-text-fill: #1C2333;" +
                "-fx-border-color: #1C2333; -fx-border-radius: 8;" +
                "-fx-background-radius: 8; -fx-font-weight: bold;" +
                "-fx-cursor: hand; -fx-padding: 10 28; -fx-font-size: 13;");
        return b;
    }
}