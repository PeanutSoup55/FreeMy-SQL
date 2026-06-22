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

        refreshStatusFromManager();
    }

    // -------------------------------------------------------------------------
    // Connection
    // -------------------------------------------------------------------------

    private void processConnectionRequest() {
        // Save-and-connect in one click: if a profile name was typed, persist
        // it before doing anything else. Leaving the name blank just connects.
        view.maybeSaveProfileFromForm();

        view.setStatus("Establishing SSH tunnel...", "#B8860B", false);
        view.getConnectButton().setDisable(true);
        view.setDiagramStage(0);

        String sHost = view.getSshHost();
        int    sPort = view.getSshPort();
        String sUser = view.getSshUser();
        String sPass = view.getSshPassword();

        String dHost = view.getDbHost();
        int    dPort = view.getDbPort();
        String dUser = view.getDbUser();
        String dPass = view.getDbPassword();
        String dName = view.getDbName();

        long startedAt = System.currentTimeMillis();

        new Thread(() -> {
            try {
                Connection conn = sshService.connect(
                        sHost, sPort, sUser, sPass,
                        dHost, dPort, dUser, dPass, dName,
                        stage -> Platform.runLater(() -> view.setDiagramStage(stage))
                );

                long setupMs = System.currentTimeMillis() - startedAt;

                ConnectionManager.getInstance().attach(conn, sshService, dHost, dName, setupMs);
                ConnectionProfileStore.addHistory(dHost, dName);

                Platform.runLater(() -> {
                    creds.setTunnel(ConnectionManager.getInstance().getLocalBridgePort(), dUser, dPass);
                    view.setStatus("Tunnel secured — connection live.", "#1E9E5A", true);
                    view.getDisconnectButton().setDisable(false);
                    view.switchToDashboard();

                    if (onSuccess != null) onSuccess.run();
                });

            } catch (Exception ex) {
                Platform.runLater(() -> {
                    view.setStatus("Failed: " + ex.getMessage(), "#D9434B", false);
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
        view.switchToForm();
        view.setStatus("Disconnected.", "#6B7280", false);
        view.getConnectButton().setDisable(false);
        view.getDisconnectButton().setDisable(true);
        view.setDiagramStage(0);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /** Keeps the panel honest if the user navigates away and comes back. */
    private void refreshStatusFromManager() {
        if (ConnectionManager.getInstance().isAlive()) {
            view.setStatus("Tunnel already active.", "#1E9E5A", true);
            view.getConnectButton().setDisable(true);
            view.getDisconnectButton().setDisable(false);
            view.switchToDashboard();
        } else {
            view.getDisconnectButton().setDisable(true);
        }
    }
}