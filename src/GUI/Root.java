package GUI;

import GUI.Schemas.SchemasRoot;
import SSH.SSHConnection;
import globalfuncs.creds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Separator;
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
        VBox expanded  = buildExpanded();
        VBox collapsed = buildCollapsed();

        collapsed.setVisible(isCollapsed);
        collapsed.setManaged(isCollapsed);
        expanded.setVisible(!isCollapsed);
        expanded.setManaged(!isCollapsed);

        StackPane wrapper = new StackPane(expanded, collapsed);
        wrapper.setAlignment(Pos.TOP_LEFT);
        BorderPane.setMargin(wrapper, new Insets(10));
        setLeft(wrapper);
    }
    private VBox buildExpanded() {
        VBox vBox = new VBox();
        vBox.setStyle("-fx-background-radius: 15;" +
                "-fx-background-color: #FFFFFF;" +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 12, 0, 0, 0);");
        vBox.setPrefWidth(260);

        HBox top = createTopHBox(creds.getInitials(), creds.getUser(), creds.getUrl(), true);
        vBox.getChildren().add(top);

        for (int i = 0; i < LABELS.length; i++) {
            HBox item = createMenuItem(ICONS[i], LABELS[i], LABELS[i].equals(activeMenu), true);
            vBox.getChildren().add(item);

            if ("Schemas".equals(LABELS[i]) && "Schemas".equals(activeMenu)) {
                if (schemasRoot == null) schemasRoot = new SchemasRoot();
                Separator sep = new Separator();
                sep.setPadding(new Insets(4, 10, 4, 10));
                vBox.getChildren().add(sep);

                Node schemaPanel = schemasRoot.buildSidebarContent();
                if (schemaPanel instanceof Region r) {
                    r.setPrefWidth(Double.MAX_VALUE);
                    r.setMaxWidth(Double.MAX_VALUE);
                    r.setStyle(r.getStyle()
                            .replace("-fx-border-color: #DEDEDE;", "-fx-border-color: transparent;")
                            .replace("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 8, 0, 1, 2);", "")
                    );
                }
                VBox.setVgrow(schemaPanel, Priority.ALWAYS);
                vBox.getChildren().add(schemaPanel);
            }
        }

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        vBox.getChildren().add(spacer);

        return vBox;
    }

    private VBox buildCollapsed() {
        VBox vBox = new VBox();
        vBox.setStyle("-fx-background-radius: 15;" +
                        "-fx-background-color: #FFFFFF;" +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 12, 0, 0, 0);");
        vBox.setPrefWidth(68);
        vBox.setAlignment(Pos.TOP_CENTER);

        HBox top = createTopHBox(creds.getInitials(), null, null, false);
        vBox.getChildren().add(top);

        for (int i = 0; i < LABELS.length; i++) {
            HBox item = createMenuItem(ICONS[i], LABELS[i], LABELS[i].equals(activeMenu), false);
            vBox.getChildren().add(item);
        }

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        return vBox;
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
        rec.setFill(Color.web("#285A48"));

        StackPane avatar = new StackPane(rec, initialLabel);

        ImageView arrowIcon = new ImageView(new Image(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream("assets/arrow.png"))));
        arrowIcon.setFitWidth(16);
        arrowIcon.setFitHeight(16);
        if (expanded) arrowIcon.setRotate(180);

        StackPane arrowBtn = new StackPane(arrowIcon);
        arrowBtn.setStyle("-fx-background-color: #2E5A47; -fx-background-radius: 50;");
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
        name.setStyle("-fx-font-weight: bold; -fx-fill: #333;");
        String rawUrl = (url != null) ? url : "";
        String limitedUrl = (rawUrl.length() > 20) ? rawUrl.substring(0, 20) + "..." : rawUrl;
        Text urlText = new Text(limitedUrl);
        urlText.setStyle("-fx-fill: gray; -fx-font-size: 10px;");
        info.getChildren().addAll(name, urlText);
        info.setAlignment(Pos.CENTER_LEFT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox hbox = new HBox(12, avatar, info, spacer, arrowBtn);
        hbox.setAlignment(Pos.CENTER_LEFT);
        hbox.setPadding(new Insets(12, 12, 8, 12));
        return hbox;
    }

    private HBox createMenuItem(String icon, String labelText, boolean isSelected, boolean showLabel) {
        ImageView imageView = new ImageView(new Image(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(icon))));
        imageView.setFitHeight(22);
        imageView.setFitWidth(22);

        HBox hbox;
        Text label = null;

        if (showLabel) {
            label = new Text(labelText);
            label.setFont(Font.font("System", 13));
            label.setStyle("-fx-font-weight: bold;");
            hbox = new HBox(20, imageView, label);
            hbox.setPadding(new Insets(9, 12, 9, 12));
            hbox.setMinWidth(200);
            hbox.setAlignment(Pos.CENTER_LEFT);
        } else {
            hbox = new HBox(imageView);
            hbox.setAlignment(Pos.CENTER);
            hbox.setPrefWidth(68);
            hbox.setPadding(new Insets(9, 0, 9, 0));
        }

        hbox.setPrefHeight(30);
        VBox.setMargin(hbox, new Insets(1, 6, 1, 6));

        final Text finalLabel = label;
        if (isSelected) applySelectedStyle(hbox, finalLabel);
        else            applyDefaultStyle(hbox, finalLabel);

        hbox.setOnMouseClicked(e -> {
            activeMenu = labelText;
            switchCenterContent(labelText);
            createSide();
        });

        return hbox;
    }

    private void applySelectedStyle(HBox hbox, Text label) {
        hbox.setBackground(new Background(new BackgroundFill(
                Color.web("#2E5A47"), new CornerRadii(8), Insets.EMPTY)));
        if (label != null) label.setFill(Color.WHITE);

        if (!hbox.getChildren().isEmpty() && hbox.getChildren().getFirst() instanceof ImageView iv)
            iv.setStyle("-fx-effect: null;");
    }

    private void applyDefaultStyle(HBox hbox, Text label) {
        hbox.setBackground(null);
        if (label != null) label.setFill(Color.web("#4A4A4A"));
    }

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