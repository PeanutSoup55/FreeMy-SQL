package GUI;

import globalfuncs.db;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.List;

public class Query extends VBox {

    private static final List<String> history = new ArrayList<>();
    private static int historyIndex = -1;
    private static String savedQuery = "";
    private final VBox historyList = new VBox(4);
    private final TextArea sqlInput;
    private TextArea outputArea = null;
    private final Label statusLabel;
    private final ScrollPane histScroll;

    public Query() {
        setSpacing(0);
        setStyle("-fx-background-color: #F2F4F2;");

        Text title = new Text("SQL Query");
        title.setFont(Font.font("System", FontWeight.BOLD, 20));
        title.setFill(Color.web("#1E3D30"));

        HBox header = new HBox(title);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(20, 28, 16, 28));
        header.setStyle("-fx-background-color: #F2F4F2;");

        Label editorLabel = new Label("Query");
        editorLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 11; -fx-font-weight: bold;");

        sqlInput = new TextArea();
        sqlInput.setPromptText("Write your SQL here...  (Ctrl+Enter to run)");
        sqlInput.setPrefRowCount(22);
        sqlInput.setText(savedQuery);
        sqlInput.setStyle("-fx-font-family: 'Monospace';" +
                "-fx-font-size: 13px;" +
                "-fx-background-color: white;" +
                "-fx-border-color: transparent;" +
                "-fx-padding: 14;" +
                "-fx-text-fill: #1E3D30;"
        );
        sqlInput.textProperty().addListener((obs, oldVal, newVal) -> savedQuery = newVal);
        this.addEventFilter(KeyEvent.KEY_PRESSED, e -> {
            if (e.isControlDown() && e.getCode() == KeyCode.ENTER) { e.consume(); runQuery(); }
            if (e.isControlDown() && e.getCode() == KeyCode.DOWN)    { e.consume(); navigateHistory(-1); }
            if (e.isControlDown() && e.getCode() == KeyCode.UP)  { e.consume(); navigateHistory(1); }
        });
        HBox.setHgrow(sqlInput, Priority.ALWAYS);

        VBox inputCol = new VBox(6, editorLabel, sqlInput);
        VBox.setVgrow(sqlInput, Priority.ALWAYS);
        HBox.setHgrow(inputCol, Priority.ALWAYS);

        Label histLabel = new Label("History");
        histLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 11; -fx-font-weight: bold;");

        historyList.setPadding(new Insets(4, 0, 4, 0));
        historyList.setStyle("-fx-background-color: transparent;");
        histScroll = new ScrollPane(historyList);
        refreshHistoryPanel();

        histScroll.setFitToWidth(true);
        histScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        histScroll.setStyle("-fx-background-color: transparent;" +
                "-fx-background: transparent;" +
                "-fx-border-color: transparent;"
        );
        histScroll.setMaxHeight(Region.USE_PREF_SIZE);
        histScroll.setPrefHeight(380);
        histScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        VBox.setVgrow(histScroll, Priority.ALWAYS);

        VBox histCol = new VBox(6, histLabel, histScroll);
        VBox.setVgrow(histScroll, Priority.ALWAYS);
        HBox.setHgrow(histCol, Priority.ALWAYS);
        HBox editorRow = new HBox(14, inputCol, histCol);
        HBox.setHgrow(inputCol, Priority.ALWAYS);
        HBox.setHgrow(histCol, Priority.ALWAYS);
        VBox.setVgrow(editorRow, Priority.ALWAYS);

