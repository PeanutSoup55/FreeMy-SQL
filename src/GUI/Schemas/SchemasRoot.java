package GUI.Schemas;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import globalfuncs.db;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import Objects.*;

import java.util.*;

public class SchemasRoot extends BorderPane {
    public static List<Schema> schemas = db.Schemas();

    private final Map<String, HBox> rowNodeMap  = new HashMap<>();
    private final Map<String, VBox> cardNodeMap = new HashMap<>();
    private final TabPane  dataTabPane = new TabPane();
    private SplitPane mainSplit  = null;
    private String selectedSchemaName = "";
    private boolean isRemoteSelected = false;
    private static final java.util.prefs.Preferences PREFS = java.util.prefs.Preferences.userRoot().node("Free_My_SQL/table_positions");
    private static final java.util.prefs.Preferences REMOTE_LINK_PREFS =
            java.util.prefs.Preferences.userRoot().node("Free_My_SQL/remote_linked_schemas");

    public static boolean isRemoteLinked(String schemaName) {
        return REMOTE_LINK_PREFS.getBoolean(schemaName, false);
    }

    public static void markRemoteLinked(String schemaName) {
        REMOTE_LINK_PREFS.putBoolean(schemaName, true);
    }

    public static void clearRemoteLink(String schemaName) {
        REMOTE_LINK_PREFS.remove(schemaName);
    }

    public static List<Schema> remoteSchemas = new ArrayList<>();
    private double scale = 1.0;
    private static final double SCALE_MIN = 0.3;
    private static final double SCALE_MAX = 3.0;

    public void setSelectedSchema(String schemaName, boolean remote) {
        this.selectedSchemaName = schemaName;
        this.isRemoteSelected = remote;
    }

    public SchemasRoot() {
        createTables();
    }

    private Runnable onRequestNavigateHome = () -> {};

    public void setOnRequestNavigateHome(Runnable callback) {
        this.onRequestNavigateHome = callback;
    }


