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

    public static Map<String, List<String[]>> GetTablesInSchema(String schema){
        Map<String, List<String[]>> tablemap = new LinkedHashMap<>();
        String query = "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = ?";

        try(Connection connection = Connect(); PreparedStatement stmt = connection.prepareStatement(query)){
            stmt.setString(1, schema);
            ResultSet resultSet = stmt.executeQuery();

            while (resultSet.next()){
                String tableName = resultSet.getString("TABLE_NAME");
                List<String[]> columns = new ArrayList<>();
                String colQuery = "SELECT COLUMN_NAME, DATA_TYPE, COLUMN_KEY " +
                        "FROM INFORMATION_SCHEMA.COLUMNS " +
                        "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ? " +
                        "ORDER BY ORDINAL POSITION";
                try(PreparedStatement statement = connection.prepareStatement(colQuery)){
                    statement.setString(1, schema);
                    statement.setString(2, tableName);
                    ResultSet cols = statement.executeQuery();

                    while (cols.next()){
                        String colName = cols.getString("COLUMN_NAME");
                        String dataType = cols.getString("DATA_TYPE");
                        String keyType = cols.getString("COLUMN_KEY");
                        columns.add(new String[]{
                                colName,
                                dataType,
                                keyType
                        });
                    }
                }
            }
        }catch (SQLException e){
            for (StackTraceElement el : e.getStackTrace()){
                System.err.println(el);
            }
        }
        return tablemap;
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
