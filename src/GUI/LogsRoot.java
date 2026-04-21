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

    private final TableView<String[]>  table = new TableView<>();
    private final ComboBox<String>     filterBox;
    private final TextField            limitField;
    private final Label                statusLabel;
    private ScheduledExecutorService   scheduler;

    public LogsRoot() {
        setPadding(new Insets(24, 28, 24, 28));
        setStyle("-fx-background-color: #F2F4F2;");

        // ── Title ─────────────────────────────────────────────────────────
        Text title = new Text("MySQL Logs");
        title.setFont(Font.font("System", FontWeight.BOLD, 20));
        title.setFill(Color.web("#1E3D30"));

        // ── Filter dropdown ───────────────────────────────────────────────
        filterBox = new ComboBox<>(FXCollections.observableArrayList(
                "ALL", "Query", "Execute", "Connect", "Quit", "Init DB"));
        filterBox.setValue("ALL");
        styleCombo(filterBox);
        filterBox.setOnAction(e -> loadLogs());

        // ── Limit field ───────────────────────────────────────────────────
        limitField = new TextField("200");
        limitField.setPrefWidth(70);
        limitField.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 8;" +
                        "-fx-border-color: #D0D8D0;" +
                        "-fx-border-radius: 8;" +
                        "-fx-padding: 8 10;" +
                        "-fx-font-size: 13;");

        // ── Buttons ───────────────────────────────────────────────────────
        Button refreshBtn = filledBtn();
        refreshBtn.setOnAction(e -> loadLogs());

        Button autoBtn = outlineBtn();
        autoBtn.setOnAction(e -> toggleAutoRefresh(autoBtn));


        // ── Header row ────────────────────────────────────────────────────
        Label typeLabel  = smallLabel("Type");
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

        // ── Table ─────────────────────────────────────────────────────────
        TableColumn<String[], String> timeCol  = col("Time",         0, 175);
        TableColumn<String[], String> userCol  = col("User / Host",  1, 175);
        TableColumn<String[], String> typeCol  = col("Command",      2, 100);
        TableColumn<String[], String> queryCol = col("Query",        3, -1);

        // Query column fills remaining width
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
                "-fx-background-color: white;" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: transparent;");

        // Header background + text via CSS on the scene — applied inline per column instead
        styleTableHeaders();

        // Row coloring by command type
        table.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(String[] row, boolean empty) {
                super.updateItem(row, empty);
                if (empty || row == null) {
                    setStyle("-fx-background-color: white;");
                    return;
                }
                // Alternate subtle stripe + type color
                int idx = getIndex();
                String base = (idx % 2 == 0) ? "#FFFFFF" : "#F7FAF8";
                setStyle(switch (row[2] == null ? "" : row[2]) {
                    case "Query",
                         "Execute" -> "-fx-background-color: " + base + ";";
                    case "Connect" -> "-fx-background-color: #EDF5F1;";
                    case "Quit"    -> "-fx-background-color: #FDF6EE;";
                    default        -> "-fx-background-color: " + base + ";";
                });
            }
        });

        // ── Status bar ────────────────────────────────────────────────────
        statusLabel = new Label("Ready.");
        statusLabel.setTextFill(Color.web("#888888"));
        statusLabel.setFont(Font.font("System", 12));
        statusLabel.setPadding(new Insets(6, 0, 0, 2));

        // ── Layout ────────────────────────────────────────────────────────
        VBox center = new VBox(0, header, table, statusLabel);
        VBox.setVgrow(table, Priority.ALWAYS);
        setCenter(center);
        db.EnableLogging();
        loadLogs();
    }

    private void loadLogs() {
        statusLabel.setText("Loading…");
        statusLabel.setTextFill(Color.web("#888888"));

        int limit = 200;
        try { limit = Integer.parseInt(limitField.getText().trim()); }
        catch (NumberFormatException ignored) {}

        final int    finalLimit = limit;
        final String filter     = filterBox.getValue();

        Thread.ofVirtual().start(() -> {
            List<String[]> rows = db.GetLogs(finalLimit, filter);
            Platform.runLater(() -> {
                table.setItems(FXCollections.observableArrayList(rows));
                statusLabel.setText(rows.size() + " rows  ·  last updated " +
                        java.time.LocalTime.now().withNano(0));
                statusLabel.setTextFill(Color.web("#888888"));
            });
        });
    }

    // ── Auto-refresh ──────────────────────────────────────────────────────

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
                    "-fx-background-color: #CC5500;" +
                            "-fx-text-fill: white;" +
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
                    "-fx-background-color: white;" +
                            "-fx-text-fill: #2E5A47;" +
                            "-fx-border-color: #2E5A47;" +
                            "-fx-border-radius: 8;" +
                            "-fx-background-radius: 8;" +
                            "-fx-font-weight: bold;" +
                            "-fx-cursor: hand;" +
                            "-fx-padding: 10 20;" +
                            "-fx-font-size: 13;");
        }
    }

    // ── Table styling ─────────────────────────────────────────────────────

    private void styleTableHeaders() {
        // JavaFX doesn't allow per-column header styling inline, so we inject
        // a stylesheet string onto the scene via the table's own style class.
        table.getStylesheets().add("data:text/css," +
                ".table-view .column-header-background {" +
                "   -fx-background-color: %232E5A47;" +
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
                "   -fx-background-color: %232E5A4730;" +
                "}" +
                ".table-row-cell:selected .table-cell {" +
                "   -fx-text-fill: %231E3D30;" +
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

                // Command type column — colored badge
                if (idx == 2) {
                    HBox cell = new HBox(6);
                    cell.setAlignment(Pos.CENTER_LEFT);

                    // Colored left-edge bar
                    Region bar = new Region();
                    bar.setPrefWidth(5);
                    bar.setPrefHeight(18);
                    bar.setStyle("-fx-background-radius: 2; -fx-background-color: " +
                            switch (s) {
                                case "Query",
                                     "Execute" -> "#2E5A47;";
                                case "Connect" -> "#3A6B8A;";
                                case "Quit"    -> "#8A3A3A;";
                                default        -> "#888888;";
                            });

                    // Monospace uppercase label — no background, just colored text
                    Label lbl = new Label(s.toUpperCase());
                    lbl.setFont(Font.font("Monospace", FontWeight.BOLD, 13));
                    lbl.setTextFill(Color.web(
                            switch (s) {
                                case "Query",
                                     "Execute" -> "#2E5A47";
                                case "Connect" -> "#3A6B8A";
                                case "Quit"    -> "#8A3A3A";
                                default        -> "#888888";
                            }));

                    cell.getChildren().addAll(bar, lbl);
                    setGraphic(cell);
                    setText(null);
                    return;
                }

                // Query column — monospace, wraps
                if (idx == 3) {
                    Text t = new Text(s);
                    t.setFont(Font.font("Monospace", 11));
                    t.setFill(Color.web("#2A2A2A"));
                    t.wrappingWidthProperty().bind(tc.widthProperty().subtract(20));
                    setGraphic(t);
                    setText(null);
                    setPrefHeight(Control.USE_COMPUTED_SIZE);
                    return;
                }

                // Time + User columns
                setText(s);
                setFont(Font.font("System", 12));
                setTextFill(Color.web("#444444"));
                setGraphic(null);
            }
        });

        if (width > 0) { c.setPrefWidth(width); c.setMinWidth(width); c.setMaxWidth(width); }
        return c;
    }

    private static Label styledPlaceholder() {
        Label l = new Label("No logs loaded — press Refresh.");
        l.setTextFill(Color.web("#AAAAAA"));
        l.setFont(Font.font("System", 13));
        return l;
    }

    // ── Shared style helpers ──────────────────────────────────────────────

    private static void styleCombo(ComboBox<String> cb) {
        cb.setStyle(
                "-fx-background-color: #2E5A47;" +
                        "-fx-background-radius: 8;" +
                        "-fx-cursor: hand;" +
                        "-fx-padding: 2 4;");
        cb.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                setText(empty || s == null ? "" : s);
                setTextFill(Color.WHITE);
                setStyle("-fx-background-color: transparent; -fx-font-weight: bold;");
            }
        });
    }

    private static Label smallLabel(String text) {
        Label l = new Label(text);
        l.setTextFill(Color.web("#666666"));
        l.setFont(Font.font("System", FontWeight.BOLD, 12));
        return l;
    }

    private static Button filledBtn() {
        Button b = new Button("↻  Refresh");
        b.setStyle(
                "-fx-background-color: #2E5A47;" +
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
                "-fx-background-color: white;" +
                        "-fx-text-fill: #2E5A47;" +
                        "-fx-border-color: #2E5A47;" +
                        "-fx-border-radius: 8;" +
                        "-fx-background-radius: 8;" +
                        "-fx-font-weight: bold;" +
                        "-fx-cursor: hand;" +
                        "-fx-padding: 10 20;" +
                        "-fx-font-size: 13;");
        return b;
    }
}