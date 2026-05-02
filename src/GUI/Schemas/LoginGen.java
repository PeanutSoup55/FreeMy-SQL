package GUI.Schemas;

import Objects.Field;
import Objects.Table;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.util.*;

public class LoginGen extends VBox {

    private final SchemasRoot root;
    private final String      schemaName;
    private final Table       table;

    private ComboBox<String> identifierBox;
    private ComboBox<String> passwordBox;
    private TextArea         codeArea;

    public LoginGen(SchemasRoot root, String schemaName, Table table) {
        this.root       = root;
        this.schemaName = schemaName;
        this.table      = table;

        setSpacing(0);
        setStyle("-fx-background-color: #F2F4F2;");
        VBox.setVgrow(this, Priority.ALWAYS);
        getChildren().addAll(buildHeader(), buildBody());
    }

    // ── Header ────────────────────────────────────────────────────────────

    private HBox buildHeader() {
        Button backBtn = new Button("← Back");
        backBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #2E5A47;" +
                "-fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 13;");
        backBtn.setOnAction(e -> root.createTables());

        Text title = new Text("Login Generator");
        title.setFont(Font.font("System", FontWeight.BOLD, 20));
        title.setFill(Color.web("#1E3D30"));

        Label badge = new Label(schemaName + "  ›  " + table.getName());
        badge.setStyle("-fx-background-color: #E9F5E8; -fx-text-fill: #2E5A47;" +
                "-fx-background-radius: 6; -fx-padding: 4 10;" +
                "-fx-font-weight: bold; -fx-font-size: 11;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox header = new HBox(14, backBtn, title, badge, spacer);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(18, 24, 14, 24));
        header.setStyle("-fx-background-color: white;" +
                "-fx-border-color: #EEEEEE; -fx-border-width: 0 0 1 0;");
        return header;
    }

    // ── Body ──────────────────────────────────────────────────────────────

    private SplitPane buildBody() {
        SplitPane split = new SplitPane();
        split.setOrientation(javafx.geometry.Orientation.HORIZONTAL);
        split.getItems().addAll(buildConfig(), buildOutput());
        split.setDividerPositions(0.28);
        VBox.setVgrow(split, Priority.ALWAYS);
        return split;
    }

    // ── Left config ───────────────────────────────────────────────────────

    private VBox buildConfig() {
        Label configTitle = new Label("Configuration");
        configTitle.setFont(Font.font("System", FontWeight.BOLD, 14));
        configTitle.setTextFill(Color.web("#1E3D30"));

        HBox topBar = new HBox(configTitle);
        topBar.setPadding(new Insets(14, 16, 10, 16));
        topBar.setStyle("-fx-background-color: white;" +
                "-fx-border-color: #EEEEEE; -fx-border-width: 0 0 1 0;");

        List<String> fieldNames = new ArrayList<>();
        for (Field f : table.getFields()) fieldNames.add(f.getName());

        identifierBox = styledCombo(fieldNames);
        passwordBox   = styledCombo(fieldNames);
        autoSelect(identifierBox, fieldNames, List.of("email", "username", "name", "user"));
        autoSelect(passwordBox,   fieldNames, List.of("password", "pass", "pwd", "hash"));

        // Fields list
        VBox fieldsList = new VBox(4);
        fieldsList.setPadding(new Insets(10));
        fieldsList.setStyle("-fx-background-color: #F8FAF8; -fx-background-radius: 6;" +
                "-fx-border-color: #E2E6E2; -fx-border-radius: 6;");
        for (Field f : table.getFields()) {
            boolean isFk = f.getReference() != null && !f.getReference().isEmpty();
            String  tag  = f.isPrimary() ? " [PK]" : isFk ? " [FK]" : "";
            Label   lbl  = new Label(f.getName() + "  " + f.getType() + tag);
            lbl.setFont(Font.font("Monospace", 11));
            lbl.setTextFill(f.isPrimary() ? Color.web("#2E5A47")
                    : isFk          ? Color.web("#8B5E3C")
                    :                 Color.web("#444444"));
            fieldsList.getChildren().add(lbl);
        }

        Button genBtn = filledBtn("Generate");
        genBtn.setMaxWidth(Double.MAX_VALUE);
        genBtn.setOnAction(e -> generate());

        VBox content = new VBox(14,
                sectionLabel("Identifier Field"),
                identifierBox,
                sectionLabel("Password Field"),
                passwordBox,
                sectionLabel("Table Fields"),
                fieldsList,
                genBtn);
        content.setPadding(new Insets(16));
        content.setStyle("-fx-background-color: white;");

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background-color: white; -fx-background: white;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        VBox panel = new VBox(0, topBar, scroll);
        VBox.setVgrow(panel, Priority.ALWAYS);
        panel.setStyle("-fx-background-color: white;");
        return panel;
    }

