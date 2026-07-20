package GUI.Settings.Documentation;

import GUI.Settings.Theme;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.util.List;
import GUI.Settings.Documentation.DocTypes.DocCategory;
import GUI.Settings.Documentation.DocTypes.DocPage;

public class Documentation extends BorderPane {

    private static final List<DocCategory> CATEGORIES = List.of(
            GettingStartedDocs.CATEGORY,
            SchemasDocs.CATEGORY,
            TablesDocs.CATEGORY,
            DataDocs.CATEGORY,
            SshDocs.CATEGORY
    );

    private String selectedCategory = CATEGORIES.get(0).key();
    private String selectedPage = CATEGORIES.get(0).pages().get(0).key();

    public Documentation() {
        rebuild();
        Theme.registerThemeListener(this, this::rebuild);
    }

    private void rebuild() {
        Platform.runLater(() -> {
            setStyle("-fx-background-color: " + Theme.colour2 + ";");
            setTop(buildTabBar());
            setLeft(buildSidebar());
            setCenter(buildContent());
        });
    }

    private HBox buildTabBar() {
        HBox bar = new HBox(4);
        bar.setPadding(new Insets(16, 24, 16, 24));
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setStyle("-fx-background-color: " + Theme.colour1 + "; -fx-border-color: transparent transparent "
                + Theme.colour3 + " transparent; -fx-border-width: 0 0 1 0;");

        for (DocCategory category : CATEGORIES) {
            Label tab = new Label(category.label());
            boolean selected = category.key().equals(selectedCategory);
            tab.setPadding(new Insets(8, 16, 8, 16));
            tab.setStyle(tabStyle(selected));
            tab.setOnMouseClicked(e -> {
                selectedCategory = category.key();
                selectedPage = category.pages().get(0).key();
                rebuild();
            });
            bar.getChildren().add(tab);
        }
        return bar;
    }

    private String tabStyle(boolean selected) {
        String fg = selected ? Theme.colour6 : Theme.colour7;
        String border = selected ? Theme.colourDark : "transparent";
        String weight = selected ? "bold" : "normal";
        return "-fx-text-fill: " + fg + "; -fx-font-size: 13px; -fx-font-weight: " + weight
                + "; -fx-border-color: transparent transparent " + border + " transparent; "
                + "-fx-border-width: 0 0 2 0; -fx-cursor: hand;";
    }

    private VBox buildSidebar() {
        DocCategory category = currentCategory();

        VBox navList = new VBox(2);
        navList.setPadding(new Insets(12));
        for (DocPage page : category.pages()) {
            navList.getChildren().add(buildNavItem(page));
        }

        VBox sidebar = new VBox(navList);
        sidebar.setPrefWidth(220);
        sidebar.setMinWidth(220);
        sidebar.setStyle("-fx-background-color: " + Theme.colour1 + "; -fx-background-radius: 14; "
                + "-fx-border-color: " + Theme.colour3 + "; -fx-border-radius: 14;");

        // floats the sidebar off the window edges instead of sitting flush
        VBox.setMargin(sidebar, new Insets(20, 0, 20, 20));
        VBox wrapper = new VBox(sidebar);
        wrapper.setPadding(new Insets(20, 0, 20, 20));

        return wrapper;
    }

    private Label buildNavItem(DocPage page) {
        Label item = new Label(page.title());
        boolean selected = page.key().equals(selectedPage);
        item.setPadding(new Insets(8, 12, 8, 12));
        item.setMaxWidth(Double.MAX_VALUE);
        item.setStyle(navItemStyle(selected));
        item.setOnMouseClicked(e -> {
            selectedPage = page.key();
            rebuild();
        });
        return item;
    }

    private String navItemStyle(boolean selected) {
        String bg = selected ? Theme.colourDark + "22" : "transparent";
        String fg = selected ? Theme.colour6 : Theme.colour7;
        String weight = selected ? "bold" : "normal";
        return "-fx-background-color: " + bg + "; -fx-text-fill: " + fg
                + "; -fx-font-size: 13px; -fx-font-weight: " + weight
                + "; -fx-background-radius: 6; -fx-cursor: hand;";
    }

    private ScrollPane buildContent() {
        DocPage page = currentPage();

        Label heading = new Label(page.title());
        heading.setStyle("-fx-text-fill: " + Theme.colour6 + "; -fx-font-size: 26px; -fx-font-weight: bold;");

        Label body = new Label(page.body());
        body.setWrapText(true);
        body.setStyle("-fx-text-fill: " + Theme.colour7 + "; -fx-font-size: 14px;");
        body.setMaxWidth(680);

        VBox column = new VBox(16, heading, body);
        column.setPadding(new Insets(40, 48, 48, 48));
        column.setMaxWidth(760);

        ImageView image = loadDocImage(page.imagePath());
        if (image != null) {
            image.setFitWidth(680);
            image.setPreserveRatio(true);
            VBox imageFrame = new VBox(image);
            imageFrame.setPadding(new Insets(4));
            imageFrame.setStyle("-fx-background-color: " + Theme.colour1 + "; -fx-background-radius: 10; "
                    + "-fx-border-color: " + Theme.colour3 + "; -fx-border-radius: 10;");
            column.getChildren().add(imageFrame);
        }

        ScrollPane scroll = new ScrollPane(column);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: " + Theme.colour2 + "; -fx-background-color: transparent;");
        return scroll;
    }

    private ImageView loadDocImage(String resourcePath) {
        var stream = getClass().getClassLoader().getResourceAsStream(resourcePath);
        if (stream == null) return null;
        return new ImageView(new Image(stream));
    }

    private DocCategory currentCategory() {
        return CATEGORIES.stream().filter(c -> c.key().equals(selectedCategory))
                .findFirst().orElse(CATEGORIES.get(0));
    }

    private DocPage currentPage() {
        return currentCategory().pages().stream().filter(p -> p.key().equals(selectedPage))
                .findFirst().orElse(currentCategory().pages().get(0));
    }
}