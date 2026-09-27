package com.example.airesearchassistant.controller.lab;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * LabScenesController — Handles B5 (scene switch on same stage)
 * and B6 (data transfer via loader.getController().setData(text)).
 */
public class LabScenesController {

    @FXML private Button b5SwitchButton;
    @FXML private TextField b6TextField;
    @FXML private Button b6PassButton;

    /**
     * B5: Full scene swap to Page 2 on the same Stage.
     */
    @FXML
    private void handleB5SwitchScene() {
        try {
            Stage stage = (Stage) b5SwitchButton.getScene().getWindow();
            Scene currentScene = b5SwitchButton.getScene();

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/airesearchassistant/lab/lab-b5-page2.fxml")
            );
            Parent root = loader.load();

            LabB5Page2Controller controller = loader.getController();
            controller.setPreviousScene(currentScene);

            Scene page2Scene = new Scene(root, currentScene.getWidth(), currentScene.getHeight());
            page2Scene.getStylesheets().addAll(currentScene.getStylesheets());
            stage.setScene(page2Scene);
        } catch (IOException e) {
            showError("Error switching scene (B5)", e.getMessage());
        }
    }

    /**
     * B6: Full scene swap with data transfer via loader.getController().setData(text).
     */
    @FXML
    private void handleB6PassData() {
        try {
            Stage stage = (Stage) b6PassButton.getScene().getWindow();
            Scene currentScene = b6PassButton.getScene();

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/airesearchassistant/lab/lab-b6-page2.fxml")
            );
            Parent root = loader.load();

            LabB6Page2Controller controller = loader.getController();
            controller.setPreviousScene(currentScene);
            // Exact requirement: loader.getController().setData(text)
            controller.setData(b6TextField.getText());

            Scene page2Scene = new Scene(root, currentScene.getWidth(), currentScene.getHeight());
            page2Scene.getStylesheets().addAll(currentScene.getStylesheets());
            stage.setScene(page2Scene);
        } catch (IOException e) {
            showError("Error passing data (B6)", e.getMessage());
        }
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Scene Navigation Error");
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