        VBox editorCard = new VBox(editorRow);
        editorCard.setSpacing(6);
        editorCard.setPadding(new Insets(16, 20, 16, 20));
        editorCard.setStyle("-fx-background-color: white;" +
                "-fx-background-radius: 12;" +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.07), 14, 0, 0, 3);"
        );

        Button runBtn = new Button("▶  Run");
        runBtn.setStyle("-fx-background-color: #2E5A47;" +
                "-fx-text-fill: white;" +
                "-fx-font-weight: bold;" +
                "-fx-background-radius: 8;" +
                "-fx-cursor: hand;" +
                "-fx-padding: 10 28;" +
                "-fx-font-size: 13;"
        );
        runBtn.setOnAction(e -> runQuery());

        Button clearBtn = new Button("Clear");
        clearBtn.setStyle("-fx-background-color: white;" +
                "-fx-text-fill: #2E5A47;" +
                "-fx-border-color: #2E5A47;" +
                "-fx-border-radius: 8;" +
                "-fx-background-radius: 8;" +
                "-fx-font-weight: bold;" +
                "-fx-cursor: hand;" +
                "-fx-padding: 10 28;" +
                "-fx-font-size: 13;"
        );
        clearBtn.setOnAction(e -> {
            sqlInput.clear();
            outputArea.clear();
            savedQuery = "";
            setStatus("", false);
        });

        Button histPrev = new Button("↓ Prev");
        histPrev.setStyle("-fx-background-color: white;" +
                "-fx-text-fill: #555;" +
                "-fx-border-color: #CCCCCC;" +
                "-fx-border-radius: 8;" +
                "-fx-background-radius: 8;" +
                "-fx-cursor: hand;" +
                "-fx-padding: 10 16;" +
                "-fx-font-size: 12;"
        );
        histPrev.setOnAction(e -> navigateHistory(-1));

        Button histNext = new Button("↑ Next");
        histNext.setStyle("-fx-background-color: white;" +
                "-fx-text-fill: #555;" +
                "-fx-border-color: #CCCCCC;" +
                "-fx-border-radius: 8;" +
                "-fx-background-radius: 8;" +
                "-fx-cursor: hand;" +
                "-fx-padding: 10 16;" +
                "-fx-font-size: 12;"
        );
        histNext.setOnAction(e -> navigateHistory(1));

        statusLabel = new Label("");
        statusLabel.setStyle("-fx-font-size: 12; -fx-font-weight: bold;");

        Region actionSpacer = new Region();
        HBox.setHgrow(actionSpacer, Priority.ALWAYS);

        HBox actionRow = new HBox(12, runBtn, clearBtn, histPrev, histNext, actionSpacer, statusLabel);
        actionRow.setAlignment(Pos.CENTER_LEFT);

        Label outputLabel = new Label("Output");
        outputLabel.setStyle("-fx-text-fill: #888; -fx-font-size: 11; -fx-font-weight: bold;");

        outputArea = new TextArea();
        outputArea.setEditable(false);
        outputArea.setPromptText("Results will appear here...");
        outputArea.setStyle("-fx-font-family: 'Monospace';" +
                "-fx-font-size: 12px;" +
                "-fx-background-color: #FAFAFA;" +
                "-fx-border-color: transparent;" +
                "-fx-text-fill: #2C2C2C;" +
                "-fx-padding: 14;"
        );
        VBox.setVgrow(outputArea, Priority.ALWAYS);

        VBox outputCard = new VBox(outputLabel, outputArea);
        outputCard.setSpacing(6);
        outputCard.setPadding(new Insets(16, 20, 16, 20));
        outputCard.setStyle("-fx-background-color: white;" +
                "-fx-background-radius: 12;" +
                "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.07), 14, 0, 0, 3);"
        );
        VBox.setVgrow(outputCard, Priority.ALWAYS);

        Label hint = new Label("Ctrl+Enter  run  ·  Ctrl+↑ / Ctrl+↓  history");
        hint.setStyle("-fx-text-fill: #AAAAAA; -fx-font-size: 11;");
        HBox hintBar = new HBox(hint);
        hintBar.setAlignment(Pos.CENTER_RIGHT);
        hintBar.setPadding(new Insets(0, 28, 0, 0));

        VBox body = new VBox(16, editorCard, hintBar, actionRow, outputCard);
        body.setPadding(new Insets(0, 28, 28, 28));
        VBox.setVgrow(body, Priority.ALWAYS);
        VBox.setVgrow(outputCard, Priority.ALWAYS);

        getChildren().addAll(header, body);
        VBox.setVgrow(this, Priority.ALWAYS);
    }

    private void runQuery() {
        String sql = sqlInput.getText().trim();
        if (sql.isEmpty()) { setStatus("No query to run.", false); return; }

        if (history.isEmpty() || !history.getLast().equals(sql)) {
            history.add(sql);
        }
        historyIndex = history.size();
        refreshHistoryPanel();

        setStatus("Running...", false);
        String result = db.ExecuteRaw(sql);
        outputArea.setText(result);
        setStatus(result.startsWith("ERROR:") ? "✕  Error" : "✓  Done", result.startsWith("ERROR:"));
    }

    private void navigateHistory(int direction) {
        if (history.isEmpty()) return;
        historyIndex = Math.max(0, Math.min(history.size() - 1, historyIndex + direction));
        sqlInput.setText(history.get(historyIndex));
        sqlInput.positionCaret(sqlInput.getText().length());
        refreshHistoryPanel();
    }

    private void refreshHistoryPanel() {
        if (histScroll == null) return;
        historyList.getChildren().clear();

        if (history.isEmpty()) {
            Label empty = new Label("No history yet.");
            empty.setStyle("-fx-text-fill: #CCCCCC; -fx-font-size: 11; -fx-padding: 6 8;");
            historyList.getChildren().add(empty);
            return;
        }

        for (int i = history.size() - 1; i >= 0; i--) {
            final int index = i;
            String sql = history.get(i);

            String display = sql.replace("\n", " ").strip();
            if (display.length() > 60) display = display.substring(0, 58) + "…";
            boolean isCurrent = (index == historyIndex);

            Label entry = new Label(display);
            entry.setWrapText(false);
            entry.setCursor(javafx.scene.Cursor.HAND);
            entry.setMaxWidth(Double.MAX_VALUE);
            entry.setPadding(new Insets(6, 10, 6, 10));
            HBox.setHgrow(entry, Priority.ALWAYS);

            if (isCurrent) {
                entry.setStyle("-fx-font-family: Monospace;" +
                        "-fx-font-size: 11;" +
                        "-fx-text-fill: #1E3D30;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-color: #E8F0ED;" +
                        "-fx-background-radius: 6;"
                );
            } else {
                entry.setStyle("-fx-font-family: Monospace;" +
                        "-fx-font-size: 11;" +
                        "-fx-text-fill: #BBBBBB;" +
                        "-fx-background-color: transparent;" +
                        "-fx-background-radius: 6;"
                );
            }

            entry.setOnMouseClicked(e -> {
                historyIndex = index;
                sqlInput.setText(history.get(index));
                sqlInput.positionCaret(sqlInput.getText().length());
                refreshHistoryPanel();
            });

            historyList.getChildren().add(entry);
        }
        Platform.runLater(() -> {
            int activePos = history.size() - 1 - historyIndex; // inverted because newest is at top
            if (activePos >= 0 && activePos < historyList.getChildren().size()) {
                Node activeNode = historyList.getChildren().get(activePos);
                histScroll.layout();
                double scrollHeight = historyList.getHeight() - histScroll.getViewportBounds().getHeight();
                if (scrollHeight > 0) {
                    double nodeY = activeNode.getBoundsInParent().getMinY();
                    histScroll.setVvalue(nodeY / scrollHeight);
                }
            }
        });
    }

    private void setStatus(String msg, boolean isError) {
        statusLabel.setText(msg);
        statusLabel.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: " +
                (isError ? "#c0392b;" : "#2E5A47;")
        );
    }

}