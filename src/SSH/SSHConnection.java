package SSH;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

/**
 * The SSH portal panel. Wires itself up the moment it is placed on screen —
 * no user action is needed to start the service, and the Main entry point
 * is never touched.
 *
 * Shows a credentials form (with saved profiles) while disconnected, and
 * swaps to a live dashboard (tunnel diagram + server stats) once a
 * connection is established.
 *
 */
public class SSHConnection extends VBox {

    // --- Palette (light) ---
    private static final String BG        = "#F4F5F9";
    private static final String CARD      = "#FFFFFF";
    private static final String FIELD_BG  = "#F7F8FB";
    private static final String BORDER    = "#E1E5EC";
    private static final String ACCENT    = "#3D6FE0";
    private static final String ACCENT_BG = "#EAF0FD";
    private static final String TEXT      = "#1C2230";
    private static final String MUTED     = "#6B7280";
    private static final String GREEN     = "#1E9E5A";
    private static final String RED       = "#D9434B";
    private static final String LINE_OFF  = "#D7DCE5";
    private static final String SHADOW    = "dropshadow(gaussian, rgba(28,34,48,0.06), 14, 0, 0, 3)";

    // --- SSH Fields ---
    private final TextField     sshHostField     = new TextField("127.0.0.1");
    private final TextField     sshPortField     = new TextField("22");
    private final TextField     sshUserField     = new TextField("");
    private final PasswordField sshPasswordField = new PasswordField();

    // --- DB Fields ---
    private final TextField     dbHostField      = new TextField("127.0.0.1");
    private final TextField     dbPortField      = new TextField("3306");
    private final TextField     dbUserField      = new TextField("root");
    private final PasswordField dbPasswordField  = new PasswordField();
    private final TextField     dbNameField      = new TextField("");

    // --- Profiles ---
    private final ComboBox<String> profileSelector     = new ComboBox<>();
    private final TextField        profileNameField    = new TextField();
    private final Button           deleteProfileButton = new Button("Delete");

    // --- Controls (shared between form and dashboard) ---
    private final Button connectButton         = new Button("Connect");
    private final Button connectAndSaveButton   = new Button("Connect & Save");
    private final Button disconnectButton       = new Button("Disconnect");
    private final Label  statusLabel            = new Label("Status: Disconnected");

    // --- Views ---
    private final VBox formView           = new VBox(20);
    private final VBox dashboardView      = new VBox(20);
    private final HBox buttonRow          = new HBox(12);
    private final HBox dashboardHeaderRow = new HBox(12);

    // --- Diagrams ---
    private final TunnelDiagram formDiagram      = new TunnelDiagram();
    private final TunnelDiagram dashboardDiagram = new TunnelDiagram();

    // --- Dashboard stat labels ---
    private final Label connectedTitleLabel = new Label();
    private final Label uptimeValueLabel     = new Label("0s");
    private final Label bridgePortValueLabel = new Label("—");
    private final Label setupTimeValueLabel  = new Label("—");
    private final Label versionValueLabel        = new Label("Loading…");
    private final Label dbUptimeValueLabel       = new Label("Loading…");
    private final Label threadsValueLabel        = new Label("Loading…");
    private final Label maxConnectionsValueLabel = new Label("Loading…");
    private final Label openTablesValueLabel     = new Label("Loading…");
    private final Label slowQueriesValueLabel    = new Label("Loading…");
    private final Label charsetValueLabel        = new Label("Loading…");
    private final Label dbSizeValueLabel         = new Label("Loading…");
    private final Label tableCountValueLabel     = new Label("Loading…");

    private Timeline uptimeTimeline;

    public SSHConnection(Runnable onConnected) {
        super(20);
        setStyle("-fx-background-color: " + BG + ";");
        setPadding(new Insets(28));

        buildFormView();
        buildDashboardView();
        refreshProfileList();

        getChildren().add(formView);

        new ConnectionController(this, new SSH(), onConnected);
    }

    // -------------------------------------------------------------------------
    // Form view
    // -------------------------------------------------------------------------

