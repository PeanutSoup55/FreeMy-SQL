package SSH;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class SSH {

    /** Lets callers (e.g. the tunnel diagram) know which stage just completed. */
    public interface ProgressListener {
        int STAGE_SSH_CONNECTED  = 1;
        int STAGE_PORT_BOUND     = 2;
        int STAGE_JDBC_CONNECTED = 3;
        void onStage(int stage);
    }

    private Session sshSession;
    private Connection dbConnection;
    private int localBridgePort = -1;

    public Connection connect(
            String sshHost, int sshPort, String sshUser, String sshPassword,
            String dbHost, int dbPort, String dbUser, String dbPassword, String dbName
    ) throws Exception {
        return connect(sshHost, sshPort, sshUser, sshPassword, dbHost, dbPort, dbUser, dbPassword, dbName, stage -> {});
    }

    /**
     * Opens the SSH tunnel then bridges a JDBC connection through it.
     * Tries local port 3309 first; falls back to 3310/3311 if already bound.
     * Reports progress via listener so the UI can show exactly where things
     * are at (or where they died, on failure).
     */
    public Connection connect(
            String sshHost, int sshPort, String sshUser, String sshPassword,
            String dbHost, int dbPort, String dbUser, String dbPassword, String dbName,
            ProgressListener listener
    ) throws Exception {

        closeAll();

        JSch jsch = new JSch();
        sshSession = jsch.getSession(sshUser, sshHost, sshPort);
        sshSession.setPassword(sshPassword);

        java.util.Properties config = new java.util.Properties();
        config.put("StrictHostKeyChecking", "no");
        sshSession.setConfig(config);

        sshSession.connect(10_000);
        listener.onStage(ProgressListener.STAGE_SSH_CONNECTED);

        localBridgePort = bindLocalPort(dbHost, dbPort);
        listener.onStage(ProgressListener.STAGE_PORT_BOUND);

        String jdbcUrl = "jdbc:mysql://127.0.0.1:" + localBridgePort
                + "/" + dbName
                + "?useSSL=false&allowPublicKeyRetrieval=true&connectTimeout=8000";

        dbConnection = DriverManager.getConnection(jdbcUrl, dbUser, dbPassword);
        listener.onStage(ProgressListener.STAGE_JDBC_CONNECTED);

        return dbConnection;
    }

    private int bindLocalPort(String dbHost, int dbPort) throws Exception {
        int[] candidates = {3309, 3310, 3311};
        Exception last = null;

        for (int port : candidates) {
            try {
                sshSession.setPortForwardingL(port, dbHost, dbPort);
                return port;
            } catch (Exception e) {
                last = e;
            }
        }
        throw new Exception("All local bridge ports (3309-3311) are occupied: " + last.getMessage());
    }

    public int getLocalBridgePort() {
        return localBridgePort;
    }

    public void closeAll() {
        try {
            if (dbConnection != null && !dbConnection.isClosed()) {
                dbConnection.close();
            }
        } catch (SQLException ignored) {}

        if (sshSession != null && sshSession.isConnected()) {
            sshSession.disconnect();
        }

        dbConnection = null;
        sshSession = null;
        localBridgePort = -1;
    }
}