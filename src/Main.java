import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;

public class Main extends Application {
    @Override
    public void start(Stage stage) {

        stage.setTitle("FreeMySQL");
        Login login = new Login(stage);
        Scene scene = new Scene(login, 800, 500);
        String cssPath = Objects.requireNonNull(getClass().getResource("assets/style.css")).toExternalForm();
        scene.getStylesheets().add(cssPath);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