    private void buildFormView() {
        styleField(sshHostField); styleField(sshPortField); styleField(sshUserField); styleField(sshPasswordField);
        styleField(dbHostField);  styleField(dbPortField);  styleField(dbUserField);  styleField(dbPasswordField); styleField(dbNameField);

        // -- Page header --
        Label pageTitle = new Label("Database Connection");
        pageTitle.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-size: 20px; -fx-font-weight: bold;");
        Label pageSubtitle = new Label("Connect to your MySQL server through a secure SSH tunnel.");
        pageSubtitle.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 13px;");
        VBox header = new VBox(4, pageTitle, pageSubtitle);

        // -- Profile card --
        VBox profileCard = card();
        Label profileTitle = cardTitle("Saved Connections", "load, save, or remove a profile");

        profileSelector.setPromptText("Select a saved connection…");
        profileSelector.setPrefWidth(240);
        styleComboBox(profileSelector);
        profileSelector.setOnAction(e -> loadSelectedProfile());
        stylePillButton(deleteProfileButton, CARD, RED, RED);
        deleteProfileButton.setOnAction(e -> deleteSelectedProfile());

        Label loadLabel = fieldLabel("Load");
        HBox loadRow = new HBox(10, profileSelector, deleteProfileButton);
        loadRow.setAlignment(Pos.CENTER_LEFT);

        profileNameField.setPromptText("e.g. Production DB");
        styleField(profileNameField);
        Label saveLabel = fieldLabel("Save as");

        GridPane profileGrid = formGrid();
        profileGrid.add(loadLabel, 0, 0);
        profileGrid.add(loadRow, 1, 0);
        profileGrid.add(saveLabel, 0, 1);
        profileGrid.add(profileNameField, 1, 1);
        GridPane.setHgrow(profileNameField, Priority.ALWAYS);
        profileNameField.setMaxWidth(Double.MAX_VALUE);

        profileCard.getChildren().addAll(profileTitle, profileGrid);

        // -- SSH card --
        VBox sshCard = card();
        Label sshTitle = cardTitle("SSH Tunnel", "the server gate");
        GridPane sshGrid = formGrid();
        addRow(sshGrid, 0, "Host / IP", sshHostField);
        addRow(sshGrid, 1, "Port", sshPortField);
        addRow(sshGrid, 2, "Username", sshUserField);
        addRow(sshGrid, 3, "Password", sshPasswordField);
        sshCard.getChildren().addAll(sshTitle, sshGrid);

        // -- DB card --
        VBox dbCard = card();
        Label dbTitle = cardTitle("MySQL Database", "inside the server");
        GridPane dbGrid = formGrid();
        addRow(dbGrid, 0, "DB Host", dbHostField);
        addRow(dbGrid, 1, "DB Port", dbPortField);
        addRow(dbGrid, 2, "DB Username", dbUserField);
        addRow(dbGrid, 3, "DB Password", dbPasswordField);
        addRow(dbGrid, 4, "Database Name", dbNameField);
        dbCard.getChildren().addAll(dbTitle, dbGrid);

        HBox cardsRow = new HBox(16, sshCard, dbCard);
        HBox.setHgrow(sshCard, Priority.ALWAYS);
        HBox.setHgrow(dbCard, Priority.ALWAYS);

        // -- Diagram preview (idle pre-connect, lights up live during connecting) --
        formDiagram.setStage(0);
        HBox diagramWrap = new HBox(formDiagram);
        diagramWrap.setAlignment(Pos.CENTER);
        diagramWrap.setPadding(new Insets(4, 0, 4, 0));

        // -- Buttons: Connect (secondary) vs Connect & Save (primary) --
        styleSecondaryAccentButton(connectButton);
        stylePrimaryButton(connectAndSaveButton);
        styleSecondaryDangerButton(disconnectButton);
        disconnectButton.setDisable(true);

        buttonRow.getChildren().addAll(connectButton, connectAndSaveButton);
        buttonRow.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(connectButton, Priority.ALWAYS);
        HBox.setHgrow(connectAndSaveButton, Priority.ALWAYS);
        connectButton.setMaxWidth(Double.MAX_VALUE);
        connectAndSaveButton.setMaxWidth(Double.MAX_VALUE);

        statusLabel.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 12px;");

        formView.getChildren().addAll(header, profileCard, cardsRow, diagramWrap, buttonRow, statusLabel);
    }

