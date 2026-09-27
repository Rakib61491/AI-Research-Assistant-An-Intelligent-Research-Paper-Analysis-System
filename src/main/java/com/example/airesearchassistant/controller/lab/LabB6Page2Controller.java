package com.example.airesearchassistant.controller.lab;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * LabB6Page2Controller — Target page for B6 data passing.
 * Implements the required setData(String) method and provides a Back button.
 */
public class LabB6Page2Controller {

    @FXML private Label receivedDataLabel;
    @FXML private Button backButton;

    private Scene previousScene;

    /**
     * Stores the previous scene so the Back button can return cleanly.
     *
     * @param scene the previous Scene to return to
     */
    public void setPreviousScene(Scene scene) {
        this.previousScene = scene;
    }

    /**
     * Exact required API method: setData(String text)
     * Displays the passed data in the receivedDataLabel.
     *
     * @param text the data passed from Page 1
     */
    public void setData(String text) {
        if (text == null || text.trim().isEmpty()) {
            receivedDataLabel.setText("(No data entered on Page 1)");
        } else {
            receivedDataLabel.setText(text);
        }
    }

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
