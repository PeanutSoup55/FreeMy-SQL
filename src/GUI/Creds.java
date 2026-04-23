package GUI;

import globalfuncs.creds;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;


public class Creds extends VBox{
    private static final String user = creds.getUser();
    private static final String pass = creds.getPass();
    private static final String url = creds.getUrl();
    private static final String init = creds.getInitials();

    public Creds(){
        getChildren().addAll(UserBox());
    }

    public VBox UserBox(){
        HBox top = new HBox();
        top.setStyle("");
        ImageView userIcon = new ImageView(new Image("./assets/user.png"));
        Label userLabel = new Label(user);
        top.getChildren().addAll(userIcon, userLabel);

        HBox bot = new HBox();
        TextField changeUser = new TextField("root");
        Button change = new Button("Enter");
        change.setOnAction(e -> {
            changeUser(changeUser.getText());
        });
        bot.getChildren().addAll(changeUser, change);
        VBox box = new VBox();
        box.getChildren().addAll(top, bot);
        return box;
    }
    public void changeUser(String newUser){
        if (newUser != null){
            creds.setUser(newUser);
        }
    }

//    public VBox PassBox(){
//
//    }
//    public VBox URLBox(){
//
//    }
//    public VBox InitialsBox(){
//
//    }
}
