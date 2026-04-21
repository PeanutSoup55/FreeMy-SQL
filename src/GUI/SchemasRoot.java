package GUI;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import globalfuncs.db;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import Objects.*;

import java.util.*;

public class SchemasRoot extends BorderPane {
    public static List<Schema> schemas = db.Schemas();
    private HBox selectedTab;
    private final Map<String, HBox> rowNodeMap  = new HashMap<>();
    private final Map<String, VBox> cardNodeMap = new HashMap<>();

    public SchemasRoot() {
        createSide();
        createTables();
    }

    private void createSide() {
        VBox vBox = new VBox();
        vBox.setPadding(new Insets(10));
        vBox.setStyle("-fx-background-radius: 15;" +
                "-fx-background-color: #FFFFFF;" +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 12, 0, 0, 0);");
        BorderPane.setMargin(vBox, new Insets(10));

        Text top = new Text("Schemas");
        top.setStyle("-fx-font-weight: 600;");
        top.setTextAlignment(TextAlignment.CENTER);
        Button refreshBtn = new Button("↻");
        refreshBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #2E5A47;" +
                "-fx-font-size: 16; -fx-cursor: hand; -fx-padding: 0 4;");
        refreshBtn.setOnAction(e -> refresh());

        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, Priority.ALWAYS);

        HBox topRow = new HBox(top, topSpacer, refreshBtn);
        topRow.setAlignment(Pos.CENTER_LEFT);
        topRow.setPadding(new Insets(0, 0, 4, 0));

        Separator sep = new Separator();
        sep.setPadding(new Insets(5, 0, 5, 0));
        List<HBox> tabs = new ArrayList<>();
        for (Schema schema : schemas) tabs.add(generateTab(schema));

        Region region = new Region();
        VBox.setVgrow(region, Priority.ALWAYS);
        HBox makeSchema = new HBox();
        makeSchema.setPadding(new Insets(15, 15, 15, 15));
        makeSchema.setBackground(new Background(new BackgroundFill(Color.web("#2E5A47"), new CornerRadii(8), Insets.EMPTY)));
        Text makeSchemaLabel = new Text("Make Schema");
        makeSchemaLabel.setFill(Color.WHITE);
        makeSchemaLabel.setTextAlignment(TextAlignment.CENTER);
        makeSchemaLabel.setStyle("-fx-font-weight: 700;");
        makeSchema.getChildren().add(makeSchemaLabel);
        makeSchema.setOnMouseClicked(e -> setCenter(new SchemasAdd(this)));

        vBox.getChildren().addAll(topRow, sep);
        vBox.getChildren().addAll(tabs);
        vBox.getChildren().addAll(region, makeSchema);
        setLeft(vBox);
    }

    private HBox generateTab(Schema schema) {
        HBox hbox = new HBox();
        hbox.setAlignment(Pos.CENTER);
        hbox.setPadding(new Insets(10));
        hbox.setMinWidth(100);
        hbox.setPrefHeight(15);

        Text text = new Text(schema.getName());
        text.setFont(Font.font("System", 13));
        text.setTextAlignment(TextAlignment.LEFT);
        text.setStyle("-fx-font-weight: bold;");

        if (selectedTab == null) {
            applySelectedStyle(hbox, text);
            selectedTab = hbox;
        } else{
            applyDefaultStyle(hbox, text);
        }
        hbox.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY){
                if (selectedTab != null) applyDefaultStyle(selectedTab, (Text) selectedTab.getChildren().getFirst());
                applySelectedStyle(hbox, text);
                selectedTab = hbox;
                createTables();
            }else if (e.getButton() == MouseButton.SECONDARY){
                ContextMenu contextMenu = new ContextMenu();
                contextMenu.setStyle("-fx-background-color: white;" +
                                "-fx-background-radius: 8;" +
                                "-fx-border-radius: 8;" +
                                "-fx-border-color: #E0E0E0;" +
                                "-fx-padding: 4;" +
                                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 10, 0, 0, 2);"
                );
                MenuItem deleteItem = new MenuItem("Delete " + schema.getName());
                deleteItem.setOnAction(event -> {
                    Dialog<ButtonType> dialog = new Dialog<>();
                    dialog.setTitle("Delete Schema");

                    dialog.setHeaderText(null);

                    ButtonType deleteButtonType = new ButtonType("Delete", ButtonBar.ButtonData.OK_DONE);
                    dialog.getDialogPane().getButtonTypes().addAll(deleteButtonType, ButtonType.CANCEL);

                    Label header = new Label("Delete Schema");
                    header.setTextFill(Color.WHITE);
                    header.setFont(Font.font("System", FontWeight.BOLD, 14));

                    HBox headerBox = new HBox(header);
                    headerBox.setPadding(new Insets(10, 12, 10, 12));
                    headerBox.setStyle("-fx-background-color: #2E5A47; -fx-background-radius: 8 8 0 0;");

                    Label warning = new Label("This will permanently delete the schema and all its tables.");
                    warning.setStyle("-fx-text-fill: #444;");

                    Label instruction = new Label("Type '" + schema.getName() + "' to confirm:");
                    instruction.setStyle("-fx-font-weight: 600;");

                    TextField input = new TextField();
                    input.setPromptText(schema.getName());
                    input.setStyle("-fx-background-radius: 6;" +
                                    "-fx-border-radius: 6;" +
                                    "-fx-border-color: #CCCCCC;" +
                                    "-fx-padding: 6;"
                    );

                    VBox content = new VBox(10, warning, instruction, input);
                    content.setPadding(new Insets(12));
                    VBox wrapper = new VBox(headerBox, content);
                    wrapper.setStyle("-fx-background-color: white;" +
                                    "-fx-background-radius: 8;" +
                                    "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 12, 0, 0, 0);"
                    );
                    dialog.getDialogPane().setContent(wrapper);

                    Node deleteButton = dialog.getDialogPane().lookupButton(deleteButtonType);
                    Node cancelButton = dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
                    deleteButton.setDisable(true);
                    deleteButton.setStyle("-fx-background-color: #CCCCCC;" +
                                    "-fx-text-fill: white;" +
                                    "-fx-background-radius: 6;"
                    );

                    cancelButton.setStyle("-fx-background-color: transparent;" +
                                    "-fx-text-fill: #2E5A47;" +
                                    "-fx-font-weight: 600;"
                    );

                    input.textProperty().addListener((obs, oldVal, newVal) -> {
                        boolean valid = newVal.equals(schema.getName());
                        deleteButton.setDisable(!valid);
                        deleteButton.setStyle(valid ? "-fx-background-color: #c0392b; -fx-text-fill: white; -fx-background-radius: 6;" : "-fx-background-color: #CCCCCC; -fx-text-fill: white; -fx-background-radius: 6;");
                    });

                    Optional<ButtonType> result = dialog.showAndWait();

                    if (result.isPresent() && result.get() == deleteButtonType) {
                        db.deleteSchema(schema);
                        ((Pane) hbox.getParent()).getChildren().remove(hbox);
                        refresh();
                        System.out.println("Schema deleted successfully.");
                    }
                });
                contextMenu.getItems().add(deleteItem);
                contextMenu.setOnShown(ev -> {
                    for (MenuItem item : contextMenu.getItems()) {
                        Node n = item.getGraphic();

                        Node itemNode = contextMenu.getSkin().getNode().lookup(".menu-item");

                        if (itemNode != null) {
                            itemNode.setStyle("-fx-background-radius: 6;" +
                                            "-fx-padding: 6 12;" +
                                            "-fx-text-fill: #333333;"
                            );
                        }
                    }
                });
                contextMenu.show(hbox, e.getScreenX(), e.getScreenY());
            }
        });
        hbox.getChildren().add(text);
        return hbox;
    }

    private void applySelectedStyle(HBox h, Text t) {
        h.setBackground(new Background(new BackgroundFill(
                Color.web("#2E5A47"), new CornerRadii(8), Insets.EMPTY)));
        t.setFill(Color.WHITE);
    }
    private void applyDefaultStyle(HBox h, Text t) {
        h.setBackground(null);
        t.setFill(Color.web("#4A4A4A"));
    }
    public void createTables() {
        rowNodeMap.clear();
        cardNodeMap.clear();

        String selectedSchema = selectedTab != null ? ((Text) selectedTab.getChildren().getFirst()).getText() : (schemas.isEmpty() ? "" : schemas.getFirst().getName());

        Schema schema = db.GetTablesInSchema(selectedSchema);
        List<String[]> foreignKeys = db.GetForeignKeys(selectedSchema);
        Pane canvas = new Pane();
        canvas.setMinSize(1000, 800);
        Pane overlay = new Pane();
        overlay.setMouseTransparent(true);
        overlay.prefWidthProperty().bind(canvas.widthProperty());
        overlay.prefHeightProperty().bind(canvas.heightProperty());

        // StackPane: canvas below, overlay above
        StackPane stackPane = new StackPane(canvas, overlay);

        // Lay cards out in a grid initially
        int perRow  = 4;
        double hGap = 260, vGap = 280;
        double originX = 30, originY = 30;
        int i = 0;

        for (Table table : schema.getTables()) {
            VBox card = buildCard(table);
            card.setLayoutX(originX + (i % perRow) * hGap);
            card.setLayoutY(originY + ((double) i / perRow) * vGap);
            makeDraggable(card, overlay, foreignKeys, stackPane);
            canvas.getChildren().add(card);
            i++;
        }

        ScrollPane scroll = new ScrollPane(stackPane);
        scroll.setFitToWidth(false);
        scroll.setFitToHeight(false);
        scroll.setStyle("-fx-background-color: transparent;");
        setCenter(scroll);
        // Fire after two layout passes so all Bounds are valid
        Platform.runLater(() -> Platform.runLater(() ->
                drawConnectors(overlay, foreignKeys, stackPane)));
    }

    public VBox buildCard(Table table) {
        VBox card = new VBox();
        card.setStyle("-fx-background-radius: 10;" +
                "-fx-background-color: #FFFFFF;" +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.18), 10, 0, 0, 3);");
        card.setMinWidth(170);
        card.setPrefWidth(Region.USE_COMPUTED_SIZE);
        card.setMaxWidth(Region.USE_COMPUTED_SIZE);
        ImageView edit = new ImageView(new Image("./assets/editW.png"));
        ImageView delete = new ImageView(new Image("./assets/deleteW.png"));
        StackPane editWrapper = new StackPane(edit);
        StackPane deleteWrapper = new StackPane(delete);

        editWrapper.setPrefSize(30, 30);
        deleteWrapper.setPrefSize(30, 30);

        editWrapper.setCursor(Cursor.HAND);
        deleteWrapper.setCursor(Cursor.HAND);
        editWrapper.setOnMouseClicked(e ->{
            setCenter(new TableEdit());
        });
        deleteWrapper.setOnMouseClicked(e -> {
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Delete Table");
            dialog.setHeaderText(null);

            ButtonType deleteButtonType = new ButtonType("Delete", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(deleteButtonType, ButtonType.CANCEL);

            Label header = new Label("Delete Table");
            header.setTextFill(Color.WHITE);
            header.setFont(Font.font("System", FontWeight.BOLD, 14));

            HBox headerBox = new HBox(header);
            headerBox.setPadding(new Insets(10, 12, 10, 12));
            headerBox.setStyle("-fx-background-color: #2E5A47; -fx-background-radius: 8 8 0 0;");

            Label warning = new Label("This will permanently delete the table and all its data.");
            warning.setStyle("-fx-text-fill: #444;");

            Label instruction = new Label("Type '" + table.getName() + "' to confirm:");
            instruction.setStyle("-fx-font-weight: 600;");

            TextField input = new TextField();
            input.setPromptText(table.getName());
            input.setStyle("-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: #CCCCCC; -fx-padding: 6;");

            VBox content = new VBox(10, warning, instruction, input);
            content.setPadding(new Insets(12));
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
                String selectedSchemaName = selectedTab != null
                        ? ((Text) selectedTab.getChildren().getFirst()).getText()
                        : (schemas.isEmpty() ? "" : schemas.getFirst().getName());

                db.deleteTable(new Schema(selectedSchemaName), table);
                createTables();
                System.out.println("Table " + table.getName() + " deleted successfully.");
            }
        });
        HBox imageBox = new HBox(editWrapper, deleteWrapper);
        imageBox.setAlignment(Pos.CENTER_RIGHT);
        Label title = new Label(table.getName());
        title.setTextFill(Color.WHITE);
        title.setFont(Font.font("System", FontWeight.BOLD, 13));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(title, spacer, imageBox);
        header.setPadding(new Insets(8, 12, 8, 12));
        header.setStyle("-fx-background-color: #2E5A47; -fx-background-radius: 8 8 0 0;");
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
            colText.setFont(Font.font("Monospace", 11));
            colText.setFill(txtColor);
            row.getChildren().add(colText);
            card.getChildren().add(row);
        }

        cardNodeMap.put(table.getName(), card);
        return card;
    }

    private void makeDraggable(VBox card, Pane overlay, List<String[]> foreignKeys, StackPane stack) {
        final double[] prev = new double[2];

        card.setOnMousePressed(e -> {
            prev[0] = e.getSceneX();
            prev[1] = e.getSceneY();
            card.toFront();
            e.consume();
        });

        card.setOnMouseDragged(e -> {
            double dx = e.getSceneX() - prev[0];
            double dy = e.getSceneY() - prev[1];
            card.setCursor(Cursor.CLOSED_HAND);
            card.setLayoutX(Math.max(0, card.getLayoutX() + dx));
            card.setLayoutY(Math.max(0, card.getLayoutY() + dy));
            prev[0] = e.getSceneX();
            prev[1] = e.getSceneY();
            drawConnectors(overlay, foreignKeys, stack);
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

            Bounds fkRowB  = stack.sceneToLocal(fkRow .localToScene(fkRow .getBoundsInLocal()));
            Bounds pkRowB  = stack.sceneToLocal(pkRow .localToScene(pkRow .getBoundsInLocal()));
            Bounds fkCardB = stack.sceneToLocal(fkCard.localToScene(fkCard.getBoundsInLocal()));
            Bounds pkCardB = stack.sceneToLocal(pkCard.localToScene(pkCard.getBoundsInLocal()));

            // Self-referencing FK → small loop on the right side of the card
            if (fkCard == pkCard) {
                drawSelfLoop(overlay, fkCardB, fkRowB, pkRowB);
                continue;
            }

            // Choose exit / entry edges based on horizontal relationship
            boolean fkIsLeft = fkCardB.getCenterX() <= pkCardB.getCenterX();
            double sx = fkIsLeft ? fkCardB.getMaxX() : fkCardB.getMinX();
            double sy = fkRowB.getCenterY();
            double ex = fkIsLeft ? pkCardB.getMinX() : pkCardB.getMaxX();
            double ey = pkRowB.getCenterY();

            // Control-point tension: at least 80 px, scales with distance
            double tension = Math.max(Math.abs(ex - sx) * 0.45, 80);
            double cp1x = fkIsLeft ? sx + tension : sx - tension;
            double cp1y = sy;
            double cp2x = fkIsLeft ? ex - tension : ex + tension;
            double cp2y = ey;

            // Collision avoidance: push control points to route around blocking cards
            double[] cps = avoidCollisions(sx, sy, cp1x, cp1y, cp2x, cp2y, ex, ey,
                    fkCard, pkCard, stack);
            cp1x = cps[0]; cp1y = cps[1];
            cp2x = cps[2]; cp2y = cps[3];

            CubicCurve curve = new CubicCurve(sx, sy, cp1x, cp1y, cp2x, cp2y, ex, ey);
            curve.setStroke(Color.web("#2E5A47"));
            curve.setStrokeWidth(1.8);
            curve.setFill(Color.TRANSPARENT);

            // Filled dot at FK (many) end
            Circle dot = new Circle(sx, sy, 4.5, Color.web("#2E5A47"));
            dot.setStroke(Color.WHITE);
            dot.setStrokeWidth(1.2);

            // Tangent-aligned arrowhead at PK (one) end
            double angle = Math.atan2(ey - cp2y, ex - cp2x);
            Polygon arrow = buildArrow(ex, ey, angle);

            overlay.getChildren().addAll(curve, dot, arrow);
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

    private double[] avoidCollisions(
            double sx, double sy,
            double cp1x, double cp1y,
            double cp2x, double cp2y,
            double ex, double ey,
            VBox fkCard, VBox pkCard, StackPane stack) {

        final int SAMPLES = 28;
        final double MARGIN = 8.0;    // extra padding around each card box

        double maxPenetration = 0;
        double pushDir = 0;     // +1 → push down, -1 → push up

        for (VBox card : cardNodeMap.values()) {
            if (card == fkCard || card == pkCard) continue;

            Bounds b = stack.sceneToLocal(card.localToScene(card.getBoundsInLocal()));
            double minX = b.getMinX() - MARGIN;
            double maxX = b.getMaxX() + MARGIN;
            double minY = b.getMinY() - MARGIN;
            double maxY = b.getMaxY() + MARGIN;

            for (int s = 1; s < SAMPLES; s++) {
                double t  = (double) s / SAMPLES;
                double bx = cubic(sx, cp1x, cp2x, ex, t);
                double by = cubic(sy, cp1y, cp2y, ey, t);

                if (bx > minX && bx < maxX && by > minY && by < maxY) {
                    // How deep is the sample inside the card vertically?
                    double pen = (b.getHeight() / 2) + MARGIN
                            + Math.min(by - minY, maxY - by);
                    if (pen > maxPenetration) {
                        maxPenetration = pen;
                        // Push away from the card's vertical center
                        pushDir = (by < b.getCenterY()) ? -1.0 : 1.0;
                    }
                }
            }
        }

        if (maxPenetration > 0) {
            double shift = maxPenetration + 20;
            cp1y += pushDir * shift;
            cp2y += pushDir * shift;
        }

        return new double[]{ cp1x, cp1y, cp2x, cp2y };
    }

    private double cubic(double p0, double p1, double p2, double p3, double t) {
        double u = 1 - t;
        return u*u*u*p0 + 3*u*u*t*p1 + 3*u*t*t*p2 + t*t*t*p3;
    }

    private Polygon buildArrow(double tipX, double tipY, double angleRad) {
        double s  = 9.0;
        // Back-centre of the arrowhead
        double bx = tipX - Math.cos(angleRad) * s;
        double by = tipY - Math.sin(angleRad) * s;
        // Perpendicular wing offset
        double wx = -Math.sin(angleRad) * (s * 0.45);
        double wy =  Math.cos(angleRad) * (s * 0.45);

        Polygon arrow = new Polygon(
                tipX,        tipY,
                bx + wx,     by + wy,
                bx - wx,     by - wy);
        arrow.setFill(Color.web("#2E5A47"));
        return arrow;
    }

    public void refresh() {
        schemas = db.Schemas();
        if (selectedTab != null) {
            String current = ((Text) selectedTab.getChildren().getFirst()).getText();
            if (!schemas.contains(current)) selectedTab = null;
        }
        createSide();
        createTables();
    }
}