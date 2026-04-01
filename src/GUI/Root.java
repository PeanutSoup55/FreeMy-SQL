package GUI;

import globalfuncs.creds;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import java.awt.*;

public class Root extends BorderPane {

    public Root(){
        createSide();
        SchemasRoot sRoot = new SchemasRoot();
        setCenter(sRoot);
    }

    public void createSide(){
        VBox vBox = new VBox();
        HBox tophbox = new HBox();
        VBox topInnerHBox = new VBox();

        HBox schemas = createMenuItems("../assets/schema", "Schemas");
        HBox query = createMenuItems("../assets/query", "Query");
        HBox dashboard = createMenuItems("../assets/dashboard", "Dashboard");
        HBox credentials = createMenuItems("../assets/creds", "Credentials");
        HBox logs = createMenuItems("../assets/logs", "Logs");

    }
    public HBox createMenuItems(String icon, String labelText){
        Text label = new Text(labelText);
        label.setFont(Font.font("System", 13));
        label.setFill(Color.web("#1a1a1a"));

        Image image = new Image(icon);

        HBox hbox = new HBox(5, image, label);
        hbox.setMaxWidth(300);
        return hbox;
    }
}
