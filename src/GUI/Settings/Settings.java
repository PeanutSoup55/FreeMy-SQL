package GUI.Settings;

import GUI.FeedbackDialog;
import GUI.Settings.Documentation.Documentation;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

public class Settings extends BorderPane {

    private final String[] LABELS = {"Account", "Theme", "Docs", "Give Feedback", "Version News"};
    private final String[] ICONS = { "assets/account.svg", "assets/theme.svg", "assets/docs.svg", "assets/feedback.svg", "assets/version.svg"};
    private VBox navBar;
    private Label title;
    private String activePage = "Account";

    public Settings() {
        setStyle("-fx-background-color: #FFFFFF;");
        buildNav();
        switchPage(activePage);
        Theme.registerThemeListener(this, this::refreshTheme);
    }

    private void buildNav() {
        navBar = new VBox();
        navBar.setSpacing(2);
        navBar.setPrefWidth(180);
        navBar.setMinWidth(180);
        navBar.setPadding(new Insets(16, 8, 16, 8));
        navBar.setStyle("-fx-background-color: " + Theme.colour1 + ";");

        title = new Label("Settings");
        title.setStyle("-fx-text-fill: " + Theme.colour6 + "; -fx-font-size: 16; -fx-font-weight: bold;");
        VBox titleWrap = new VBox(title);
        titleWrap.setPadding(new Insets(0, 8, 16, 8));
        navBar.getChildren().add(titleWrap);

        for (int i = 0; i < LABELS.length; i++){
            navBar.getChildren().add(createNavItem(LABELS[i], ICONS[i]));
        }

        setLeft(navBar);
    }

    private HBox createNavItem(String label, String icon) {
        boolean active = label.equals(activePage);
        Group iconGroup = SvgIcon.load(icon, 16, active ? Theme.colour6 : Theme.colour7);

        Label item = new Label(label);
        item.setCursor(Cursor.HAND);

        HBox nav = new HBox(10, iconGroup, item);
        nav.setAlignment(Pos.CENTER_LEFT);
        nav.setPadding(new Insets(8, 10, 8, 10));
        nav.setMaxWidth(Double.MAX_VALUE);
        nav.setCursor(Cursor.HAND);
        nav.setUserData(label);
        nav.getProperties().put("icon", icon);

        applyNavStyle(nav, item, label.equals(activePage));

        nav.setOnMouseEntered(e -> {
            if (!label.equals(activePage))
                nav.setStyle(nav.getStyle() + "-fx-background-color: " + Theme.colour3 + ";");
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
                ? "-fx-background-color: " + Theme.colour3 + "; -fx-background-radius: 6;"
                : "-fx-background-color: transparent; -fx-background-radius: 6;");
        item.setStyle(active
                ? "-fx-text-fill: " + Theme.colour6 + "; -fx-font-weight: bold;"
                : "-fx-text-fill: " + Theme.colour7 + "; -fx-font-weight: normal;");

        if (nav.getChildren().get(0) instanceof Group iconGroup && nav.getProperties().get("icon") instanceof String icon) {
            SvgIcon.setContent(iconGroup, icon, 16, active ? Theme.colour6 : Theme.colour7);
        }
    }

    // NEW: re-apply theme colours to already-built nav without rebuilding the whole tree
    private void refreshTheme() {
        navBar.setStyle("-fx-background-color: " + Theme.colour1 + ";");
        title.setStyle("-fx-text-fill: " + Theme.colour6 + "; -fx-font-size: 16; -fx-font-weight: bold;");
        rebuildNavStyles();
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