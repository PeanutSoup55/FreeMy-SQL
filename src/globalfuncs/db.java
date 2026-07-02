package globalfuncs;

import java.sql.*;
import java.util.*;

import Objects.*;


public class db {

    public static Connection Connect() throws SQLException {
        return DriverManager.getConnection(
                creds.getUrl(),
                creds.getUser(),
                creds.getPass()
        );
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
            ResultSet rs = stmt.executeQuery("SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = '" + schema.getName() +"'");
            boolean schemaExists = rs.next();
            rs.close();
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS `" + schema.getName() + "`");
            if (schemaExists){
                return;
            }
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

    public static void renameSchema(String oldName, String newName) {
        try (Connection conn = Connect(); Statement stmt = conn.createStatement()) {
            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS `" + newName + "`");
            stmt.execute("SET FOREIGN_KEY_CHECKS = 0");

            List<String> tableNames = new ArrayList<>();
            ResultSet rs = stmt.executeQuery(
                    "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = '" + oldName + "'");
            while (rs.next()) tableNames.add(rs.getString("TABLE_NAME"));
            rs.close();

            for (String table : tableNames) {
                String sql = "RENAME TABLE `" + oldName + "`.`" + table + "` TO `" + newName + "`.`" + table + "`";
                System.out.println("[SQL] " + sql);
                stmt.executeUpdate(sql);
            }

            stmt.execute("SET FOREIGN_KEY_CHECKS = 1");
            stmt.executeUpdate("DROP DATABASE IF EXISTS `" + oldName + "`");
            System.out.println("[SQL] Renamed schema: " + oldName + " -> " + newName);

        } catch (SQLException e) {
            System.err.println("[SQL ERROR] renameSchema — " + e.getMessage());
            for (StackTraceElement el : e.getStackTrace()) System.err.println(el);
        }
    }

    public static void CreateTable(String schemaName, Table table) {
        try (Connection conn = Connect(); Statement stmt = conn.createStatement()) {
            stmt.execute("USE `" + schemaName + "`");
            stmt.execute("SET FOREIGN_KEY_CHECKS = 0");

            StringBuilder query = new StringBuilder(
                    "CREATE TABLE IF NOT EXISTS `" + schemaName + "`.`" + table.getName() + "` (");
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
                    fkClauses.add("FOREIGN KEY (`" + field.getName() + "`) " + "REFERENCES `" + schemaName + "`.`" + refTbl + "`(`" + refCol + "`)"
                    );
                }
            }

            for (String fk : fkClauses) query.append(fk).append(", ");
            query.setLength(query.length() - 2);
            query.append(")");
            System.out.println("[SQL] " + query);
            stmt.executeUpdate(query.toString());

            stmt.execute("SET FOREIGN_KEY_CHECKS = 1");
            System.out.println("[SQL] Created table: " + table.getName());

        } catch (SQLException e) {
            System.err.println("[SQL ERROR] CreateTable — " + e.getMessage());
            for (StackTraceElement el : e.getStackTrace()) System.err.println(el);
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
                ResultSet refRs = conn.createStatement().executeQuery(
                        "SELECT TABLE_NAME, CONSTRAINT_NAME, COLUMN_NAME, REFERENCED_COLUMN_NAME " +
                                "FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE " +
                                "WHERE TABLE_SCHEMA = '" + schemaName + "' " +
                                "AND REFERENCED_TABLE_NAME = '" + originalTableName + "'"
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
                    stmt.executeUpdate("ALTER TABLE `" + schemaName + "`.`" + ref[0] + "` DROP FOREIGN KEY `" + ref[1] + "`");
                    stmt.executeUpdate("ALTER TABLE `" + schemaName + "`.`" + ref[0] + "` ADD FOREIGN KEY (`" + ref[2] + "`) REFERENCES `" + schemaName + "`.`" + newName + "`(`" + ref[3] + "`)");
                }
                stmt.executeUpdate("RENAME TABLE `" + schemaName + "`.`" + originalTableName + "` TO `" + schemaName + "`.`" + newName + "`");
                System.out.println("[SQL] Renamed: " + originalTableName + " → " + newName);
            }

