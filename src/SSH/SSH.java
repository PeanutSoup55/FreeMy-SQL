package SSH;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class SSH {

    private Session sshSession;
    private Connection dbConnection;

    /**
     * Opens the SSH tunnel then bridges a JDBC connection through it.
     * Tries local port 3309 first; falls back to 3310 if already bound
     * (e.g. a leftover tunnel from a previous session).
     */
    public Connection connect(
            String sshHost, int sshPort, String sshUser, String sshPassword,
            String dbHost, int dbPort, String dbUser, String dbPassword, String dbName
    ) throws Exception {

        // Always close stale sessions before opening a new one
        closeAll();

        JSch jsch = new JSch();
        sshSession = jsch.getSession(sshUser, sshHost, sshPort);
        sshSession.setPassword(sshPassword);

        java.util.Properties config = new java.util.Properties();
        config.put("StrictHostKeyChecking", "no");
        sshSession.setConfig(config);

        sshSession.connect(10_000); // 10-second handshake timeout

        // Try 3309 → 3310 → 3311 to survive port conflicts
        int localBridgePort = bindLocalPort(dbHost, dbPort);

        String jdbcUrl = "jdbc:mysql://127.0.0.1:" + localBridgePort
                + "/" + dbName
                + "?useSSL=false&allowPublicKeyRetrieval=true&connectTimeout=8000";

        dbConnection = DriverManager.getConnection(jdbcUrl, dbUser, dbPassword);
        return dbConnection;
    }

    /** Attempts to bind a local forwarding port, trying up to 3 candidates. */
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
    }
}