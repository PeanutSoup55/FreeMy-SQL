import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.util.Objects;

public class Login extends HBox {

    private Button login;
    private Text userLabel, passLabel, urlLabel, logoTxt;
    private TextField user, pass, url;
    private Stage stage;

    public Login(Stage stage){
        this.stage = stage;
        this.getStyleClass().add("login");
        stage.setWidth(800);
        stage.setHeight(600);
        stage.show();
        createLoginPage();
    }

    public void createLoginPage(){
        VBox vbox = new VBox();
        userLabel = new Text("MySQL username");
        user = new TextField();
        passLabel = new Text("MySQL password");
        pass = new TextField();
        urlLabel = new Text("MySQL url");
        url = new TextField();
        login = new Button("Login");

        logoTxt = new Text("Free My-SQL");


        vbox.getChildren().addAll(userLabel, user, passLabel, pass, urlLabel, url, login);
        getChildren().addAll(logoTxt, vbox);

    }


}
