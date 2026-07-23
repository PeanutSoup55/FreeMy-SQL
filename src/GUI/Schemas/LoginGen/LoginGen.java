package GUI.Schemas.LoginGen;

import GUI.Schemas.SchemasRoot;
import GUI.Settings.Theme;
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
    private final String schemaName;
    private final Table table;
    private final Runnable onDone;

    private ComboBox<String> identifierBox;
    private ComboBox<String> passwordBox;
    private TextArea codeArea;
    private TextArea bcryptArea;
    private TabPane tabPane;

    public LoginGen(SchemasRoot root, String schemaName, Table table, Runnable onDone) {
        this.root = root;
        this.schemaName = schemaName;
        this.table = table;
        this.onDone = onDone;

        setSpacing(0);
        setStyle("-fx-background-color: #F2F4F2;");
        VBox.setVgrow(this, Priority.ALWAYS);
        getChildren().addAll(buildHeader(), buildBody());
    }


    private BorderPane buildHeader() {
        Button backBtn = new Button("← Back");
        backBtn.setStyle("-fx-background-color: rgba(255,255,255,0.12); -fx-text-fill: white;" +
                "-fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 12;" +
                "-fx-background-radius: 8; -fx-padding: 8 16;" +
                "-fx-border-color: rgba(255,255,255,0.28); -fx-border-radius: 8; -fx-border-width: 1;");
        backBtn.setOnAction(e -> onDone.run());

        Text title = new Text("Login Generator");
        title.setFont(Font.font("System", FontWeight.BOLD, 20));
        title.setStyle("-fx-fill: " + Theme.colour6 + ";");

        HBox leftBox = new HBox(backBtn);
        leftBox.setAlignment(Pos.CENTER_LEFT);

        HBox centerBox = new HBox(title);
        centerBox.setAlignment(Pos.CENTER);

        // Mirrors leftBox's width so centerBox stays visually centered on the full bar
        HBox rightSpacer = new HBox();
        rightSpacer.setMinWidth(Region.USE_PREF_SIZE);
        rightSpacer.prefWidthProperty().bind(leftBox.widthProperty());

        BorderPane topBar = new BorderPane();
        topBar.setPadding(new Insets(18, 24, 18, 24));
        topBar.setStyle("-fx-background-color: " + Theme.colour2 + ";");
        topBar.setLeft(leftBox);
        topBar.setCenter(centerBox);
        topBar.setRight(rightSpacer);

        return topBar;
    }


    private SplitPane buildBody() {
        SplitPane split = new SplitPane();
        split.setOrientation(javafx.geometry.Orientation.HORIZONTAL);
        split.getItems().addAll(buildConfig(), buildOutput());
        split.setDividerPositions(0.28);
        VBox.setVgrow(split, Priority.ALWAYS);
        return split;
    }


    private VBox buildConfig() {
        Label configTitle = new Label("Configuration");
        configTitle.setFont(Font.font("System", FontWeight.BOLD, 14));
        configTitle.setTextFill(Color.web("#000000"));

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

        VBox fieldsList = new VBox(4);
        fieldsList.setPadding(new Insets(10));
        fieldsList.setStyle("-fx-background-color: #F8FAF8; -fx-background-radius: 6;" +
                "-fx-border-color: #E2E6E2; -fx-border-radius: 6;");
        for (Field f : table.getFields()) {
            boolean isFk = f.getReference() != null && !f.getReference().isEmpty();
            String  tag  = f.isPrimary() ? " [PK]" : isFk ? " [FK]" : "";
            Label   lbl  = new Label(f.getName() + "  " + f.getType() + tag);
            lbl.setFont(Font.font("Courier New", 11));
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


    private VBox buildOutput() {
        Label outputTitle = new Label("Generated Code");
        outputTitle.setFont(Font.font("System", FontWeight.BOLD, 14));
        outputTitle.setTextFill(Color.web("#000000"));

        Button copyBtn = outlineBtn("Copy");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox outputTopBar = new HBox(10, outputTitle, spacer, copyBtn);
        outputTopBar.setAlignment(Pos.CENTER_LEFT);
        outputTopBar.setPadding(new Insets(10, 16, 10, 16));
        outputTopBar.setStyle("-fx-background-color: white;" +
                "-fx-border-color: #EEEEEE; -fx-border-width: 0 0 1 0;");

        Font monoFont = Font.font("Courier New", 12);
        String darkAreaStyle =
                "-fx-control-inner-background: #1E1E2E;" +
                        "-fx-text-fill: #CDD6F4;" +
                        "-fx-prompt-text-fill: #6B7099;" +
                        "-fx-highlight-fill: #2E5A47;" +
                        "-fx-font-family: 'Courier New';" +
                        "-fx-font-size: 12;";

        // --- Tab 1: Generated Code ---
        codeArea = new TextArea();
        codeArea.setFont(monoFont);
        codeArea.setEditable(false);
        codeArea.setWrapText(false);
        codeArea.setPromptText("Press Generate to produce code.");
        codeArea.setStyle(darkAreaStyle);
        codeArea.textProperty().addListener((obs, old, nw) -> {
            if (!monoFont.equals(codeArea.getFont())) codeArea.setFont(monoFont);
        });

        Tab codeTab = new Tab("Generated Code", codeArea);
        codeTab.setClosable(false);

        // --- Tab 2: BCrypt.txt ---
        bcryptArea = new TextArea();
        bcryptArea.setFont(monoFont);
        bcryptArea.setEditable(false);
        bcryptArea.setWrapText(false);
        bcryptArea.setStyle(darkAreaStyle);
        bcryptArea.setText(loadBCryptFile());

        Tab bcryptTab = new Tab("BCrypt.java", bcryptArea);
        bcryptTab.setClosable(false);

        // --- TabPane ---
        tabPane = new TabPane(codeTab, bcryptTab);
        VBox.setVgrow(tabPane, Priority.ALWAYS);
        tabPane.setTabMinWidth(100);
        // Dark tab header to blend with the code area background
        tabPane.setStyle(
                "-fx-background-color: #1E1E2E;" +
                        "-fx-tab-min-height: 32;" +
                        "-fx-open-tab-animation: NONE;" +
                        "-fx-close-tab-animation: NONE;" +
                        "-fx-focus-color: transparent;" +
                        "-fx-faint-focus-color: transparent;"
        );
        tabPane.getStylesheets().add(
                "data:text/css," +
                        ".tab-pane > .tab-header-area > .headers-region > .tab {" +
                        "    -fx-background-color: #2A2A3E;" +
                        "    -fx-background-radius: 6 6 0 0;" +
                        "    -fx-padding: 4 14;" +
                        "}" +
                        ".tab-pane > .tab-header-area > .headers-region > .tab:selected {" +
                        "    -fx-background-color: #1E1E2E;" +
                        "}" +
                        ".tab-pane > .tab-header-area > .headers-region > .tab .tab-label {" +
                        "    -fx-text-fill: #888aaa;" +
                        "    -fx-font-family: 'System';" +
                        "    -fx-font-weight: bold;" +
                        "    -fx-font-size: 12;" +
                        "}" +
                        ".tab-pane > .tab-header-area > .headers-region > .tab:selected .tab-label {" +
                        "    -fx-text-fill: #CDD6F4;" +
                        "}" +
                        ".tab-pane > .tab-header-area {" +
                        "    -fx-background-color: #2A2A3E;" +
                        "    -fx-padding: 6 0 0 6;" +
                        "}" +
                        ".tab-pane > .tab-content-area {" +
                        "    -fx-background-color: #1E1E2E;" +
                        "    -fx-padding: 0;" +
                        "}"
        );

        // Copy button copies from whichever tab is active
        copyBtn.setOnAction(e -> {
            ClipboardContent cc = new ClipboardContent();
            Tab selected = tabPane.getSelectionModel().getSelectedItem();
            cc.putString(selected == bcryptTab ? bcryptArea.getText() : codeArea.getText());
            Clipboard.getSystemClipboard().setContent(cc);
        });

        VBox panel = new VBox(0, outputTopBar, tabPane);
        VBox.setVgrow(panel, Priority.ALWAYS);
        panel.setStyle("-fx-background-color: #1E1E2E;");
        return panel;
    }


    /**
     * Reads BCrypt.txt from the same directory as the running JAR/classes.
     * Falls back to the process working directory, then the classpath.
     */
    private String loadBCryptFile() {
        // 1. Same package as LoginGen.class — works after IntelliJ copies resources to output
        for (String name : new String[]{"BCrypt.txt", "BCrypt"}) {
            try (java.io.InputStream is = LoginGen.class.getResourceAsStream(name)) {
                if (is != null)
                    return new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            } catch (Exception ignored) {}
        }

        // 2. Fallback: working directory (beside the JAR)
        for (String name : new String[]{"BCrypt.txt", "BCrypt"}) {
            try {
                java.nio.file.Path p = java.nio.file.Paths.get(name);
                if (java.nio.file.Files.exists(p))
                    return java.nio.file.Files.readString(p);
            } catch (Exception ignored) {}
        }

        return "// BCrypt file not found.\n" +
                "// Ensure BCrypt.txt is in GUI/Schemas/LoginGen/\n" +
                "// and add '?*.txt' to Settings > Build > Compiler > Resource patterns.";
    }


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
        tabPane.getSelectionModel().select(0);
    }

    private String buildCode(String id, String pw) {
        List<Field> returnFields = new ArrayList<>();
        for (Field f : table.getFields()) {
            if (f.isPrimary() || f.getName().equals(pw)) continue;
            returnFields.add(f);
        }

        StringBuilder userFields = new StringBuilder();
        for (Field f : returnFields)
            userFields.append("    private final ")
                    .append(sqlToJava(f.getType())).append(" ").append(f.getName()).append(";\n");

        StringBuilder ctorParams = new StringBuilder();
        StringBuilder ctorAssign = new StringBuilder();
        for (int i = 0; i < returnFields.size(); i++) {
            Field f = returnFields.get(i);
            if (i > 0) ctorParams.append(", ");
            ctorParams.append(sqlToJava(f.getType())).append(" ").append(f.getName());
            ctorAssign.append("        this.").append(f.getName())
                    .append(" = ").append(f.getName()).append(";\n");
        }

        StringBuilder getters = new StringBuilder();
        for (Field f : returnFields)
            getters.append("    public ").append(sqlToJava(f.getType()))
                    .append(" get").append(cap(f.getName()))
                    .append("() { return ").append(f.getName()).append("; }\n");

        StringBuilder detailsBody = new StringBuilder();
        for (int i = 0; i < returnFields.size(); i++) {
            Field f = returnFields.get(i);
            if (i == 0)
                detailsBody.append("\"").append(cap(f.getName())).append(": \" + ").append(f.getName());
            else
                detailsBody.append(" + \"\\n").append(cap(f.getName())).append(": \" + ").append(f.getName());
        }

        StringBuilder selectCols = new StringBuilder();
        selectCols.append("`").append(getpk()).append("`");
        for (Field f : returnFields)
            selectCols.append(", `").append(f.getName()).append("`");
        selectCols.append(", `").append(pw).append("`");

        StringBuilder scanArgs = new StringBuilder();
        for (int i = 0; i < returnFields.size(); i++) {
            Field f = returnFields.get(i);
            if (i > 0) scanArgs.append(",\n                    ");
            scanArgs.append(rsGetter(f.getType())).append("(\"").append(f.getName()).append("\")");
        }

        StringBuilder makeParams  = new StringBuilder();
        StringBuilder makeInsert  = new StringBuilder();
        StringBuilder makeValues  = new StringBuilder();
        StringBuilder makeSetters = new StringBuilder();
        int paramIdx = 1;
        for (Field f : returnFields) {
            if (makeParams.length() > 0) makeParams.append(", ");
            makeParams.append(sqlToJava(f.getType())).append(" ").append(f.getName());
            if (makeInsert.length() > 0) { makeInsert.append(", "); makeValues.append(", "); }
            makeInsert.append("`").append(f.getName()).append("`");
            makeValues.append("?");
            makeSetters.append("            ps.").append(psSetter(f.getType()))
                    .append("(").append(paramIdx++).append(", ").append(f.getName()).append(");\n");
        }
        makeParams.append(", String password");
        makeInsert.append(", `").append(pw).append("`");
        makeValues.append(", ?");
        makeSetters.append("            ps.setString(").append(paramIdx).append(", hashedPassword);\n");

        String tbl = "`" + schemaName + "`.`" + table.getName() + "`";

        return
                "import org.mindrot.jbcrypt.BCrypt;\n" +
                        "import java.sql.*;\n" +
                        "// Requires: org.mindrot:jbcrypt:0.4 in your build file\n" +
                        "public class Auth {\n" +
                        "    public static class User {\n" +
                        userFields +
                        "        public User(" + ctorParams + ") {\n" +
                        ctorAssign +
                        "        }\n" +
                        getters +
                        "        public String details(){\n" +
                        "            return " + detailsBody + ";\n" +
                        "        }\n" +
                        "    }\n" +
                        "    public static User login(String " + id + ", String plainPassword) throws SQLException {\n" +
                        "        String sql = \"SELECT " + selectCols + " FROM " + tbl + " WHERE `" + id + "` = ? LIMIT 1\";\n" +
                        "        try (Connection conn = DriverManager.getConnection(Creds.getURL(), Creds.getUSER(), Creds.getPASS());\n" +
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
                        "    public static void MakeUser(" + makeParams + ") throws SQLException {\n" +
                        "        String query = \"INSERT INTO " + tbl + " (" + makeInsert + ") VALUES (" + makeValues + ")\";\n" +
                        "        String hashedPassword = hashPassword(password);\n" +
                        "        try (Connection conn = DriverManager.getConnection(Creds.getURL(), Creds.getUSER(), Creds.getPASS()); PreparedStatement ps = conn.prepareStatement(query)) {\n" +
                        makeSetters +
                        "            ps.executeUpdate();\n" +
                        "        }\n" +
                        "    }\n" +
                        "    public static String hashPassword(String plain) {\n" +
                        "        return BCrypt.hashpw(plain, BCrypt.gensalt(12));\n" +
                        "    }\n" +
                        "}\n" +
                        "\n" +
                        "public class Creds {\n" +
                        "    private static String USER = \"root\";\n" +
                        "    private static String PASS = \"yourPassword\";\n" +
                        "    private static String URL = \"jdbc:mysql://localhost:3306/" + schemaName + "\";\n" +
                        "    public static String getUSER() {\n" +
                        "        return USER;\n" +
                        "    }\n" +
                        "    public static void setUSER(String USER) {\n" +
                        "        Creds.USER = USER;\n" +
                        "    }\n" +
                        "    public static String getPASS() {\n" +
                        "        return PASS;\n" +
                        "    }\n" +
                        "    public static void setPASS(String PASS) {\n" +
                        "        Creds.PASS = PASS;\n" +
                        "    }\n" +
                        "    public static String getURL() {\n" +
                        "        return URL;\n" +
                        "    }\n" +
                        "    public static void setURL(String URL) {\n" +
                        "        Creds.URL = URL;\n" +
                        "    }\n" +
                        "}\n";
    }


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

    private static String psSetter(String sql) {
        String t = sql.toUpperCase();
        if (t.contains("BIGINT"))  return "setLong";
        if (t.contains("INT"))     return "setInt";
        if (t.contains("DOUBLE") || t.contains("FLOAT")) return "setDouble";
        if (t.contains("DECIMAL")) return "setBigDecimal";
        if (t.contains("BOOL"))    return "setBoolean";
        if (t.contains("DATE"))    return "setDate";
        return "setString";
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
        cb.setStyle("-fx-background-color: " + Theme.colour1 + "; -fx-background-radius: 8; -fx-cursor: hand;");
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
        l.setStyle("-fx-text-fill: " + Theme.colour1 + ";");
        return l;
    }

    private static Button filledBtn(String label) {
        Button b = new Button(label);
        b.setStyle("-fx-background-color: " + Theme.colour1 + "; -fx-text-fill: white;" +
                "-fx-background-radius: 8; -fx-font-weight: bold;" +
                "-fx-cursor: hand; -fx-padding: 10 28; -fx-font-size: 13;");
        return b;
    }

    private static Button outlineBtn(String label) {
        Button b = new Button(label);
        b.setStyle("-fx-background-color: white; -fx-text-fill: " + Theme.colour2 + ";" +
                "-fx-border-color: " + Theme.colour2 + "; -fx-border-radius: 8;" +
                "-fx-background-radius: 8; -fx-font-weight: bold;" +
                "-fx-cursor: hand; -fx-padding: 8 18; -fx-font-size: 12;");
        return b;
    }
}