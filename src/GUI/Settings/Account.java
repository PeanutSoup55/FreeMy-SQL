package GUI.Settings;

import auth.AuthClient;
import auth.LicenseStore;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class Account extends BorderPane {

    // ---- fixed semantic status colours, not theme-driven ----
    private static final String GREEN_OK  = "#2E9E6D";
    private static final String AMBER_WARN = "#C08A2E";
    private static final String RED_BAD    = "#D9534F";

    private final Button billingBtn = new Button("Manage billing");

    private final ImageView accountIcon = new ImageView(new Image(Objects.requireNonNull(
            getClass().getClassLoader().getResourceAsStream("assets/account.png"))));

    private final Label pageTitle    = new Label("Account Settings");
    private final Label pageSubtitle = new Label("Manage your account and subscription");
    private final Label detailsHeader = new Label("Account");
    private final Label subHeader     = new Label("Subscription");
    private final Label emailLabel  = new Label("Loading...");
    private final Label statusBadge = new Label("...");
    private final Label renewLabel  = new Label("");
    private final Label graceBanner = new Label();
    private final ProgressIndicator spinner = new ProgressIndicator();
    private final Button refreshBtn = new Button("Refresh status");

    private VBox detailsCard;
    private VBox subCard;
    private HBox emailBox;

    public Account() {
        buildLayout();
        applyTheme();
        loadAccountInfo();
        Theme.registerThemeListener(this, this::applyTheme);
    }

    private void buildLayout() {
        VBox pageHeader = new VBox(4, pageTitle, pageSubtitle);

        accountIcon.setFitWidth(48);
        accountIcon.setFitHeight(48);

        emailBox = boxed(emailLabel);
        VBox emailField = new VBox(4, fieldCaption("Email"), emailBox);

        HBox detailsTop = new HBox(16, accountIcon, emailField);
        detailsTop.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(emailField, Priority.ALWAYS);
        emailField.setMaxWidth(Double.MAX_VALUE);

        detailsCard = card(new VBox(18, detailsHeader, detailsTop));

        statusBadge.setPadding(new Insets(4, 10, 4, 10));
        spinner.setMaxSize(16, 16);

        HBox statusRow = new HBox(10, statusBadge, spinner);
        statusRow.setAlignment(Pos.CENTER_LEFT);

        VBox statusField = new VBox(4, fieldCaption("Status"), statusRow);
        VBox renewField = new VBox(4, fieldCaption("Renews"), renewLabel);
        HBox statusLine = new HBox(40, statusField, renewField);

        refreshBtn.setOnAction(e -> loadAccountInfo());

        billingBtn.setOnAction(e -> openBillingPortal());
        HBox subActions = new HBox(20, billingBtn, refreshBtn);
        subCard = card(new VBox(18, subHeader, statusLine, subActions));

        graceBanner.setPadding(new Insets(10, 14, 10, 14));
        graceBanner.setWrapText(true);
        graceBanner.setVisible(false);
        graceBanner.setManaged(false);

        VBox content = new VBox(24, pageHeader, graceBanner, detailsCard, subCard);
        content.setMaxWidth(640);
        setCenter(content);
        setPadding(new Insets(40));
    }

    /** Re-applies current Theme colours to every themed element. Safe to call repeatedly. */
    private void applyTheme() {
        String bg     = Theme.colour2;   // page background
        String surface= Theme.colour1;   // card background
        String border = Theme.colour3;
        String text   = Theme.colour6;
        String muted  = Theme.colour7;

        Platform.runLater(() -> {
            setStyle("-fx-background-color: " + bg + ";");

            pageTitle.setFont(Font.font("System", FontWeight.BOLD, 24));
            pageTitle.setTextFill(Color.web(text));
            pageSubtitle.setStyle("-fx-text-fill: " + muted + "; -fx-font-size: 13px;");

            detailsHeader.setFont(Font.font("System", FontWeight.SEMI_BOLD, 16));
            detailsHeader.setTextFill(Color.web(text));
            subHeader.setFont(Font.font("System", FontWeight.SEMI_BOLD, 16));
            subHeader.setTextFill(Color.web(text));

            emailLabel.setStyle("-fx-text-fill: " + text + "; -fx-font-size: 13px;");
            renewLabel.setStyle("-fx-text-fill: " + muted + "; -fx-font-size: 12px;");

            detailsCard.setStyle(cardStyle(surface, border));
            subCard.setStyle(cardStyle(surface, border));
            emailBox.setStyle(boxedStyle(surface, border));

            refreshBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: " + text
                    + "; -fx-font-weight: bold; -fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 0;");
            billingBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: " + text
                    + "; -fx-font-weight: bold; -fx-font-size: 12px; -fx-cursor: hand; -fx-padding: 0;");

            graceBanner.setStyle("-fx-background-color: " + AMBER_WARN + "1A; -fx-text-fill: " + AMBER_WARN
                    + "; -fx-background-radius: 8; -fx-font-size: 12px;");

            for (var node : rowsWithCaption()) {
                node.setStyle("-fx-text-fill: " + muted + "; -fx-font-size: 10px; -fx-font-weight: bold;");
            }
        });
    }

    private void openBillingPortal() {
        String token = LicenseStore.getToken();
        if (token == null) return;

        billingBtn.setDisable(true);
        billingBtn.setText("Opening...");

        AuthClient.createBillingPortalSession(token, "https://freemyquery.com/account")
                .thenAccept(url -> Platform.runLater(() -> {
                    billingBtn.setDisable(false);
                    billingBtn.setText("Manage billing");
                    if (url == null) {
                        billingBtn.setText("Unavailable");
                        return;
                    }
                    try {
                        java.awt.Desktop.getDesktop().browse(java.net.URI.create(url));
                    } catch (Exception ex) {
                        billingBtn.setText("Unavailable");
                    }
                }));
    }

    private java.util.List<Label> rowsWithCaption() {
        // fieldCaption labels created inline aren't stored, so re-style on rebuild only.
        // Captions get their style set once at creation time via fieldCaption(); theme
        // changes to muted text are covered by emailLabel/renewLabel/status above.
        return java.util.Collections.emptyList();
    }

    private void loadAccountInfo() {
        String userId = LicenseStore.getUserId();
        String token = LicenseStore.getToken();
        spinner.setVisible(true);
        refreshBtn.setDisable(true);

        if (userId == null || token == null) {
            emailLabel.setText("Not signed in");
            statusBadge.setText("—");
            statusBadge.setStyle(badgeStyle(Theme.colour7));
            spinner.setVisible(false);
            refreshBtn.setDisable(false);
            return;
        }

        AuthClient.getUserEmail(token).thenAccept(email ->
                Platform.runLater(() -> emailLabel.setText(email != null ? email : "unavailable")));

        AuthClient.getSubscriptionDetails(userId, token).thenAccept(sub ->
                Platform.runLater(() -> {
                    spinner.setVisible(false);
                    refreshBtn.setDisable(false);

                    if (sub == null) {
                        boolean inGrace = LicenseStore.isWithinGracePeriod();
                        statusBadge.setText(inGrace ? "Grace period" : "Unverified");
                        statusBadge.setStyle(badgeStyle(inGrace ? AMBER_WARN : RED_BAD));
                        if (inGrace) {
                            graceBanner.setText("Couldn't reach Supabase to verify your subscription. "
                                    + "You have " + LicenseStore.daysLeftInGracePeriod()
                                    + " day(s) of offline access left.");
                            graceBanner.setVisible(true);
                            graceBanner.setManaged(true);
                        }
                        return;
                    }

                    graceBanner.setVisible(false);
                    graceBanner.setManaged(false);

                    boolean active = "active".equals(sub.status()) || "trialing".equals(sub.status());
                    statusBadge.setText(sub.status().substring(0, 1).toUpperCase() + sub.status().substring(1));
                    statusBadge.setStyle(badgeStyle(active ? GREEN_OK : RED_BAD));

                    if (sub.currentPeriodEnd() != null) {
                        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM d, yyyy")
                                .withZone(ZoneId.systemDefault());
                        renewLabel.setText(fmt.format(sub.currentPeriodEnd()));
                    } else {
                        renewLabel.setText("—");
                    }
                }));
    }

    // ---- style helpers ----
    private VBox card(javafx.scene.Node content) {
        VBox box = new VBox(content);
        box.setPadding(new Insets(24));
        return box;
    }

    private String cardStyle(String surface, String border) {
        return "-fx-background-color: " + surface + "; -fx-background-radius: 10; "
                + "-fx-border-color: " + border + "; -fx-border-radius: 10;";
    }

    private Label fieldCaption(String text) {
        Label l = new Label(text.toUpperCase());
        l.setStyle("-fx-text-fill: " + Theme.colour7 + "; -fx-font-size: 10px; -fx-font-weight: bold;");
        return l;
    }

    private HBox boxed(Label valueLabel) {
        HBox box = new HBox(valueLabel);
        box.setPadding(new Insets(8, 12, 8, 12));
        return box;
    }

    private String boxedStyle(String surface, String border) {
        return "-fx-background-color: " + surface + "; -fx-border-color: " + border
                + "; -fx-border-radius: 6; -fx-background-radius: 6;";
    }

    private String badgeStyle(String hexColor) {
        return "-fx-background-color: " + hexColor + "22; -fx-text-fill: " + hexColor
                + "; -fx-background-radius: 12; -fx-font-size: 12px; -fx-font-weight: bold;";
    }
}