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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.prefs.Preferences;

public class Theme extends BorderPane {

    private static final LinkedHashMap<String, LinkedHashMap<String, String[]>> CATEGORIES = new LinkedHashMap<>();
    static {
        LinkedHashMap<String, String[]> dark = new LinkedHashMap<>();
        dark.put("Default", new String[]{"#1C2333", "#080C14", "#121723", "#ffffff", "#ffffff", "#FFFFFF", "#A9B4C7"});
        dark.put("Neon Tokyo", new String[]{"#1A0B2E", "#0D0517", "#FF2E8E", "#00F0FF", "#D9C9F0", "#FFFFFF", "#C9B8E8"});
        dark.put("Dust and Rust", new String[]{"#3E2723", "#251612", "#8D5524", "#F4E4C1", "#E0B888", "#F5EBDD", "#C9A876"});
        dark.put("Moss and Pine", new String[]{"#1B2E23", "#0F1912", "#3D5A45", "#E8F0E5", "#B5D2C0", "#E8F0E5", "#9DBFA8"});
        dark.put("Blood Moon", new String[]{"#2B0A0A", "#150404", "#7A1F1F", "#F5E6D3", "#D9B896", "#F5E6D3", "#C99B7A"});
        dark.put("Soft Beach (Dark)", new String[]{"#0F2B30", "#081A1D", "#173942", "#9DF9EF", "#51E2F5", "#E7FBFA", "#B8F5F0"});
        dark.put("Violet Iceberg (Dark)", new String[]{"#2A2438", "#1A1624", "#3D3450", "#D0BDF4", "#A0D2EB", "#EDE6F7", "#C3B3E0"});
        dark.put("Contrast Blast (Dark)", new String[]{"#4A0E24", "#2E0916", "#6B1438", "#FFF685", "#FF1D58", "#FFF9C4", "#FF7A9E"});
        dark.put("Dust and Rust (Darker)", new String[]{"#231412", "#140B0A", "#3A1F1B", "#C99B6E", "#8D5524", "#EAD9BE", "#B98F5E"});
        dark.put("Moss and Pine (Darker)", new String[]{"#0F1C15", "#08110C", "#1E2E23", "#8FAF9A", "#3D5A45", "#D7E6DB", "#A9C7B5"});
        CATEGORIES.put("Dark", dark);

        LinkedHashMap<String, String[]> mellow = new LinkedHashMap<>();
        mellow.put("Sage", new String[]{"#DCE5DC", "#C8D3C8", "#B4C4B4", "#2E3B2E", "#5A6B5A", "#1F2A1F", "#4A5A4A"});
        mellow.put("Warm Sand", new String[]{"#F0E6D6", "#E8DAC4", "#DCC9A8", "#3E3324", "#6B5D48", "#2A2015", "#5C4D38"});
        mellow.put("Dusty Rose", new String[]{"#E8D5D3", "#DCC2C0", "#C9A8A5", "#3E2C2B", "#6B4F4D", "#2A1817", "#5C3E3B"});
        mellow.put("Slate Blue", new String[]{"#D4DDE5", "#C0CCD8", "#A8B9C9", "#26313D", "#4C5C6B", "#16202A", "#3A4A5A"});
        mellow.put("Muted Lavender", new String[]{"#E2DCE8", "#D2C7DC", "#BBA9CC", "#332B3D", "#5C4F6B", "#241D2E", "#4A3D5C"});
        CATEGORIES.put("Mellow", mellow);

        LinkedHashMap<String, String[]> light = new LinkedHashMap<>();
        light.put("Soft Beach", new String[]{"#51E2F5", "#9DF9EF", "#EDF756", "#0F2B30", "#3A5A61", "#072B30", "#1F4A50"});
        light.put("Violet Iceberg", new String[]{"#A0D2EB", "#E5EAF5", "#D0BDF4", "#3D2B5C", "#2E313D", "#1A2438", "#3D4A5C"});
        light.put("Contrast Blast", new String[]{"#FF1D58", "#F75990", "#FFF685", "#FFFFFF", "#FFE5EC", "#FFFFFF", "#FFD6E3"});
        light.put("Vapor Dream", new String[]{"#E0C3FC", "#C9A3F0", "#F5B7E0", "#2D1B3D", "#4A3459", "#2D1B3D", "#5C4370"});
        light.put("Glacier", new String[]{"#EAF4FB", "#FFFFFF", "#D6EAF5", "#1A2B3C", "#3D5568", "#12202E", "#3D5568"});
        CATEGORIES.put("Light", light);
    }

    public static String colourDark;
    private static final Preferences prefs = Preferences.userNodeForPackage(Theme.class);
    private static final String PREF_KEY = "selectedTheme";

    public static final Map<Object, Runnable> themeChangeListeners =
            java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

    public static void registerThemeListener(Object owner, Runnable listener) {
        themeChangeListeners.put(owner, listener);
    }

    public static String colour1;
    public static String colour2;
    public static String colour3;
    public static String colour4;
    public static String colour5;
    public static String colour6; // primary text — guaranteed contrast vs colour2
    public static String colour7; // secondary/muted text — guaranteed contrast vs colour2
    public static boolean isLight;

    private static String selectedTheme;

    static {
        selectedTheme = prefs.get(PREF_KEY, "Default");
        applySelection(findColours(selectedTheme));
    }

    private static String[] findColours(String name) {
        for (LinkedHashMap<String, String[]> group : CATEGORIES.values()) {
            if (group.containsKey(name)) return group.get(name);
        }
        return CATEGORIES.get("Dark").get("Default");
    }

