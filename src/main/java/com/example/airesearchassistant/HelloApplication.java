package com.example.airesearchassistant;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {

        FXMLLoader fxmlLoader =
                new FXMLLoader(
                        HelloApplication.class.getResource("hello-view.fxml")
                );

        Scene scene = new Scene(fxmlLoader.load(), 1100, 700);

        String css =HelloApplication.class.getResource("style.css").toExternalForm();

        scene.getStylesheets().add(css);

        stage.setTitle("AI Research Assistant");
        stage.setScene(scene);

        stage.setMinWidth(900);
        stage.setMinHeight(600);

        stage.show();
    }
}