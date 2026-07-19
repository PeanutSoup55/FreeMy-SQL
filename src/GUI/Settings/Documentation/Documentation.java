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

public class Documentation extends BorderPane {

    private record DocPage(String key, String title, String body, String imagePath) {}
    private record DocCategory(String key, String label, List<DocPage> pages) {}

    private static final List<DocCategory> CATEGORIES = List.of(
            new DocCategory("getting-started", "Getting Started", List.of(
                    new DocPage("first-connection", "First Connection",
                            "Connect to your first MySQL database. Enter your host, port, username, and password, "
                                    + "then Free My Query will read the schema and render it as an interactive diagram.",
                            "assets/docs/getting-started/first-connection.png"),
                    new DocPage("troubleshoot-first-connection", "Troubleshooting First Connection",
                            "Common causes of connection failure: incorrect host/port, firewall rules blocking the "
                                    + "MySQL port, or a user account that doesn't have remote-login privileges.",
                            "assets/docs/getting-started/troubleshoot-first-connection.png")
            )),
            new DocCategory("schemas", "Schemas", List.of(
                    new DocPage("add-schema", "Add a Schema",
                            "Create a new schema from the sidebar. Free My Query will scaffold an empty canvas you "
                                    + "can start adding tables to, or connect it to an existing database.",
                            "assets/docs/schemas/add-schema.png"),
                    new DocPage("edit-schema", "Edit a Schema",
                            "Rename, reorganize, or delete a schema from the schema card grid. Deleting a schema does "
                                    + "not drop the underlying database — it only removes it from the app.",
                            "assets/docs/schemas/edit-schema.png")
            )),
            new DocCategory("tables", "Tables", List.of(
                    new DocPage("make-table", "Make a Table",
                            "Add a new table to the canvas, define columns, types, and constraints, then push it "
                                    + "live to your database.",
                            "assets/docs/tables/make-table.png"),
                    new DocPage("edit-table", "Edit a Table",
                            "Modify columns, keys, and relationships on an existing table directly from the ER diagram.",
                            "assets/docs/tables/edit-table.png")
            )),
            new DocCategory("data", "Data", List.of(
                    new DocPage("view-data", "View Data",
                            "Browse table rows in a searchable, sortable grid without writing any SQL.",
                            "assets/docs/data/view-data.png"),
                    new DocPage("crud-data", "CRUD Data",
                            "Insert, update, and delete rows directly through the GUI. Changes are validated against "
                                    + "the table's schema before being committed.",
                            "assets/docs/data/crud-data.png"),
                    new DocPage("generate-login-function", "Generate Login Function",
                            "Generate a ready-to-use login code snippet scoped to a specific table's credentials structure.",
                            "assets/docs/data/generate-login-function.png")
            )),
            new DocCategory("ssh", "SSH", List.of(
                    new DocPage("ssh-first-connection", "First Connection",
                            "Set up an SSH tunnel to a remote MySQL instance using a host, port, and private key or "
                                    + "password authentication.",
                            "assets/docs/ssh/first-connection.png"),
                    new DocPage("ssh-troubleshoot", "Troubleshoot",
                            "Common SSH tunnel issues: wrong port, key permissions, or the remote MySQL user not "
                                    + "being allowed to connect from the tunnel's bind address.",
                            "assets/docs/ssh/troubleshoot.png"),
                    new DocPage("ssh-cloning", "Cloning Schemas / Early Version Control",
                            "Clone a remote schema locally to snapshot its structure before making changes — a "
                                    + "lightweight stand-in for version control until proper migrations are supported.",
                            "assets/docs/ssh/cloning.png")
            ))
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

    // ---------------- Top tab bar ----------------

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

    // ---------------- Left sidebar (subsections of active tab) ----------------

    private ScrollPane buildSidebar() {
        DocCategory category = currentCategory();

        VBox navList = new VBox(2);
        navList.setPadding(new Insets(20, 12, 12, 12));
        for (DocPage page : category.pages()) {
            navList.getChildren().add(buildNavItem(page));
        }

        VBox sidebar = new VBox(navList);
        sidebar.setPrefWidth(220);
        sidebar.setMinWidth(220);
        sidebar.setStyle("-fx-background-color: " + Theme.colour1 + "; -fx-border-color: transparent "
                + Theme.colour3 + " transparent transparent; -fx-border-width: 0 1 0 0;");

        ScrollPane scroll = new ScrollPane(sidebar);
        scroll.setFitToWidth(true);
        scroll.setPrefWidth(220);
        scroll.setStyle("-fx-background: " + Theme.colour1 + "; -fx-background-color: transparent;");
        return scroll;
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

    // ---------------- Content ----------------

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

    // ---------------- Lookups ----------------

    private DocCategory currentCategory() {
        return CATEGORIES.stream().filter(c -> c.key().equals(selectedCategory))
                .findFirst().orElse(CATEGORIES.get(0));
    }

    private DocPage currentPage() {
        return currentCategory().pages().stream().filter(p -> p.key().equals(selectedPage))
                .findFirst().orElse(currentCategory().pages().get(0));
    }
}