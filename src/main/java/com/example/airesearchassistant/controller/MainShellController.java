package com.example.airesearchassistant.controller;

import com.example.airesearchassistant.model.AppSettings;
import com.example.airesearchassistant.service.SettingsService;
import com.example.airesearchassistant.util.Constants;
import com.example.airesearchassistant.util.SceneManager;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * MainShellController — Master controller for the application layout.
 * Controls top bar, sidebar navigation, status bar, and theme toggling.
 */
public class MainShellController {

    @FXML private BorderPane rootPane;
    @FXML private StackPane contentArea;

    // Top Bar Controls
    @FXML private Label pageTitleLabel;
    @FXML private TextField globalSearchField;
    @FXML private Circle ollamaStatusDot;
    @FXML private Label ollamaModelLabel;
    @FXML private Button themeToggleButton;

    // Sidebar Navigation Buttons
    @FXML private Button navDashboard;
    @FXML private Button navLibrary;
    @FXML private Button navAddPaper;
    @FXML private Button navAssistant;
    @FXML private Button navComparison;
    @FXML private Button navCollections;
    @FXML private Button navFavorites;
    @FXML private Button navLab;
    @FXML private Button navSettings;
    @FXML private Button navAbout;

    // Bottom Status Bar
    @FXML private Label paperCountStatusLabel;
    @FXML private Label ollamaStatusLabel;
    @FXML private Label dbStatusLabel;

    private final Map<String, Button> navButtonMap = new HashMap<>();
    private final Map<String, String> pageTitleMap = new HashMap<>();
    private final com.example.airesearchassistant.service.PaperService paperService = new com.example.airesearchassistant.service.PaperService();
    private final SettingsService settingsService = new SettingsService();
    private boolean isDarkMode = true;

    @FXML
    public void initialize() {
        // Map navigation paths to buttons and titles
        navButtonMap.put(Constants.FXML_DASHBOARD, navDashboard);
        navButtonMap.put(Constants.FXML_LIBRARY, navLibrary);
        navButtonMap.put(Constants.FXML_ADD_PAPER, navAddPaper);
        navButtonMap.put(Constants.FXML_ASSISTANT, navAssistant);
        navButtonMap.put(Constants.FXML_COMPARISON, navComparison);
        navButtonMap.put(Constants.FXML_COLLECTIONS, navCollections);
        navButtonMap.put(Constants.FXML_SETTINGS, navSettings);
        navButtonMap.put(Constants.FXML_ABOUT, navAbout);
        navButtonMap.put(Constants.FXML_LAB_VIEW, navLab);

        pageTitleMap.put(Constants.FXML_DASHBOARD, "Dashboard");
        pageTitleMap.put(Constants.FXML_LIBRARY, "Research Paper Library");
        pageTitleMap.put(Constants.FXML_ADD_PAPER, "Add / Import Research Paper");
        pageTitleMap.put(Constants.FXML_ASSISTANT, "AI Research Assistant");
        pageTitleMap.put(Constants.FXML_COMPARISON, "Paper Comparison");
        pageTitleMap.put(Constants.FXML_COLLECTIONS, "Paper Collections");
        pageTitleMap.put(Constants.FXML_SETTINGS, "Application Settings");
        pageTitleMap.put(Constants.FXML_ABOUT, "About AI Research Assistant");
        pageTitleMap.put(Constants.FXML_LAB_VIEW, "JavaFX Lab Exercises (B1–B6, C1–C20)");

        setupTooltips();

        // Initialize status bar defaults
        updatePaperCount(paperService.getTotalPaperCount());
        if (ollamaStatusLabel != null) ollamaStatusLabel.setText("Ollama: offline");
        if (dbStatusLabel != null) dbStatusLabel.setText("DB: OK");

        AppSettings settings = settingsService.getSettings();
        if (ollamaModelLabel != null) ollamaModelLabel.setText(settings.getDefaultModel());

        // Asynchronously check Ollama status on startup
        checkOllamaAsync(settings);

        // Register with SceneManager and load default page (Dashboard)
        SceneManager.getInstance().init(contentArea, this);
        Platform.runLater(() -> {
            SceneManager.getInstance().load(Constants.FXML_DASHBOARD);
            if (rootPane != null && rootPane.getScene() != null) {
                registerShortcuts(rootPane.getScene());
            } else if (rootPane != null) {
                rootPane.sceneProperty().addListener((obs, oldS, newS) -> {
                    if (newS != null) registerShortcuts(newS);
                });
            }
        });
    }

    private void setupTooltips() {
        if (globalSearchField != null) globalSearchField.setTooltip(new Tooltip("Search papers across library (Ctrl+F)"));
        if (themeToggleButton != null) themeToggleButton.setTooltip(new Tooltip("Toggle Dark / Light theme"));
        if (navDashboard != null) navDashboard.setTooltip(new Tooltip("Overview and library metrics"));
        if (navLibrary != null) navLibrary.setTooltip(new Tooltip("Browse, filter, edit, and export papers"));
        if (navAddPaper != null) navAddPaper.setTooltip(new Tooltip("Add paper manually or import PDF (Ctrl+N)"));
        if (navAssistant != null) navAssistant.setTooltip(new Tooltip("AI summarization, extraction, and Q&A chat"));
        if (navComparison != null) navComparison.setTooltip(new Tooltip("Compare two papers with local similarity & AI"));
        if (navCollections != null) navCollections.setTooltip(new Tooltip("Organize papers into custom collections"));
        if (navFavorites != null) navFavorites.setTooltip(new Tooltip("View favorite papers"));
        if (navLab != null) navLab.setTooltip(new Tooltip("JavaFX Lab demo exercises (B1–B6, C1–C20)"));
        if (navSettings != null) navSettings.setTooltip(new Tooltip("Configure Ollama endpoint and AI parameters"));
        if (navAbout != null) navAbout.setTooltip(new Tooltip("About AI Research Assistant"));
    }

