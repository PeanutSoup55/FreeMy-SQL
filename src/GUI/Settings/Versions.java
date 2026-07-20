package GUI.Settings;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.List;

public class Versions extends BorderPane {

    private record VersionEntry(String version, String tag, String date, List<String> notes) {}

    // Newest first. Placeholder notes — swap in your real changelog.
    private static final List<VersionEntry> ENTRIES = List.of(
            new VersionEntry("v2.0", "Full Release", "July 20, 2026", List.of(
                    "First stable, full release of Free My Query.",
                    "Additions:",
                    "   - new Account page with live subscription status and billing management",
                    "   - Stripe billing portal integration for updating payment methods and viewing invoices",
                    "   - new in-app Documentation page covering Getting Started, Schemas, Tables, Data, and SSH",
                    "   - new Version News page to track release history",
                    "   - expanded theming system with dedicated text colours for better readability across all palettes",
                    "   - floating, rounded sidebar navigation on the Documentation page",
                    "Improvements:",
                    "   - offline grace period messaging now shown clearly when subscription status can't be verified",
                    "   - theme changes now apply live across all settings pages without needing to reopen them"
            )),
            new VersionEntry("v1.2", "Beta", "June 8, 2026", List.of(
                    "Bugs fixed:",
                    "   - tables now stay where they are placed after moving them",
                    "   - fixed CRUDing to tabels" ,
                    "   - fixed broken editing to tabels",
                    "   - added an integrated feedback system",
                    "   - fixed a downloading error issue",
                    "   - logs now refresh automatically",
                    "Additions: ",
                    "   - added ssh portal",
                    "   - added new sidebar for schemas with table dropdowns",
                    "   - added better styling"
            )),
            new VersionEntry("v1.1", "Beta", "June 1, 2026", List.of(
                    "Initial beta release."
            ))
    );

    public Versions() {
        rebuild();
        Theme.registerThemeListener(this, this::rebuild);
    }

    private void rebuild() {
        Platform.runLater(() -> {
            setStyle("-fx-background-color: " + Theme.colour2 + ";");
            setCenter(buildContent());
        });
    }

    private ScrollPane buildContent() {
        Label title = new Label("Version News");
        title.setFont(Font.font("System", FontWeight.BOLD, 24));
        title.setTextFill(Color.web(Theme.colour6));

        Label subtitle = new Label("What's changed across recent releases");
        subtitle.setFont(Font.font("System", 13));
        subtitle.setTextFill(Color.web(Theme.colour7));

        VBox header = new VBox(4, title, subtitle);

        VBox postList = new VBox(20);
        for (VersionEntry entry : ENTRIES) {
            postList.getChildren().add(buildPost(entry));
        }

        VBox column = new VBox(28, header, postList);
        column.setPadding(new Insets(40, 48, 48, 48));
        column.setMaxWidth(720);

        ScrollPane scroll = new ScrollPane(column);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: " + Theme.colour2 + "; -fx-background-color: transparent;");
        return scroll;
    }

    private VBox buildPost(VersionEntry entry) {
        Label versionLabel = new Label(entry.version());
        versionLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        versionLabel.setTextFill(Color.web(Theme.colour6));

        Label tagLabel = new Label(entry.tag());
        tagLabel.setPadding(new Insets(3, 10, 3, 10));
        tagLabel.setStyle(tagStyle(entry.tag()));

        Label dateLabel = new Label(entry.date());
        dateLabel.setFont(Font.font("System", 12));
        dateLabel.setTextFill(Color.web(Theme.colour7));

        HBox headerRow = new HBox(10, versionLabel, tagLabel);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        VBox notesList = new VBox(6);
        for (String note : entry.notes()) {
            HBox bulletRow = new HBox(8);
            bulletRow.setAlignment(Pos.TOP_LEFT);

            Label bullet = new Label("•");
            bullet.setTextFill(Color.web(Theme.colour7));

            Label noteLabel = new Label(note);
            noteLabel.setWrapText(true);
            noteLabel.setTextFill(Color.web(Theme.colour7));
            noteLabel.setFont(Font.font("System", 13));

            bulletRow.getChildren().addAll(bullet, noteLabel);
            notesList.getChildren().add(bulletRow);
        }

        VBox post = new VBox(10, headerRow, dateLabel, notesList);
        post.setPadding(new Insets(20));
        post.setStyle(
                "-fx-background-color: " + Theme.colour1 + ";" +
                        "-fx-background-radius: 12;" +
                        "-fx-border-color: " + Theme.colour3 + ";" +
                        "-fx-border-radius: 12;"
        );

        return post;
    }

    private String tagStyle(String tag) {
        return "-fx-background-color: " + Theme.colour7 + "22; -fx-text-fill: " + Theme.colour7
                + "; -fx-background-radius: 10; -fx-font-size: 11px; -fx-font-weight: bold;";
    }
}