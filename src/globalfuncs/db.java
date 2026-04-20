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

    public static List<String> Schemas(){
        List<String> databases = new ArrayList<>();
        String query = "SHOW DATABASES WHERE `Database` NOT IN ('information_schema','mysql','performance_schema','sys')";

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

    public static void MakeSchema(Schema schema) {
        try (Connection conn = Connect(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS `" + schema.getName() + "`");
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

        } catch (SQLException e) {
            System.err.println("[SQL ERROR] " + e.getMessage());
            for (StackTraceElement el : e.getStackTrace()) System.err.println(el);
        }
    }
}
