import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ChildrenDisplay extends Application {
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Children Information");

        Label child1 = new Label("儿子: 王子谦");
        Label child2 = new Label("女儿: 赵旭阳");

        VBox vbox = new VBox(child1, child2);
        
        Scene scene = new Scene(vbox, 200, 100);
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}