    public void createTables() {
        mainSplit = null;
        dataTabPane.getTabs().clear();
        rowNodeMap.clear();
        cardNodeMap.clear();

        String selectedSchema = !selectedSchemaName.isEmpty()
                ? selectedSchemaName
                : (schemas.isEmpty() ? "" : schemas.getFirst().getName());

        Schema schema = isRemoteSelected
                ? db.GetTablesInSchemaRemote(selectedSchema)
                : db.GetTablesInSchema(selectedSchema);
        List<String[]> foreignKeys = isRemoteSelected
                ? db.GetForeignKeysRemote(selectedSchema)
                : db.GetForeignKeys(selectedSchema);

        final double CANVAS_W = 3000;
        final double CANVAS_H = 3000;
        // Tables will be laid out around this center point
        final double CENTER_X = CANVAS_W / 2.0;
        final double CENTER_Y = CANVAS_H / 2.0;

        javafx.scene.canvas.Canvas gridCanvas =
                new javafx.scene.canvas.Canvas(CANVAS_W, CANVAS_H);
        drawGrid(gridCanvas, CANVAS_W, CANVAS_H);

        Pane cardPane = new Pane();
        cardPane.setMinSize(CANVAS_W, CANVAS_H);
        cardPane.setPrefSize(CANVAS_W, CANVAS_H);

        Pane overlay = new Pane();
        overlay.setMouseTransparent(true);
        overlay.setMinSize(CANVAS_W, CANVAS_H);
        overlay.setPrefSize(CANVAS_W, CANVAS_H);

        StackPane world = new StackPane(gridCanvas, cardPane, overlay);
        world.setAlignment(javafx.geometry.Pos.TOP_LEFT);
        world.setMinSize(CANVAS_W, CANVAS_H);
        world.setPrefSize(CANVAS_W, CANVAS_H);

        Pane viewport = new Pane(world);
        viewport.setStyle("-fx-background-color: #F4F5F9;");
        viewport.layoutBoundsProperty().addListener((obs, o, n) ->
                viewport.setClip(new javafx.scene.shape.Rectangle(n.getWidth(), n.getHeight())));

        final double[] tx = {0};
        final double[] ty = {0};

        Runnable applyTransform = () -> {
            world.setTranslateX(tx[0] - (1 - scale) * CANVAS_W / 2.0);
            world.setTranslateY(ty[0] - (1 - scale) * CANVAS_H / 2.0);
            world.setScaleX(scale);
            world.setScaleY(scale);
        };

        Map<String, double[]> layout = computeForceLayout(schema.getTables(), foreignKeys);
        separateCards(layout, schema.getTables());

        // Compute bounding box of the layout so we can center it
        double minLx = Double.MAX_VALUE, minLy = Double.MAX_VALUE;
        double maxLx = Double.MIN_VALUE, maxLy = Double.MIN_VALUE;
        for (double[] pos : layout.values()) {
            minLx = Math.min(minLx, pos[0]);
            minLy = Math.min(minLy, pos[1]);
            maxLx = Math.max(maxLx, pos[0]);
            maxLy = Math.max(maxLy, pos[1]);
        }
        // Offset to apply to each card so the group is centered on CENTER_X/Y
        final double offsetX = CENTER_X - (minLx + maxLx) / 2.0;
        final double offsetY = CENTER_Y - (minLy + maxLy) / 2.0;

        // keep your original card loop exactly as it was
        int i = 0;
        for (Table table : schema.getTables()) {
            VBox card = buildCard(table);
            double[] saved = loadPosition(selectedSchema, table.getName());
            if (saved != null) {
                card.setLayoutX(saved[0]);
                card.setLayoutY(saved[1]);
            } else {
                double[] computed = layout.getOrDefault(table.getName(),
                        new double[]{20 + (i % 4) * 260.0, 20 + (i / 4) * 280.0});
                card.setLayoutX(computed[0] + offsetX);
                card.setLayoutY(computed[1] + offsetY);
            }
            makeDraggable(card, overlay, foreignKeys, world, selectedSchema, table.getName());
            cardPane.getChildren().add(card);
            i++;
        }

// compute center from actual placed positions
        double sumX = 0, sumY = 0;
        for (javafx.scene.Node n : cardPane.getChildren()) {
            sumX += ((VBox) n).getLayoutX();
            sumY += ((VBox) n).getLayoutY();
        }
        final double centerX = cardPane.getChildren().isEmpty() ? CANVAS_W / 2.0 : sumX / cardPane.getChildren().size();
        final double centerY = cardPane.getChildren().isEmpty() ? CANVAS_H / 2.0 : sumY / cardPane.getChildren().size();

        viewport.layoutBoundsProperty().addListener(new javafx.beans.value.ChangeListener<>() {
            public void changed(javafx.beans.value.ObservableValue<? extends javafx.geometry.Bounds> o,
                                javafx.geometry.Bounds a, javafx.geometry.Bounds b) {
                if (b.getWidth() > 0) {
                    tx[0] = b.getWidth()  / 2.0 - centerX * scale;
                    ty[0] = b.getHeight() / 2.0 - centerY * scale;
                    applyTransform.run();
                    viewport.layoutBoundsProperty().removeListener(this);
                }
            }
        });

        // Middle-mouse pan
        final double[] panStart = {0, 0, 0, 0};
        viewport.setOnMousePressed(e -> {
            if (e.isMiddleButtonDown()) {
                panStart[0] = e.getX(); panStart[1] = e.getY();
                panStart[2] = tx[0];   panStart[3] = ty[0];
                viewport.setCursor(Cursor.MOVE);
            }
        });
        viewport.setOnMouseDragged(e -> {
            if (e.isMiddleButtonDown()) {
                tx[0] = panStart[2] + e.getX() - panStart[0];
                ty[0] = panStart[3] + e.getY() - panStart[1];
                applyTransform.run();
                drawConnectors(overlay, foreignKeys, world);
            }
        });
        viewport.setOnMouseReleased(e -> viewport.setCursor(Cursor.DEFAULT));

        // Scroll = zoom toward cursor, Shift+Scroll = pan left/right
        viewport.setOnScroll(e -> {
            if (e.isShiftDown()) {
                tx[0] -= e.getDeltaY();
                applyTransform.run();
                drawConnectors(overlay, foreignKeys, world);
            } else {
                double oldScale = scale;
                scale = Math.max(SCALE_MIN, Math.min(SCALE_MAX,
                        scale * (e.getDeltaY() > 0 ? 1.1 : 1.0 / 1.1)));
                if (scale == oldScale) return;
                tx[0] = e.getX() - (e.getX() - tx[0]) * (scale / oldScale);
                ty[0] = e.getY() - (e.getY() - ty[0]) * (scale / oldScale);
                applyTransform.run();
                drawConnectors(overlay, foreignKeys, world);
            }
            e.consume();
        });

        viewport.setVisible(false);
        setRight(null);
        setCenter(viewport);
        Platform.runLater(() -> {
            tx[0] = viewport.getWidth()  / 2.0 - (CANVAS_W / 2.0) * scale;
            ty[0] = viewport.getHeight() / 2.0 - (CANVAS_H / 2.0) * scale;
            applyTransform.run();
            drawConnectors(overlay, foreignKeys, world);
            viewport.setVisible(true);
        });
    }

    private void drawGrid(javafx.scene.canvas.Canvas gridCanvas, double w, double h) {
        javafx.scene.canvas.GraphicsContext gc = gridCanvas.getGraphicsContext2D();
        gc.setFill(javafx.scene.paint.Color.web("#F4F5F9"));
        gc.fillRect(0, 0, w, h);

        final double CELL = 28;
        gc.setFill(javafx.scene.paint.Color.web("#C8D0C8"));
        for (double x = 0; x < w; x += CELL) {
            for (double y = 0; y < h; y += CELL) {
                gc.fillOval(x - 1.2, y - 1.2, 2.4, 2.4);
            }
        }
    }

