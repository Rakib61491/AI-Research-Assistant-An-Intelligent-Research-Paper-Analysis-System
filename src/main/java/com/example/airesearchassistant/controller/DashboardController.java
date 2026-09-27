package com.example.airesearchassistant.controller;

import com.example.airesearchassistant.service.CollectionService;
import com.example.airesearchassistant.service.PaperService;
import com.example.airesearchassistant.util.Constants;
import com.example.airesearchassistant.util.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

/**
 * DashboardController — Overview of research library, metrics, and quick actions.
 * Fetches real metrics directly from the SQLite database via PaperService & CollectionService.
 */
public class DashboardController {

    @FXML private Label totalPapersLabel;
    @FXML private Label totalCollectionsLabel;
    @FXML private Label totalFavoritesLabel;
    @FXML private Label ollamaStatusLabel;

    private final PaperService paperService = new PaperService();
    private final CollectionService collectionService = new CollectionService();

    @FXML
    public void initialize() {
        loadMetrics();
    }

    public void loadMetrics() {
        int totalPapers = paperService.getTotalPaperCount();
        int totalCollections = collectionService.getTotalCollectionCount();
        int totalFavorites = paperService.getFavoritePaperCount();

        if (totalPapersLabel != null) totalPapersLabel.setText(String.valueOf(totalPapers));
        if (totalCollectionsLabel != null) totalCollectionsLabel.setText(String.valueOf(totalCollections));
        if (totalFavoritesLabel != null) totalFavoritesLabel.setText(String.valueOf(totalFavorites));
        if (ollamaStatusLabel != null) ollamaStatusLabel.setText("Ready");
    }

    @FXML
    private void handleQuickAddPaper() {
        SceneManager.getInstance().load(Constants.FXML_ADD_PAPER);
    }

    @FXML
    private void handleQuickBrowseLibrary() {
        SceneManager.getInstance().load(Constants.FXML_LIBRARY);
    }

    @FXML
    private void handleQuickOpenAssistant() {
        SceneManager.getInstance().load(Constants.FXML_ASSISTANT);
    }
}