    private void registerShortcuts(Scene scene) {
        if (scene == null) return;
        // Ctrl+F: Focus search
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.F, KeyCombination.SHORTCUT_DOWN),
                () -> {
                    if (globalSearchField != null) {
                        globalSearchField.requestFocus();
                        globalSearchField.selectAll();
                    }
                }
        );
        // Ctrl+N: Add Paper
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN),
                () -> SceneManager.getInstance().load(Constants.FXML_ADD_PAPER)
        );
        // Ctrl+Q: Exit
        scene.getAccelerators().put(
                new KeyCodeCombination(KeyCode.Q, KeyCombination.SHORTCUT_DOWN),
                Platform::exit
        );
    }

    private void checkOllamaAsync(AppSettings settings) {
        Task<Boolean> checkTask = new Task<>() {
            @Override
            protected Boolean call() {
                return settingsService.testOllamaConnection(settings.getOllamaUrl());
            }
        };
        checkTask.setOnSucceeded(e -> updateOllamaStatus(checkTask.getValue(), settings.getDefaultModel()));
        checkTask.setOnFailed(e -> updateOllamaStatus(false, settings.getDefaultModel()));
        Thread thread = new Thread(checkTask, "ollama-status-checker");
        thread.setDaemon(true);
        thread.start();
    }

    // ----- Navigation Handlers -----------------------------------------------

    @FXML private void handleNavDashboard()   { SceneManager.getInstance().load(Constants.FXML_DASHBOARD); }
    @FXML private void handleNavLibrary()     { SceneManager.getInstance().load(Constants.FXML_LIBRARY); }
    @FXML private void handleNavAddPaper()    { SceneManager.getInstance().load(Constants.FXML_ADD_PAPER); }
    @FXML private void handleNavAssistant()   { SceneManager.getInstance().load(Constants.FXML_ASSISTANT); }
    @FXML private void handleNavComparison()  { SceneManager.getInstance().load(Constants.FXML_COMPARISON); }
    @FXML private void handleNavCollections() { SceneManager.getInstance().load(Constants.FXML_COLLECTIONS); }
    @FXML
    private void handleNavFavorites() {
        LibraryController.filterFavoritesInitial = true;
        SceneManager.getInstance().load(Constants.FXML_LIBRARY);
    }
    @FXML private void handleNavLab()         { SceneManager.getInstance().load(Constants.FXML_LAB_VIEW); }
    @FXML private void handleNavSettings()    { SceneManager.getInstance().load(Constants.FXML_SETTINGS); }
    @FXML private void handleNavAbout()       { SceneManager.getInstance().load(Constants.FXML_ABOUT); }

    /**
     * Updates active navigation button highlight and top title label.
     */
    public void updateActiveNavigation(String fxmlPath) {
        navButtonMap.values().forEach(btn -> {
            if (btn != null) {
                btn.getStyleClass().remove("nav-button-active");
            }
        });

        Button activeBtn = navButtonMap.get(fxmlPath);
        if (activeBtn != null) {
            if (!activeBtn.getStyleClass().contains("nav-button-active")) {
                activeBtn.getStyleClass().add("nav-button-active");
            }
        }

        if (pageTitleLabel != null) {
            String title = pageTitleMap.getOrDefault(fxmlPath, "AI Research Assistant");
            pageTitleLabel.setText(title);
        }

        updatePaperCount(paperService.getTotalPaperCount());
    }

    // ----- Top Bar Actions ---------------------------------------------------

    @FXML
    private void handleGlobalSearch() {
        if (globalSearchField != null && !globalSearchField.getText().trim().isEmpty()) {
            SceneManager.getInstance().load(Constants.FXML_LIBRARY);
        }
    }

    @FXML
    private void handleToggleTheme() {
        if (rootPane == null || rootPane.getScene() == null) {
            return;
        }

        Scene scene = rootPane.getScene();
        String darkCss = Objects.requireNonNull(getClass().getResource(Constants.CSS_DARK)).toExternalForm();

        isDarkMode = !isDarkMode;
        if (isDarkMode) {
            if (!scene.getStylesheets().contains(darkCss)) {
                scene.getStylesheets().add(darkCss);
            }
            if (themeToggleButton != null) {
                themeToggleButton.setText("☀️ Light Mode");
            }
        } else {
            scene.getStylesheets().remove(darkCss);
            if (themeToggleButton != null) {
                themeToggleButton.setText("🌙 Dark Mode");
            }
        }
    }

    // ----- Status Updaters ---------------------------------------------------

    public void updatePaperCount(int count) {
        if (paperCountStatusLabel != null) {
            paperCountStatusLabel.setText(count + (count == 1 ? " paper" : " papers"));
        }
    }

    public void updateOllamaStatus(boolean connected, String modelName) {
        if (ollamaStatusLabel != null) {
            ollamaStatusLabel.setText(connected ? "Ollama: connected" : "Ollama: offline");
        }
        if (ollamaModelLabel != null) {
            ollamaModelLabel.setText(modelName);
        }
        if (ollamaStatusDot != null) {
            ollamaStatusDot.setStyle(connected ? "-fx-fill: #a6e3a1;" : "-fx-fill: #f38ba8;");
        }
    }

    public void refreshSettings() {
        AppSettings settings = settingsService.getSettings();
        if (ollamaModelLabel != null) {
            ollamaModelLabel.setText(settings.getDefaultModel());
        }
        checkOllamaAsync(settings);
    }
}
