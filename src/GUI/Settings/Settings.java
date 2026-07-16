package GUI.Settings;

import GUI.FeedbackDialog;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.util.Objects;

public class Settings extends BorderPane {

    private final String[] LABELS = {"Account", "Theme", "Docs", "Give Feedback", "Version News"};
    private final String[] ICONS = { "assets/account.png", "assets/theme.png", "assets/docs.png", "assets/feedback.png", "assets/version.png"};
    private VBox navBar;
    private String activePage = "Account";

    public Settings() {
        setStyle("-fx-background-color: #FFFFFF;");
        buildNav();
        switchPage(activePage);
    }

    private void buildNav() {
        navBar = new VBox();
        navBar.setSpacing(2);
        navBar.setPrefWidth(180);
        navBar.setMinWidth(180);
        navBar.setPadding(new Insets(16, 8, 16, 8));
        navBar.setStyle("-fx-background-color: #080C14;");

        Label title = new Label("Settings");
        title.setStyle("-fx-text-fill: #FFFFFF; -fx-font-size: 16; -fx-font-weight: bold;");
        VBox titleWrap = new VBox(title);
        titleWrap.setPadding(new Insets(0, 8, 16, 8));
        navBar.getChildren().add(titleWrap);

        for (int i = 0; i < LABELS.length; i++){
            navBar.getChildren().add(createNavItem(LABELS[i], ICONS[i]));
        }

        setLeft(navBar);
    }

    private HBox createNavItem(String label, String icon) {
        ImageView iv = new ImageView(new Image(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(icon))));
        iv.setFitWidth(16);
        iv.setFitHeight(16);

        Label item = new Label(label);
        item.setCursor(Cursor.HAND);

        HBox nav = new HBox(10, iv, item);
        nav.setAlignment(Pos.CENTER_LEFT);
        nav.setPadding(new Insets(8, 10, 8, 10));
        nav.setMaxWidth(Double.MAX_VALUE);
        nav.setCursor(Cursor.HAND);
        nav.setUserData(label);

        applyNavStyle(nav, item, label.equals(activePage));

        nav.setOnMouseEntered(e -> {
            if (!label.equals(activePage))
                nav.setStyle(nav.getStyle() + "-fx-background-color: rgba(255,255,255,0.08);");
        });
        nav.setOnMouseExited(e -> {
            if (!label.equals(activePage))
                applyNavStyle(nav, item, false);
        });
        nav.setOnMouseClicked(e -> {
            activePage = label;
            switchPage(label);
            rebuildNavStyles();
        });

        return nav;
    }

    private void rebuildNavStyles() {
        for (var node : navBar.getChildren()) {
            if (node instanceof HBox nav && nav.getUserData() instanceof String label) {
                Label item = (Label) nav.getChildren().get(1);
                applyNavStyle(nav, item, label.equals(activePage));
            }
        }
    }

    private void applyNavStyle(HBox nav, Label item, boolean active) {
        nav.setStyle(active
                ? "-fx-background-color: rgba(255,255,255,0.15); -fx-background-radius: 6;"
                : "-fx-background-color: transparent; -fx-background-radius: 6;");
        item.setStyle(active
                ? "-fx-text-fill: #FFFFFF; -fx-font-weight: bold;"
                : "-fx-text-fill: #C8D0D8; -fx-font-weight: normal;");
    }

    private void switchPage(String page) {
        switch (page) {
            case "Account" -> setCenter(new Account());
            case "Theme" -> setCenter(new Theme());
            case "Docs" -> setCenter(new Documentation());
            case "Give Feedback" -> setCenter(new FeedbackDialog());
            case "Version News" -> setCenter(new Versions());
        }
    }
}