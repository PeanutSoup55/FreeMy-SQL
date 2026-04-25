package globalfuncs;

import java.sql.*;
import java.util.ArrayList;
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

    public static boolean UpdateRow(String schemaName, String tableName,
                                    Map<String, String> values, String pkCol, String pkVal) {
        String sets  = values.keySet().stream().map(k -> "`" + k + "` = ?")
                .collect(java.util.stream.Collectors.joining(", "));
        String query = "UPDATE `" + schemaName + "`.`" + tableName + "` SET " + sets +
                " WHERE `" + pkCol + "` = ?";
        try (Connection conn = Connect();
             PreparedStatement ps = conn.prepareStatement(query)) {
            int i = 1;
            for (String val : values.values()) ps.setString(i++, val);
            ps.setString(i, pkVal);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("[SQL ERROR] UpdateRow — " + e.getMessage()); return false;
        }
    }

}