    public VBox buildCard(Table table) {
        return buildCard(table, this, () -> {}, () -> {});
    }

    public VBox buildCard(Table table, BorderPane editHost, Runnable beforeNavigate, Runnable afterDelete) {
        String selectedSchemaName = !this.selectedSchemaName.isEmpty()
                ? this.selectedSchemaName
                : (schemas.isEmpty() ? "" : schemas.getFirst().getName());

        VBox card = new VBox();
        card.setStyle("-fx-background-radius: 10;" +
                "-fx-background-color: #FFFFFF;" +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.18), 10, 0, 0, 3);");
        card.setMinWidth(200);
        card.setPrefWidth(Region.USE_COMPUTED_SIZE);
        card.setMaxWidth(Region.USE_COMPUTED_SIZE);

        Label hamburger = new Label("☰");
        hamburger.setTextFill(Color.WHITE);
        hamburger.setFont(Font.font("System", FontWeight.BOLD, 18));
        hamburger.setCursor(Cursor.HAND);
        hamburger.setPadding(new Insets(0, 4, 0, 4));

        ContextMenu menu = new ContextMenu();
        menu.setStyle("-fx-background-color: #FFFFFF; -fx-background-radius: 8;" +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.18), 10, 0, 0, 3);" +
                "-fx-selection-bar: #EAEAEA;" +
                "-fx-selection-bar-text: #333333;");

        MenuItem showDataItem  = makeMenuItem("Show Data");
        MenuItem crudItem = makeMenuItem("CRUD Operations");
        MenuItem editItem = makeMenuItem("Edit Table");
        MenuItem deleteItem = makeMenuItem("Delete Table");
        deleteItem.setStyle(deleteItem.getStyle() + "-fx-text-fill: #c0392b;");

        menu.getItems().addAll(showDataItem, crudItem, new SeparatorMenuItem(), editItem, deleteItem);

        hamburger.setOnMouseClicked(e -> {
            menu.show(hamburger, javafx.geometry.Side.BOTTOM, 0, 4);
            e.consume();
        });

        showDataItem.setOnAction(e -> {
            beforeNavigate.run();
            showTableData(selectedSchemaName, table);
        });

        crudItem.setOnAction(e -> {
            beforeNavigate.run();
            setCenter(new TableCRUD(this, selectedSchemaName, table));
        });

        editItem.setOnAction(e -> {
            Schema fullSchema = db.GetTablesInSchema(selectedSchemaName);
            List<String> pks = new ArrayList<>();
            for (Table t : fullSchema.getTables()) {
                for (Field f : t.getFields()) {
                    if (f.isPrimary()) {
                        pks.add(t.getName() + "(" + f.getName() + ")");
                    }
                }
            }
            TableEdit editTable = new TableEdit(this, editHost, selectedSchemaName, table, pks);
            editHost.setRight(editTable);
            editTable.setPrefWidth(400);
        });

        deleteItem.setOnAction(e -> {
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Delete Table");
            dialog.setHeaderText(null);

            ButtonType deleteButtonType = new ButtonType("Delete", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(deleteButtonType, ButtonType.CANCEL);

            Label headerLabel = new Label("Delete Table");
            headerLabel.setTextFill(Color.WHITE);
            headerLabel.setFont(Font.font("System", FontWeight.BOLD, 14));

            HBox headerBox = new HBox(headerLabel);
            headerBox.setPadding(new Insets(10, 12, 10, 12));
            headerBox.setStyle("-fx-background-color: #2E5A47; -fx-background-radius: 8 8 0 0;");

            Label warning = new Label("This will permanently delete the table and all its data.");
            warning.setStyle("-fx-text-fill: #444;");

            Label instruction = new Label("Type '" + table.getName() + "' to confirm:");
            instruction.setStyle("-fx-font-weight: 600;");

            TextField input = new TextField();
            input.setPromptText(table.getName());
            input.setStyle("-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: #CCCCCC; -fx-padding: 6;");

            VBox content = new VBox(10);
            content.setPadding(new Insets(12));

            String connections = db.getTableConnections(selectedSchemaName, table.getName());
            if (!connections.isEmpty()) {
                Label connectionLabel = new Label("Warning: Foreign Key Connections Found");
                connectionLabel.setStyle("-fx-text-fill: #c0392b; -fx-font-weight: bold;");

                TextArea connectionArea = new TextArea(connections);
                connectionArea.setEditable(false);
                connectionArea.setPrefHeight(100);
                connectionArea.setStyle("-fx-font-family: 'Monospace'; -fx-font-size: 11px;");

                content.getChildren().addAll(connectionLabel, connectionArea, new Separator());
            }
            content.getChildren().addAll(warning, instruction, input);

            VBox wrapper = new VBox(headerBox, content);
            wrapper.setStyle("-fx-background-color: white; -fx-background-radius: 8;");
            dialog.getDialogPane().setContent(wrapper);

            Node deleteButton = dialog.getDialogPane().lookupButton(deleteButtonType);
            Node cancelButton = dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
            deleteButton.setDisable(true);
            deleteButton.setStyle("-fx-background-color: #CCCCCC; -fx-text-fill: white; -fx-background-radius: 6;");
            cancelButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #2E5A47; -fx-font-weight: 600;");

            input.textProperty().addListener((obs, oldVal, newVal) -> {
                boolean valid = newVal.equals(table.getName());
                deleteButton.setDisable(!valid);
                deleteButton.setStyle(valid
                        ? "-fx-background-color: #c0392b; -fx-text-fill: white; -fx-background-radius: 6; -fx-cursor: hand;"
                        : "-fx-background-color: #CCCCCC; -fx-text-fill: white; -fx-background-radius: 6;");
            });

            Optional<ButtonType> result = dialog.showAndWait();
            if (result.isPresent() && result.get() == deleteButtonType) {
                PREFS.remove(selectedSchemaName + "|" + table.getName());
                db.deleteTable(new Schema(selectedSchemaName), table);
                createTables();
                afterDelete.run();
            }
        });

        Label title = new Label(table.getName());
        title.setTextFill(Color.WHITE);
        title.setFont(Font.font("System", FontWeight.BOLD, 13));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(title, spacer, hamburger);
        header.setPadding(new Insets(8, 12, 8, 12));
        header.setStyle("-fx-background-color: #1C2333; -fx-background-radius: 8 8 0 0;");
        header.setAlignment(Pos.CENTER_LEFT);
        card.getChildren().add(header);

        for (Field field : table.getFields()) {
            boolean isFk = field.getReference() != null && !field.getReference().isEmpty();
            String prefix = field.isPrimary() ? "PK  " : isFk ? "FK  " : "";
            Color txtColor = field.isPrimary() ? Color.web("#2E5A47") : isFk ? Color.web("#8B5E3C") : Color.web("#333333");

            HBox row = new HBox(6);
            rowNodeMap.put(table.getName() + "." + field.getName(), row);
            row.setPadding(new Insets(5, 12, 5, 12));
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-border-color: #EEEEEE; -fx-border-width: 0 0 1 0;");

            Text colText = new Text(prefix + field.getType() + "  " + field.getName());
            colText.setFont(Font.font("System", FontWeight.BOLD, 11));
            colText.setFill(txtColor);
            row.getChildren().add(colText);
            card.getChildren().add(row);
        }

        cardNodeMap.put(table.getName(), card);
        return card;
    }

    private MenuItem makeMenuItem(String text) {
        MenuItem item = new MenuItem(text);
        item.setStyle("-fx-font-size: 12px; -fx-padding: 6 12 6 12;");
        return item;
    }

    public void showTableData(String schemaName, Table table) {

        for (Tab existing : dataTabPane.getTabs()) {
            if (existing.getText().equals(table.getName())) {
                dataTabPane.getSelectionModel().select(existing);
                return;
            }
        }

        List<String> columns = new ArrayList<>();
        List<String[]> rows = isRemoteSelected
                ? db.GetTableDataRemote(schemaName, table.getName(), columns)
                : db.GetTableData(schemaName, table.getName(), columns);

        TableView<String[]> tv = new TableView<>();
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        tv.setFixedCellSize(36);
        tv.setStyle("-fx-background-color: white; -fx-border-color: #E2E6E2;");

        for (int i = 0; i < columns.size(); i++) {
            final int col = i;
            TableColumn<String[], String> tc = new TableColumn<>(columns.get(col));
            tc.setCellValueFactory(data ->
                    new javafx.beans.property.SimpleStringProperty(
                            col < data.getValue().length ? data.getValue()[col] : ""));
            tc.setCellFactory(column -> new TableCell<>() {
                @Override protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setStyle("-fx-background-color: transparent;");
                    } else {
                        setText(item);
                        setFont(Font.font("Monospace", 12));
                        setTextFill(Color.web("#2C2C2C"));
                        setStyle("-fx-background-color: " + (getIndex() % 2 == 0 ? "#FFFFFF" : "#F7FAF8") + "; -fx-padding: 0 12; -fx-border-color: #EEEEEE; -fx-border-width: 0 0 1 0; -fx-alignment: CENTER-LEFT;");
                    }
                }
            });
            tv.getColumns().add(tc);
        }
        tv.getItems().addAll(rows);
        if (rows.isEmpty()) {
            Label empty = new Label("No rows in this table.");
            empty.setStyle("-fx-text-fill: #888; -fx-font-size: 13;");
            tv.setPlaceholder(empty);
        }

        tv.skinProperty().addListener((obs, oldSkin, newSkin) -> {
            if (newSkin != null) {
                Platform.runLater(() -> {
                    tv.lookupAll(".column-header-background").forEach(n ->
                            n.setStyle("-fx-background-color: #1C2333;"));
                    tv.lookupAll(".column-header").forEach(n ->
                            n.setStyle("-fx-background-color: transparent; -fx-border-color: transparent; -fx-size: 38px;"));
                    tv.lookupAll(".column-header > .label").forEach(n ->
                            n.setStyle("-fx-text-fill: white; -fx-font-family: Monospace; -fx-font-size: 12px; -fx-font-weight: bold; -fx-alignment: CENTER-LEFT; -fx-padding: 0 12;"));
                    tv.lookupAll(".filler").forEach(n ->
                            n.setStyle("-fx-background-color: #1C2333;"));
                });
            }
        });

        Label rowCount = new Label(rows.size() + " row" + (rows.size() == 1 ? "" : "s"));
        rowCount.setStyle("-fx-text-fill: #888; -fx-font-size: 11; -fx-padding: 4 12;");

        VBox tabContent = new VBox(tv, rowCount);
        VBox.setVgrow(tv, Priority.ALWAYS);
        tabContent.setStyle("-fx-background-color: white;");

        Tab tab = new Tab(table.getName(), tabContent);
        tab.setStyle(inactiveTabStyle());
        tab.setOnClosed(e -> {
            if (dataTabPane.getTabs().isEmpty()) {
                mainSplit = null;
                createTables();
            }
        });

        dataTabPane.setTabMinWidth(110);
        dataTabPane.setTabMaxWidth(160);
        dataTabPane.setStyle("-fx-background-color: white;" +
                "-fx-focus-color: transparent;" +
                "-fx-faint-focus-color: transparent;"
        );

        dataTabPane.getSelectionModel().selectedItemProperty().addListener((obs, oldTab, newTab) -> {
            if (oldTab != null) oldTab.setStyle(inactiveTabStyle());
            if (newTab != null) newTab.setStyle(activeTabStyle());
        });

        dataTabPane.getTabs().add(tab);
        dataTabPane.getSelectionModel().select(tab);
        tab.setStyle(activeTabStyle());

        if (mainSplit == null) {
            Node canvas = getCenter();
            mainSplit = new SplitPane();
            mainSplit.setOrientation(javafx.geometry.Orientation.VERTICAL);
            mainSplit.getItems().addAll(canvas, dataTabPane);
            mainSplit.setDividerPositions(0.55);
            setCenter(mainSplit);
        }
    }

    private String activeTabStyle() {
        return "-fx-background-color: #1C2333;" +
                "-fx-text-base-color: white;" +
                "-fx-mark-color: white;" +
                "-fx-font-weight: bold;" +
                "-fx-background-radius: 6 6 0 0;" +
                "-fx-focus-color: transparent;" +
                "-fx-faint-focus-color: transparent;";
    }

    private String inactiveTabStyle() {
        return "-fx-background-color: #F2F4F2;" +
                "-fx-text-base-color: #555555;" +
                "-fx-font-weight: normal;" +
                "-fx-background-radius: 6 6 0 0;" +
                "-fx-focus-color: transparent;" +
                "-fx-faint-focus-color: transparent;";
    }

    private void makeDraggable(VBox card, Pane overlay, List<String[]> foreignKeys,
                               StackPane world, String schemaName, String tableName) {
        final double[] prev = new double[2];
        card.setOnMousePressed(e -> {
            if (e.isPrimaryButtonDown()) {
                prev[0] = e.getSceneX();
                prev[1] = e.getSceneY();
                card.toFront();
                e.consume();
            }
        });
        card.setOnMouseDragged(e -> {
            if (e.isPrimaryButtonDown()) {
                card.setLayoutX(Math.max(0, card.getLayoutX() + (e.getSceneX() - prev[0]) / scale));
                card.setLayoutY(Math.max(0, card.getLayoutY() + (e.getSceneY() - prev[1]) / scale));
                prev[0] = e.getSceneX();
                prev[1] = e.getSceneY();
                card.setCursor(Cursor.CLOSED_HAND);
                drawConnectors(overlay, foreignKeys, world);
                e.consume();
            }
        });
        card.setOnMouseReleased(e -> {
            card.setCursor(Cursor.DEFAULT);
            savePosition(schemaName, tableName, card.getLayoutX(), card.getLayoutY());
            e.consume();
        });
    }
    private void drawConnectors(Pane overlay, List<String[]> foreignKeys, StackPane stack) {
        overlay.getChildren().clear();
        if (stack.getScene() == null) return;

        for (String[] fk : foreignKeys) {
            HBox fkRow  = rowNodeMap.get(fk[0] + "." + fk[1]);
            HBox pkRow  = rowNodeMap.get(fk[2] + "." + fk[3]);
            VBox fkCard = cardNodeMap.get(fk[0]);
            VBox pkCard = cardNodeMap.get(fk[2]);
            if (fkRow == null || pkRow == null || fkCard == null || pkCard == null) continue;

            Bounds fkRowB = stack.sceneToLocal(fkRow.localToScene(fkRow.getBoundsInLocal()));
            Bounds pkRowB = stack.sceneToLocal(pkRow.localToScene(pkRow.getBoundsInLocal()));
            Bounds fcB    = stack.sceneToLocal(fkCard.localToScene(fkCard.getBoundsInLocal()));
            Bounds pcB    = stack.sceneToLocal(pkCard.localToScene(pkCard.getBoundsInLocal()));

            if (fkCard == pkCard) { drawSelfLoop(overlay, fcB, fkRowB, pkRowB); continue; }

            double[] pts = portPoints(fcB, pcB, fkRowB.getCenterY(), pkRowB.getCenterY());
            double sx=pts[0], sy=pts[1], ex=pts[2], ey=pts[3];
            double sdx=pts[4], sdy=pts[5], edx=pts[6], edy=pts[7];

            double d = Math.max(Math.hypot(ex-sx, ey-sy) * 0.45, 70);
            double cp1x=sx+sdx*d, cp1y=sy+sdy*d;
            double cp2x=ex+edx*d, cp2y=ey+edy*d;

            double[] r = routeAround(sx,sy,cp1x,cp1y,cp2x,cp2y,ex,ey,
                    sdx,sdy, fkCard,pkCard,stack);
            cp1x=r[0]; cp1y=r[1]; cp2x=r[2]; cp2y=r[3];

            CubicCurve curve = new CubicCurve(sx,sy,cp1x,cp1y,cp2x,cp2y,ex,ey);
            curve.setStroke(Color.web("#2E5A47"));
            curve.setStrokeWidth(1.8);
            curve.setFill(Color.TRANSPARENT);

            Circle dot = new Circle(sx, sy, 4.5, Color.web("#2E5A47"));
            dot.setStroke(Color.WHITE);
            dot.setStrokeWidth(1.2);

            double angle = Math.atan2(ey-cp2y, ex-cp2x);
            overlay.getChildren().addAll(curve, dot, buildArrow(ex, ey, angle));
        }
    }

    private void drawSelfLoop(Pane overlay, Bounds cardB, Bounds fkRowB, Bounds pkRowB) {
        double sx   = cardB.getMaxX();
        double sy   = fkRowB.getCenterY();
        double ex   = cardB.getMaxX();
        double ey   = pkRowB.getCenterY();
        double bulge = 60;

        CubicCurve loop = new CubicCurve(
                sx, sy,
                sx + bulge, sy - 20,
                ex + bulge, ey + 20,
                ex, ey);
        loop.setStroke(Color.web("#2E5A47"));
        loop.setStrokeWidth(1.6);
        loop.setFill(Color.TRANSPARENT);

        Circle dot = new Circle(sx, sy, 4, Color.web("#2E5A47"));
        double angle = Math.atan2(ey - (ey + 20), ex - (ex + bulge));
        overlay.getChildren().addAll(loop, dot, buildArrow(ex, ey, angle));
    }

    private double estimateCardHeight(String tableName, List<Table> tables) {
        for (Table t : tables) {
            if (t.getName().equals(tableName)) {
                return 36 + t.getFields().size() * 31 + 4;
            }
        }
        return 160;
    }

    private void separateCards(Map<String, double[]> positions, List<Table> tables) {
        final double CARD_W  = 230;
        final double MIN_GAP = 30;

        List<String> names = new ArrayList<>(positions.keySet());

        for (int iter = 0; iter < 300; iter++) {
            boolean moved = false;
            for (int i = 0; i < names.size(); i++) {
                for (int j = i + 1; j < names.size(); j++) {
                    double[] pa = positions.get(names.get(i));
                    double[] pb = positions.get(names.get(j));
                    double   ha = estimateCardHeight(names.get(i), tables);
                    double   hb = estimateCardHeight(names.get(j), tables);

                    double overlapX = Math.min(pa[0] + CARD_W, pb[0] + CARD_W)
                            - Math.max(pa[0], pb[0]) + MIN_GAP;
                    double overlapY = Math.min(pa[1] + ha, pb[1] + hb)
                            - Math.max(pa[1], pb[1]) + MIN_GAP;

                    if (overlapX > 0 && overlapY > 0) {
                        moved = true;
                        double push;
                        if (overlapX < overlapY) {
                            push = overlapX / 2.0 + 1;
                            if (pa[0] <= pb[0]) { pa[0] -= push; pb[0] += push; }
                            else                { pa[0] += push; pb[0] -= push; }
                        } else {
                            push = overlapY / 2.0 + 1;
                            if (pa[1] <= pb[1]) { pa[1] -= push; pb[1] += push; }
                            else                { pa[1] += push; pb[1] -= push; }
                        }
                        pa[0] = Math.max(20, pa[0]);
                        pa[1] = Math.max(20, pa[1]);
                        pb[0] = Math.max(20, pb[0]);
                        pb[1] = Math.max(20, pb[1]);
                    }
                }
            }
            if (!moved) break;
        }
    }

    private double[] portPoints(Bounds fc, Bounds pc, double fkRowY, double pkRowY) {
        fkRowY = Math.max(fc.getMinY()+18, Math.min(fc.getMaxY()-6, fkRowY));
        pkRowY = Math.max(pc.getMinY()+18, Math.min(pc.getMaxY()-6, pkRowY));

        double dx = pc.getCenterX() - fc.getCenterX();
        double dy = pc.getCenterY() - fc.getCenterY();

        double sx, sy, ex, ey, sdx, sdy, edx, edy;

        if (Math.abs(dx) >= Math.abs(dy) * 0.58) {
            if (dx >= 0) {
                sx=fc.getMaxX(); sy=fkRowY; sdx= 1; sdy=0;
                ex=pc.getMinX(); ey=pkRowY; edx=-1; edy=0;
            } else {
                sx=fc.getMinX(); sy=fkRowY; sdx=-1; sdy=0;
                ex=pc.getMaxX(); ey=pkRowY; edx= 1; edy=0;
            }
        } else {
            if (dy >= 0) {
                sx=fc.getCenterX(); sy=fc.getMaxY(); sdx=0; sdy= 1;
                ex=pc.getCenterX(); ey=pc.getMinY(); edx=0; edy=-1;
            } else {
                sx=fc.getCenterX(); sy=fc.getMinY(); sdx=0; sdy=-1;
                ex=pc.getCenterX(); ey=pc.getMaxY(); edx=0; edy= 1;
            }
        }
        return new double[]{sx,sy,ex,ey,sdx,sdy,edx,edy};
    }

    private double[] routeAround(
            double sx, double sy, double cp1x, double cp1y,
            double cp2x, double cp2y, double ex, double ey,
            double exitDx, double exitDy,
            VBox fkCard, VBox pkCard, StackPane stack) {

        final double MARGIN  = 20.0;
        final int    SAMPLES = 36;
        boolean horizontal = Math.abs(exitDx) > Math.abs(exitDy);

        double needPos = 0;
        double needNeg = 0;

        for (VBox card : cardNodeMap.values()) {
            if (card == fkCard || card == pkCard) continue;
            Bounds b = stack.sceneToLocal(card.localToScene(card.getBoundsInLocal()));

            double bx1=b.getMinX()-MARGIN, bx2=b.getMaxX()+MARGIN;
            double by1=b.getMinY()-MARGIN, by2=b.getMaxY()+MARGIN;

            double connMinX=Math.min(sx,ex)-80, connMaxX=Math.max(sx,ex)+80;
            double connMinY=Math.min(sy,ey)-80, connMaxY=Math.max(sy,ey)+80;
            if (bx2<connMinX || bx1>connMaxX || by2<connMinY || by1>connMaxY) continue;

            double hitSum = 0; int hits = 0;
            double maxPen = 0;
            for (int s=1; s<SAMPLES; s++) {
                double t  = (double)s / SAMPLES;
                double bpx = bez(sx,cp1x,cp2x,ex,t);
                double bpy = bez(sy,cp1y,cp2y,ey,t);
                if (bpx>=bx1 && bpx<=bx2 && bpy>=by1 && bpy<=by2) {
                    double pen = horizontal
                            ? Math.min(bpy-by1, by2-bpy) + MARGIN
                            : Math.min(bpx-bx1, bx2-bpx) + MARGIN;
                    maxPen  = Math.max(maxPen, pen);
                    hitSum += horizontal ? bpy : bpx;
                    hits++;
                }
            }
            if (hits == 0) continue;

            double avgHit = hitSum / hits;
            double center = horizontal ? b.getCenterY() : b.getCenterX();
            double clearance = maxPen + 15;

            if (avgHit <= center) needNeg = Math.max(needNeg, clearance);
            else                  needPos = Math.max(needPos, clearance);
        }

        if (needNeg == 0 && needPos == 0) return new double[]{cp1x,cp1y,cp2x,cp2y};

        double shift = (needNeg <= needPos || needPos == 0) ? -needNeg : needPos;
        if (horizontal) { cp1y += shift; cp2y += shift; }
        else            { cp1x += shift; cp2x += shift; }

        return new double[]{cp1x,cp1y,cp2x,cp2y};
    }

    private double bez(double p0, double p1, double p2, double p3, double t) {
        double u = 1-t;
        return u*u*u*p0 + 3*u*u*t*p1 + 3*u*t*t*p2 + t*t*t*p3;
    }

    private Polygon buildArrow(double tipX, double tipY, double angleRad) {
        double s  = 9.0;
        double bx = tipX - Math.cos(angleRad) * s;
        double by = tipY - Math.sin(angleRad) * s;
        double wx = -Math.sin(angleRad) * (s * 0.45);
        double wy =  Math.cos(angleRad) * (s * 0.45);
        Polygon arrow = new Polygon(
                tipX,        tipY,
                bx + wx,     by + wy,
                bx - wx,     by - wy);
        arrow.setFill(Color.web("#2E5A47"));
        return arrow;
    }

    public void refreshData() {
        schemas = db.Schemas();
        if (!selectedSchemaName.isEmpty()) {
            boolean stillExists = schemas.stream().anyMatch(s -> s.getName().equals(selectedSchemaName));
            if (!stillExists) {
                selectedSchemaName = "";
                isRemoteSelected = false;
            }
        }
    }
    public Schema getTablesFor(String schemaName, boolean remote) {
        return remote ? db.GetTablesInSchemaRemote(schemaName) : db.GetTablesInSchema(schemaName);
    }

    private void savePosition(String schema, String table, double x, double y) {
        PREFS.put(schema + "|" + table, x + "," + y);
    }

    private double[] loadPosition(String schema, String table) {
        String val = PREFS.get(schema + "|" + table, null);
        if (val == null) return null;
        String[] parts = val.split(",");
        return new double[]{ Double.parseDouble(parts[0]), Double.parseDouble(parts[1]) };
    }

    private Map<String, double[]> computeForceLayout(List<Table> tables, List<String[]> foreignKeys) {

        if (tables.isEmpty()) return new LinkedHashMap<>();

        Map<String, double[]> pos = new LinkedHashMap<>();
        Random rng = new Random(42);

        int n = tables.size();
        double cx=550, cy=420, radius=Math.max(180, n*38);
        for (int i=0; i<n; i++) {
            double angle = 2*Math.PI*i/n;
            pos.put(tables.get(i).getName(), new double[]{
                    cx + radius*Math.cos(angle) + rng.nextDouble()*20,
                    cy + radius*Math.sin(angle) + rng.nextDouble()*20
            });
        }

        Set<String> tableNames = new HashSet<>(pos.keySet());
        List<String> names = new ArrayList<>(tableNames);

        final double IDEAL   = 300;
        final double ATTRACT = 0.12;
        final double REPEL   = 90_000;
        final int    ITERS   = 500;

        for (int iter=0; iter<ITERS; iter++) {
            Map<String,double[]> forces = new HashMap<>();
            for (String n2 : names) forces.put(n2, new double[]{0,0});

            for (int i=0; i<names.size(); i++) {
                for (int j=i+1; j<names.size(); j++) {
                    String a=names.get(i), b=names.get(j);
                    double[] pa=pos.get(a), pb=pos.get(b);
                    double dx=pa[0]-pb[0], dy=pa[1]-pb[1];
                    double dist=Math.max(Math.hypot(dx,dy),1);
                    double f=REPEL/(dist*dist);
                    forces.get(a)[0]+=dx/dist*f; forces.get(a)[1]+=dy/dist*f;
                    forces.get(b)[0]-=dx/dist*f; forces.get(b)[1]-=dy/dist*f;
                }
            }

            for (String[] fk : foreignKeys) {
                String a=fk[0], b=fk[2];
                if (a.equals(b)||!tableNames.contains(a)||!tableNames.contains(b)) continue;
                double[] pa=pos.get(a), pb=pos.get(b);
                double dx=pb[0]-pa[0], dy=pb[1]-pa[1];
                double dist=Math.max(Math.hypot(dx,dy),1);
                double f=ATTRACT*(dist-IDEAL);
                forces.get(a)[0]+=dx/dist*f; forces.get(a)[1]+=dy/dist*f;
                forces.get(b)[0]-=dx/dist*f; forces.get(b)[1]-=dy/dist*f;
            }

            double cool=Math.max(1.0-(double)iter/ITERS, 0.01);
            double maxStep=55*cool+3;
            for (String name : names) {
                double[] f=forces.get(name), p=pos.get(name);
                double mag=Math.hypot(f[0],f[1]);
                if (mag>maxStep){f[0]=f[0]/mag*maxStep; f[1]=f[1]/mag*maxStep;}
                p[0]=Math.max(30,p[0]+f[0]);
                p[1]=Math.max(30,p[1]+f[1]);
            }
        }
        return pos;
    }
}