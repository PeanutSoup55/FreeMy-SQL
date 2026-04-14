package GUI;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import globalfuncs.db;
import javafx.scene.text.Text;

import java.util.ArrayList;
import java.util.List;

public class SchemasRoot extends BorderPane{
    public static List<String> schemas = db.Schemas();

    public SchemasRoot(){
        createSide();
        createTables();
    }

    private VBox createSide(){
        VBox vBox = new VBox();
        List <HBox> schemaTabs = new ArrayList<>();
        for (String schema : schemas){
            schemaTabs.add(generateTab(schema));
        }
        vBox.getChildren().addAll(schemaTabs);
        return vBox;
    }

    private HBox generateTab(String schema){
        HBox hbox = new HBox();
        Text text = new Text(schema);
        hbox.getChildren().add(text);
        return hbox;
    }

    public GridPane createTables(){

    }

}