            ResultSet fkRs = conn.createStatement().executeQuery("SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE " + "WHERE TABLE_SCHEMA = '" + schemaName + "' AND TABLE_NAME = '" + newName + "' AND REFERENCED_TABLE_NAME IS NOT NULL"
            );
            List<String> ownConstraints = new ArrayList<>();
            while (fkRs.next()) ownConstraints.add(fkRs.getString("CONSTRAINT_NAME"));
            fkRs.close();
            for (String c : ownConstraints) {
                stmt.executeUpdate("ALTER TABLE `" + schemaName + "`.`" + newName + "` DROP FOREIGN KEY `" + c + "`");
                System.out.println("[SQL] Dropped FK: " + c);
            }

            Map<String, String> existingCols = new LinkedHashMap<>();
            ResultSet colRs = conn.createStatement().executeQuery("SHOW COLUMNS FROM `" + schemaName + "`.`" + newName + "`");
            while (colRs.next()) existingCols.put(colRs.getString("Field").toLowerCase(), colRs.getString("Type").toUpperCase());
            colRs.close();

            String pkColName = "";
            for (Field f : updatedTable.getFields()) {
                if (f.isPrimary()) { pkColName = f.getName().toLowerCase(); break; }
            }

            Set<String> keptOldNames = new HashSet<>();
            keptOldNames.add(pkColName);
            for (Field f : updatedTable.getFields()) {
                if (f.isPrimary()) continue;
                if (f.getOldName() != null) keptOldNames.add(f.getOldName().toLowerCase());
                if (f.getName()    != null) keptOldNames.add(f.getName().toLowerCase());
            }

            for (String col : new ArrayList<>(existingCols.keySet())) {
                if (keptOldNames.contains(col)) continue;
                String sql = "ALTER TABLE `" + schemaName + "`.`" + newName + "` DROP COLUMN `" + col + "`";
                System.out.println("[SQL] " + sql);
                stmt.executeUpdate(sql);
                existingCols.remove(col);
            }

