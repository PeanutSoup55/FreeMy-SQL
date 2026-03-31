import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage stage) {
        stage.setScene(new Scene(new StackPane(new Label("It Works!")), 300, 200));
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}
