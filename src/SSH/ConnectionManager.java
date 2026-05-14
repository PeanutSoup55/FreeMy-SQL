package SSH;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Singleton that holds the active SSH-tunneled database connection.
 * Any view (Schemas, Query, Logs, etc.) reads from here instead of
 * managing their own connection.
 */
public class ConnectionManager {

    private static ConnectionManager instance;

    private Connection activeConnection;
    private SSH activeService;

    private ConnectionManager() {}

    public static ConnectionManager getInstance() {
        if (instance == null) {
            instance = new ConnectionManager();
        }
        return instance;
    }

    /** Called by ConnectionController after a successful tunnel + JDBC handshake. */
    public void attach(Connection connection, SSH service) {
        this.activeConnection = connection;
        this.activeService = service;
    }

    public Connection getConnection() {
        return activeConnection;
    }

    /** Returns true only when a live, open connection exists. */
    public boolean isAlive() {
        try {
            return activeConnection != null && !activeConnection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Cleanly tears down the tunnel and JDBC bridge.
     * Call this on app exit or when the user disconnects.
     */
    public void teardown() {
        if (activeService != null) {
            activeService.closeAll();
        }
        activeConnection = null;
        activeService = null;
    }
}
