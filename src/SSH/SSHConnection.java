package SSH;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * The SSH portal panel. Wires itself up the moment it is placed on screen —
 * no user action is needed to start the service, and the Main entry point
 * is never touched.
 *
 * @param onConnected  Runnable called after a successful connection.
 *                     Pass a lambda from your layout to switch pages:
 *                     new SSHConnection(() -> switchCenterContent("Schemas"))
 */
public class SSHConnection extends VBox {

    // --- SSH Fields ---
    private final TextField    sshHostField     = new TextField("127.0.0.1");
    private final TextField    sshPortField     = new TextField("22");
    private final TextField    sshUserField     = new TextField("");
    private final PasswordField sshPasswordField = new PasswordField();

    // --- DB Fields ---
    private final TextField    dbHostField      = new TextField("127.0.0.1");
    private final TextField    dbPortField      = new TextField("3306");
    private final TextField    dbUserField      = new TextField("root");
    private final PasswordField dbPasswordField  = new PasswordField();
    private final TextField    dbNameField      = new TextField("");

    // --- Controls ---
    private final Button connectButton    = new Button("Establish Secure Connection");
    private final Button disconnectButton = new Button("Disconnect");
    private final Label  statusLabel      = new Label("Status: Disconnected");

    public SSHConnection(Runnable onConnected) {
        super(15);
        setPadding(new Insets(20));

        buildLayout();

        // ✅ Service starts NOW — when the panel is constructed on navigation,
        //    not somewhere in the login/startup flow that could lock the user out.
        new ConnectionController(this, new SSH(), onConnected);
    }

    // -------------------------------------------------------------------------
    // Layout
    // -------------------------------------------------------------------------

    private void buildLayout() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));

        // -- SSH section --
        Label sshTitle = new Label("1. SSH Tunnel  (the server gate)");
        sshTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        grid.add(sshTitle,              0, 0, 2, 1);
        grid.add(new Label("Host / IP:"),  0, 1); grid.add(sshHostField,     1, 1);
        grid.add(new Label("Port:"),       0, 2); grid.add(sshPortField,     1, 2);
        grid.add(new Label("Username:"),   0, 3); grid.add(sshUserField,     1, 3);
        grid.add(new Label("Password:"),   0, 4); grid.add(sshPasswordField, 1, 4);

        // -- DB section --
        Label dbTitle = new Label("2. MySQL Database  (inside the server)");
        dbTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        grid.add(dbTitle,                     0, 5, 2, 1);
        grid.add(new Label("DB Host:"),           0, 6); grid.add(dbHostField,     1, 6);
        grid.add(new Label("DB Port:"),           0, 7); grid.add(dbPortField,     1, 7);
        grid.add(new Label("DB Username:"),       0, 8); grid.add(dbUserField,     1, 8);
        grid.add(new Label("DB Password:"),       0, 9); grid.add(dbPasswordField, 1, 9);
        grid.add(new Label("Database Name:"),     0, 10); grid.add(dbNameField,    1, 10);

        // -- Button row --
        connectButton.setMaxWidth(Double.MAX_VALUE);
        disconnectButton.setMaxWidth(Double.MAX_VALUE);
        disconnectButton.setDisable(true);

        HBox buttonRow = new HBox(10, connectButton, disconnectButton);
        buttonRow.setMaxWidth(Double.MAX_VALUE);
        connectButton.setMaxWidth(Double.MAX_VALUE);
        disconnectButton.setMaxWidth(Double.MAX_VALUE);
        javafx.scene.layout.HBox.setHgrow(connectButton,    javafx.scene.layout.Priority.ALWAYS);
        javafx.scene.layout.HBox.setHgrow(disconnectButton, javafx.scene.layout.Priority.ALWAYS);

        getChildren().addAll(grid, buttonRow, statusLabel);
    }

    // -------------------------------------------------------------------------
    // Controller API
    // -------------------------------------------------------------------------

    public void setStatus(String message, String colour, boolean bold) {
        String weight = bold ? "-fx-font-weight: bold;" : "";
        statusLabel.setStyle("-fx-text-fill: " + colour + "; " + weight);
        statusLabel.setText("Status: " + message);
    }

    // SSH getters
    public String getSshHost()     { return sshHostField.getText().trim(); }
    public int    getSshPort()     { return parsePort(sshPortField, 22); }
    public String getSshUser()     { return sshUserField.getText().trim(); }
    public String getSshPassword() { return sshPasswordField.getText(); }

    // DB getters
    public String getDbHost()     { return dbHostField.getText().trim(); }
    public int    getDbPort()     { return parsePort(dbPortField, 3306); }
    public String getDbUser()     { return dbUserField.getText().trim(); }
    public String getDbPassword() { return dbPasswordField.getText(); }
    public String getDbName()     { return dbNameField.getText().trim(); }

    public Button getConnectButton()    { return connectButton; }
    public Button getDisconnectButton() { return disconnectButton; }

    /** Graceful fallback so a typo in the port field never throws on construction. */
    private int parsePort(TextField field, int fallback) {
        try { return Integer.parseInt(field.getText().trim()); }
        catch (NumberFormatException e) { return fallback; }
    }
}