            for (Field f : updatedTable.getFields()) {
                if (f.isPrimary()) continue;
                if (f.getName() == null || f.getName().isBlank()) continue;

                String safeType  = f.getType();
                if (safeType == null || safeType.isBlank() ||
                        safeType.equalsIgnoreCase("VARCHAR") ||
                        safeType.equalsIgnoreCase("CHAR") ||
                        safeType.equalsIgnoreCase("DECIMAL")) continue;

                String colKey    = f.getName().toLowerCase();
                String oldColKey = f.getOldName() != null ? f.getOldName().toLowerCase() : colKey;

                if (existingCols.containsKey(oldColKey)) {
                    if (!oldColKey.equals(colKey)) {
                        String sql = "ALTER TABLE `" + schemaName + "`.`" + newName + "` RENAME COLUMN `" + f.getOldName() + "` TO `" + f.getName() + "`";
                        System.out.println("[SQL] " + sql);
                        stmt.executeUpdate(sql);
                    }
                    if (!existingCols.get(oldColKey).equalsIgnoreCase(safeType)) {
                        String sql = "ALTER TABLE `" + schemaName + "`.`" + newName + "` MODIFY COLUMN `" + f.getName() + "` " + safeType;
                        System.out.println("[SQL] " + sql);
                        stmt.executeUpdate(sql);
                    }
                } else if (!existingCols.containsKey(colKey)) {
                    String sql = "ALTER TABLE `" + schemaName + "`.`" + newName + "` ADD COLUMN `" + f.getName() + "` " + safeType;
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
                String sql = "ALTER TABLE `" + schemaName + "`.`" + newName + "` ADD FOREIGN KEY (`" + f.getName() + "`) REFERENCES `" + schemaName + "`.`" + refTbl + "`(`" + refCol + "`)";
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

    public static String ExecuteRaw(String sql) {
        if (sql == null || sql.isBlank()) return "ERROR: Query is empty.";

        try (Connection conn = Connect(); Statement stmt = conn.createStatement()) {
            boolean hasResult = stmt.execute(sql);
            StringBuilder sb = new StringBuilder();

            do {
                if (hasResult) {
                    ResultSet rs = stmt.getResultSet();
                    ResultSetMetaData meta = rs.getMetaData();
                    int colCount = meta.getColumnCount();

                    for (int i = 1; i <= colCount; i++)
                        sb.append(String.format("%-20s", meta.getColumnName(i)));
                    sb.append("\n").append("-".repeat(colCount * 20)).append("\n");

                    int rowCount = 0;
                    while (rs.next()) {
                        for (int i = 1; i <= colCount; i++) {
                            String val = rs.getString(i);
                            sb.append(String.format("%-20s", val != null ? val : "NULL"));
                        }
                        sb.append("\n");
                        rowCount++;
                    }
                    sb.append("\n(").append(rowCount).append(" row").append(rowCount == 1 ? "" : "s").append(")\n\n");

                } else {
                    int count = stmt.getUpdateCount();
                    if (count != -1) {
                        sb.append("OK — ").append(count).append(" row").append(count == 1 ? "" : "s").append(" affected.\n");
                    }
                }

                hasResult = stmt.getMoreResults();
            } while (hasResult || stmt.getUpdateCount() != -1);

            return sb.isEmpty() ? "OK — executed successfully." : sb.toString().trim();

        } catch (SQLException e) {
            return "ERROR: " + e.getMessage();
        }
    }

    public static Connection ConnectRemote() throws SQLException {
        return DriverManager.getConnection(
                creds.getRemoteUrl(),
                creds.getRemoteUser(),
                creds.getRemotePass()
        );
    }

    public static List<Schema> SchemasRemote() {
        List<Schema> databases = new ArrayList<>();
        String query = "SHOW DATABASES WHERE `Database` NOT IN ('information_schema','mysql','performance_schema','sys')";
        try (Connection conn = ConnectRemote();
             Statement stmt = conn.createStatement();
             ResultSet rs   = stmt.executeQuery(query)) {
            while (rs.next()) databases.add(new Schema(rs.getString(1)));
        } catch (SQLException e) {
            System.err.println("[REMOTE SQL ERROR] SchemasRemote — " + e.getMessage());
        }
        return databases;
    }

    public static Schema GetTablesInSchemaRemote(String schemaName) {
        Schema schema = new Schema(schemaName);
        String tablesQuery = "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = ?";
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

        try (Connection conn = ConnectRemote();
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
                    String refTable   = cols.getString("REFERENCED_TABLE_NAME");
                    String refCol     = cols.getString("REFERENCED_COLUMN_NAME");
                    String reference  = refTable != null ? refTable + "(" + refCol + ")" : null;
                    table.addField(new Field(reference, isPrimary,
                            cols.getString("DATA_TYPE"), cols.getString("COLUMN_NAME")));
                }
                cols.close();
                colPs.clearParameters();
                schema.addTable(table);
            }
        } catch (SQLException e) {
            System.err.println("[REMOTE SQL ERROR] GetTablesInSchemaRemote — " + e.getMessage());
        }
        return schema;
    }

    public static List<String[]> GetForeignKeysRemote(String schemaName) {
        List<String[]> fks = new ArrayList<>();
        String query =
                "SELECT TABLE_NAME, COLUMN_NAME, REFERENCED_TABLE_NAME, REFERENCED_COLUMN_NAME " +
                        "FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE " +
                        "WHERE TABLE_SCHEMA = ? AND REFERENCED_TABLE_NAME IS NOT NULL";
        try (Connection conn = ConnectRemote();
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
            System.err.println("[REMOTE SQL ERROR] GetForeignKeysRemote — " + e.getMessage());
        }
        return fks;
    }

    public static List<String[]> GetTableDataRemote(String schemaName, String tableName, List<String> outColumns) {
        List<String[]> rows = new ArrayList<>();
        String query = "SELECT * FROM `" + schemaName + "`.`" + tableName + "`";
        try (Connection conn = ConnectRemote();
             Statement stmt  = conn.createStatement();
             ResultSet rs    = stmt.executeQuery(query)) {
            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();
            for (int i = 1; i <= colCount; i++) outColumns.add(meta.getColumnName(i));
            while (rs.next()) {
                String[] row = new String[colCount];
                for (int i = 1; i <= colCount; i++) {
                    String val = rs.getString(i);
                    row[i - 1] = val != null ? val : "NULL";
                }
                rows.add(row);
            }
        } catch (SQLException e) {
            System.err.println("[REMOTE SQL ERROR] GetTableDataRemote — " + e.getMessage());
        }
        return rows;
    }

