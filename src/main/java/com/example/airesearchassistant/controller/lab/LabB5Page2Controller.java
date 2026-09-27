package com.example.airesearchassistant.controller.lab;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * LabB5Page2Controller — Target page for B5 scene switch.
 * Displays Page 2 and provides a Back button to return to the previous Scene.
 */
public class LabB5Page2Controller {

    @FXML private Button backButton;

    private Scene previousScene;

    /**
     * Stores the previous scene so the Back button can perform a clean swap back.
     *
     * @param scene the previous Scene to return to
     */
    public void setPreviousScene(Scene scene) {
        this.previousScene = scene;
    }

    /**
     * Returns to the previous scene or reloads the main lab view if previous scene is unavailable.
     */
    @FXML
    private void handleBack() {
        Stage stage = (Stage) backButton.getScene().getWindow();
        if (previousScene != null) {
            stage.setScene(previousScene);
        } else {
            try {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("/com/example/airesearchassistant/lab/lab-view.fxml")
                );
                Parent root = loader.load();
                Scene newScene = new Scene(root, 1100, 700);
                newScene.getStylesheets().add(
                        getClass().getResource("/com/example/airesearchassistant/style.css").toExternalForm()
                );
                stage.setScene(newScene);
            } catch (IOException e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Navigation Error");
                alert.setHeaderText("Failed to load previous view");
                alert.setContentText(e.getMessage());
                alert.showAndWait();
            }
        }
    }
}