    // ── Right output ──────────────────────────────────────────────────────

    private VBox buildOutput() {
        Label outputTitle = new Label("Generated Code");
        outputTitle.setFont(Font.font("System", FontWeight.BOLD, 14));
        outputTitle.setTextFill(Color.web("#1E3D30"));

        Button copyBtn = outlineBtn("⎘  Copy");
        copyBtn.setOnAction(e -> {
            ClipboardContent cc = new ClipboardContent();
            cc.putString(codeArea.getText());
            Clipboard.getSystemClipboard().setContent(cc);
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox topBar = new HBox(10, outputTitle, spacer, copyBtn);
        topBar.setAlignment(Pos.CENTER_LEFT);
        topBar.setPadding(new Insets(10, 16, 10, 16));
        topBar.setStyle("-fx-background-color: white;" +
                "-fx-border-color: #EEEEEE; -fx-border-width: 0 0 1 0;");

        codeArea = new TextArea();
        codeArea.setFont(Font.font("Monospace", 12));
        codeArea.setEditable(false);
        codeArea.setWrapText(false);
        codeArea.setPromptText("Press Generate to produce code.");
        codeArea.setStyle(
                "-fx-control-inner-background: #1E1E2E;" +
                        "-fx-text-fill: #CDD6F4;" +
                        "-fx-highlight-fill: #2E5A47;" +
                        "-fx-font-size: 12;");
        VBox.setVgrow(codeArea, Priority.ALWAYS);

        VBox panel = new VBox(0, topBar, codeArea);
        VBox.setVgrow(panel, Priority.ALWAYS);
        panel.setStyle("-fx-background-color: #1E1E2E;");
        return panel;
    }

    // ── Generator ─────────────────────────────────────────────────────────

    private void generate() {
        String id = identifierBox.getValue();
        String pw = passwordBox.getValue();

        if (id == null || pw == null) {
            new Alert(Alert.AlertType.WARNING,
                    "Select both an identifier and a password field.", ButtonType.OK)
                    .showAndWait();
            return;
        }
        if (id.equals(pw)) {
            new Alert(Alert.AlertType.WARNING,
                    "Identifier and password must be different fields.", ButtonType.OK)
                    .showAndWait();
            return;
        }
        codeArea.setText(buildCode(id, pw));
    }

    private String buildCode(String id, String pw) {
        // Collect non-PK, non-id, non-pw fields for the User record
        StringBuilder userFields   = new StringBuilder();
        StringBuilder scanArgs     = new StringBuilder();
        StringBuilder selectCols   = new StringBuilder();

        List<Field> returnFields = new ArrayList<>();
        for (Field f : table.getFields()) {
            if (f.isPrimary() || f.getName().equals(pw)) continue;
            returnFields.add(f);
        }

        // SELECT: id col, identifier col, rest — password intentionally excluded
        selectCols.append("`").append(getpk()).append("`");
        selectCols.append(", `").append(id).append("`");
        for (Field f : returnFields) {
            if (!f.getName().equals(id))
                selectCols.append(", `").append(f.getName()).append("`");
        }
        // password is fetched separately for verify, then discarded
        selectCols.append(", `").append(pw).append("`");

        // User record fields
        userFields.append("    private final String ").append(id).append(";\n");
        for (Field f : returnFields) {
            if (!f.getName().equals(id))
                userFields.append("    private final ")
                        .append(sqlToJava(f.getType())).append(" ")
                        .append(f.getName()).append(";\n");
        }

        // Scan args match SELECT order
        scanArgs.append("rs.getString(\"").append(id).append("\")");
        for (Field f : returnFields) {
            if (!f.getName().equals(id))
                scanArgs.append(",\n                    ")
                        .append(rsGetter(f.getType())).append("(\"").append(f.getName()).append("\")");
        }

        // Constructor params
        StringBuilder ctorParams = new StringBuilder("String " + id);
        StringBuilder ctorAssign = new StringBuilder("        this." + id + " = " + id + ";\n");
        for (Field f : returnFields) {
            if (!f.getName().equals(id)) {
                ctorParams.append(", ").append(sqlToJava(f.getType())).append(" ").append(f.getName());
                ctorAssign.append("        this.").append(f.getName())
                        .append(" = ").append(f.getName()).append(";\n");
            }
        }

        // Getters
        StringBuilder getters = new StringBuilder();
        getters.append("    public String get").append(cap(id)).append("() { return ").append(id).append("; }\n");
        for (Field f : returnFields) {
            if (!f.getName().equals(id))
                getters.append("    public ").append(sqlToJava(f.getType()))
                        .append(" get").append(cap(f.getName()))
                        .append("() { return ").append(f.getName()).append("; }\n");
        }

        return
                "import globalfuncs.creds;\n" +
                        "import org.mindrot.jbcrypt.BCrypt;\n" +
                        "import java.sql.*;\n" +
                        "\n" +
                        "// Requires: org.mindrot:jbcrypt:0.4 in your build file\n" +
                        "\n" +
                        "public class Auth {\n" +
                        "\n" +
                        "    public static class User {\n" +
                        userFields +
                        "\n" +
                        "        public User(" + ctorParams + ") {\n" +
                        ctorAssign +
                        "        }\n" +
                        "\n" +
                        getters +
                        "    }\n" +
                        "\n" +
                        "    public static User login(String " + id + ", String plainPassword) throws SQLException {\n" +
                        "        String sql = \"SELECT " + selectCols + " FROM `" + schemaName + "`.`" + table.getName() + "` WHERE `" + id + "` = ? LIMIT 1\";\n" +
                        "        try (Connection conn = DriverManager.getConnection(creds.getUrl(), creds.getUser(), creds.getPass());\n" +
                        "             PreparedStatement ps = conn.prepareStatement(sql)) {\n" +
                        "            ps.setString(1, " + id + ");\n" +
                        "            ResultSet rs = ps.executeQuery();\n" +
                        "            if (!rs.next()) return null;\n" +
                        "            String hash = rs.getString(\"" + pw + "\");\n" +
                        "            if (!BCrypt.checkpw(plainPassword, hash)) return null;\n" +
                        "            return new User(\n" +
                        "                    " + scanArgs + "\n" +
                        "            );\n" +
                        "        }\n" +
                        "    }\n" +
                        "\n" +
                        "    public static String hashPassword(String plain) {\n" +
                        "        return BCrypt.hashpw(plain, BCrypt.gensalt(12));\n" +
                        "    }\n" +
                        "}\n";
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private String getpk() {
        for (Field f : table.getFields()) if (f.isPrimary()) return f.getName();
        return "id";
    }

    private static String sqlToJava(String sql) {
        String t = sql.toUpperCase();
        if (t.startsWith("VARCHAR") || t.startsWith("TEXT") || t.startsWith("CHAR")) return "String";
        if (t.contains("BIGINT"))  return "long";
        if (t.contains("INT"))     return "int";
        if (t.contains("DOUBLE") || t.contains("FLOAT")) return "double";
        if (t.contains("DECIMAL")) return "java.math.BigDecimal";
        if (t.contains("BOOL"))    return "boolean";
        if (t.contains("DATE") || t.contains("TIME")) return "String";
        return "String";
    }

    private static String rsGetter(String sql) {
        String t = sql.toUpperCase();
        if (t.contains("BIGINT"))  return "rs.getLong";
        if (t.contains("INT"))     return "rs.getInt";
        if (t.contains("DOUBLE") || t.contains("FLOAT")) return "rs.getDouble";
        if (t.contains("DECIMAL")) return "rs.getBigDecimal";
        if (t.contains("BOOL"))    return "rs.getBoolean";
        return "rs.getString";
    }

    private static String cap(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static void autoSelect(ComboBox<String> box,
                                   List<String> names, List<String> candidates) {
        for (String c : candidates)
            for (String n : names)
                if (n.toLowerCase().contains(c)) { box.setValue(n); return; }
        if (!names.isEmpty()) box.setValue(names.get(0));
    }

    private static ComboBox<String> styledCombo(List<String> items) {
        ComboBox<String> cb = new ComboBox<>();
        cb.getItems().addAll(items);
        cb.setMaxWidth(Double.MAX_VALUE);
        cb.setStyle("-fx-background-color: #2E5A47; -fx-background-radius: 8; -fx-cursor: hand;");
        cb.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(String s, boolean empty) {
                super.updateItem(s, empty);
                setText(empty || s == null ? "" : s);
                setTextFill(Color.WHITE);
                setStyle("-fx-background-color: transparent; -fx-font-weight: bold;");
            }
        });
        return cb;
    }

    private static Label sectionLabel(String text) {
        Label l = new Label(text);
        l.setFont(Font.font("System", FontWeight.BOLD, 12));
        l.setTextFill(Color.web("#1E3D30"));
        return l;
    }

    private static Button filledBtn(String label) {
        Button b = new Button(label);
        b.setStyle("-fx-background-color: #2E5A47; -fx-text-fill: white;" +
                "-fx-background-radius: 8; -fx-font-weight: bold;" +
                "-fx-cursor: hand; -fx-padding: 10 28; -fx-font-size: 13;");
        return b;
    }

    private static Button outlineBtn(String label) {
        Button b = new Button(label);
        b.setStyle("-fx-background-color: white; -fx-text-fill: #2E5A47;" +
                "-fx-border-color: #2E5A47; -fx-border-radius: 8;" +
                "-fx-background-radius: 8; -fx-font-weight: bold;" +
                "-fx-cursor: hand; -fx-padding: 8 18; -fx-font-size: 12;");
        return b;
    }
}