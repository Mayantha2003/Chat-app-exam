package lk.ijse.chatappexam;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApplication extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {

        // Server Side FXML
        FXMLLoader serverLoader = new FXMLLoader(getClass().getResource("Server.fxml"));
        Scene serverScene = new Scene(serverLoader.load());
        Stage serverStage = new Stage();
        serverStage.setTitle("Server-Side!");
        serverStage.setScene(serverScene);
        serverStage.show();

        // Client 1 FXML
        FXMLLoader clientLoader1 = new FXMLLoader(getClass().getResource("Client.fxml"));
        Scene clientScene1 = new Scene(clientLoader1.load());
        Stage clientStage1 = new Stage();
        clientStage1.setTitle("Client 1");
        clientStage1.setScene(clientScene1);
        clientStage1.show();

        // Client 2 FXML
        FXMLLoader clientLoader2 = new FXMLLoader(getClass().getResource("Client.fxml"));
        Scene clientScene2 = new Scene(clientLoader2.load());
        Stage clientStage2 = new Stage();
        clientStage2.setTitle("Client 2");
        clientStage2.setScene(clientScene2);
        clientStage2.show();

        primaryStage.close();
    }

    public static void main(String[] args) {
        launch(args);
    }
}