package globalfuncs;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class dbcrud {
    public static Connection Connect() throws SQLException {
        return DriverManager.getConnection(
                creds.getUrl(),
                creds.getUser(),
                creds.getPass()
        );
    }

    public static List<String> GetColumnValues(String schemaName, String tableName, String columnName){
        List<String> values = new ArrayList<>();
        String query = "SELECT `" + columnName + "` FROM `" + schemaName + "`.`" + tableName + "`";
        try (Connection conn = Connect(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(query)){
            while (rs.next()){
                String val = rs.getString(1);
                if (val != null){
                    values.add(val);
                }
            }
        }catch (SQLException e){
            System.err.println("[SQL ERROR] - GetColumnValues - " + e.getMessage());
        }
        return values;
    }

    public static boolean InsertRow(String schemaName, String tableName, Map<String, String> values){
        String cols  = values.keySet().stream().map(k -> "`" + k + "`").collect(java.util.stream.Collectors.joining(", "));
        String marks = values.keySet().stream().map(k -> "?").collect(java.util.stream.Collectors.joining(", "));
        String query = "INSERT INTO `" + schemaName + "`.`" + tableName + "` (" + cols + ") VALUES (" + marks + ")";
        try (Connection conn = Connect(); PreparedStatement ps = conn.prepareStatement(query)) {
            int i = 1;
            for (String val : values.values()) ps.setString(i++, val);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("[SQL ERROR] InsertRow — " + e.getMessage()); return false;
        }
    }

    public static boolean UpdateRow(String schemaName, String tableName, Map<String, String> values, String pkCol, String pkVal) {
        String sets  = values.keySet().stream().map(k -> "`" + k + "` = ?").collect(java.util.stream.Collectors.joining(", "));
        String query = "UPDATE `" + schemaName + "`.`" + tableName + "` SET " + sets + " WHERE `" + pkCol + "` = ?";
        try (Connection conn = Connect(); PreparedStatement ps = conn.prepareStatement(query)) {
            int i = 1;
            for (String val : values.values()) ps.setString(i++, val);
            ps.setString(i, pkVal);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("[SQL ERROR] UpdateRow — " + e.getMessage()); return false;
        }
    }

    public static boolean DeleteRow(String schemaName, String tableName, String pkCol, String pkVal) {
        String query = "DELETE FROM `" + schemaName + "`.`" + tableName + "` WHERE `" + pkCol + "` = ?";
        try (Connection conn = Connect(); PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, pkVal);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("[SQL ERROR] DeleteRow — " + e.getMessage()); return false;
        }
    }

    public static Map<String, List<String[]>> GetReferencingRows(String schemaName, String pkTable, String pkCol, String pkVal, int maxPerTable) {

        Map<String, List<String[]>> result = new LinkedHashMap<>();

        String fkQuery =
                "SELECT TABLE_NAME, COLUMN_NAME " +
                        "FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE " +
                        "WHERE TABLE_SCHEMA = ? " +
                        "  AND REFERENCED_TABLE_NAME = ? " +
                        "  AND REFERENCED_COLUMN_NAME = ?";

        try (Connection conn = Connect();
             PreparedStatement ps = conn.prepareStatement(fkQuery)) {
            ps.setString(1, schemaName);
            ps.setString(2, pkTable);
            ps.setString(3, pkCol);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String childTable = rs.getString("TABLE_NAME");
                String childCol = rs.getString("COLUMN_NAME");
                String key = childTable + "." + childCol;

                String dataQuery = "SELECT * FROM `" + schemaName + "`.`" + childTable + "` " + "WHERE `" + childCol + "` = ? LIMIT " + maxPerTable;

                try (PreparedStatement dps = conn.prepareStatement(dataQuery)) {
                    dps.setString(1, pkVal);
                    ResultSet drs = dps.executeQuery();
                    ResultSetMetaData meta = drs.getMetaData();
                    int colCount = meta.getColumnCount();
                    List<String[]> rows = new ArrayList<>();
                    String[] headers = new String[colCount];
                    for (int i = 1; i <= colCount; i++) headers[i-1] = meta.getColumnName(i);
                    rows.add(headers);

                    while (drs.next()) {
                        String[] row = new String[colCount];
                        for (int i = 1; i <= colCount; i++) {
                            String v = drs.getString(i);
                            row[i-1] = v != null ? v : "NULL";
                        }
                        rows.add(row);
                    }
                    drs.close();
                    if (rows.size() > 1) result.put(key, rows);
                }
            }
        } catch (SQLException e) {
            System.err.println("[SQL ERROR] GetReferencingRows — " + e.getMessage());
        }
        return result;
    }


    public static boolean DeleteRowCascade(String schemaName, String tableName, String pkCol, String pkVal) {
        String fkQuery = "SELECT TABLE_NAME, COLUMN_NAME " +
                        "FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE " +
                        "WHERE TABLE_SCHEMA = ? AND REFERENCED_TABLE_NAME = ? AND REFERENCED_COLUMN_NAME = ?";

        try (Connection conn = Connect()) {
            conn.setAutoCommit(false);
            try (Statement s = conn.createStatement()) {
                s.execute("SET FOREIGN_KEY_CHECKS = 0");
            }
            try (PreparedStatement ps = conn.prepareStatement(fkQuery)) {
                ps.setString(1, schemaName);
                ps.setString(2, tableName);
                ps.setString(3, pkCol);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    String childTable = rs.getString("TABLE_NAME");
                    String childCol   = rs.getString("COLUMN_NAME");
                    try (PreparedStatement dps = conn.prepareStatement("DELETE FROM `" + schemaName + "`.`" + childTable + "` WHERE `" + childCol + "` = ?")) {
                        dps.setString(1, pkVal);
                        dps.executeUpdate();
                    }
                }
            }
            try (PreparedStatement dps = conn.prepareStatement("DELETE FROM `" + schemaName + "`.`" + tableName + "` WHERE `" + pkCol + "` = ?")) {
                dps.setString(1, pkVal);
                dps.executeUpdate();
            }
            try (Statement s = conn.createStatement()) {
                s.execute("SET FOREIGN_KEY_CHECKS = 1");
            }
            conn.commit();
            return true;
        } catch (SQLException e) {
            System.err.println("[SQL ERROR] DeleteRowCascade — " + e.getMessage());
            return false;
        }
    }

    public static boolean DeleteRowSetNull(String schemaName, String tableName, String pkCol, String pkVal) {
        String fkQuery = "SELECT TABLE_NAME, COLUMN_NAME " +
                        "FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE " +
                        "WHERE TABLE_SCHEMA = ? AND REFERENCED_TABLE_NAME = ? AND REFERENCED_COLUMN_NAME = ?";

        try (Connection conn = Connect()) {
            conn.setAutoCommit(false);
            try (Statement s = conn.createStatement()) {
                s.execute("SET FOREIGN_KEY_CHECKS = 0");
            }
            try (PreparedStatement ps = conn.prepareStatement(fkQuery)) {
                ps.setString(1, schemaName);
                ps.setString(2, tableName);
                ps.setString(3, pkCol);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    String childTable = rs.getString("TABLE_NAME");
                    String childCol   = rs.getString("COLUMN_NAME");
                    try (PreparedStatement ups = conn.prepareStatement("UPDATE `" + schemaName + "`.`" + childTable + "` SET `" + childCol + "` = NULL WHERE `" + childCol + "` = ?")) {
                        ups.setString(1, pkVal);
                        ups.executeUpdate();
                    }
                }
            }
            try (PreparedStatement dps = conn.prepareStatement("DELETE FROM `" + schemaName + "`.`" + tableName + "` WHERE `" + pkCol + "` = ?")) {
                dps.setString(1, pkVal);
                dps.executeUpdate();
            }
            try (Statement s = conn.createStatement()) {
                s.execute("SET FOREIGN_KEY_CHECKS = 1");
            }
            conn.commit();
            return true;
        } catch (SQLException e) {
            System.err.println("[SQL ERROR] DeleteRowSetNull — " + e.getMessage());
            return false;
        }
    }

}
