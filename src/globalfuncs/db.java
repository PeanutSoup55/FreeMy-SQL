package globalfuncs;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class db {
    private static final String USER = creds.getUser();
    private static final String URL = creds.getUrl();
    private static final String PASS = creds.getPass();

    public static Connection Connect() throws SQLException{
        return DriverManager.getConnection(URL, USER, PASS);
    }

    public static List<String> Schemas(){
        List<String> databases = new ArrayList<>();
        String query = "SHOW DATABASES";

        try(Connection conn = Connect(); Statement stmt = conn.createStatement(); ResultSet rslt = stmt.executeQuery(query)){
            while (rslt.next()){
                databases.add(rslt.getString(1));
            }
        }catch (SQLException e){
            for (StackTraceElement el : e.getStackTrace()){
                System.err.println(el);
            }
        }
        return databases;
    }

    public static Map<String, List<String[]>> GetTablesInSchema(String schemaName) {
        Map<String, List<String[]>> tableMap = new LinkedHashMap<>();

        String tablesQuery = "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = ?";
        String colQuery = "SELECT COLUMN_NAME, DATA_TYPE, COLUMN_KEY " +
                "FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? " +
                "ORDER BY ORDINAL_POSITION";

        try (Connection conn = Connect();
             PreparedStatement tablePs = conn.prepareStatement(tablesQuery);
             PreparedStatement colPs = conn.prepareStatement(colQuery)) {

            tablePs.setString(1, schemaName);
            ResultSet tables = tablePs.executeQuery();

            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");
                List<String[]> columns = new ArrayList<>();

                colPs.setString(1, schemaName);
                colPs.setString(2, tableName);
                ResultSet cols = colPs.executeQuery();

                while (cols.next()) {
                    columns.add(new String[]{
                            cols.getString("COLUMN_NAME"),
                            cols.getString("DATA_TYPE"),
                            cols.getString("COLUMN_KEY")
                    });
                }
                cols.close();
                colPs.clearParameters();
                tableMap.put(tableName, columns);
            }

        } catch (SQLException e) {
            for (StackTraceElement el : e.getStackTrace()) {
                System.err.println(el);
            }
        }

        return tableMap;
    }

    public static void MakeSchema(String name){
        String query = "CREATE DATABASE IF NOT EXISTS" + name ;

        try(Connection conn = Connect(); Statement stmt = conn.createStatement()){
            stmt.executeQuery(query);
        }catch (SQLException e){
            for (StackTraceElement el : e.getStackTrace()){
                System.err.println(el);
            }
        }
    }
}