    private VBox rowsContainer;

    public Theme() {
        rebuild();
        registerThemeListener(this, this::rebuild);
    }

    private void rebuild() {
        setStyle("-fx-background-color: " + colour2 + ";");
        setTop(buildHeader());
        setCenter(buildContent());
    }

    private HBox buildHeader() {
        Label title = new Label("Theme");
        title.setStyle("-fx-text-fill: " + colour6 + "; -fx-font-size: 24px; -fx-font-weight: bold;");

        Label subtitle = new Label("Choose a colour palette for the app");
        subtitle.setStyle("-fx-text-fill: " + colour7 + "; -fx-font-size: 13px;");

        VBox titles = new VBox(4, title, subtitle);

        HBox header = new HBox(titles);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(40, 48, 24, 48));
        header.setStyle("-fx-background-color: " + colour2 + "; -fx-border-color: transparent transparent "
                + colour3 + " transparent; -fx-border-width: 0 0 1 0;");

        return header;
    }

    private ScrollPane buildContent() {
        rowsContainer = new VBox(24);
        rowsContainer.setPadding(new Insets(32, 48, 48, 48));

        for (Map.Entry<String, LinkedHashMap<String, String[]>> category : CATEGORIES.entrySet()) {
            rowsContainer.getChildren().add(buildCategoryHeader(category.getKey()));
            VBox group = new VBox(10);
            for (Map.Entry<String, String[]> theme : category.getValue().entrySet()) {
                group.getChildren().add(makeRow(theme.getKey(), theme.getValue()));
            }
            rowsContainer.getChildren().add(group);
        }

        refreshSelection();

        ScrollPane scroll = new ScrollPane(rowsContainer);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: " + colour2 + "; -fx-background-color: transparent;");

        return scroll;
    }

    private Label buildCategoryHeader(String name) {
        Label label = new Label(name.toUpperCase());
        label.setStyle("-fx-text-fill: " + colour7 + "; -fx-font-size: 11px; -fx-font-weight: bold;");
        return label;
    }

    public VBox makeRow(String name, String[] colours) {
        Label label = new Label(name);
        label.setStyle("-fx-text-fill: " + colour6 + "; -fx-font-size: 14px; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox swatchRow = new HBox(6);
        swatchRow.setAlignment(Pos.CENTER_RIGHT);

        for (String hex : colours) {
            Rectangle swatch = new Rectangle(60, 40);
            swatch.setArcWidth(6);
            swatch.setArcHeight(6);
            swatch.setStyle("-fx-fill: " + hex + "; -fx-stroke: rgba(0,0,0,0.08); -fx-stroke-width: 1;");
            swatchRow.getChildren().add(swatch);
        }

        HBox content = new HBox(label, spacer, swatchRow);
        content.setAlignment(Pos.CENTER_LEFT);

        VBox container = new VBox(content);
        container.setId(name);
        container.setPadding(new Insets(8, 16, 8, 16));
        container.setStyle(
                "-fx-background-color: " + colour1 + ";" +
                        "-fx-background-radius: 14;" +
                        "-fx-border-radius: 14;" +
                        "-fx-border-color: " + colour3 + ";" +
                        "-fx-border-width: 2;" +
                        "-fx-cursor: hand;"
        );

        container.setOnMouseClicked(e -> {
            selectedTheme = name;
            applySelection(colours);
            prefs.put(PREF_KEY, name);
            rebuild();
            for (Runnable listener : new java.util.ArrayList<>(themeChangeListeners.values())) listener.run();
        });

        return container;
    }

    private static void applySelection(String[] colours) {
        colour1 = colours[0];
        colour2 = colours[1];
        colour3 = colours[2];
        colour4 = colours[3];
        colour5 = colours[4];
        colour6 = colours[5];
        colour7 = colours[6];
        isLight = isLightColour(colour1);
        colourDark = colours[0];
        double darkestLum = Double.MAX_VALUE;
        for (String hex : colours) {
            double lum = luminance(hex);
            if (lum < darkestLum) { darkestLum = lum; colourDark = hex; }
        }
    }

    private static double luminance(String hex) {
        javafx.scene.paint.Color c = javafx.scene.paint.Color.web(hex);
        return 0.299 * c.getRed() + 0.587 * c.getGreen() + 0.114 * c.getBlue();
    }

    private static boolean isLightColour(String hex) {
        javafx.scene.paint.Color c = javafx.scene.paint.Color.web(hex);
        double luminance = 0.299 * c.getRed() + 0.587 * c.getGreen() + 0.114 * c.getBlue();
        return luminance > 0.5;
    }

    private void refreshSelection() {
        if (rowsContainer == null) {
            return;
        }
        for (var outer : rowsContainer.getChildren()) {
            if (!(outer instanceof VBox group)) continue;
            for (var node : group.getChildren()) {
                if (!(node instanceof VBox row)) continue;
                boolean selected = row.getId() != null && row.getId().equals(selectedTheme);
                String border = selected
                        ? "-fx-border-color: " + colourDark + ";"
                        : "-fx-border-color: " + colour3 + ";";
                row.setStyle(
                        "-fx-background-color: " + colour1 + ";" +
                                "-fx-background-radius: 14;" +
                                "-fx-border-radius: 14;" +
                                border +
                                "-fx-border-width: 2;" +
                                "-fx-cursor: hand;"
                );
            }
        }
    }

    public String getSelectedTheme() {
        return selectedTheme;
    }
}