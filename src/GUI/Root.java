package GUI;

import GUI.Schemas.*;
import GUI.Schemas.LoginGen.LoginGen;
import GUI.Settings.Settings;
import Objects.*;
import SSH.SSHConnection;
import globalfuncs.creds;
import globalfuncs.db;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import tempFiles.TempCred;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class Root extends BorderPane {

    private String  activeMenu = "Schemas";
    private final String[] LABELS = {"Schemas", "Query", "Credentials", "Logs", "SSH"};
    private final String[] ICONS  = {
            "assets/schema.png",
            "assets/query.png",
            "assets/creds.png",
            "assets/logs.png",
            "assets/ssh.png"
    };
    private SchemasRoot schemasRoot;
    private HBox selectedTab;
    private boolean isRemoteSelected = false;
    private boolean sortAscending = true;
    private final List<VBox> schemaWrappers = new ArrayList<>();
    private VBox localSection;
    private VBox remoteSection;


    public Root() {
        schemasRoot = new SchemasRoot();
        createSide();
        setCenter(schemasRoot);
    }

    public void createSide() {
        // LEFT RAIL - icons only
        VBox rail = new VBox();
        rail.setPadding(new Insets(0, 0, 20, 0));
        rail.setSpacing(10);
        rail.setPrefWidth(52);
        rail.setMinWidth(52);
        rail.setMaxWidth(52);
        rail.setStyle("-fx-background-color: #080C14;");

        // Avatar at top
        Text initialLabel = new Text(creds.getInitials());
        initialLabel.setStyle("-fx-font-weight: bold; -fx-fill: white; -fx-font-size: 14;");
        Rectangle rec = new Rectangle(36, 36);
        rec.setArcWidth(8); rec.setArcHeight(8);
        rec.setFill(Color.web("#000000"));
        StackPane avatar = new StackPane(rec, initialLabel);
        avatar.setPadding(new Insets(10, 0, 10, 0));
        HBox avatarRow = new HBox(avatar);
        avatarRow.setAlignment(Pos.CENTER);
        avatarRow.setPadding(new Insets(10, 0, 10, 0));
        rail.getChildren().add(avatarRow);

        for (int i = 0; i < LABELS.length; i++) {
            rail.getChildren().add(createRailIcon(ICONS[i], LABELS[i]));
        }

        Region railSpacer = new Region();
        VBox.setVgrow(railSpacer, Priority.ALWAYS);
        rail.getChildren().add(railSpacer);

        rail.getChildren().add(createRailIcon("assets/settings.png", "Settings"));

        // SCHEMA TREE PANEL - only shown when Schemas is active
        VBox schemaPanel = new VBox();
        schemaPanel.setPrefWidth(220);
        schemaPanel.setMinWidth(220);
        schemaPanel.setStyle("-fx-background-color: #1C2333; -fx-border-color: transparent;");

        if (schemasRoot == null) schemasRoot = new SchemasRoot();
        Node sidebar = buildSchemaTree();
        if (sidebar instanceof Region r) {
            r.setPrefWidth(Double.MAX_VALUE);
            r.setMaxWidth(Double.MAX_VALUE);
            r.setStyle(r.getStyle()
                    .replace("-fx-border-color: #DEDEDE;", "-fx-border-color: transparent;")
                    .replace("-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.08), 8, 0, 1, 2);", ""));
        }
        VBox.setVgrow(sidebar, Priority.ALWAYS);
        schemaPanel.getChildren().add(sidebar);
        VBox.setVgrow((Node) sidebar, Priority.ALWAYS);

        HBox sidebarWrapper = new HBox(rail, schemaPanel);
        BorderPane.setMargin(sidebarWrapper, Insets.EMPTY);
        setLeft(sidebarWrapper);
    }

    private HBox createRailIcon(String iconPath, String label) {
        ImageView iv = new ImageView(new Image(
                Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(iconPath))));
        iv.setFitWidth(20);
        iv.setFitHeight(20);

        StackPane iconWrap = new StackPane(iv);
        iconWrap.setPrefSize(36, 36);
        boolean isActive = label.equals(activeMenu);
        iconWrap.setStyle(isActive
                ? "-fx-background-color: rgba(255,255,255,0.2); -fx-background-radius: 8;"
                : "-fx-background-color: transparent; -fx-background-radius: 8;");
        iconWrap.setCursor(javafx.scene.Cursor.HAND);

        HBox row = new HBox(iconWrap);
        row.setAlignment(Pos.CENTER);
        row.setPrefHeight(40);
        row.setOnMouseEntered(e -> {
            if (!label.equals(activeMenu))
                iconWrap.setStyle("-fx-background-color: rgba(255,255,255,0.1); -fx-background-radius: 8;");
        });
        row.setOnMouseExited(e -> {
            if (!label.equals(activeMenu))
                iconWrap.setStyle("-fx-background-color: transparent; -fx-background-radius: 8;");
        });
        row.setOnMouseClicked(e -> {
            activeMenu = label;
            switchCenterContent(label);
            createSide();
        });

        Tooltip.install(row, new Tooltip(label));
        return row;
    }

    private void switchCenterContent(String menuTitle) {
        switch (menuTitle) {
            case "Schemas" -> {
                if (schemasRoot == null) schemasRoot = new SchemasRoot();
                setCenter(schemasRoot);
            }
            case "Query" -> setCenter(new Query());
            case "Credentials" -> setCenter(new Creds());
            case "Logs" -> setCenter(new LogsRoot());
            case "SSH" -> setCenter(new SSHConnection(null));
            case "Settings" -> setCenter(new Settings());
        }
    }

    public Node buildSchemaTree() {
        VBox shell = new VBox();
        shell.setPrefWidth(220);
        shell.setMaxWidth(220);
        shell.setStyle(
                "-fx-background-color: #1C2333;" +
                        "-fx-background-radius: 0;" +
                        "-fx-border-color: transparent;" +
                        "-fx-effect: none;"
        );

        HBox toolbar = new HBox(2);
        toolbar.setPadding(new Insets(5, 6, 5, 6));
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setStyle("-fx-background-color: #1C2333; -fx-border-color: transparent;");

        Button refreshBtn  = makeToolBtn("/assets/refresh.png",  "Refresh");
        Button collapseBtn = makeToolBtn("/assets/collapse.png", "Collapse all");
        Button sortBtn     = makeToolBtn("/assets/sort.png",     "Sort A→Z / Z→A");

        // CHANGED: was schemasRoot.refresh() -> now refreshData() + rebuild tree + createTables
        refreshBtn.setOnMouseClicked(e -> {
            schemasRoot.refreshData();
            schemasRoot.createTables();
            createSide();
        });

        collapseBtn.setOnAction(e -> {
            for (VBox wrapper : schemaWrappers) {
                if (wrapper.getChildren().size() > 1) {
                    Node tableList = wrapper.getChildren().get(1);
                    if (tableList.isVisible()) {
                        tableList.setVisible(false);
                        tableList.setManaged(false);
                        HBox row        = (HBox) wrapper.getChildren().get(0);
                        StackPane caret = (StackPane) row.getChildren().get(0);
                        ((ImageView) caret.getChildren().getFirst()).setImage(
                                new Image(getClass().getResourceAsStream("/assets/right.png"))
                        );
                        ((Label) row.getChildren().get(3)).setVisible(false);
                    }
                }
            }
        });

        sortBtn.setOnAction(e -> {
            sortAscending = !sortAscending;
            resortSection(localSection, sortAscending);
            if (creds.hasRemote()) resortSection(remoteSection, sortAscending);
        });

        Region tbSpacer = new Region();
        HBox.setHgrow(tbSpacer, Priority.ALWAYS);

        Button newBtn = makeToolBtn("/assets/add.png", "New schema");
        newBtn.setOnAction(e -> setCenter(new SchemasAdd(schemasRoot, () -> {
            schemasRoot.refreshData();
            schemasRoot.createTables();
            createSide();
            setCenter(schemasRoot);
        })));

        toolbar.getChildren().addAll(refreshBtn, collapseBtn, sortBtn, tbSpacer, newBtn);

        HBox searchRow = new HBox(6);
        searchRow.setPadding(new Insets(5, 8, 5, 8));
        searchRow.setAlignment(Pos.CENTER_LEFT);
        searchRow.setStyle(
                "-fx-background-color: #252D3D;" +
                        "-fx-background-radius: 6;" +
                        "-fx-border-color: transparent;"
        );

        ImageView searchIcon = new ImageView(new Image(getClass().getResourceAsStream("/assets/search.png")));
        searchIcon.setFitWidth(13);
        searchIcon.setFitHeight(13);
        searchIcon.setPreserveRatio(true);

        TextField searchField = new TextField();
        searchField.setPromptText("Filter schemas…");
        searchField.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-border-color: transparent;" +
                        "-fx-text-fill: #FFFFFF;" +
                        "-fx-prompt-text-fill: #6B7A8D;" +
                        "-fx-padding: 0;"
        );
        HBox.setHgrow(searchField, Priority.ALWAYS);
        searchRow.getChildren().addAll(searchIcon, searchField);

        VBox treeContent = new VBox(0);
        schemaWrappers.clear();

        treeContent.getChildren().add(buildSectionHeader("LOCAL"));
        localSection = new VBox(0);
        for (Schema schema : SchemasRoot.schemas) {         // CHANGED: schemas -> SchemasRoot.schemas
            VBox wrapper = generateTab(schema, false);
            schemaWrappers.add(wrapper);
            localSection.getChildren().add(wrapper);
        }
        treeContent.getChildren().add(localSection);

        if (creds.hasRemote()) {
            SchemasRoot.remoteSchemas = db.SchemasRemote();  // CHANGED: qualified
            Separator divider = new Separator();
            divider.setPadding(new Insets(4, 0, 4, 0));
            treeContent.getChildren().addAll(divider, buildSectionHeader("REMOTE"));
            remoteSection = new VBox(0);
            for (Schema schema : SchemasRoot.remoteSchemas) {
                VBox wrapper = generateTab(schema, true);
                schemaWrappers.add(wrapper);
                remoteSection.getChildren().add(wrapper);
            }
            treeContent.getChildren().add(remoteSection);
        }

        searchField.textProperty().addListener((obs, oldVal, query) -> {
            String lc = query.toLowerCase().trim();
            for (VBox wrapper : schemaWrappers) {
                HBox row = (HBox) wrapper.getChildren().get(0);
                String name = (String) row.getUserData();
                boolean show = lc.isEmpty() || name.toLowerCase().contains(lc);
                wrapper.setVisible(show);
                wrapper.setManaged(show);
            }
        });

        ScrollPane scrollPane = new ScrollPane(treeContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle(
                "-fx-background: #1C2333;" +
                        "-fx-background-color: #1C2333;" +
                        "-fx-border-color: transparent;"
        );
        scrollPane.skinProperty().addListener((obs, o, n) -> {
            if (n != null)
                scrollPane.lookup(".viewport").setStyle("-fx-background-color: #1C2333;");
        });
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        HBox footer = new HBox(8);
        footer.setPadding(new Insets(8, 10, 8, 10));
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.setCursor(Cursor.HAND);
        footer.setStyle(
                "-fx-background-color: #1C2333;" +
                        "-fx-border-color: #2A3244 transparent transparent transparent;" +
                        "-fx-border-width: 1;"
        );
        footer.setOnMouseEntered(e -> footer.setStyle(
                "-fx-background-color: #121723;" +
                        "-fx-border-color: #2A3244 transparent transparent transparent;" +
                        "-fx-background-radius: 5;" +
                        "-fx-border-radius: 5;" +
                        "-fx-border-width: 1;"
        ));
        footer.setOnMouseExited(e -> footer.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-border-color: #2A3244 transparent transparent transparent;" +
                        "-fx-background-radius: 5;" +
                        " -fx-border-radius: 5;" +
                        "-fx-border-width: 1;"
        ));
        footer.setOnMouseClicked(e -> setCenter(new SchemasAdd(schemasRoot, () -> {
            schemasRoot.refreshData();
            schemasRoot.createTables();
            createSide();
            setCenter(schemasRoot);
        })));

        ImageView footerIcon = new ImageView(new Image(getClass().getResourceAsStream("/assets/add.png")));
        footerIcon.setFitWidth(13);
        footerIcon.setFitHeight(13);
        footerIcon.setPreserveRatio(true);

        Label footerLabel = new Label("New schema");
        footerLabel.setStyle("-fx-text-fill: #A0ADB8;");

        footer.getChildren().addAll(footerIcon, footerLabel);

        shell.getChildren().addAll(toolbar, searchRow, scrollPane, footer);
        // REMOVED: setLeft(shell);  -- Root's createSide() handles placement now
        // REMOVED: createTables();  -- caller (createSide/generateTab) triggers this explicitly now
        return shell;
    }

    // ── Section header ─────────────────────────────────────────────────
    private HBox buildSectionHeader(String title) {
        Label label = new Label(title);
        label.setStyle("-fx-text-fill: #4A5568; -fx-font-size: 10; -fx-font-weight: bold;");
        HBox hdr = new HBox(label);
        hdr.setPadding(new Insets(8, 8, 2, 10));
        hdr.setAlignment(Pos.CENTER_LEFT);
        return hdr;
    }

    // ── Toolbar button ─────────────────────────────────────────────────
    private Button makeToolBtn(String iconPath, String tooltipText) {
        ImageView icon = new ImageView(
                new Image(getClass().getResourceAsStream(iconPath))
        );
        icon.setFitWidth(14);
        icon.setFitHeight(14);
        icon.setPreserveRatio(true);

        Button btn = new Button();
        btn.setGraphic(icon);
        btn.setTooltip(new Tooltip(tooltipText));
        btn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-cursor: hand;" +
                        "-fx-padding: 4 6;" +
                        "-fx-background-radius: 4;"
        );
        btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color: #121723;" +
                        "-fx-cursor: hand;" +
                        "-fx-padding: 4 6;" +
                        "-fx-background-radius: 4;"
        ));
        btn.setOnMouseExited(e -> btn.setStyle(
                "-fx-background-color: transparent;" +
                        "-fx-cursor: hand;" +
                        "-fx-padding: 4 6;" +
                        "-fx-background-radius: 4;"
        ));
        return btn;
    }

    // ── generateTab ────────────────────────────────────────────────────
    private VBox generateTab(Schema schema, boolean remote) {
        Image imgRight = new Image(getClass().getResourceAsStream("/assets/right.png"));
        Image imgDown  = new Image(getClass().getResourceAsStream("/assets/down.png"));

        ImageView arrowIcon = new ImageView(imgRight);
        arrowIcon.setFitWidth(10);
        arrowIcon.setFitHeight(10);
        arrowIcon.setPreserveRatio(true);

        StackPane caretBtn = new StackPane(arrowIcon);
        caretBtn.setPrefWidth(28);
        caretBtn.setPrefHeight(30);
        caretBtn.setMinWidth(28);
        caretBtn.setCursor(Cursor.HAND);

        Label nameLabel = new Label(schema.getName());

        Label badge = new Label();
        badge.setStyle(
                "-fx-background-color: #EEEEEE;" +
                        "-fx-background-radius: 10;" +
                        "-fx-text-fill: #888888;" +
                        "-fx-font-size: 10;" +
                        "-fx-padding: 0 5;"
        );
        badge.setVisible(false);

        Region rowSpacer = new Region();
        HBox.setHgrow(rowSpacer, Priority.ALWAYS);

        HBox schemaRow = new HBox(0, caretBtn, nameLabel, rowSpacer, badge);
        schemaRow.setAlignment(Pos.CENTER_LEFT);
        schemaRow.setPadding(new Insets(0, 8, 0, 0));
        schemaRow.setPrefHeight(30);
        schemaRow.setMinHeight(30);
        schemaRow.setCursor(Cursor.HAND);
        schemaRow.setUserData(schema.getName());

        VBox tableList = new VBox(0);
        tableList.setVisible(false);
        tableList.setManaged(false);

        VBox wrapper = new VBox(schemaRow, tableList);

        if (selectedTab == null && !remote) {
            applySelectedStyle(schemaRow, nameLabel);
            selectedTab = schemaRow;
            isRemoteSelected = false;
            schemasRoot.setSelectedSchema(schema.getName(), false); // NEW: keep SchemasRoot in sync
        } else {
            applyDefaultStyle(schemaRow, nameLabel);
        }

        Runnable populateIfEmpty = () -> {
            if (tableList.getChildren().isEmpty()) {
                Schema full = schemasRoot.getTablesFor(schema.getName(), remote); // CHANGED: was db.GetTablesInSchema[Remote] inline
                badge.setText(String.valueOf(full.getTables().size()));
                for (Table table : full.getTables()) {
                    Label tableLabel = new Label(table.getName());
                    tableLabel.setStyle("-fx-text-fill: #8FA0B4;");

                    HBox tableRow = new HBox(tableLabel);
                    tableRow.setPadding(new Insets(0, 8, 0, 38));
                    tableRow.setPrefHeight(26);
                    tableRow.setMinHeight(26);
                    tableRow.setAlignment(Pos.CENTER_LEFT);
                    tableRow.setStyle("-fx-background-color: transparent;");
                    tableRow.setCursor(Cursor.HAND);

                    tableRow.setOnMouseEntered(ev -> tableRow.setStyle("-fx-background-color: #121723; -fx-background-radius: 5; -fx-border-radius: 5;"));
                    tableRow.setOnMouseExited(ev -> tableRow.setStyle("-fx-background-color: transparent;"));

                    ContextMenu tableMenu = new ContextMenu();
                    tableMenu.setStyle(
                            "-fx-background-color: white; -fx-background-radius: 8;" +
                                    "-fx-border-radius: 8; -fx-border-color: #E0E0E0; -fx-padding: 4;" +
                                    "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 10, 0, 0, 2);" +
                                    "-fx-selection-bar: #F5F5F5;" +
                                    "-fx-selection-bar-non-focused: #F5F5F5;"
                    );

                    MenuItem showDataMI  = makeMenuItem("Show Data");
                    String schemaName = schema.getName();
                    if (!remote) {
                        MenuItem crudMI = makeMenuItem("CRUD Operations");
                        MenuItem editMI = makeMenuItem("Edit Table");
                        MenuItem deleteMI = makeMenuItem("Delete Table");
                        deleteMI.setStyle(deleteMI.getStyle() + "-fx-text-fill: #c0392b;");

                        tableMenu.getItems().addAll(showDataMI, crudMI, new SeparatorMenuItem(), editMI, deleteMI);

                        crudMI.setOnAction(ev -> {
                            selectedTab = schemaRow;
                            isRemoteSelected = remote;
                            schemasRoot.setSelectedSchema(schemaName, remote); // NEW
                            setCenter(new TableCRUD(schemasRoot, schemaName, table)); // CHANGED: this -> schemasRoot
                        });

                        editMI.setOnAction(ev -> {
                            selectedTab = schemaRow;
                            isRemoteSelected = remote;
                            schemasRoot.setSelectedSchema(schemaName, remote); // NEW
                            Schema fullSchema = db.GetTablesInSchema(schemaName);
                            List<String> pks = new ArrayList<>();
                            for (Table t : fullSchema.getTables()) {
                                for (Field f : t.getFields()) {
                                    if (f.isPrimary()) pks.add(t.getName() + "(" + f.getName() + ")");
                                }
                            }
                            TableEdit editTable = new TableEdit(schemasRoot, schemaName, table, pks);
                            schemasRoot.setRight(editTable);
                            editTable.setPrefWidth(400);
                        });

                        deleteMI.setOnAction(ev -> {
                            Dialog<ButtonType> dialog = new Dialog<>();
                            dialog.setTitle("Delete Table");
                            dialog.setHeaderText(null);

                            ButtonType deleteButtonType = new ButtonType("Delete", ButtonBar.ButtonData.OK_DONE);
                            dialog.getDialogPane().getButtonTypes().addAll(deleteButtonType, ButtonType.CANCEL);

                            Label headerLabel = new Label("Delete Table");
                            headerLabel.setTextFill(Color.WHITE);
                            headerLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
                            HBox headerBox = new HBox(headerLabel);
                            headerBox.setPadding(new Insets(10, 12, 10, 12));
                            headerBox.setStyle("-fx-background-color: #2E5A47; -fx-background-radius: 8 8 0 0;");

                            Label warning = new Label("This will permanently delete the table and all its data.");
                            warning.setStyle("-fx-text-fill: #444;");
                            Label instruction = new Label("Type '" + table.getName() + "' to confirm:");
                            instruction.setStyle("-fx-font-weight: 600;");
                            TextField input = new TextField();
                            input.setPromptText(table.getName());
                            input.setStyle("-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: #CCCCCC; -fx-padding: 6;");

                            VBox content = new VBox(10);
                            content.setPadding(new Insets(12));
                            String connections = db.getTableConnections(schemaName, table.getName());
                            if (!connections.isEmpty()) {
                                Label connLabel = new Label("Warning: Foreign Key Connections Found");
                                connLabel.setStyle("-fx-text-fill: #c0392b; -fx-font-weight: bold;");

                                TextArea connArea = new TextArea(connections);
                                connArea.setEditable(false);
                                connArea.setPrefHeight(100);
                                connArea.setStyle("-fx-font-family: 'Monospace'; -fx-font-size: 11px;");
                                content.getChildren().addAll(connLabel, connArea, new Separator());
                            }
                            content.getChildren().addAll(warning, instruction, input);

                            VBox dialogWrapper = new VBox(headerBox, content);
                            dialogWrapper.setStyle("-fx-background-color: white; -fx-background-radius: 8;");
                            dialog.getDialogPane().setContent(dialogWrapper);

                            Node deleteButton = dialog.getDialogPane().lookupButton(deleteButtonType);
                            Node cancelButton = dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
                            deleteButton.setDisable(true);
                            deleteButton.setStyle("-fx-background-color: #CCCCCC; -fx-text-fill: white; -fx-background-radius: 6;");
                            cancelButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #2E5A47; -fx-font-weight: 600;");

                            input.textProperty().addListener((obs, oldVal, newVal) -> {
                                boolean valid = newVal.equals(table.getName());
                                deleteButton.setDisable(!valid);
                                deleteButton.setStyle(valid
                                        ? "-fx-background-color: #c0392b; -fx-text-fill: white; -fx-background-radius: 6; -fx-cursor: hand;"
                                        : "-fx-background-color: #CCCCCC; -fx-text-fill: white; -fx-background-radius: 6;");
                            });

                            Optional<ButtonType> result = dialog.showAndWait();
                            if (result.isPresent() && result.get() == deleteButtonType) {
                                db.deleteTable(new Schema(schemaName), table);
                                schemasRoot.createTables(); // CHANGED: was createTables()
                                schemasRoot.refreshData();  // CHANGED: was refresh()
                                createSide();                // NEW: rebuild the tree
                            }
                        });
                    } else {
                        tableMenu.getItems().add(showDataMI);
                    }

                    showDataMI.setOnAction(ev -> {
                        if (selectedTab != schemaRow) {
                            if (selectedTab != null) {
                                Label prevName = (Label) selectedTab.getChildren().get(1);
                                applyDefaultStyle(selectedTab, prevName);
                            }
                            applySelectedStyle(schemaRow, nameLabel);
                            selectedTab = schemaRow;
                            isRemoteSelected = remote;
                            schemasRoot.setSelectedSchema(schemaName, remote); // NEW
                            schemasRoot.createTables(); // CHANGED
                        }
                        schemasRoot.showTableData(schemaName, table); // CHANGED: needs to be public — see note below
                    });

                    tableRow.setOnMouseClicked(ev -> {
                        if (ev.getButton() == MouseButton.SECONDARY) {
                            tableMenu.show(tableRow, ev.getScreenX(), ev.getScreenY());
                        }
                        ev.consume();
                    });

                    tableList.getChildren().add(tableRow);
                }
            }
        };

        Runnable expand = () -> {
            populateIfEmpty.run();
            arrowIcon.setImage(imgDown);
            badge.setVisible(true);
            tableList.setVisible(true);
            tableList.setManaged(true);
        };

        Runnable collapse = () -> {
            arrowIcon.setImage(imgRight);
            badge.setVisible(false);
            tableList.setVisible(false);
            tableList.setManaged(false);
        };

        caretBtn.setOnMouseClicked(e -> {
            if (tableList.isVisible()) collapse.run();
            else                       expand.run();
            e.consume();
        });

        schemaRow.setOnMouseEntered(e -> {
            if (selectedTab != schemaRow)
                schemaRow.setStyle("-fx-background-color: #121723; -fx-background-radius: 5; -fx-border-radius: 5;");
        });
        schemaRow.setOnMouseExited(e -> {
            if (selectedTab != schemaRow)
                applyDefaultStyle(schemaRow, nameLabel);
        });

        schemaRow.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                boolean alreadyHome = "Schemas".equals(activeMenu) && getCenter() == schemasRoot;
                if (!alreadyHome) {
                    activeMenu = "Schemas";
                    setCenter(schemasRoot);
                }

                if (selectedTab != null && selectedTab != schemaRow) {
                    Label prevName = (Label) selectedTab.getChildren().get(1);
                    applyDefaultStyle(selectedTab, prevName);
                }
                applySelectedStyle(schemaRow, nameLabel);
                selectedTab      = schemaRow;
                isRemoteSelected = remote;
                schemasRoot.setSelectedSchema(schema.getName(), remote);
                schemasRoot.createTables();
                e.consume();
            } else if (e.getButton() == MouseButton.SECONDARY) {
                ContextMenu contextMenu = new ContextMenu();
                contextMenu.setStyle(
                        "-fx-background-color: white; -fx-background-radius: 8;" +
                                "-fx-border-radius: 8; -fx-border-color: #E0E0E0; -fx-padding: 4;" +
                                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.15), 10, 0, 0, 2);" +
                                "-fx-selection-bar: #F5F5F5;" +
                                "-fx-selection-bar-non-focused: #F5F5F5;"
                );

                if (remote) {
                    MenuItem cloneItem = new MenuItem("Clone to Local");
                    cloneItem.setOnAction(event -> {
                        new Thread(() -> {
                            String result = db.CloneSchemaFromRemote(schema.getName());
                            Platform.runLater(() -> {
                                if (result != null && !result.toLowerCase().contains("fail") && !result.toLowerCase().contains("error")) {
                                    SchemasRoot.markRemoteLinked(schema.getName());
                                }
                                schemasRoot.refreshData();
                                createSide();
                                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                                alert.setTitle("Clone Complete");
                                alert.setHeaderText(null);
                                alert.setContentText(result);
                                alert.showAndWait();
                            });
                        }).start();
                    });
                    contextMenu.getItems().add(cloneItem);

                } else {
                    if (creds.hasRemote() && SchemasRoot.isRemoteLinked(schema.getName())) {
                        MenuItem pushItem = new MenuItem("Push to Remote");
                        pushItem.setOnAction(event -> {
                            new Thread(() -> {
                                String result = db.PushSchemaToRemote(schema.getName());
                                Platform.runLater(() -> {
                                    // TODO once push is fully implemented: mark linked on first successful push
                                    // so schemas that started local-only also unlock this item going forward.
                                    // if (result != null && !result.toLowerCase().contains("fail")) {
                                    //     SchemasRoot.markRemoteLinked(schema.getName());
                                    // }
                                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                                    alert.setTitle("Push to Remote");
                                    alert.setHeaderText(null);
                                    alert.setContentText(result);
                                    alert.showAndWait();
                                });
                            }).start();
                        });
                        contextMenu.getItems().add(pushItem);
                    }

                    Menu generateLoginMenu = new Menu("Generate Login Code");
                    generateLoginMenu.setStyle("-fx-font-size: 12px; -fx-padding: 6 12 6 12;");
                    Schema full = db.GetTablesInSchema(schema.getName());
                    for (Table t : full.getTables()) {
                        MenuItem tableOption = makeMenuItem(t.getName());
                        tableOption.setOnAction(event -> {
                            if (selectedTab != null && selectedTab != schemaRow) {
                                Label prevName = (Label) selectedTab.getChildren().get(1);
                                applyDefaultStyle(selectedTab, prevName);
                            }
                            applySelectedStyle(schemaRow, nameLabel);
                            selectedTab = schemaRow;
                            isRemoteSelected = false;
                            schemasRoot.setSelectedSchema(schema.getName(), false);
                            setCenter(new LoginGen(schemasRoot, schema.getName(), t, () -> {
                                schemasRoot.createTables();
                                setCenter(schemasRoot);
                            }));
                        });
                        generateLoginMenu.getItems().add(tableOption);
                    }
                    contextMenu.getItems().add(generateLoginMenu);

                    MenuItem editSchemaItem = new MenuItem("Edit Schema");
                    editSchemaItem.setStyle("-fx-font-size: 12px; -fx-padding: 6 12 6 12;");
                    editSchemaItem.setOnAction(event -> {
                        setCenter(new SchemasEdit(this, schemasRoot, schema.getName(), () -> {
                            schemasRoot.refreshData();
                            schemasRoot.createTables();
                            createSide();
                            setCenter(schemasRoot);
                        }));
                    });
                    contextMenu.getItems().add(editSchemaItem);

                    MenuItem deleteItem = new MenuItem("Delete " + schema.getName());
                    deleteItem.setStyle("-fx-font-size: 12px; -fx-padding: 6 12 6 12;");
                    deleteItem.setOnAction(event -> {
                        Dialog<ButtonType> dialog = new Dialog<>();
                        dialog.setTitle("Delete Schema");
                        dialog.setHeaderText(null);

                        ButtonType deleteButtonType = new ButtonType("Delete", ButtonBar.ButtonData.OK_DONE);
                        dialog.getDialogPane().getButtonTypes().addAll(deleteButtonType, ButtonType.CANCEL);

                        Label header = new Label("Delete Schema");
                        header.setTextFill(Color.WHITE);
                        header.setFont(Font.font("System", FontWeight.BOLD, 14));
                        HBox headerBox = new HBox(header);
                        headerBox.setPadding(new Insets(10, 12, 10, 12));
                        headerBox.setStyle("-fx-background-color: #2E5A47; -fx-background-radius: 8 8 0 0;");

                        Label warning = new Label("This will permanently delete the schema and all its tables.");
                        warning.setStyle("-fx-text-fill: #444;");
                        Label instruction = new Label("Type '" + schema.getName() + "' to confirm:");
                        instruction.setStyle("-fx-font-weight: 600;");
                        TextField input = new TextField();
                        input.setPromptText(schema.getName());
                        input.setStyle("-fx-background-radius: 6; -fx-border-radius: 6; -fx-border-color: #CCCCCC; -fx-padding: 6;");

                        VBox content = new VBox(10, warning, instruction, input);
                        content.setPadding(new Insets(12));
                        VBox dialogWrapper = new VBox(headerBox, content);
                        dialogWrapper.setStyle("-fx-background-color: white; -fx-background-radius: 8;");
                        dialog.getDialogPane().setContent(dialogWrapper);

                        Node deleteButton = dialog.getDialogPane().lookupButton(deleteButtonType);
                        Node cancelButton = dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
                        deleteButton.setDisable(true);
                        deleteButton.setStyle("-fx-background-color: #CCCCCC; -fx-text-fill: white; -fx-background-radius: 6;");
                        cancelButton.setStyle("-fx-background-color: transparent; -fx-text-fill: #2E5A47; -fx-font-weight: 600;");

                        input.textProperty().addListener((obs, oldVal, newVal) -> {
                            boolean valid = newVal.equals(schema.getName());
                            deleteButton.setDisable(!valid);
                            deleteButton.setStyle(valid
                                    ? "-fx-background-color: #c0392b; -fx-text-fill: white; -fx-background-radius: 6;"
                                    : "-fx-background-color: #CCCCCC; -fx-text-fill: white; -fx-background-radius: 6;");
                        });

                        Optional<ButtonType> result = dialog.showAndWait();
                        if (result.isPresent() && result.get() == deleteButtonType) {
                            db.deleteSchema(schema);
                            ((Pane) wrapper.getParent()).getChildren().remove(wrapper);
                            schemaWrappers.remove(wrapper);
                            SchemasRoot.clearRemoteLink(schema.getName());   // NEW
                            schemasRoot.refreshData();
                            createSide();
                        }
                    });
                    contextMenu.getItems().add(deleteItem);
                }

                contextMenu.show(schemaRow, e.getScreenX(), e.getScreenY());
            }
        });

        return wrapper;
    }

    private MenuItem makeMenuItem(String text) {
        MenuItem item = new MenuItem(text);
        item.setStyle("-fx-font-size: 12px; -fx-padding: 6 12 6 12;");
        return item;
    }

    // ── Style helpers ──────────────────────────────────────────────────
    private void applySelectedStyle(HBox row, Label label) {
        row.setStyle("-fx-background-color: #080C14; -fx-background-radius: 5; -fx-border-radius: 5;");
        label.setStyle("-fx-text-fill: #FFFFFF; -fx-font-weight: bold;");
    }

    private void applyDefaultStyle(HBox row, Label label) {
        row.setStyle("-fx-background-color: transparent;");
        label.setStyle("-fx-text-fill: #C8D0D8; -fx-font-weight: normal;");
    }

    private void resortSection(VBox section, boolean ascending) {
        List<Node> items = new ArrayList<>(section.getChildren());
        items.sort((a, b) -> {
            String nameA = (String)((HBox)((VBox)a).getChildren().get(0)).getUserData();
            String nameB = (String)((HBox)((VBox)b).getChildren().get(0)).getUserData();
            return ascending ? nameA.compareToIgnoreCase(nameB)
                    : nameB.compareToIgnoreCase(nameA);
        });
        section.getChildren().setAll(items);
    }
}