    public static String CloneSchemaFromRemote(String schemaName) {
        System.out.println("[CLONE] Starting clone of remote schema: " + schemaName);

        Schema remoteSchema      = GetTablesInSchemaRemote(schemaName);
        List<String[]> remoteFKs = GetForeignKeysRemote(schemaName);

        if (remoteSchema.getTables().isEmpty()) {
            return "WARNING: Remote schema '" + schemaName + "' has no tables or could not be read.";
        }

        // ── 1. Drop + recreate locally (always a clean copy) ──────────────
        try (Connection localConn = Connect();
             Statement   stmt     = localConn.createStatement()) {

            stmt.executeUpdate("DROP DATABASE IF EXISTS `" + schemaName + "`");
            stmt.executeUpdate("CREATE DATABASE `" + schemaName + "`");
            stmt.execute("SET FOREIGN_KEY_CHECKS = 0");

            for (Table table : remoteSchema.getTables()) {
                StringBuilder q = new StringBuilder(
                        "CREATE TABLE `" + schemaName + "`.`" + table.getName() + "` (");
                List<String> fkClauses = new ArrayList<>();

                for (Field field : table.getFields()) {
                    q.append("`").append(field.getName()).append("` ").append(normalizeSqlType(field.getType()));

                    if (field.isPrimary()) q.append(" PRIMARY KEY AUTO_INCREMENT");
                    q.append(", ");

                    String ref = field.getReference();
                    if (ref != null && !ref.isEmpty()) {
                        int    paren  = ref.indexOf('(');
                        String refTbl = ref.substring(0, paren);
                        String refCol = ref.substring(paren + 1, ref.length() - 1);
                        fkClauses.add("FOREIGN KEY (`" + field.getName() + "`) " +
                                "REFERENCES `" + schemaName + "`.`" + refTbl + "`(`" + refCol + "`)");
                    }
                }

                for (String fk : fkClauses) q.append(fk).append(", ");
                q.setLength(q.length() - 2);
                q.append(") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

                System.out.println("[CLONE DDL] " + q);
                stmt.executeUpdate(q.toString());
            }

            stmt.execute("SET FOREIGN_KEY_CHECKS = 1");

        } catch (SQLException e) {
            System.err.println("[CLONE ERROR] DDL phase — " + e.getMessage());
            return "ERROR (structure): " + e.getMessage();
        }

        // ── 2. Copy data — parent tables first so FK constraints are satisfied ──
        List<Table> ordered = topoSort(remoteSchema.getTables(), remoteFKs);
        int totalRows = 0;

        try (Connection localConn  = Connect();
             Connection remoteConn = ConnectRemote()) {

            localConn.setAutoCommit(false);

            try (Statement s = localConn.createStatement()) { s.execute("SET FOREIGN_KEY_CHECKS = 0"); }

            for (Table table : ordered) {
                List<String>   columns = new ArrayList<>();
                List<String[]> rows    = new ArrayList<>();

                String sel = "SELECT * FROM `" + schemaName + "`.`" + table.getName() + "`";
                try (Statement rs2 = remoteConn.createStatement();
                     ResultSet rs  = rs2.executeQuery(sel)) {
                    ResultSetMetaData meta = rs.getMetaData();
                    int colCount = meta.getColumnCount();
                    for (int i = 1; i <= colCount; i++) columns.add(meta.getColumnName(i));
                    while (rs.next()) {
                        String[] row = new String[colCount];
                        for (int i = 1; i <= colCount; i++) row[i - 1] = rs.getString(i);
                        rows.add(row);
                    }
                }

                if (rows.isEmpty()) continue;

                StringBuilder ins = new StringBuilder(
                        "INSERT INTO `" + schemaName + "`.`" + table.getName() + "` (");
                for (int i = 0; i < columns.size(); i++) {
                    ins.append("`").append(columns.get(i)).append("`");
                    if (i < columns.size() - 1) ins.append(", ");
                }
                ins.append(") VALUES (");
                ins.append("?,".repeat(columns.size()));
                ins.setLength(ins.length() - 1);
                ins.append(")");

                try (PreparedStatement ps = localConn.prepareStatement(ins.toString())) {
                    for (String[] row : rows) {
                        for (int i = 0; i < row.length; i++) ps.setString(i + 1, row[i]);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                totalRows += rows.size();
                System.out.println("[CLONE DATA] " + table.getName() + " — " + rows.size() + " rows");
            }

            try (Statement s = localConn.createStatement()) { s.execute("SET FOREIGN_KEY_CHECKS = 1"); }
            localConn.commit();

        } catch (SQLException e) {
            System.err.println("[CLONE ERROR] Data phase — " + e.getMessage());
            return "ERROR (data copy): " + e.getMessage();
        }

        return "Cloned '" + schemaName + "' — "
                + remoteSchema.getTables().size() + " table(s), "
                + totalRows + " row(s) copied.";
    }

    public static String PushSchemaToRemote(String schemaName) {
        Schema local  = GetTablesInSchema(schemaName);
        Schema remote = GetTablesInSchemaRemote(schemaName);

        if (local.getTables().isEmpty())
            return "WARNING: Local schema '" + schemaName + "' has no tables.";

        Map<String, Table> remoteTables = new HashMap<>();
        for (Table t : remote.getTables()) remoteTables.put(t.getName().toLowerCase(), t);

        StringBuilder log = new StringBuilder();

        try (Connection remoteConn = ConnectRemote();
             Statement  stmt       = remoteConn.createStatement()) {

            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS `" + schemaName + "`");
            stmt.execute("SET FOREIGN_KEY_CHECKS = 0");

            for (Table localTable : local.getTables()) {
                String key = localTable.getName().toLowerCase();

                if (!remoteTables.containsKey(key)) {
                    // ── Table absent on remote → CREATE ───────────────────
                    StringBuilder q = new StringBuilder(
                            "CREATE TABLE IF NOT EXISTS `" + schemaName + "`.`" + localTable.getName() + "` (");
                    List<String> fkClauses = new ArrayList<>();

                    for (Field f : localTable.getFields()) {
                        q.append("`").append(f.getName()).append("` ").append(normalizeSqlType(f.getType()));
                        if (f.isPrimary()) q.append(" PRIMARY KEY AUTO_INCREMENT");
                        q.append(", ");

                        String ref = f.getReference();
                        if (ref != null && !ref.isEmpty()) {
                            int    paren  = ref.indexOf('(');
                            String refTbl = ref.substring(0, paren);
                            String refCol = ref.substring(paren + 1, ref.length() - 1);
                            fkClauses.add("FOREIGN KEY (`" + f.getName() + "`) " +
                                    "REFERENCES `" + schemaName + "`.`" + refTbl + "`(`" + refCol + "`)");
                        }
                    }

                    for (String fk : fkClauses) q.append(fk).append(", ");
                    q.setLength(q.length() - 2);
                    q.append(") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

                    System.out.println("[PUSH DDL] " + q);
                    stmt.executeUpdate(q.toString());
                    log.append("Created table: ").append(localTable.getName()).append("\n");

                } else {
                    // ── Table present → diff columns ──────────────────────
                    Table remoteTable = remoteTables.get(key);
                    Map<String, String> remoteColTypes = new HashMap<>();
                    for (Field f : remoteTable.getFields())
                        remoteColTypes.put(f.getName().toLowerCase(), f.getType().toLowerCase());

                    Set<String> claimedRemoteCols = new HashSet<>(); // columns accounted for by a local field

                    for (Field localField : localTable.getFields()) {
                        if (localField.getName() == null || localField.getName().isBlank()) continue;

                        String colKey = localField.getName().toLowerCase();

                        if (localField.isPrimary()) {
                            claimedRemoteCols.add(colKey);
                            continue;
                        }

                        String rawType = localField.getType();
                        if (rawType == null || rawType.isBlank()) {
                            claimedRemoteCols.add(colKey);
                            continue;
                        }
                        String sqlType = normalizeSqlType(rawType);

                        String oldKey = (localField.getOldName() != null && !localField.getOldName().isBlank())
                                ? localField.getOldName().toLowerCase()
                                : null;

                        if (oldKey != null && !oldKey.equals(colKey) && remoteColTypes.containsKey(oldKey)
                                && !remoteColTypes.containsKey(colKey)) {
                            // ── Renamed locally ──
                            String renameSql = "ALTER TABLE `" + schemaName + "`.`" + localTable.getName() +
                                    "` RENAME COLUMN `" + localField.getOldName() + "` TO `" + localField.getName() + "`";
                            System.out.println("[PUSH RENAME] " + renameSql);
                            stmt.executeUpdate(renameSql);
                            log.append("Renamed column: ")
                                    .append(localTable.getName()).append(".").append(localField.getOldName())
                                    .append(" → ").append(localField.getName()).append("\n");

                            String oldType = remoteColTypes.remove(oldKey);
                            remoteColTypes.put(colKey, oldType);
                            claimedRemoteCols.add(colKey);

                            if (!remoteColTypes.get(colKey).equalsIgnoreCase(sqlType)) {
                                String modSql = "ALTER TABLE `" + schemaName + "`.`" + localTable.getName() +
                                        "` MODIFY COLUMN `" + localField.getName() + "` " + sqlType;
                                System.out.println("[PUSH ALTER] " + modSql);
                                stmt.executeUpdate(modSql);
                                log.append("Modified: ")
                                        .append(localTable.getName()).append(".").append(localField.getName())
                                        .append(" → ").append(sqlType).append("\n");
                            }

                        } else if (!remoteColTypes.containsKey(colKey)) {
                            // ── New column → ADD ──
                            String sql = "ALTER TABLE `" + schemaName + "`.`" + localTable.getName() +
                                    "` ADD COLUMN `" + localField.getName() + "` " + sqlType;
                            System.out.println("[PUSH ALTER] " + sql);
                            stmt.executeUpdate(sql);
                            log.append("Added column: ")
                                    .append(localTable.getName()).append(".").append(localField.getName()).append("\n");
                            claimedRemoteCols.add(colKey);

                        } else {
                            claimedRemoteCols.add(colKey);
                            if (!remoteColTypes.get(colKey).equalsIgnoreCase(sqlType)) {
                                String sql = "ALTER TABLE `" + schemaName + "`.`" + localTable.getName() +
                                        "` MODIFY COLUMN `" + localField.getName() + "` " + sqlType;
                                System.out.println("[PUSH ALTER] " + sql);
                                stmt.executeUpdate(sql);
                                log.append("Modified: ")
                                        .append(localTable.getName()).append(".").append(localField.getName())
                                        .append(" → ").append(sqlType).append("\n");
                            }
                        }
                    }

                    // ── Drop remote columns that no longer exist locally ──────────────
                    for (String remoteColKey : remoteColTypes.keySet()) {
                        if (!claimedRemoteCols.contains(remoteColKey)) {
                            String origName = null;
                            for (Field f : remoteTable.getFields()) {
                                if (f.getName().equalsIgnoreCase(remoteColKey)) { origName = f.getName(); break; }
                            }
                            String sql = "ALTER TABLE `" + schemaName + "`.`" + localTable.getName() +
                                    "` DROP COLUMN `" + origName + "`";
                            System.out.println("[PUSH DROP] " + sql);
                            stmt.executeUpdate(sql);
                            log.append("Dropped column: ")
                                    .append(localTable.getName()).append(".").append(origName).append("\n");
                        }
                    }
                }
            }

            stmt.execute("SET FOREIGN_KEY_CHECKS = 1");

        } catch (SQLException e) {
            System.err.println("[PUSH ERROR] " + e.getMessage());
            return "ERROR: " + e.getMessage();
        }

        // ── 3. Sync row data (upsert local → remote) ──────────────────────────
        try (Connection localConn  = Connect();
             Connection remoteConn = ConnectRemote()) {

            remoteConn.setAutoCommit(false);

            try (Statement s = remoteConn.createStatement()) {
                s.execute("SET FOREIGN_KEY_CHECKS = 0");
            }

            for (Table localTable : local.getTables()) {
                // Fetch all local rows
                List<String>   columns = new ArrayList<>();
                List<String[]> rows    = new ArrayList<>();

                String sel = "SELECT * FROM `" + schemaName + "`.`" + localTable.getName() + "`";
                try (Statement st = localConn.createStatement();
                     ResultSet rs = st.executeQuery(sel)) {
                    ResultSetMetaData meta = rs.getMetaData();
                    int colCount = meta.getColumnCount();
                    for (int i = 1; i <= colCount; i++) columns.add(meta.getColumnName(i));
                    while (rs.next()) {
                        String[] row = new String[colCount];
                        for (int i = 1; i <= colCount; i++) row[i - 1] = rs.getString(i);
                        rows.add(row);
                    }
                }

                if (rows.isEmpty()) continue;

                // Build INSERT ... ON DUPLICATE KEY UPDATE
                // This inserts new rows and updates existing ones by PK — never deletes.
                StringBuilder ins = new StringBuilder(
                        "INSERT INTO `" + schemaName + "`.`" + localTable.getName() + "` (");
                for (int i = 0; i < columns.size(); i++) {
                    ins.append("`").append(columns.get(i)).append("`");
                    if (i < columns.size() - 1) ins.append(", ");
                }
                ins.append(") VALUES (");
                ins.append("?,".repeat(columns.size()));
                ins.setLength(ins.length() - 1);
                ins.append(") ON DUPLICATE KEY UPDATE ");

                // Update every non-PK column on conflict
                List<String> nonPkCols = new ArrayList<>();
                for (Field f : localTable.getFields()) {
                    if (!f.isPrimary()) nonPkCols.add(f.getName());
                }
                // Fallback: if we can't determine PKs, update all columns
                List<String> updateCols = nonPkCols.isEmpty() ? columns : nonPkCols;
                for (int i = 0; i < updateCols.size(); i++) {
                    ins.append("`").append(updateCols.get(i)).append("` = VALUES(`")
                            .append(updateCols.get(i)).append("`)");
                    if (i < updateCols.size() - 1) ins.append(", ");
                }

                try (PreparedStatement ps = remoteConn.prepareStatement(ins.toString())) {
                    for (String[] row : rows) {
                        for (int i = 0; i < row.length; i++) ps.setString(i + 1, row[i]);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                log.append("Synced data: ").append(localTable.getName())
                        .append(" — ").append(rows.size()).append(" row(s)\n");
            }

            try (Statement s = remoteConn.createStatement()) {
                s.execute("SET FOREIGN_KEY_CHECKS = 1");
            }
            remoteConn.commit();

        } catch (SQLException e) {
            System.err.println("[PUSH ERROR] Data sync — " + e.getMessage());
            return "ERROR (data sync): " + e.getMessage();
        }


        return log.isEmpty()
                ? "Remote schema '" + schemaName + "' is already up to date."
                : log.toString().trim();
    }

    private static List<Table> topoSort(List<Table> tables, List<String[]> foreignKeys) {
        Map<String, Set<String>> deps   = new HashMap<>();
        Map<String, Table>       byName = new HashMap<>();

        for (Table t : tables) {
            deps.put(t.getName(), new HashSet<>());
            byName.put(t.getName(), t);
        }
        for (String[] fk : foreignKeys) {
            String child  = fk[0];   // table that holds the FK column
            String parent = fk[2];   // table being referenced
            if (!child.equals(parent) && deps.containsKey(child))
                deps.get(child).add(parent);
        }

        List<Table> sorted  = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        for (Table t : tables)
            topoVisit(t.getName(), deps, visited, sorted, byName);

        return sorted;
    }

    private static void topoVisit(String name,
                                  Map<String, Set<String>> deps,
                                  Set<String> visited,
                                  List<Table> sorted,
                                  Map<String, Table> byName) {
        if (visited.contains(name)) return;
        visited.add(name);
        for (String parent : deps.getOrDefault(name, Collections.emptySet()))
            topoVisit(parent, deps, visited, sorted, byName);
        if (byName.containsKey(name)) sorted.add(byName.get(name));
    }

    private static String normalizeSqlType(String rawType) {
        if (rawType == null || rawType.isBlank()) return "VARCHAR(255)";
        String t = rawType.trim();
        // bare VARCHAR / CHAR / NVARCHAR need a length — default to 255
        if (t.matches("(?i)varchar|char|nvarchar"))        return t + "(255)";
        // bare DECIMAL / NUMERIC need precision & scale
        if (t.matches("(?i)decimal|numeric"))              return t + "(10,2)";
        return t;
    }
}