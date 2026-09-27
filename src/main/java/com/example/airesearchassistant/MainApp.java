package com.example.airesearchassistant;

import com.example.airesearchassistant.util.Constants;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.util.Objects;

/**
 * MainApp — JavaFX Application entry point.
 *
 * Stage 3: Loads the main application shell (main-shell.fxml) with navigation,
 * top bar, sidebar, and theme support.
 */
public class MainApp extends Application {

    public static final String APP_TITLE = Constants.APP_NAME + " — " + Constants.APP_SUBTITLE;
    public static final double WINDOW_WIDTH  = 1100;
    public static final double WINDOW_HEIGHT = 700;

    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource(Constants.FXML_MAIN_SHELL)
        );
        Parent root = loader.load();

        Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);

        // Attach base light styles and dark theme override by default
        scene.getStylesheets().add(
                Objects.requireNonNull(
                        getClass().getResource(Constants.CSS_STYLE)
                ).toExternalForm()
        );
        scene.getStylesheets().add(
                Objects.requireNonNull(
                        getClass().getResource(Constants.CSS_DARK)
                ).toExternalForm()
        );

        primaryStage.setTitle(APP_TITLE);
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
