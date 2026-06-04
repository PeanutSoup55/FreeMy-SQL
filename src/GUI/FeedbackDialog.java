package GUI;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class FeedbackDialog extends BorderPane {

    private int selectedStars = 0;
    private final Label[] starLabels = new Label[5];
    private Label inlineError;
    private Button submitBtn;

    private static final String STAR_FILLED = "★";
    private static final String STAR_EMPTY  = "☆";
    private static final String GREEN       = "#2E5A47";
    private static final String STAR_COLOR  = "#f5a623";

    public FeedbackDialog() {
        setPadding(new Insets(24, 28, 24, 28));
        setStyle("-fx-background-color: #F2F4F2;");

        // --- Card ---
        VBox card = new VBox(24);
        card.setPadding(new Insets(36));
        card.setMaxWidth(520);
        card.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 14;" +
                        "-fx-border-color: #D0D8D0;" +
                        "-fx-border-radius: 14;" +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.07), 10, 0, 0, 3);"
        );

        // --- Header inside card ---
        Text title = new Text("Send Feedback");
        title.setFont(Font.font("System", FontWeight.BOLD, 22));
        title.setFill(Color.web("#1E3D30"));

        Label subtitle = new Label("How is Free My Query working for you?");
        subtitle.setFont(Font.font("System", 13));
        subtitle.setTextFill(Color.web("#777777"));

        VBox titleBlock = new VBox(4, title, subtitle);

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #E8EDE8;");

        // --- Star rating ---
        Label starsHeading = smallLabel("Your Rating");

        HBox starsRow = new HBox(6);
        starsRow.setAlignment(Pos.CENTER_LEFT);

        for (int i = 0; i < 5; i++) {
            Label star = new Label(STAR_EMPTY);
            star.setFont(Font.font("System", 34));
            star.setTextFill(Color.web("#D8D8D8"));
            star.setStyle("-fx-cursor: hand; -fx-focus-color: transparent; -fx-faint-focus-color: transparent;");
            final int index = i + 1;

            star.setOnMouseEntered(e -> highlightStars(index));
            star.setOnMouseExited(e -> highlightStars(selectedStars));
            star.setOnMouseClicked(e -> {
                selectedStars = index;
                highlightStars(selectedStars);
            });

            starLabels[i] = star;
            starsRow.getChildren().add(star);
        }

        VBox starsBlock = new VBox(8, starsHeading, starsRow);

        // --- Message ---
        Label messageHeading = smallLabel("Your Message");

        TextArea messageArea = new TextArea();
        messageArea.setPromptText("Tell us what you think, report a bug, or suggest a feature...");
        messageArea.setPrefRowCount(6);
        messageArea.setWrapText(true);
        messageArea.setStyle(
                "-fx-border-color: #D0D8D0;" +
                        "-fx-border-radius: 8;" +
                        "-fx-background-radius: 8;" +
                        "-fx-font-size: 13px;" +
                        "-fx-padding: 10;" +
                        "-fx-focus-color: " + GREEN + ";" +
                        "-fx-faint-focus-color: transparent;"
        );

        VBox messageBlock = new VBox(8, messageHeading, messageArea);

        // --- Inline error ---
        inlineError = new Label();
        inlineError.setTextFill(Color.web("#d93025"));
        inlineError.setFont(Font.font("System", 12));
        inlineError.setVisible(false);
        inlineError.setManaged(false);

        // --- Buttons ---
        submitBtn = filledBtn("Send Feedback");
        Button clearBtn = outlineBtn("Clear");

        clearBtn.setOnAction(e -> {
            messageArea.clear();
            selectedStars = 0;
            highlightStars(0);
            hideError();
            submitBtn.setText("Send Feedback");
            submitBtn.setDisable(false);
            submitBtn.setStyle(filledStyle());
        });

        submitBtn.setOnAction(e -> {
            if (selectedStars == 0) {
                showError("Please select a star rating.");
                return;
            }
            if (messageArea.getText().trim().isEmpty()) {
                showError("Please write a message before submitting.");
                return;
            }
            hideError();
            submitBtn.setDisable(true);
            submitBtn.setText("Sending…");
            sendFeedback(selectedStars, messageArea.getText().trim(), messageArea);
        });

        HBox buttonRow = new HBox(10, submitBtn, clearBtn);
        buttonRow.setAlignment(Pos.CENTER_LEFT);

        card.getChildren().addAll(
                titleBlock,
                sep,
                starsBlock,
                messageBlock,
                inlineError,
                buttonRow
        );

        // --- Center the card on the page ---
        StackPane centered = new StackPane(card);
        StackPane.setAlignment(card, Pos.CENTER);
        setCenter(centered);
    }

    private void highlightStars(int count) {
        for (int i = 0; i < 5; i++) {
            if (i < count) {
                starLabels[i].setText(STAR_FILLED);
                starLabels[i].setTextFill(Color.web(STAR_COLOR));
            } else {
                starLabels[i].setText(STAR_EMPTY);
                starLabels[i].setTextFill(Color.web("#D8D8D8"));
            }
        }
    }

    private void showError(String message) {
        inlineError.setText("⚠  " + message);
        inlineError.setVisible(true);
        inlineError.setManaged(true);
    }

    private void hideError() {
        inlineError.setVisible(false);
        inlineError.setManaged(false);
    }

    private void sendFeedback(int stars, String message, TextArea messageArea) {
        String safeMessage = message
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "");

        String json = String.format(
                "{\"stars\": %d, \"message\": \"%s\"}",
                stars, safeMessage
        );

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://freemyquery.com/api/feedback"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        CompletableFuture.runAsync(() -> {
            try {
                HttpResponse<String> response = client.send(
                        request, HttpResponse.BodyHandlers.ofString()
                );
                boolean success = response.statusCode() == 200;
                Platform.runLater(() -> {
                    if (success) {
                        messageArea.clear();
                        selectedStars = 0;
                        highlightStars(0);
                        submitBtn.setDisable(false);
                        submitBtn.setText("Feedback Sent!");
                        submitBtn.setStyle(
                                "-fx-background-color: #3A7D5A;" +
                                        "-fx-text-fill: white;" +
                                        "-fx-background-radius: 8;" +
                                        "-fx-font-weight: bold;" +
                                        "-fx-cursor: hand;" +
                                        "-fx-padding: 10 20;" +
                                        "-fx-font-size: 13;" +
                                        "-fx-focus-color: transparent;" +
                                        "-fx-faint-focus-color: transparent;"
                        );
                    } else {
                        submitBtn.setDisable(false);
                        submitBtn.setText("Send Feedback");
                        showError("Failed to send. Please check your connection and try again.");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    submitBtn.setDisable(false);
                    submitBtn.setText("Send Feedback");
                    showError("Failed to send. Please check your connection and try again.");
                });
            }
        });
    }

    private static String filledStyle() {
        return "-fx-background-color: #2E5A47;" +
                "-fx-text-fill: white;" +
                "-fx-background-radius: 8;" +
                "-fx-font-weight: bold;" +
                "-fx-cursor: hand;" +
                "-fx-padding: 10 20;" +
                "-fx-font-size: 13;" +
                "-fx-focus-color: transparent;" +
                "-fx-faint-focus-color: transparent;";
    }

    private static Button filledBtn(String text) {
        Button b = new Button(text);
        b.setStyle(filledStyle());
        return b;
    }

    private static Button outlineBtn(String text) {
        Button b = new Button(text);
        b.setStyle(
                "-fx-background-color: white;" +
                        "-fx-text-fill: #2E5A47;" +
                        "-fx-border-color: #2E5A47;" +
                        "-fx-border-radius: 8;" +
                        "-fx-background-radius: 8;" +
                        "-fx-font-weight: bold;" +
                        "-fx-cursor: hand;" +
                        "-fx-padding: 10 20;" +
                        "-fx-font-size: 13;" +
                        "-fx-focus-color: transparent;" +
                        "-fx-faint-focus-color: transparent;"
        );
        return b;
    }

    private static Label smallLabel(String text) {
        Label l = new Label(text);
        l.setTextFill(Color.web("#555555"));
        l.setFont(Font.font("System", FontWeight.BOLD, 12));
        return l;
    }
}