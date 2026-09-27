package com.example.airesearchassistant;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class HelloController {

    @FXML
    private Label totalPapersLabel;

    @FXML
    private Label topicsLabel;

    @FXML
    private Label analysesLabel;

    @FXML
    private Label statusLabel;

    @FXML
    private TextField searchField;

    @FXML
    private ListView<String> recentPapersList;

    @FXML
    private void initialize() {
        topicsLabel.setText("0");
        analysesLabel.setText("0");

        loadPapers();
    }

    private void loadPapers() {
        try {
            List<ResearchPaper> papers = Database.getAllPapers();

            recentPapersList.setItems(
                    FXCollections.observableArrayList(
                            papers.stream()
                                    .map(ResearchPaper::getTitle)
                                    .toList()
                    )
            );

            totalPapersLabel.setText(
                    String.valueOf(Database.getPaperCount())
            );

            statusLabel.setText("Papers loaded successfully");

        } catch (SQLException e) {
            statusLabel.setText("Unable to load papers");
            e.printStackTrace();
        }
    }

    @FXML
    private void onDashboardClick() {
        loadPapers();
        statusLabel.setText("Dashboard selected");
    }

    @FXML
    private void onPapersClick() {
        loadPapers();
        statusLabel.setText("Research Papers section selected");
    }

    @FXML
    private void onAddPaperClick() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    HelloApplication.class.getResource("add-paper-view.fxml")
            );

            Scene scene = new Scene(loader.load(), 1100, 700);
            Stage stage = (Stage) statusLabel.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Add Research Paper");

        } catch (IOException e) {
            statusLabel.setText("Could not open Add Paper form.");
            e.printStackTrace();
        }
    }

    @FXML
    private void onAnalysisClick() {
        statusLabel.setText("Analysis section selected");
    }

    @FXML
    private void onSearchClick() {
        String searchText = searchField.getText().trim();

        if (searchText.isEmpty()) {
            statusLabel.setText("Please enter a search term");
        } else {
            statusLabel.setText("Searching for: " + searchText);
        }
    }

    @FXML
    private void onExitClick() {
        Platform.exit();
    }
}