    // -------------------------------------------------------------------------
    // Dashboard view
    // -------------------------------------------------------------------------

    private void buildDashboardView() {
        connectedTitleLabel.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-size: 18px; -fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        dashboardHeaderRow.setAlignment(Pos.CENTER_LEFT);
        dashboardHeaderRow.getChildren().addAll(connectedTitleLabel, spacer);

        dashboardDiagram.setStage(3);
        HBox diagramWrap = new HBox(dashboardDiagram);
        diagramWrap.setAlignment(Pos.CENTER);
        diagramWrap.setPadding(new Insets(10, 0, 10, 0));

        // -- Stat chips --
        HBox statsRow = new HBox(16,
                statChip("TUNNEL UPTIME", uptimeValueLabel),
                statChip("BRIDGE PORT", bridgePortValueLabel),
                statChip("SETUP TIME", setupTimeValueLabel));
        for (javafx.scene.Node n : statsRow.getChildren()) HBox.setHgrow(n, Priority.ALWAYS);

        // -- Server info card --
        VBox infoCard = card();
        Label infoTitle = cardTitle("Server Info", "live from the connected instance");
        GridPane infoGrid = formGrid();
        addInfoRow(infoGrid, 0, "MySQL Version", versionValueLabel);
        addInfoRow(infoGrid, 1, "Server Uptime", dbUptimeValueLabel);
        addInfoRow(infoGrid, 2, "Active Threads", threadsValueLabel);
        addInfoRow(infoGrid, 3, "Max Connections", maxConnectionsValueLabel);
        addInfoRow(infoGrid, 4, "Open Tables", openTablesValueLabel);
        addInfoRow(infoGrid, 5, "Slow Queries", slowQueriesValueLabel);
        addInfoRow(infoGrid, 6, "Server Charset", charsetValueLabel);
        addInfoRow(infoGrid, 7, "Database Size", dbSizeValueLabel);
        addInfoRow(infoGrid, 8, "Table Count", tableCountValueLabel);
        infoCard.getChildren().addAll(infoTitle, infoGrid);

        dashboardView.getChildren().addAll(dashboardHeaderRow, diagramWrap, statsRow, infoCard, statusLabel);
    }

    // -------------------------------------------------------------------------
    // View switching — always reads identity from ConnectionManager, never
    // from form fields, so a freshly-built panel shows the real connection.
    // -------------------------------------------------------------------------

    public void switchToDashboard() {
        ConnectionManager mgr = ConnectionManager.getInstance();
        getChildren().setAll(dashboardView);
        buttonRow.getChildren().clear();
        dashboardHeaderRow.getChildren().add(disconnectButton);

        String host = mgr.getConnectedHost();
        String db   = mgr.getConnectedDbName();
        connectedTitleLabel.setText("Connected — " + (db == null || db.isBlank() ? "(no db)" : db) + " @ " + host);

        dashboardDiagram.setStage(3);
        bridgePortValueLabel.setText(String.valueOf(mgr.getLocalBridgePort()));
        long setupMs = mgr.getSetupMillis();
        setupTimeValueLabel.setText(setupMs >= 0 ? setupMs + " ms" : "—");

        startUptimeTimeline();
        fetchServerStatsAsync(db);
    }

    public void switchToForm() {
        stopUptimeTimeline();
        getChildren().setAll(formView);
        dashboardHeaderRow.getChildren().remove(disconnectButton);
        if (!buttonRow.getChildren().contains(connectButton)) {
            buttonRow.getChildren().addAll(connectButton, connectAndSaveButton);
        }
        formDiagram.setStage(0);
        versionValueLabel.setText("Loading…");
        dbUptimeValueLabel.setText("Loading…");
        threadsValueLabel.setText("Loading…");
        maxConnectionsValueLabel.setText("Loading…");
        openTablesValueLabel.setText("Loading…");
        slowQueriesValueLabel.setText("Loading…");
        charsetValueLabel.setText("Loading…");
        dbSizeValueLabel.setText("Loading…");
        tableCountValueLabel.setText("Loading…");
    }

