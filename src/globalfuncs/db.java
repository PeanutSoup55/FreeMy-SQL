package globalfuncs;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class db {
    private static final String USER = creds.getUser();
    private static final String URL = creds.getUrl();
    private static final String PASS = creds.getPass();

    public static Connection Connect() throws SQLException{
        return DriverManager.getConnection(URL, USER, PASS);
    }

    public List<String> Schemas(){
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
