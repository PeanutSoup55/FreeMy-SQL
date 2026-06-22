package GUI;

import GUI.Schemas.SchemasRoot;
import SSH.SSHConnection;
import globalfuncs.creds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Separator;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import tempFiles.TempCred;

import java.util.Objects;

public class Root extends BorderPane {

    private String  activeMenu = "Schemas";
    private final String[] LABELS = {"Schemas", "Query", "Credentials", "Logs", "SSH", "Send Feedback..."};
    private final String[] ICONS  = {
            "assets/schema.png",
            "assets/query.png",
            "assets/creds.png",
            "assets/logs.png",
            "assets/ssh.png",
            "assets/feedback.png"
    };
    private SchemasRoot schemasRoot;

    public Root() {
        schemasRoot = new SchemasRoot();
        createSide();
        setCenter(schemasRoot);
    }

    public void createSide() {
        // LEFT RAIL - icons only
        VBox rail = new VBox();
        rail.setPrefWidth(52);
        rail.setMinWidth(52);
        rail.setMaxWidth(52);
        rail.setStyle("-fx-background-color: #2E5A47;");

        // Avatar at top
        Text initialLabel = new Text(creds.getInitials());
        initialLabel.setStyle("-fx-font-weight: bold; -fx-fill: white; -fx-font-size: 14;");
        Rectangle rec = new Rectangle(36, 36);
        rec.setArcWidth(8); rec.setArcHeight(8);
        rec.setFill(Color.web("#1d3d30"));
        StackPane avatar = new StackPane(rec, initialLabel);
        avatar.setPadding(new Insets(10, 0, 10, 0));
        HBox avatarRow = new HBox(avatar);
        avatarRow.setAlignment(Pos.CENTER);
        avatarRow.setPadding(new Insets(10, 0, 10, 0));
        rail.getChildren().add(avatarRow);

        for (int i = 0; i < LABELS.length; i++) {
            rail.getChildren().add(createRailIcon(ICONS[i], LABELS[i]));
        }

        // SCHEMA TREE PANEL - only shown when Schemas is active
        VBox schemaPanel = new VBox();
        schemaPanel.setPrefWidth(220);
        schemaPanel.setMinWidth(220);
        schemaPanel.setStyle("-fx-background-color: #FFFFFF; " +
                "-fx-border-color: #DEDEDE; -fx-border-width: 0 1 0 0;");

        if (schemasRoot == null) schemasRoot = new SchemasRoot();
        Node sidebar = schemasRoot.buildSidebarContent();
        if (sidebar instanceof Region r) {
            r.setPrefWidth(Double.MAX_VALUE);
            r.setMaxWidth(Double.MAX_VALUE);
            r.setStyle(r.getStyle()
                    .replace("-fx-border-color: #DEDEDE;", "-fx-border-color: transparent;")
                    .replace("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 8, 0, 1, 2);", ""));
        }
        VBox.setVgrow(sidebar, Priority.ALWAYS);
        schemaPanel.getChildren().add(sidebar);
        VBox.setVgrow((Node) sidebar, Priority.ALWAYS);

        HBox sidebarWrapper = new HBox(rail, schemaPanel);
        BorderPane.setMargin(sidebarWrapper, Insets.EMPTY);
        setLeft(sidebarWrapper);
    }

    private HBox createRailIcon(String iconPath, String label) {
        ImageView iv = new ImageView(new Image(
                Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(iconPath))));
        iv.setFitWidth(20);
        iv.setFitHeight(20);

        StackPane iconWrap = new StackPane(iv);
        iconWrap.setPrefSize(36, 36);
        boolean isActive = label.equals(activeMenu);
        iconWrap.setStyle(isActive
                ? "-fx-background-color: rgba(255,255,255,0.2); -fx-background-radius: 8;"
                : "-fx-background-color: transparent; -fx-background-radius: 8;");
        iconWrap.setCursor(javafx.scene.Cursor.HAND);

        HBox row = new HBox(iconWrap);
        row.setAlignment(Pos.CENTER);
        row.setPrefHeight(40);
        row.setOnMouseEntered(e -> {
            if (!label.equals(activeMenu))
                iconWrap.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-background-radius: 8;");
        });
        row.setOnMouseExited(e -> {
            if (!label.equals(activeMenu))
                iconWrap.setStyle("-fx-background-color: transparent; -fx-background-radius: 8;");
        });
        row.setOnMouseClicked(e -> {
            activeMenu = label;
            switchCenterContent(label);
            createSide();
        });

        Tooltip.install(row, new Tooltip(label));
        return row;
    }

    private void switchCenterContent(String menuTitle) {
        switch (menuTitle) {
            case "Schemas" -> {
                if (schemasRoot == null) schemasRoot = new SchemasRoot();
                setCenter(schemasRoot);
            }
            case "Query" -> setCenter(new Query());
            case "Credentials" -> setCenter(new Creds());
            case "Logs" -> setCenter(new LogsRoot());
            case "SSH" -> setCenter(new SSHConnection(null));
            case "Send Feedback..." -> setCenter(new FeedbackDialog());
        }
    }
}