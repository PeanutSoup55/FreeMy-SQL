package globalfuncs;

import java.sql.*;
import java.util.*;
import Objects.*;


public class db {
    private static final String USER = creds.getUser();
    private static final String URL = creds.getUrl();
    private static final String PASS = creds.getPass();

    public static Connection Connect() throws SQLException{
        return DriverManager.getConnection(URL, USER, PASS);
    }

    public static List<Schema> Schemas(){
        List<Schema> databases = new ArrayList<>();
        String query = "SHOW DATABASES WHERE `Database` NOT IN ('information_schema','mysql','performance_schema','sys')";

        try(Connection conn = Connect(); Statement stmt = conn.createStatement(); ResultSet rslt = stmt.executeQuery(query)){
            while (rslt.next()){
                String name = rslt.getString(1);
                databases.add(new Schema(name));
            }
        }catch (SQLException e){
            for (StackTraceElement el : e.getStackTrace()){
                System.err.println(el);
            }
        }
        return databases;
    }

    public static Schema GetTablesInSchema(String schemaName) {
        Schema schema = new Schema(schemaName);

        String tablesQuery =
                "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = ?";

        // Join KEY_COLUMN_USAGE so FK reference lands on the Field in one pass
        String colQuery =
                "SELECT c.COLUMN_NAME, c.DATA_TYPE, c.COLUMN_KEY, " +
                        "       k.REFERENCED_TABLE_NAME, k.REFERENCED_COLUMN_NAME " +
                        "FROM INFORMATION_SCHEMA.COLUMNS c " +
                        "LEFT JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE k " +
                        "       ON k.TABLE_SCHEMA = c.TABLE_SCHEMA " +
                        "      AND k.TABLE_NAME   = c.TABLE_NAME " +
                        "      AND k.COLUMN_NAME  = c.COLUMN_NAME " +
                        "      AND k.REFERENCED_TABLE_NAME IS NOT NULL " +
                        "WHERE c.TABLE_SCHEMA = ? AND c.TABLE_NAME = ? " +
                        "ORDER BY c.ORDINAL_POSITION";

        try (Connection conn = Connect();
             PreparedStatement tablePs = conn.prepareStatement(tablesQuery);
             PreparedStatement colPs   = conn.prepareStatement(colQuery)) {

            tablePs.setString(1, schemaName);
            ResultSet tables = tablePs.executeQuery();

            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");
                Table table = new Table(tableName);

                colPs.setString(1, schemaName);
                colPs.setString(2, tableName);
                ResultSet cols = colPs.executeQuery();

                while (cols.next()) {
                    boolean isPrimary = "PRI".equals(cols.getString("COLUMN_KEY"));
                    String  refTable  = cols.getString("REFERENCED_TABLE_NAME");
                    String  refCol    = cols.getString("REFERENCED_COLUMN_NAME");
                    String  reference = (refTable != null) ? refTable + "(" + refCol + ")" : null;

                    table.addField(new Field(
                            reference,
                            isPrimary,
                            cols.getString("DATA_TYPE"),
                            cols.getString("COLUMN_NAME")
                    ));
                }
                cols.close();
                colPs.clearParameters();
                schema.addTable(table);
            }

        } catch (SQLException e) {
            for (StackTraceElement el : e.getStackTrace()) System.err.println(el);
        }
        return schema;
    }

    public static List<String[]> GetForeignKeys(String schemaName) {
        List<String[]> fks = new ArrayList<>();
        String query =
                "SELECT TABLE_NAME, COLUMN_NAME, REFERENCED_TABLE_NAME, REFERENCED_COLUMN_NAME " +
                        "FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE " +
                        "WHERE TABLE_SCHEMA = ? AND REFERENCED_TABLE_NAME IS NOT NULL";

        try (Connection conn = Connect();
             PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, schemaName);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                fks.add(new String[]{
                        rs.getString("TABLE_NAME"),
                        rs.getString("COLUMN_NAME"),
                        rs.getString("REFERENCED_TABLE_NAME"),
                        rs.getString("REFERENCED_COLUMN_NAME")
                });
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return fks;
    }

    public static String getTableConnections(String schema, String table) {
        StringBuilder report = new StringBuilder();
        String query = "SELECT TABLE_NAME, COLUMN_NAME, REFERENCED_TABLE_NAME, REFERENCED_COLUMN_NAME " +
                "FROM information_schema.KEY_COLUMN_USAGE " +
                "WHERE CONSTRAINT_SCHEMA = ? AND (TABLE_NAME = ? OR REFERENCED_TABLE_NAME = ?)";

        try (Connection conn = Connect(); PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setString(1, schema);
            pstmt.setString(2, table);
            pstmt.setString(3, table);
            ResultSet rs = pstmt.executeQuery();
            List<String> incoming = new ArrayList<>();
            List<String> outgoing = new ArrayList<>();
            while (rs.next()) {
                String tbl = rs.getString("TABLE_NAME");
                String refTbl = rs.getString("REFERENCED_TABLE_NAME");
                if (refTbl == null) continue;

                if (tbl.equals(table)) {
                    outgoing.add(" -> " + refTbl + " (" + rs.getString("REFERENCED_COLUMN_NAME") + ")");
                } else {
                    incoming.add(" <- " + tbl + " (" + rs.getString("COLUMN_NAME") + ")");
                }
            }
            if (!incoming.isEmpty()) {
                report.append("Referenced by (will break):\n").append(String.join("\n", incoming)).append("\n\n");
            }
            if (!outgoing.isEmpty()) {
                report.append("References (depends on):\n").append(String.join("\n", outgoing));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return report.toString();
    }


    public static void MakeSchema(Schema schema) {
        try (Connection conn = Connect(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS `" + schema.getName() + "`");
            stmt.execute("USE `" + schema.getName() + "`");
            stmt.execute("SET FOREIGN_KEY_CHECKS = 0");
            for (Table table : schema.getTables()) {
                StringBuilder query = new StringBuilder("CREATE TABLE IF NOT EXISTS `" + schema.getName() + "`.`" + table.getName() + "` (");
                List<String> fkClauses = new ArrayList<>();
                for (Field field : table.getFields()) {
                    query.append("`").append(field.getName()).append("` ")
                            .append(field.getType());
                    if (field.isPrimary())
                        query.append(" PRIMARY KEY AUTO_INCREMENT");
                    query.append(", ");

                    if (field.getReference() != null && !field.getReference().isEmpty()) {
                        int paren     = field.getReference().indexOf('(');
                        String refTbl = field.getReference().substring(0, paren);
                        String refCol = field.getReference().substring(paren + 1, field.getReference().length() - 1);
                        fkClauses.add("FOREIGN KEY (`" + field.getName() + "`) " + "REFERENCES `" + schema.getName() + "`.`" + refTbl + "`(`" + refCol + "`)"
                        );
                    }
                }

                for (String fk : fkClauses) query.append(fk).append(", ");
                query.setLength(query.length() - 2);
                query.append(")");
                System.out.println("[SQL] " + query);
                stmt.executeUpdate(query.toString());
            }
            stmt.execute("SET FOREIGN_KEY_CHECKS = 1");

        } catch (SQLException e) {
            System.err.println("[SQL ERROR] " + e.getMessage());
            for (StackTraceElement el : e.getStackTrace()) System.err.println(el);
        }
    }
    public static void deleteSchema(Schema schema){
        String query = "DROP DATABASE IF EXISTS " + schema.getName();
        try (Connection conn = Connect(); Statement statement = conn.createStatement()){
            statement.executeUpdate(query);
        } catch (SQLException e) {
            System.err.println("[SQL ERROR] " + e.getMessage());
            for (StackTraceElement el : e.getStackTrace()) System.err.println(el);
        }
    }
    public static void deleteTable(Schema schema, Table table) {
        String disableChecks = "SET FOREIGN_KEY_CHECKS = 0";
        String dropTable = String.format("DROP TABLE IF EXISTS `%s`.`%s`", schema.getName(), table.getName());
        String enableChecks = "SET FOREIGN_KEY_CHECKS = 1";

        try (Connection conn = Connect(); Statement stmt = conn.createStatement()) {
            stmt.execute(disableChecks);
            stmt.executeUpdate(dropTable);
            stmt.execute(enableChecks);
            System.out.println("Successfully dropped: " + table.getName());
        } catch (SQLException e) {
            System.err.println("Failed to delete table: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void EnableLogging() {
        try (Connection conn = Connect(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("SET GLOBAL log_output = 'TABLE'");
            stmt.executeUpdate("SET GLOBAL general_log = 'ON'");
        } catch (SQLException e) {
            System.err.println("[SQL ERROR] " + e.getMessage());
        }
    }

    public static List<String[]> GetLogs(int limit, String filterType) {
        List<String[]> rows = new ArrayList<>();

        StringBuilder q = new StringBuilder(
                "SELECT event_time, user_host, command_type, argument " +
                        "FROM mysql.general_log " +
                        "WHERE argument NOT LIKE '%general_log%' " +
                        "AND argument NOT LIKE '%@@session%' " +
                        "AND argument NOT LIKE 'SET autocommit%' " +
                        "AND argument NOT LIKE 'SET character_set_results%' "
        );

        if (filterType != null && !filterType.equals("ALL")) {
            q.append("AND command_type = ? ");
        }
        q.append("ORDER BY event_time DESC LIMIT ?");

        try (Connection conn = Connect();
             PreparedStatement ps = conn.prepareStatement(q.toString())) {

            if (filterType != null && !filterType.equals("ALL")) {
                ps.setString(1, filterType);
                ps.setInt(2, limit);
            } else {
                ps.setInt(1, limit);
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                rows.add(new String[]{
                        rs.getString("event_time"),
                        rs.getString("user_host"),
                        rs.getString("command_type"),
                        rs.getString("argument")
                });
            }
        } catch (SQLException e) {
            System.err.println("[SQL ERROR] " + e.getMessage());
        }
        return rows;
    }
}
