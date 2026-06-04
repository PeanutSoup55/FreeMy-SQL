package tempFiles;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

public class TempCred extends BorderPane {
    private Text head = new Text("Apologies");
    private Text text = new Text("Not available in Beta, only available in full version");

    public TempCred() {
        setCenter(betaCard(head, text));
    }

    public VBox betaCard(Text head, Text text) {
        VBox betCard = new VBox(12);
        betCard.setAlignment(Pos.CENTER);
        betCard.setPadding(new Insets(24));

        betCard.setStyle(
                "-fx-background-color: #FFFFFF; " +
                        "-fx-background-radius: 8px; " +
                        "-fx-border-color: #E2E8F0; " +
                        "-fx-border-radius: 8px; " +
                        "-fx-border-width: 1px; " +
                        "-fx-max-width: 320px; " +
                        "-fx-max-height: 180px; " +
                        "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 10, 0, 0, 4);"
        );

        head.setFont(Font.font("System", FontWeight.BOLD, 18));
        head.setFill(Color.web("#1E293B"));

        text.setFont(Font.font("System", FontWeight.NORMAL, 13));
        text.setFill(Color.web("#64748B"));
        text.setWrappingWidth(260);
        text.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        betCard.getChildren().addAll(head, text);
        return betCard;
    }
}
