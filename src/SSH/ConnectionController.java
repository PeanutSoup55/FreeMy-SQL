package SSH;

import globalfuncs.creds;
import javafx.application.Platform;

import java.sql.Connection;

/**
 * Wires the SSHConnection view to the SSH service.
 * onSuccess is a Runnable supplied by the parent layout — it fires
 * after a live connection is stored in ConnectionManager, letting the
 * layout switch to whatever page makes sense (e.g. Schemas).
 */
public class ConnectionController {

    private final SSHConnection view;
    private final SSH sshService;
    private final Runnable onSuccess;

    public ConnectionController(SSHConnection view, SSH sshService, Runnable onSuccess) {
        this.view       = view;
        this.sshService = sshService;
        this.onSuccess  = onSuccess;

        view.getConnectButton().setOnAction(e -> processConnectionRequest());
        view.getDisconnectButton().setOnAction(e -> processDisconnectRequest());

        // Reflect any existing live connection immediately when the panel opens
        refreshStatusFromManager();
    }

    // -------------------------------------------------------------------------
    // Connection
    // -------------------------------------------------------------------------

    private void processConnectionRequest() {
        view.setStatus("Establishing SSH tunnel...", "orange", false);
        view.getConnectButton().setDisable(true);

        String sHost = view.getSshHost();
        int    sPort = view.getSshPort();
        String sUser = view.getSshUser();
        String sPass = view.getSshPassword();

        String dHost = view.getDbHost();
        int    dPort = view.getDbPort();
        String dUser = view.getDbUser();
        String dPass = view.getDbPassword();
        String dName = view.getDbName();

        // Network I/O off the JavaFX thread
        new Thread(() -> {
            try {
                Connection conn = sshService.connect(
                        sHost, sPort, sUser, sPass,
                        dHost, dPort, dUser, dPass, dName
                );

                // Store in the shared singleton so every other view can use it
                ConnectionManager.getInstance().attach(conn, sshService);

                Platform.runLater(() -> {
                    creds.setTunnel(3309, dUser, dPass);
                    view.setStatus("Tunnel secured — connection live.", "green", true);
                    view.getConnectButton().setDisable(true);
                    view.getDisconnectButton().setDisable(false);

                    // Tell the parent layout to navigate (e.g. switch to Schemas)
                    if (onSuccess != null) onSuccess.run();
                });

            } catch (Exception ex) {
                Platform.runLater(() -> {
                    view.setStatus("Failed: " + ex.getMessage(), "red", false);
                    view.getConnectButton().setDisable(false);
                });
            }
        }).start();
    }

    // -------------------------------------------------------------------------
    // Disconnection
    // -------------------------------------------------------------------------

    private void processDisconnectRequest() {
        ConnectionManager.getInstance().teardown();
        creds.clearTunnel();
        view.setStatus("Disconnected.", "#555", false);
        view.getConnectButton().setDisable(false);
        view.getDisconnectButton().setDisable(true);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Keeps the status label honest if the user navigates away and comes back. */
    private void refreshStatusFromManager() {
        if (ConnectionManager.getInstance().isAlive()) {
            view.setStatus("Tunnel already active.", "green", true);
            view.getConnectButton().setDisable(true);
            view.getDisconnectButton().setDisable(false);
        } else {
            view.getDisconnectButton().setDisable(true);
        }
    }
}