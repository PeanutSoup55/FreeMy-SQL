package GUI.Settings;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

public class Theme extends BorderPane {

    private final String[] softBeach = {"#51E2F5", "#9DF9EF", "#EDF756", "#FFA8B6", "#A28089"};
    private final String[] violetIceberg = {"#A0D2EB", "#E5EAF5", "#D0BDF4", "#8458B3", "#494D5F"};
    private final String[] contrastBlast = {"#FF1D58", "#F75990", "#FFF685", "#00DDFF", "#0049B7"};

    // Global colour variables — populated with the selected palette's values
    public static String colour1;
    public static String colour2;
    public static String colour3;
    public static String colour4;
    public static String colour5;

    private VBox rowsContainer;
    private String selectedTheme = "Soft Beach";

    public Theme() {
        setStyle("-fx-background-color: #F4F5F9;");
        setTop(buildHeader());
        setCenter(buildContent());

        applySelection(softBeach);
    }

    private HBox buildHeader() {
        Label title = new Label("Theme");
        title.setStyle("-fx-text-fill: #1B1F2A; -fx-font-size: 24px; -fx-font-weight: bold;");

        Label subtitle = new Label("Choose a colour palette for the app");
        subtitle.setStyle("-fx-text-fill: #6B7280; -fx-font-size: 13px;");

        VBox titles = new VBox(4, title, subtitle);

        HBox header = new HBox(titles);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(40, 48, 24, 48));
        header.setStyle("-fx-background-color: #F4F5F9; -fx-border-color: transparent transparent rgba(0,0,0,0.06) transparent; -fx-border-width: 0 0 1 0;");

        return header;
    }

    private ScrollPane buildContent() {
        rowsContainer = new VBox(24);
        rowsContainer.setPadding(new Insets(32, 48, 48, 48));

        rowsContainer.getChildren().add(makeRow("Soft Beach", softBeach));
        rowsContainer.getChildren().add(makeRow("Violet Iceberg", violetIceberg));
        rowsContainer.getChildren().add(makeRow("Contrast Blast", contrastBlast));

        refreshSelection();

        ScrollPane scroll = new ScrollPane(rowsContainer);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #F4F5F9; -fx-background-color: transparent;");

        return scroll;
    }

    public VBox makeRow(String name, String[] colours) {
        Label label = new Label(name);
        label.setStyle("-fx-text-fill: #1B1F2A; -fx-font-size: 15px; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox swatchRow = new HBox(6);
        swatchRow.setAlignment(Pos.CENTER_RIGHT);

        for (String hex : colours) {
            Rectangle swatch = new Rectangle(56, 32);
            swatch.setArcWidth(6);
            swatch.setArcHeight(6);
            swatch.setStyle("-fx-fill: " + hex + "; -fx-stroke: rgba(0,0,0,0.08); -fx-stroke-width: 1;");
            swatchRow.getChildren().add(swatch);
        }

        HBox content = new HBox(label, spacer, swatchRow);
        content.setAlignment(Pos.CENTER_LEFT);

        VBox container = new VBox(content);
        container.setId(name);
        container.setPadding(new Insets(28, 32, 28, 32));
        container.setStyle(
                "-fx-background-color: #FFFFFF;" +
                        "-fx-background-radius: 14;" +
                        "-fx-border-radius: 14;" +
                        "-fx-border-color: rgba(0,0,0,0.06);" +
                        "-fx-border-width: 1;" +
                        "-fx-cursor: hand;"
        );

        container.setOnMouseClicked(e -> {
            selectedTheme = name;
            applySelection(colours);
            refreshSelection();
        });

        return container;
    }

    private void applySelection(String[] colours) {
        colour1 = colours[0];
        colour2 = colours[1];
        colour3 = colours[2];
        colour4 = colours[3];
        colour5 = colours[4];
    }

    private void refreshSelection() {
        if (rowsContainer == null) {
            return;
        }
        for (var node : rowsContainer.getChildren()) {
            if (!(node instanceof VBox row)) {
                continue;
            }
            boolean selected = row.getId() != null && row.getId().equals(selectedTheme);
            String border = selected
                    ? "-fx-border-color: #3D6FE0; -fx-border-width: 2;"
                    : "-fx-border-color: rgba(0,0,0,0.06); -fx-border-width: 1;";
            row.setStyle(
                    "-fx-background-color: #FFFFFF;" +
                            "-fx-background-radius: 14;" +
                            "-fx-border-radius: 14;" +
                            border +
                            "-fx-cursor: hand;"
            );
        }
    }

    public String getSelectedTheme() {
        return selectedTheme;
    }
}