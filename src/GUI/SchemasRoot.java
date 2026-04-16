package GUI;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import globalfuncs.db;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

import java.util.*;

public class SchemasRoot extends BorderPane {
    public static List<String> schemas = db.Schemas();
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
        top.setStyle("-fx-font-weight: 600; -fx-padding: 10;");
        top.setTextAlignment(TextAlignment.CENTER);
        Separator sep = new Separator();
        sep.setPadding(new Insets(5, 0, 5, 0));
        List<HBox> tabs = new ArrayList<>();
        for (String schema : schemas) tabs.add(generateTab(schema));
        vBox.getChildren().addAll(top, sep);
        vBox.getChildren().addAll(tabs);
        setLeft(vBox);
    }

    private HBox generateTab(String schema) {
        HBox hbox = new HBox();
        hbox.setAlignment(Pos.CENTER);
        hbox.setPadding(new Insets(10));
        hbox.setMinWidth(100);
        hbox.setPrefHeight(15);

        Text text = new Text(schema);
        text.setFont(Font.font("System", 13));
        text.setTextAlignment(TextAlignment.CENTER);
        text.setStyle("-fx-font-weight: bold;");

        if (selectedTab == null) {
            applySelectedStyle(hbox, text);
            selectedTab = hbox;
        } else{
            applyDefaultStyle(hbox, text);
        }
        hbox.setOnMouseClicked(e -> {
            if (selectedTab != null) applyDefaultStyle(selectedTab,
                    (Text) selectedTab.getChildren().getFirst());
            applySelectedStyle(hbox, text);
            selectedTab = hbox;
            createTables();
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

        String selectedSchema = selectedTab != null ? ((Text) selectedTab.getChildren().getFirst()).getText() : (schemas.isEmpty() ? "" : schemas.get(0));

        Map<String, List<String[]>> tableMap  = db.GetTablesInSchema(selectedSchema);
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

        for (Map.Entry<String, List<String[]>> entry : tableMap.entrySet()) {
            VBox card = buildCard(entry.getKey(), entry.getValue());
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

    private void makeDraggable(VBox card, Pane overlay, List<String[]> foreignKeys, StackPane stack) {
        final double[] prev = new double[2]; // last scene position

        card.setOnMousePressed(e -> {
            prev[0] = e.getSceneX();
            prev[1] = e.getSceneY();
            card.toFront();   // raise card inside canvas; overlay stays on top in StackPane
            e.consume();
        });

        card.setOnMouseDragged(e -> {
            // Delta in scene space works correctly for plain translation transforms
            double dx = e.getSceneX() - prev[0];
            double dy = e.getSceneY() - prev[1];
            card.setLayoutX(Math.max(0, card.getLayoutX() + dx));
            card.setLayoutY(Math.max(0, card.getLayoutY() + dy));
            prev[0] = e.getSceneX();
            prev[1] = e.getSceneY();
            drawConnectors(overlay, foreignKeys, stack);
            e.consume();
        });
    }

    public VBox buildCard(String tableName, List<String[]> columns) {
        VBox card = new VBox();
        card.setStyle("-fx-background-radius: 10;" +
                "-fx-background-color: #FFFFFF;" +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.18), 10, 0, 0, 3);");
        card.setMinWidth(170);
        card.setPrefWidth(Region.USE_COMPUTED_SIZE);
        card.setMaxWidth(Region.USE_COMPUTED_SIZE);

        Image edit = new Image("./assets/editW.png");
        ImageView editView = new ImageView(edit);
        Image delete = new Image("./assets/deleteW.png");
        ImageView deleteView = new ImageView(delete);
        HBox imageBox = new HBox(editView, deleteView);
        imageBox.setAlignment(Pos.CENTER_RIGHT);
        Label title = new Label(tableName);
        title.setTextFill(Color.WHITE);
        title.setFont(Font.font("System", FontWeight.BOLD, 13));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(title, spacer, imageBox);
        header.setPadding(new Insets(8, 12, 8, 12));
        header.setStyle("-fx-background-color: #2E5A47; -fx-background-radius: 8 8 0 0;");
        header.setAlignment(Pos.CENTER_LEFT);

        card.getChildren().add(header);

        for (String[] col : columns) {
            String colName  = col[0];
            String dataType = col[1];
            String keyType  = col[2];

            String prefix = "";
            if      ("PRI".equals(keyType)) prefix = "PK  ";
            else if ("MUL".equals(keyType)) prefix = "FK  ";

            HBox row = new HBox(6);
            rowNodeMap.put(tableName + "." + colName, row);
            row.setPadding(new Insets(5, 12, 5, 12));
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-border-color: #EEEEEE; -fx-border-width: 0 0 1 0;");

            Text colText = new Text(prefix + dataType + "  " + colName);
            colText.setFont(Font.font("Monospace", 11));
            colText.setFill("PRI".equals(keyType) ? Color.web("#2E5A47")
                    : "MUL".equals(keyType) ? Color.web("#8B5E3C")
                    : Color.web("#333333"));
            row.getChildren().add(colText);
            card.getChildren().add(row);
        }

        cardNodeMap.put(tableName, card);
        return card;
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
                        // Push away from the card's vertical centre
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
}