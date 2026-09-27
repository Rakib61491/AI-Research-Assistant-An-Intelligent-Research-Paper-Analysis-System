package com.example.airesearchassistant;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;

public class AddPaperController {

    @FXML
    private TextField titleField;

    @FXML
    private TextField authorsField;

    @FXML
    private TextArea abstractArea;

    @FXML
    private TextField yearField;

    @FXML
    private Label messageLabel;

    @FXML
    private void onSaveClick() {

        String title = titleField.getText().trim();
        String authors = authorsField.getText().trim();
        String abstractText = abstractArea.getText().trim();
        String year = yearField.getText().trim();

        if (title.isEmpty()) {
            messageLabel.setText("Please enter the paper title.");
            return;
        }

        if (!year.isEmpty() && !year.matches("\\d{4}")) {
            messageLabel.setText("Enter a valid 4-digit year.");
            return;
        }

        ResearchPaper paper = new ResearchPaper(
                title,
                authors,
                abstractText,
                year
        );

        try {
            Database.addPaper(paper);
            messageLabel.setText("Paper saved successfully.");

            goToDashboard();

        } catch (SQLException e) {
            messageLabel.setText("Could not save the paper.");
            e.printStackTrace();
        }
    }

    @FXML
    private void onCancelClick() {
        goToDashboard();
    }

    private void goToDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    HelloApplication.class.getResource("hello-view.fxml")
            );

            Scene scene = new Scene(loader.load(), 1100, 700);

            Stage stage = (Stage) titleField.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("AI Research Assistant");

        } catch (IOException e) {
            messageLabel.setText("Could not open the dashboard.");
            e.printStackTrace();
        }
    }
}