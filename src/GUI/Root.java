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

    private boolean isCollapsed = false;
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

        if ("Schemas".equals(activeMenu)) {
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
        }

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


    private void toggleCollapse() {
        isCollapsed = !isCollapsed;
        createSide();
    }
    private HBox createTopHBox(String initials, String user, String url, boolean expanded) {
        Text initialLabel = new Text(initials);
        initialLabel.setStyle("-fx-font-weight: bold; -fx-fill: white; -fx-font-size: 18;");

        Rectangle rec = new Rectangle(42, 42);
        rec.setArcWidth(10); rec.setArcHeight(10);
        rec.setFill(Color.web("#2E7D5E"));

        StackPane avatar = new StackPane(rec, initialLabel);

        ImageView arrowIcon = new ImageView(new Image(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("assets/arrow.png"))));
        arrowIcon.setFitWidth(16);
        arrowIcon.setFitHeight(16);
        if (expanded) arrowIcon.setRotate(180);

        StackPane arrowBtn = new StackPane(arrowIcon);
        arrowBtn.setStyle("-fx-background-color: #2A3244; -fx-background-radius: 50;");
        arrowBtn.setPrefSize(26, 26);
        arrowBtn.setMinSize(26, 26);
        arrowBtn.setMaxSize(26, 26);
        arrowBtn.setCursor(javafx.scene.Cursor.HAND);
        arrowBtn.setOnMouseClicked(e -> toggleCollapse());

        if (!expanded) {
            VBox col = new VBox(8, avatar, arrowBtn);
            col.setAlignment(Pos.CENTER);
            col.setPadding(new Insets(12, 0, 8, 0));
            HBox hbox = new HBox(col);
            hbox.setAlignment(Pos.CENTER);
            hbox.setPrefWidth(68);
            return hbox;
        }

        VBox info = new VBox(3);
        Text name = new Text(user != null ? user : "");
        name.setStyle("-fx-font-weight: bold; -fx-fill: #FFFFFF;");
        String rawUrl = (url != null) ? url : "";
        String limitedUrl = (rawUrl.length() > 20) ? rawUrl.substring(0, 20) + "..." : rawUrl;
        Text urlText = new Text(limitedUrl);
        urlText.setStyle("-fx-fill: #6B7A8D; -fx-font-size: 10px;");
        info.getChildren().addAll(name, urlText);
        info.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox hbox = new HBox(12, avatar, info, spacer, arrowBtn);
        hbox.setAlignment(Pos.CENTER_LEFT);
        hbox.setPadding(new Insets(12, 12, 8, 12));
        return hbox;
    }
//
//    private HBox createMenuItem(String icon, String labelText, boolean isSelected, boolean showLabel) {
//        ImageView imageView = new ImageView(new Image(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(icon))));
//        imageView.setFitHeight(22);
//        imageView.setFitWidth(22);
//
//        HBox hbox;
//        Text label = null;
//
//        if (showLabel) {
//            label = new Text(labelText);
//            label.setFont(Font.font("System", 13));
//            label.setStyle("-fx-font-weight: bold;");
//            hbox = new HBox(20, imageView, label);
//            hbox.setPadding(new Insets(9, 12, 9, 12));
//            hbox.setMinWidth(200);
//            hbox.setAlignment(Pos.CENTER_LEFT);
//        } else {
//            hbox = new HBox(imageView);
//            hbox.setAlignment(Pos.CENTER);
//            hbox.setPrefWidth(68);
//            hbox.setPadding(new Insets(9, 0, 9, 0));
//        }
//
//        hbox.setPrefHeight(30);
//        VBox.setMargin(hbox, new Insets(1, 6, 1, 6));
//
//        final Text finalLabel = label;
//        if (isSelected) applySelectedStyle(hbox, finalLabel);
//        else            applyDefaultStyle(hbox, finalLabel);
//
//        hbox.setOnMouseEntered(e -> {
//            if (!labelText.equals(activeMenu))
//                hbox.setBackground(new Background(new BackgroundFill(
//                        Color.web("#252D3D"), new CornerRadii(6), Insets.EMPTY)));
//        });
//        hbox.setOnMouseExited(e -> {
//            if (!labelText.equals(activeMenu))
//                applyDefaultStyle(hbox, finalLabel);
//        });
//        hbox.setOnMouseClicked(e -> {
//            activeMenu = labelText;
//            switchCenterContent(labelText);
//            createSide();
//        });
//
//        return hbox;
//    }



    private void switchCenterContent(String menuTitle) {
        switch (menuTitle) {
            case "Schemas" -> {
                if (schemasRoot == null) schemasRoot = new SchemasRoot();
                setCenter(schemasRoot);
            }
            case "Query", "Credentials", "Logs", "SSH" -> setCenter(new TempCred());
            case "Send Feedback..." -> setCenter(new FeedbackDialog());
        }
    }
}