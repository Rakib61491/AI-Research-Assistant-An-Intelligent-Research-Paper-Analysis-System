package com.example.airesearchassistant.util;

import com.example.airesearchassistant.controller.MainShellController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.net.URL;

/**
 * SceneManager — Manages navigation inside the main shell's contentArea.
 * Swaps views inside contentArea and coordinates sidebar highlighting and page titles.
 */
public final class SceneManager {

    private static SceneManager instance;

    private StackPane contentArea;
    private MainShellController shellController;
    private String currentFxmlPath;

    private SceneManager() {
    }

    public static synchronized SceneManager getInstance() {
        if (instance == null) {
            instance = new SceneManager();
        }
        return instance;
    }

    public void init(StackPane contentArea, MainShellController shellController) {
        this.contentArea = contentArea;
        this.shellController = shellController;
    }

    public MainShellController getShellController() {
        return shellController;
    }

    /**
     * Loads the view at fxmlPath into the contentArea and updates the sidebar active state.
     *
     * @param fxmlPath resource path to FXML
     * @return the controller of the loaded FXML, or null on error
     */
    public Object load(String fxmlPath) {
        if (contentArea == null) {
            System.err.println("SceneManager contentArea is not initialized!");
            return null;
        }

        try {
            URL url = getClass().getResource(fxmlPath);
            if (url == null) {
                showNavigationError("Resource Not Found", "Cannot find FXML file: " + fxmlPath);
                return null;
            }

            FXMLLoader loader = new FXMLLoader(url);
            Parent view = loader.load();
            contentArea.getChildren().setAll(view);
            currentFxmlPath = fxmlPath;

            if (shellController != null) {
                shellController.updateActiveNavigation(fxmlPath);
            }

            return loader.getController();
        } catch (IOException e) {
            e.printStackTrace();
            showNavigationError("Failed to Load View", "Could not load " + fxmlPath + ":\n" + e.getMessage());
            return null;
        }
    }

    public String getCurrentFxmlPath() {
        return currentFxmlPath;
    }

    private void showNavigationError(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Navigation Error");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
