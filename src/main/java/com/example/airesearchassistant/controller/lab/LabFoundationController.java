package com.example.airesearchassistant.controller.lab;

import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * LabFoundationController — handles B1, B2, B4.
 * B2 layout panes are pure FXML (no code needed).
 */
public class LabFoundationController {

    @FXML private Label b4Label;

    // ----- B1 ----------------------------------------------------------------

    /** Opens a new 400×300 Stage with a welcome Label (B1). */
    @FXML
    private void openB1Stage() {
        Label welcome = new Label("Welcome to JavaFX!");
        welcome.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #cdd6f4;");

        StackPane root = new StackPane(welcome);
        root.setStyle("-fx-background-color: #1e1e2e;");

        Stage stage = new Stage();
        stage.setTitle("B1 — Welcome Stage");
        stage.setScene(new Scene(root, 400, 300));
        stage.show();
    }

    // ----- B4 ----------------------------------------------------------------

    /** Sets label text to "Button Clicked!" (B4 — onAction="#handler"). */
    @FXML
    private void handleB4Click() {
        b4Label.setText("Button Clicked!");
    }
}
