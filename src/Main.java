import GUI.Login;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.util.Objects;

public class Main extends Application {
    @Override
    public void start(Stage stage) {

        stage.setTitle("FreeMySQL");
        Login login = new Login(stage);
        Scene scene = new Scene(login, 1700, 800);
        stage.getIcons().add(new Image(getClass().getResourceAsStream("/assets/logo3.png")));
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
