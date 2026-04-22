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

    public static List<String[]> GetTableData(String schemaName, String tableName, List<String> outColumns) {
        List<String[]> rows = new ArrayList<>();
        String query = "SELECT * FROM `" + schemaName + "`.`" + tableName + "`";

        try (Connection conn = Connect();
             Statement stmt  = conn.createStatement();
             ResultSet rs    = stmt.executeQuery(query)) {

            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();

            for (int i = 1; i <= colCount; i++)
                outColumns.add(meta.getColumnName(i));

            while (rs.next()) {
                String[] row = new String[colCount];
                for (int i = 1; i <= colCount; i++) {
                    String val = rs.getString(i);
                    row[i - 1] = val != null ? val : "NULL";
                }
                rows.add(row);
            }

        } catch (SQLException e) {
            System.err.println("[SQL ERROR] GetTableData — " + e.getMessage());
        }
        return rows;
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

    public static void EditTable(String schemaName, String originalTableName, Table updatedTable) {
        try (Connection conn = Connect()) {
            Statement stmt = conn.createStatement();
            stmt.execute("USE `" + schemaName + "`");
            stmt.execute("SET FOREIGN_KEY_CHECKS = 0");
            String newName = updatedTable.getName();
            if (!originalTableName.equals(newName)) {
                ResultSet refRs = conn.createStatement().executeQuery("SELECT TABLE_NAME, CONSTRAINT_NAME, COLUMN_NAME, REFERENCED_COLUMN_NAME " +
                                "FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE " +
                                "WHERE TABLE_SCHEMA          = '" + schemaName + "' " +
                                "  AND REFERENCED_TABLE_NAME = '" + originalTableName + "'"
                );

                List<String[]> refs = new ArrayList<>();
                while (refRs.next()) refs.add(new String[]{
                        refRs.getString("TABLE_NAME"),
                        refRs.getString("CONSTRAINT_NAME"),
                        refRs.getString("COLUMN_NAME"),
                        refRs.getString("REFERENCED_COLUMN_NAME")
                });
                refRs.close();

                for (String[] ref : refs) {
                    String refTable      = ref[0];
                    String constraintName = ref[1];
                    String fkCol         = ref[2];
                    String referencedCol = ref[3];
                    stmt.executeUpdate("ALTER TABLE `" + schemaName + "`.`" + refTable + "` " + "DROP FOREIGN KEY `" + constraintName + "`");
                    stmt.executeUpdate("ALTER TABLE `" + schemaName + "`.`" + refTable + "` " + "ADD FOREIGN KEY (`" + fkCol + "`) " + "REFERENCES `" + schemaName + "`.`" + newName + "`(`" + referencedCol + "`)");
                    System.out.println("[SQL] Updated external FK: " + refTable + "." + fkCol + " → " + newName + "(" + referencedCol + ")");
                }

                stmt.executeUpdate("RENAME TABLE `" + schemaName + "`.`" + originalTableName + "` " + "TO `" + schemaName + "`.`" + newName + "`");
                System.out.println("[SQL] Renamed: " + originalTableName + " → " + newName);
            }

            ResultSet fkRs = conn.createStatement().executeQuery("SELECT CONSTRAINT_NAME " +
                            "FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE " +
                            "WHERE TABLE_SCHEMA          = '" + schemaName + "' " +
                            "  AND TABLE_NAME            = '" + newName + "' " +
                            "  AND REFERENCED_TABLE_NAME IS NOT NULL"
            );
            List<String> ownConstraints = new ArrayList<>();
            while (fkRs.next()) ownConstraints.add(fkRs.getString("CONSTRAINT_NAME"));
            fkRs.close();

            for (String c : ownConstraints) {
                stmt.executeUpdate("ALTER TABLE `" + schemaName + "`.`" + newName + "` " + "DROP FOREIGN KEY `" + c + "`");
                System.out.println("[SQL] Dropped own FK constraint: " + c);
            }

            Map<String, String> existingCols = new LinkedHashMap<>();
            ResultSet colRs = conn.createStatement().executeQuery("SHOW COLUMNS FROM `" + schemaName + "`.`" + newName + "`");
            while (colRs.next()) {
                existingCols.put(
                        colRs.getString("Field").toLowerCase(),
                        colRs.getString("Type").toUpperCase()
                );
            }
            colRs.close();

            Map<String, Field> newColMap = new LinkedHashMap<>();
            Field pkField = null;
            for (Field f : updatedTable.getFields()) {
                if (f.isPrimary()) { pkField = f; continue; }
                if (f.getName() == null || f.getName().isBlank()) continue;
                if (f.getType() == null || f.getType().isBlank()) continue;
                newColMap.put(f.getName().toLowerCase(), f);
            }
            String pkColName = pkField != null ? pkField.getName().toLowerCase() : "";

            for (String col : existingCols.keySet()) {
                if (col.equals(pkColName)) continue;
                if (!newColMap.containsKey(col)) {
                    String sql = "ALTER TABLE `" + schemaName + "`.`" + newName + "` " + "DROP COLUMN `" + col + "`";
                    System.out.println("[SQL] " + sql);
                    stmt.executeUpdate(sql);
                }
            }

            for (Field f : updatedTable.getFields()) {
                if (f.isPrimary()) continue;
                if (f.getName() == null || f.getName().isBlank()) continue;

                String safeType = f.getType();
                if (safeType == null || safeType.isBlank() ||
                        safeType.equalsIgnoreCase("VARCHAR") ||
                        safeType.equalsIgnoreCase("CHAR") ||
                        safeType.equalsIgnoreCase("DECIMAL")) {
                    System.err.println("[SQL SKIP] Type '" + safeType + "' for column '" + f.getName() + "' is missing required size — skipping.");
                    continue;
                }

                String colKey = f.getName().toLowerCase();
                if (!existingCols.containsKey(colKey)) {
                    String sql = "ALTER TABLE `" + schemaName + "`.`" + newName + "` " + "ADD COLUMN `" + f.getName() + "` " + safeType;
                    System.out.println("[SQL] " + sql);
                    stmt.executeUpdate(sql);
                } else if (!existingCols.get(colKey).equalsIgnoreCase(safeType)) {
                    String sql = "ALTER TABLE `" + schemaName + "`.`" + newName + "` " + "MODIFY COLUMN `" + f.getName() + "` " + safeType;
                    System.out.println("[SQL] " + sql);
                    stmt.executeUpdate(sql);
                }
            }

            for (Field f : updatedTable.getFields()) {
                if (f.isPrimary()) continue;
                if (f.getReference() == null || f.getReference().isBlank()) continue;

                int paren = f.getReference().indexOf('(');
                if (paren < 0) continue;

                String refTbl = f.getReference().substring(0, paren);
                String refCol = f.getReference().substring(paren + 1, f.getReference().length() - 1);

                if (refTbl.isBlank() || refCol.isBlank()) continue;

                String sql = "ALTER TABLE `" + schemaName + "`.`" + newName + "` " +
                        "ADD FOREIGN KEY (`" + f.getName() + "`) " +
                        "REFERENCES `" + schemaName + "`.`" + refTbl + "`(`" + refCol + "`)";
                System.out.println("[SQL] " + sql);
                stmt.executeUpdate(sql);
            }

            stmt.execute("SET FOREIGN_KEY_CHECKS = 1");
            System.out.println("[SQL] Table '" + newName + "' updated successfully.");

        } catch (SQLException e) {
            System.err.println("[SQL ERROR] EditTable — " + e.getMessage());
            for (StackTraceElement el : e.getStackTrace()) System.err.println(el);
        }

    }
}
