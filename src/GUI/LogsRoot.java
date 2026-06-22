package GUI;

import globalfuncs.db;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class LogsRoot extends BorderPane {

    // --- Palette (matches SSHConnection's light theme) ---
    private static final String BG        = "#F4F5F9";
    private static final String CARD      = "#FFFFFF";
    private static final String FIELD_BG  = "#F7F8FB";
    private static final String BORDER    = "#E1E5EC";
    private static final String ACCENT    = "#3D6FE0";
    private static final String ACCENT_BG = "#EAF0FD";
    private static final String TEXT      = "#1C2230";
    private static final String MUTED     = "#6B7280";
    private static final String GREEN     = "#1E9E5A";
    private static final String GREEN_BG  = "#E8F8EF";
    private static final String RED       = "#D9434B";
    private static final String RED_BG    = "#FCEAEC";
    private static final String SHADOW    = "dropshadow(gaussian, rgba(28,34,48,0.06), 14, 0, 0, 3)";

    private final TableView<String[]> table = new TableView<>();
    private final ComboBox<String> filterBox;
    private final TextField limitField;
    private final Label statusLabel;
    private ScheduledExecutorService scheduler;

    public LogsRoot() {
        setPadding(new Insets(28));
        setStyle("-fx-background-color: " + BG + ";");

        Text title = new Text("MySQL Logs");
        title.setFont(Font.font("System", FontWeight.BOLD, 20));
        title.setFill(Color.web(TEXT));

        filterBox = new ComboBox<>(FXCollections.observableArrayList(
                "ALL", "Query", "Execute", "Connect", "Quit", "Init DB"));
        filterBox.setValue("ALL");
        styleCombo(filterBox);
        filterBox.setOnAction(e -> loadLogs());

        limitField = new TextField("200");
        limitField.setPrefWidth(70);
        styleField(limitField);

        Button refreshBtn = filledBtn();
        refreshBtn.setOnAction(e -> loadLogs());

        Button autoBtn = outlineBtn();
        autoBtn.setOnAction(e -> toggleAutoRefresh(autoBtn));


        Label typeLabel = smallLabel("Type");
        Label limitLabel = smallLabel("Limit");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(14,
                title, spacer,
                typeLabel, filterBox,
                limitLabel, limitField,
                refreshBtn, autoBtn);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 16, 0));

        TableColumn<String[], String> timeCol = col("Time", 0, 175);
        TableColumn<String[], String> userCol = col("User / Host", 1, 175);
        TableColumn<String[], String> typeCol = col("Command", 2, 100);
        TableColumn<String[], String> queryCol = col("Query", 3, -1);

        queryCol.prefWidthProperty().bind(
                table.widthProperty()
                        .subtract(timeCol.widthProperty())
                        .subtract(userCol.widthProperty())
                        .subtract(typeCol.widthProperty())
                        .subtract(20));

        table.getColumns().addAll(timeCol, userCol, typeCol, queryCol);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(styledPlaceholder());
        table.setStyle(
                "-fx-background-color: " + CARD + ";" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: transparent;");

        styleTableHeaders();

        table.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(String[] row, boolean empty) {
                super.updateItem(row, empty);
                if (empty || row == null) {
                    setStyle("-fx-background-color: " + CARD + ";");
                    return;
                }
                int idx = getIndex();
                String base = (idx % 2 == 0) ? CARD : FIELD_BG;
                setStyle(switch (row[2] == null ? "" : row[2]) {
                    case "Query",
                         "Execute" -> "-fx-background-color: " + base + ";";
                    case "Connect" -> "-fx-background-color: " + GREEN_BG + ";";
                    case "Quit" -> "-fx-background-color: " + RED_BG + ";";
                    default -> "-fx-background-color: " + base + ";";
                });
            }
        });

        statusLabel = new Label("Ready.");
        statusLabel.setTextFill(Color.web(MUTED));
        statusLabel.setFont(Font.font("System", 12));
        statusLabel.setPadding(new Insets(10, 0, 0, 2));

        VBox tableCard = new VBox(table);
        tableCard.setStyle(
                "-fx-background-color: " + CARD + ";" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: " + BORDER + ";" +
                        "-fx-border-radius: 12;" +
                        "-fx-effect: " + SHADOW + ";");
        VBox.setVgrow(table, Priority.ALWAYS);

        VBox center = new VBox(0, header, tableCard, statusLabel);
        VBox.setVgrow(tableCard, Priority.ALWAYS);
        setCenter(center);
        db.EnableLogging();
        loadLogs();
    }

    private void loadLogs() {
        statusLabel.setText("Loading…");
        statusLabel.setTextFill(Color.web(MUTED));

        int limit = 200;
        try { limit = Integer.parseInt(limitField.getText().trim()); }
        catch (NumberFormatException ignored) {}

        final int finalLimit = limit;
        final String filter = filterBox.getValue();

        Thread.ofVirtual().start(() -> {
            List<String[]> rows = db.GetLogs(finalLimit, filter);
            Platform.runLater(() -> {
                table.setItems(FXCollections.observableArrayList(rows));
                statusLabel.setText(rows.size() + " rows  ·  last updated " +
                        java.time.LocalTime.now().withNano(0));
                statusLabel.setTextFill(Color.web(MUTED));
            });
        });
    }


    private void toggleAutoRefresh(Button btn) {
        if (scheduler == null || scheduler.isShutdown()) {
            scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "log-poller");
                t.setDaemon(true);
                return t;
            });
            scheduler.scheduleAtFixedRate(
                    () -> Platform.runLater(this::loadLogs), 0, 5, TimeUnit.SECONDS);
            btn.setText("⏹  Stop Auto");
            btn.setStyle(
                    "-fx-background-color: transparent;" +
                            "-fx-text-fill: " + RED + ";" +
                            "-fx-border-color: " + RED + ";" +
                            "-fx-border-radius: 8;" +
                            "-fx-background-radius: 8;" +
                            "-fx-font-weight: bold;" +
                            "-fx-cursor: hand;" +
                            "-fx-padding: 10 20;" +
                            "-fx-font-size: 13;");
        } else {
            scheduler.shutdownNow();
            btn.setText("▶  Auto-refresh");
            btn.setStyle(
                    "-fx-background-color: " + ACCENT_BG + ";" +
                            "-fx-text-fill: " + ACCENT + ";" +
                            "-fx-background-radius: 8;" +
                            "-fx-font-weight: bold;" +
                            "-fx-cursor: hand;" +
                            "-fx-padding: 10 20;" +
                            "-fx-font-size: 13;");
        }
    }


    private void styleTableHeaders() {
        table.getStylesheets().add("data:text/css," +
                ".table-view .column-header-background {" +
                "   -fx-background-color: %233D6FE0;" +
                "   -fx-background-radius: 10 10 0 0;" +
                "}" +
                ".table-view .column-header, .table-view .filler {" +
                "   -fx-background-color: transparent;" +
                "   -fx-border-color: transparent;" +
                "   -fx-size: 38;" +
                "}" +
                ".table-view .column-header .label {" +
                "   -fx-text-fill: white;" +
                "   -fx-font-weight: bold;" +
                "   -fx-font-size: 12px;" +
                "   -fx-alignment: CENTER_LEFT;" +
                "}" +
                ".table-view .table-cell {" +
                "   -fx-border-color: transparent;" +
                "   -fx-padding: 6 10;" +
                "}" +
                ".table-row-cell:selected {" +
                "   -fx-background-color: %233D6FE030;" +
                "}" +
                ".table-row-cell:selected .table-cell {" +
                "   -fx-text-fill: %231C2230;" +
                "}");
    }

    private TableColumn<String[], String> col(String header, int idx, double width) {
        TableColumn<String[], String> c = new TableColumn<>(header);
        c.setCellValueFactory(p -> new javafx.beans.property.SimpleStringProperty(
                p.getValue()[idx] != null ? p.getValue()[idx] : ""));

        c.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) { setText(null); setGraphic(null); return; }

                if (idx == 2) {
                    HBox cell = new HBox(6);
                    cell.setAlignment(Pos.CENTER_LEFT);

                    Region bar = new Region();
                    bar.setPrefWidth(5);
                    bar.setPrefHeight(18);
                    bar.setStyle("-fx-background-radius: 2; -fx-background-color: " +
                            switch (s) {
                                case "Query",
                                     "Execute" -> ACCENT + ";";
                                case "Connect" -> GREEN + ";";
                                case "Quit" -> RED + ";";
                                default -> MUTED + ";";
                            });

                    Label lbl = new Label(s.toUpperCase());
                    lbl.setFont(Font.font("Monospace", FontWeight.BOLD, 13));
                    lbl.setTextFill(Color.web(
                            switch (s) {
                                case "Query",
                                     "Execute" -> ACCENT;
                                case "Connect" -> GREEN;
                                case "Quit" -> RED;
                                default -> MUTED;
                            }));

                    cell.getChildren().addAll(bar, lbl);
                    setGraphic(cell);
                    setText(null);
                    return;
                }

                if (idx == 3) {
                    Text t = new Text(s);
                    t.setFont(Font.font("Monospace", 11));
                    t.setFill(Color.web(TEXT));
                    t.wrappingWidthProperty().bind(tc.widthProperty().subtract(20));
                    setGraphic(t);
                    setText(null);
                    setPrefHeight(Control.USE_COMPUTED_SIZE);
                    return;
                }

                setText(s);
                setFont(Font.font("System", 12));
                setTextFill(Color.web(TEXT));
                setGraphic(null);
            }
        });

        if (width > 0) { c.setPrefWidth(width); c.setMinWidth(width); c.setMaxWidth(width); }
        return c;
    }

    private static Label styledPlaceholder() {
        Label l = new Label("No logs loaded — press Refresh.");
        l.setTextFill(Color.web(MUTED));
        l.setFont(Font.font("System", 13));
        return l;
    }


    private static void styleCombo(ComboBox<String> cb) {
        cb.setStyle(
                "-fx-background-color: " + FIELD_BG + ";" +
                        "-fx-border-color: " + BORDER + ";" +
                        "-fx-border-radius: 6;" +
                        "-fx-background-radius: 6;" +
                        "-fx-cursor: hand;" +
                        "-fx-padding: 2 4;");
        cb.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                setText(empty || s == null ? "" : s);
                setTextFill(Color.web(TEXT));
                setStyle("-fx-background-color: transparent; -fx-font-weight: bold;");
            }
        });
    }

    private static void styleField(TextField field) {
        field.setStyle(
                "-fx-background-color: " + FIELD_BG + ";" +
                        "-fx-text-fill: " + TEXT + ";" +
                        "-fx-prompt-text-fill: #9AA3B2;" +
                        "-fx-border-color: " + BORDER + ";" +
                        "-fx-border-radius: 6;" +
                        "-fx-background-radius: 6;" +
                        "-fx-highlight-fill: " + ACCENT + ";" +
                        "-fx-padding: 8 10 8 10;" +
                        "-fx-font-size: 13;");
    }

    private static Label smallLabel(String text) {
        Label l = new Label(text);
        l.setTextFill(Color.web(MUTED));
        l.setFont(Font.font("System", FontWeight.BOLD, 12));
        return l;
    }

    private static Button filledBtn() {
        Button b = new Button("↻  Refresh");
        b.setStyle(
                "-fx-background-color: " + ACCENT + ";" +
                        "-fx-text-fill: white;" +
                        "-fx-background-radius: 8;" +
                        "-fx-font-weight: bold;" +
                        "-fx-cursor: hand;" +
                        "-fx-padding: 10 20;" +
                        "-fx-font-size: 13;");
        return b;
    }

    private static Button outlineBtn() {
        Button b = new Button("▶  Auto-refresh");
        b.setStyle(
                "-fx-background-color: " + ACCENT_BG + ";" +
                        "-fx-text-fill: " + ACCENT + ";" +
                        "-fx-background-radius: 8;" +
                        "-fx-font-weight: bold;" +
                        "-fx-cursor: hand;" +
                        "-fx-padding: 10 20;" +
                        "-fx-font-size: 13;");
        return b;
    }
}