    private void startUptimeTimeline() {
        stopUptimeTimeline();
        uptimeTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            long secs = ConnectionManager.getInstance().getUptimeSeconds();
            uptimeValueLabel.setText(formatDuration(secs));
        }));
        uptimeTimeline.setCycleCount(Timeline.INDEFINITE);
        uptimeTimeline.play();
    }

    private void stopUptimeTimeline() {
        if (uptimeTimeline != null) {
            uptimeTimeline.stop();
            uptimeTimeline = null;
        }
    }

    private void fetchServerStatsAsync(String dbName) {
        new Thread(() -> {
            ConnectionManager.ServerStats stats = ConnectionManager.getInstance().fetchServerStats(dbName);
            Platform.runLater(() -> {
                versionValueLabel.setText(stats.version);
                dbUptimeValueLabel.setText(stats.dbUptime);
                threadsValueLabel.setText(stats.threads);
                maxConnectionsValueLabel.setText(stats.maxConnections);
                openTablesValueLabel.setText(stats.openTables);
                slowQueriesValueLabel.setText(stats.slowQueries);
                charsetValueLabel.setText(stats.charset);
                dbSizeValueLabel.setText(stats.dbSizeMb);
                tableCountValueLabel.setText(stats.tableCount);
            });
        }).start();
    }

    private String formatDuration(long seconds) {
        long h = seconds / 3600;
        long m = (seconds % 3600) / 60;
        long s = seconds % 60;
        if (h > 0) return h + "h " + m + "m " + s + "s";
        if (m > 0) return m + "m " + s + "s";
        return s + "s";
    }

    // -------------------------------------------------------------------------
    // Profiles
    // -------------------------------------------------------------------------

    private void refreshProfileList() {
        String current = profileSelector.getValue();
        profileSelector.getItems().setAll(ConnectionProfileStore.listProfileNames());
        if (current != null && profileSelector.getItems().contains(current)) {
            profileSelector.setValue(current);
        }
    }

    private void loadSelectedProfile() {
        String name = profileSelector.getValue();
        if (name == null) return;
        ConnectionProfileStore.Profile p = ConnectionProfileStore.load(name);
        if (p == null) return;

        sshHostField.setText(p.sshHost);
        sshPortField.setText(p.sshPort);
        sshUserField.setText(p.sshUser);
        sshPasswordField.setText(p.sshPass);
        dbHostField.setText(p.dbHost);
        dbPortField.setText(p.dbPort);
        dbUserField.setText(p.dbUser);
        dbPasswordField.setText(p.dbPass);
        dbNameField.setText(p.dbName);
        profileNameField.setText(p.name);
    }

    /**
     * Saves the current form values under the typed profile name.
     * Called by the controller when "Connect & Save" is pressed.
     * Returns false (and saves nothing) if no name was typed.
     */
    public boolean saveProfileFromForm() {
        String name = profileNameField.getText() == null ? "" : profileNameField.getText().trim();
        if (name.isEmpty()) return false;

        ConnectionProfileStore.Profile p = new ConnectionProfileStore.Profile();
        p.name    = name;
        p.sshHost = sshHostField.getText().trim();
        p.sshPort = sshPortField.getText().trim();
        p.sshUser = sshUserField.getText().trim();
        p.sshPass = sshPasswordField.getText();
        p.dbHost  = dbHostField.getText().trim();
        p.dbPort  = dbPortField.getText().trim();
        p.dbUser  = dbUserField.getText().trim();
        p.dbPass  = dbPasswordField.getText();
        p.dbName  = dbNameField.getText().trim();

        ConnectionProfileStore.save(p);
        refreshProfileList();
        profileSelector.setValue(p.name);
        return true;
    }

    private void deleteSelectedProfile() {
        String name = profileSelector.getValue();
        if (name == null) return;
        ConnectionProfileStore.delete(name);
        profileSelector.setValue(null);
        profileNameField.clear();
        refreshProfileList();
    }

    // -------------------------------------------------------------------------
    // Diagram control (called by ConnectionController)
    // -------------------------------------------------------------------------

    public void setDiagramStage(int stage) {
        formDiagram.setStage(stage);
        dashboardDiagram.setStage(stage);
    }

    // -------------------------------------------------------------------------
    // Status
    // -------------------------------------------------------------------------

    public void setStatus(String message, String colour, boolean bold) {
        String weight = bold ? "-fx-font-weight: bold;" : "";
        statusLabel.setStyle("-fx-text-fill: " + colour + "; -fx-font-size: 12px; " + weight);
        statusLabel.setText("Status: " + message);
    }

    // -------------------------------------------------------------------------
    // Getters used by ConnectionController
    // -------------------------------------------------------------------------

    public String getSshHost()     { return sshHostField.getText().trim(); }
    public int    getSshPort()     { return parsePort(sshPortField, 22); }
    public String getSshUser()     { return sshUserField.getText().trim(); }
    public String getSshPassword() { return sshPasswordField.getText(); }

    public String getDbHost()     { return dbHostField.getText().trim(); }
    public int    getDbPort()     { return parsePort(dbPortField, 3306); }
    public String getDbUser()     { return dbUserField.getText().trim(); }
    public String getDbPassword() { return dbPasswordField.getText(); }
    public String getDbName()     { return dbNameField.getText().trim(); }

    public Button getConnectButton()         { return connectButton; }
    public Button getConnectAndSaveButton()  { return connectAndSaveButton; }
    public Button getDisconnectButton()      { return disconnectButton; }

    private int parsePort(TextField field, int fallback) {
        try { return Integer.parseInt(field.getText().trim()); }
        catch (NumberFormatException e) { return fallback; }
    }

    // -------------------------------------------------------------------------
    // Styling helpers
    // -------------------------------------------------------------------------

    private void styleField(TextField field) {
        field.setStyle(
                "-fx-background-color: " + FIELD_BG + ";" +
                        "-fx-text-fill: " + TEXT + ";" +
                        "-fx-prompt-text-fill: #9AA3B2;" +
                        "-fx-border-color: " + BORDER + ";" +
                        "-fx-border-radius: 6;" +
                        "-fx-background-radius: 6;" +
                        "-fx-highlight-fill: " + ACCENT + ";" +
                        "-fx-padding: 8 10 8 10;"
        );
    }

    private void styleComboBox(ComboBox<?> box) {
        box.setStyle(
                "-fx-background-color: " + FIELD_BG + ";" +
                        "-fx-text-fill: " + TEXT + ";" +
                        "-fx-border-color: " + BORDER + ";" +
                        "-fx-border-radius: 6;" +
                        "-fx-background-radius: 6;"
        );
    }

    private VBox card() {
        VBox box = new VBox(14);
        box.setPadding(new Insets(18));
        box.setStyle(
                "-fx-background-color: " + CARD + ";" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: " + BORDER + ";" +
                        "-fx-border-radius: 12;" +
                        "-fx-effect: " + SHADOW + ";"
        );
        return box;
    }

    private Label cardTitle(String title, String subtitle) {
        Label t = new Label(title);
        t.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-weight: bold; -fx-font-size: 13px;");
        Label s = new Label("  " + subtitle);
        s.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 11px;");
        HBox row = new HBox(t, s);
        row.setAlignment(Pos.BASELINE_LEFT);
        Label combined = new Label();
        combined.setGraphic(row);
        return combined;
    }

    private Label fieldLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 12px;");
        return l;
    }

    private GridPane formGrid() {
        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        return grid;
    }

    private void addRow(GridPane grid, int row, String labelText, javafx.scene.control.Control field) {
        Label label = fieldLabel(labelText);
        grid.add(label, 0, row);
        grid.add(field, 1, row);
        GridPane.setHgrow(field, Priority.ALWAYS);
        field.setMaxWidth(Double.MAX_VALUE);
    }

    private void addInfoRow(GridPane grid, int row, String labelText, Label valueLabel) {
        Label label = fieldLabel(labelText);
        valueLabel.setStyle("-fx-text-fill: " + TEXT + "; -fx-font-size: 12px; -fx-font-weight: bold;");
        grid.add(label, 0, row);
        grid.add(valueLabel, 1, row);
    }

    private VBox statChip(String label, Label valueLabel) {
        Label l = new Label(label);
        l.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 10px; -fx-font-weight: bold;");
        valueLabel.setStyle("-fx-text-fill: " + ACCENT + "; -fx-font-size: 20px; -fx-font-weight: bold;");
        VBox box = new VBox(6, l, valueLabel);
        box.setPadding(new Insets(14, 18, 14, 18));
        box.setStyle(
                "-fx-background-color: " + CARD + ";" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: " + BORDER + ";" +
                        "-fx-border-radius: 12;" +
                        "-fx-effect: " + SHADOW + ";"
        );
        return box;
    }

    private void stylePrimaryButton(Button button) {
        button.setStyle(
                "-fx-background-color: " + ACCENT + ";" +
                        "-fx-text-fill: #FFFFFF;" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 13px;" +
                        "-fx-background-radius: 8;" +
                        "-fx-padding: 12 18 12 18;" +
                        "-fx-cursor: hand;"
        );
    }

    private void styleSecondaryAccentButton(Button button) {
        button.setStyle(
                "-fx-background-color: " + ACCENT_BG + ";" +
                        "-fx-text-fill: " + ACCENT + ";" +
                        "-fx-font-weight: bold;" +
                        "-fx-font-size: 13px;" +
                        "-fx-background-radius: 8;" +
                        "-fx-padding: 12 18 12 18;" +
                        "-fx-cursor: hand;"
        );
    }

    private void styleSecondaryDangerButton(Button button) {
        button.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-text-fill: " + RED + ";" +
                        "-fx-border-color: " + RED + ";" +
                        "-fx-border-radius: 8;" +
                        "-fx-background-radius: 8;" +
                        "-fx-padding: 10 18 10 18;" +
                        "-fx-cursor: hand;"
        );
    }

    private void stylePillButton(Button button, String bg, String border, String text) {
        button.setStyle(
                "-fx-background-color: " + bg + ";" +
                        "-fx-text-fill: " + text + ";" +
                        "-fx-border-color: " + border + ";" +
                        "-fx-border-radius: 6;" +
                        "-fx-background-radius: 6;" +
                        "-fx-padding: 7 14 7 14;" +
                        "-fx-font-size: 11px;" +
                        "-fx-cursor: hand;"
        );
    }

    // -------------------------------------------------------------------------
    // Tunnel diagram: Laptop -> SSH Server -> MySQL, lights up as stages complete.
    // Connector lines have a FIXED height so they never stretch into the labels.
    // -------------------------------------------------------------------------

    private static class TunnelDiagram extends HBox {
        private final Circle laptopDot = dot();
        private final Circle sshDot    = dot();
        private final Circle dbDot     = dot();
        private final Region line1     = connector();
        private final Region line2     = connector();

        TunnelDiagram() {
            setAlignment(Pos.CENTER);
            setSpacing(10);
            getChildren().addAll(
                    node(laptopDot, "Your Machine"),
                    line1,
                    node(sshDot, "SSH Server"),
                    line2,
                    node(dbDot, "MySQL")
            );
        }

        private static Circle dot() {
            Circle c = new Circle(7);
            c.setFill(Color.web(LINE_OFF));
            return c;
        }

        private static Region connector() {
            Region r = new Region();
            r.setPrefHeight(2);
            r.setMinHeight(2);
            r.setMaxHeight(2);
            r.setPrefWidth(70);
            r.setMinWidth(70);
            r.setMaxWidth(70);
            r.setStyle("-fx-background-color: " + LINE_OFF + ";");
            return r;
        }

        private static VBox node(Circle dot, String label) {
            Label l = new Label(label);
            l.setStyle("-fx-text-fill: " + MUTED + "; -fx-font-size: 11px;");
            VBox box = new VBox(6, dot, l);
            box.setAlignment(Pos.CENTER);
            box.setMinWidth(90);
            return box;
        }

        /** stage: 0 = idle, 1 = ssh connected, 2 = port bound, 3 = jdbc connected (fully live). */
        void setStage(int stage) {
            laptopDot.setFill(Color.web(ACCENT));
            sshDot.setFill(Color.web(stage >= 1 ? GREEN : LINE_OFF));
            dbDot.setFill(Color.web(stage >= 3 ? GREEN : LINE_OFF));
            line1.setStyle("-fx-background-color: " + (stage >= 1 ? GREEN : LINE_OFF) + ";");
            line2.setStyle("-fx-background-color: " + (stage >= 2 ? GREEN : LINE_OFF) + ";");
        }
    }
}