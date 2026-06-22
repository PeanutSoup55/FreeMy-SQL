package SSH;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;

/**
 * Singleton that holds the active SSH-tunneled database connection.
 * Any view (Schemas, Query, Logs, etc.) reads from here instead of
 * managing their own connection.
 *
 * Also holds onto the identity of the connection itself (host, db name,
 * setup time) so any view that re-displays connection info — including a
 * freshly-built one with blank form fields — shows the real thing instead
 * of guessing from whatever's currently typed into a text field.
 */
public class ConnectionManager {

    private static ConnectionManager instance;

    private Connection activeConnection;
    private SSH activeService;
    private Instant connectedAt;

    private String connectedHost;
    private String connectedDbName;
    private long setupMillis = -1;

    private ConnectionManager() {}

    public static ConnectionManager getInstance() {
        if (instance == null) {
            instance = new ConnectionManager();
        }
        return instance;
    }

    /** Called by ConnectionController after a successful tunnel + JDBC handshake. */
    public void attach(Connection connection, SSH service, String connectedHost, String connectedDbName, long setupMillis) {
        this.activeConnection = connection;
        this.activeService = service;
        this.connectedAt = Instant.now();
        this.connectedHost = connectedHost;
        this.connectedDbName = connectedDbName;
        this.setupMillis = setupMillis;
    }

    public Connection getConnection() {
        return activeConnection;
    }

    public String getConnectedHost()   { return connectedHost; }
    public String getConnectedDbName() { return connectedDbName; }
    public long   getSetupMillis()     { return setupMillis; }

    /** Returns true only when a live, open connection exists. */
    public boolean isAlive() {
        try {
            return activeConnection != null && !activeConnection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    /** Seconds since this tunnel was established. Used by the dashboard's live uptime counter. */
    public long getUptimeSeconds() {
        if (connectedAt == null) return 0;
        return Instant.now().getEpochSecond() - connectedAt.getEpochSecond();
    }

    /** The local forwarded port (3309/3310/3311) the JDBC driver is actually talking to. */
    public int getLocalBridgePort() {
        return activeService != null ? activeService.getLocalBridgePort() : -1;
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
        connectedAt = null;
        connectedHost = null;
        connectedDbName = null;
        setupMillis = -1;
    }

    /** Plain data holder for the dashboard's "Server Info" card. */
    public static class ServerStats {
        public String version        = "—";
        public String dbUptime       = "—";
        public String threads        = "—";
        public String maxConnections = "—";
        public String openTables     = "—";
        public String slowQueries    = "—";
        public String charset        = "—";
        public String dbSizeMb       = "—";
        public String tableCount     = "—";
    }

    /**
     * Runs a handful of cheap SHOW/information_schema queries.
     * Caller is responsible for running this off the JavaFX thread.
     */
    public ServerStats fetchServerStats(String dbName) {
        ServerStats stats = new ServerStats();
        if (!isAlive()) return stats;

        try (Statement st = activeConnection.createStatement();
             ResultSet rs = st.executeQuery("SELECT VERSION()")) {
            if (rs.next()) stats.version = rs.getString(1);
        } catch (SQLException ignored) {}

        try (Statement st = activeConnection.createStatement();
             ResultSet rs = st.executeQuery("SHOW GLOBAL STATUS LIKE 'Uptime'")) {
            if (rs.next()) stats.dbUptime = formatUptime(Long.parseLong(rs.getString(2)));
        } catch (SQLException ignored) {}

        try (Statement st = activeConnection.createStatement();
             ResultSet rs = st.executeQuery("SHOW GLOBAL STATUS LIKE 'Threads_connected'")) {
            if (rs.next()) stats.threads = rs.getString(2);
        } catch (SQLException ignored) {}

        try (Statement st = activeConnection.createStatement();
             ResultSet rs = st.executeQuery("SHOW VARIABLES LIKE 'max_connections'")) {
            if (rs.next()) stats.maxConnections = rs.getString(2);
        } catch (SQLException ignored) {}

        try (Statement st = activeConnection.createStatement();
             ResultSet rs = st.executeQuery("SHOW GLOBAL STATUS LIKE 'Open_tables'")) {
            if (rs.next()) stats.openTables = rs.getString(2);
        } catch (SQLException ignored) {}

        try (Statement st = activeConnection.createStatement();
             ResultSet rs = st.executeQuery("SHOW GLOBAL STATUS LIKE 'Slow_queries'")) {
            if (rs.next()) stats.slowQueries = rs.getString(2);
        } catch (SQLException ignored) {}

        try (Statement st = activeConnection.createStatement();
             ResultSet rs = st.executeQuery("SHOW VARIABLES LIKE 'character_set_server'")) {
            if (rs.next()) stats.charset = rs.getString(2);
        } catch (SQLException ignored) {}

        if (dbName != null && !dbName.isBlank()) {
            String sql = "SELECT ROUND(SUM(data_length + index_length) / 1024 / 1024, 1), COUNT(*) " +
                    "FROM information_schema.tables WHERE table_schema = ?";
            try (PreparedStatement ps = activeConnection.prepareStatement(sql)) {
                ps.setString(1, dbName);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        String size = rs.getString(1);
                        stats.dbSizeMb = (size != null ? size : "0") + " MB";
                        stats.tableCount = rs.getString(2);
                    }
                }
            } catch (SQLException ignored) {}
        }

        return stats;
    }

    private String formatUptime(long seconds) {
        long days  = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long mins  = (seconds % 3600) / 60;
        if (days > 0)  return days + "d " + hours + "h";
        if (hours > 0) return hours + "h " + mins + "m";
        return mins + "m";
    }
}