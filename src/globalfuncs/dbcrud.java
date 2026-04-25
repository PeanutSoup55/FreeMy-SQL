package globalfuncs;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

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

}
