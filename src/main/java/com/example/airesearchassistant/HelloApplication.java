package com.example.airesearchassistant;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {

        Database.initializeDatabase();

        FXMLLoader fxmlLoader =
                new FXMLLoader(
                        HelloApplication.class.getResource("hello-view.fxml")
                );

        Scene scene = new Scene(fxmlLoader.load(), 1100, 700);

        stage.setTitle("AI Research Assistant");
        stage.setScene(scene);
        stage.show();
    }
}