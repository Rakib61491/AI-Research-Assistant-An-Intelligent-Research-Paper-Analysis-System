package com.example.airesearchassistant.controller;

import com.example.airesearchassistant.model.AppSettings;
import com.example.airesearchassistant.service.OllamaException;
import com.example.airesearchassistant.service.SettingsService;
import com.example.airesearchassistant.util.AlertUtil;
import com.example.airesearchassistant.util.SceneManager;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.sql.SQLException;
import java.util.List;

/**
 * SettingsController — Configuration interface for Ollama endpoint, model selection,
 * inference hyperparameters (temperature, max tokens, timeouts), and application theme.
 */
public class SettingsController {

    @FXML private TextField ollamaUrlField;
    @FXML private Button testConnectionButton;
    @FXML private Label connectionStatusLabel;

    @FXML private ComboBox<String> modelComboBox;
    @FXML private Button refreshModelsButton;
    @FXML private ProgressIndicator modelLoadingIndicator;

    @FXML private Slider temperatureSlider;
    @FXML private Label temperatureValueLabel;

    @FXML private Spinner<Integer> maxTokensSpinner;
    @FXML private TextField timeoutField;

    @FXML private ChoiceBox<String> themeChoiceBox;
    @FXML private Label settingsStatusLabel;

    private final SettingsService settingsService = new SettingsService();

    @FXML
    public void initialize() {
        // Initialize Theme ChoiceBox
        if (themeChoiceBox != null) {
            themeChoiceBox.getItems().setAll("Dark (Mocha)", "Light");
        }

        // Initialize Temperature Slider listener
        if (temperatureSlider != null) {
            temperatureSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (temperatureValueLabel != null) {
                    temperatureValueLabel.setText(String.format("%.2f", newVal.doubleValue()));
                }
            });
        }

        // Initialize MaxTokens Spinner
        if (maxTokensSpinner != null) {
            maxTokensSpinner.setValueFactory(
                    new SpinnerValueFactory.IntegerSpinnerValueFactory(100, 8192, 800, 50)
            );
        }

        loadSavedSettings();

        // Automatically fetch models on startup so user sees installed Ollama models immediately
        Platform.runLater(this::handleRefreshModels);
    }

    private void loadSavedSettings() {
        AppSettings settings = settingsService.getSettings();

        if (ollamaUrlField != null) ollamaUrlField.setText(settings.getOllamaUrl());
        if (modelComboBox != null) {
            modelComboBox.setEditable(true);
            String savedModel = settings.getDefaultModel();
            if (savedModel != null && !savedModel.isBlank()) {
                if (!modelComboBox.getItems().contains(savedModel)) {
                    modelComboBox.getItems().add(savedModel);
                }
                modelComboBox.setValue(savedModel);
                if (modelComboBox.getEditor() != null) {
                    modelComboBox.getEditor().setText(savedModel);
                }
            }
        }
        if (temperatureSlider != null) {
            temperatureSlider.setValue(settings.getTemperature());
            if (temperatureValueLabel != null) {
                temperatureValueLabel.setText(String.format("%.2f", settings.getTemperature()));
            }
        }
        if (maxTokensSpinner != null && maxTokensSpinner.getValueFactory() != null) {
            maxTokensSpinner.getValueFactory().setValue(settings.getMaxTokens());
        }
        if (timeoutField != null) {
            timeoutField.setText(String.valueOf(settings.getTimeoutSeconds()));
        }
        if (themeChoiceBox != null) {
            themeChoiceBox.setValue("dark".equalsIgnoreCase(settings.getTheme()) ? "Dark (Mocha)" : "Light");
        }
        if (connectionStatusLabel != null) {
            connectionStatusLabel.setText("Status: Ready to test connection");
        }
    }

    @FXML
    private void handleTestConnection() {
        String url = ollamaUrlField != null ? ollamaUrlField.getText().trim() : "";
        if (url.isEmpty()) {
            AlertUtil.showWarning("Input Error", "Missing URL", "Please enter an Ollama server URL.");
            return;
        }

        if (connectionStatusLabel != null) {
            connectionStatusLabel.setText("Connecting to " + url + "...");
            connectionStatusLabel.setStyle("-fx-text-fill: #89b4fa;");
        }

        Task<Boolean> testTask = new Task<>() {
            @Override
            protected Boolean call() {
                return settingsService.testOllamaConnection(url);
            }
        };

        testTask.setOnSucceeded(e -> {
            boolean connected = testTask.getValue();
            if (connected) {
                if (connectionStatusLabel != null) {
                    connectionStatusLabel.setText("✓ Ollama is running and accessible!");
                    connectionStatusLabel.setStyle("-fx-text-fill: #a6e3a1; -fx-font-weight: bold;");
                }
                // Automatically fetch models on successful connection
                handleRefreshModels();
            } else {
                if (connectionStatusLabel != null) {
                    connectionStatusLabel.setText("✗ Ollama is not reachable at this URL.");
                    connectionStatusLabel.setStyle("-fx-text-fill: #f38ba8; -fx-font-weight: bold;");
                }
            }
        });

        testTask.setOnFailed(e -> {
            if (connectionStatusLabel != null) {
                connectionStatusLabel.setText("✗ Connection failed: " + testTask.getException().getMessage());
                connectionStatusLabel.setStyle("-fx-text-fill: #f38ba8;");
            }
        });

        Thread thread = new Thread(testTask, "OllamaConnectionTestWorker");
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void handleRefreshModels() {
        String url = ollamaUrlField != null ? ollamaUrlField.getText().trim() : "";
        if (url.isEmpty()) return;

        if (modelLoadingIndicator != null) modelLoadingIndicator.setVisible(true);

        Task<List<String>> modelsTask = new Task<>() {
            @Override
            protected List<String> call() throws Exception {
                try {
                    List<String> chatModels = settingsService.fetchAvailableChatModels(url);
                    if (!chatModels.isEmpty()) return chatModels;
                } catch (Exception ignored) {}
                return settingsService.fetchAvailableModels(url);
            }
        };

        modelsTask.setOnSucceeded(e -> {
            if (modelLoadingIndicator != null) modelLoadingIndicator.setVisible(false);
            List<String> models = modelsTask.getValue();
            if (modelComboBox != null) {
                modelComboBox.setEditable(true);
                String current = "";
                if (modelComboBox.getEditor() != null && !modelComboBox.getEditor().getText().isBlank()) {
                    current = modelComboBox.getEditor().getText().trim();
                } else if (modelComboBox.getValue() != null) {
                    current = modelComboBox.getValue().trim();
                }

                modelComboBox.getItems().clear();
                if (models.isEmpty()) {
                    if (current != null && !current.isBlank()) {
                        modelComboBox.getItems().add(current);
                    } else {
                        modelComboBox.getItems().add("llama3.2");
                    }
                    if (connectionStatusLabel != null) {
                        connectionStatusLabel.setText("✓ Connected, but no models found. Run `ollama pull <model>`.");
                    }
                } else {
                    modelComboBox.getItems().addAll(models);
                    if (connectionStatusLabel != null && !connectionStatusLabel.getText().startsWith("✗")) {
                        connectionStatusLabel.setText("✓ Ollama connected (" + models.size() + " models available)");
                        connectionStatusLabel.setStyle("-fx-text-fill: #a6e3a1; -fx-font-weight: bold;");
                    }
                }

                // If user had a selected/typed model, preserve it!
                if (!current.isBlank()) {
                    if (!modelComboBox.getItems().contains(current)) {
                        modelComboBox.getItems().add(0, current);
                    }
                    modelComboBox.setValue(current);
                    if (modelComboBox.getEditor() != null) {
                        modelComboBox.getEditor().setText(current);
                    }
                } else if (!modelComboBox.getItems().isEmpty()) {
                    modelComboBox.setValue(modelComboBox.getItems().get(0));
                    if (modelComboBox.getEditor() != null) {
                        modelComboBox.getEditor().setText(modelComboBox.getItems().get(0));
                    }
                }
            }
        });

        modelsTask.setOnFailed(e -> {
            if (modelLoadingIndicator != null) modelLoadingIndicator.setVisible(false);
            Throwable ex = modelsTask.getException();
            String msg = (ex instanceof OllamaException) ? ex.getMessage() : "Could not retrieve models.";
            if (connectionStatusLabel != null && !connectionStatusLabel.getText().startsWith("✓")) {
                connectionStatusLabel.setText("Status: " + msg);
            }
        });

        Thread thread = new Thread(modelsTask, "OllamaModelsWorker");
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void handleSaveSettings() {
        String url = ollamaUrlField != null ? ollamaUrlField.getText().trim() : "";
        if (url.isEmpty()) {
            AlertUtil.showWarning("Validation Error", "Missing URL", "Ollama URL cannot be blank.");
            return;
        }

        String model = "";
        if (modelComboBox != null) {
            if (modelComboBox.getEditor() != null && !modelComboBox.getEditor().getText().isBlank()) {
                model = modelComboBox.getEditor().getText().trim();
            } else if (modelComboBox.getValue() != null) {
                model = modelComboBox.getValue().trim();
            }
        }
        if (model.isBlank()) {
            model = "llama3.2";
        }

        double temp = temperatureSlider != null ? temperatureSlider.getValue() : 0.4;
        int maxTokens = (maxTokensSpinner != null && maxTokensSpinner.getValue() != null)
                ? maxTokensSpinner.getValue()
                : 800;

        int timeout = 120;
        if (timeoutField != null && !timeoutField.getText().trim().isEmpty()) {
            try {
                timeout = Integer.parseInt(timeoutField.getText().trim());
                if (timeout <= 0) timeout = 120;
            } catch (NumberFormatException e) {
                AlertUtil.showWarning("Validation Error", "Invalid Timeout", "Timeout must be a positive integer.");
                return;
            }
        }

        String theme = (themeChoiceBox != null && "Light".equalsIgnoreCase(themeChoiceBox.getValue()))
                ? "light"
                : "dark";

        AppSettings settings = new AppSettings(url, model, temp, maxTokens, timeout, theme);
        try {
            settingsService.saveSettings(settings);

            // Immediately notify main shell to update active model badge and connectivity indicator
            MainShellController shell = SceneManager.getInstance().getShellController();
            if (shell != null) {
                shell.refreshSettings();
            }

            if (settingsStatusLabel != null) {
                settingsStatusLabel.setText("Settings saved successfully (Model: " + model + ").");
            }
            AlertUtil.showInfo("Settings Saved", "Configuration Saved",
                    "Application configuration has been updated and permanently saved to project.\nActive Model: " + model);
        } catch (SQLException e) {
            AlertUtil.showError("Database Error", "Failed to save settings", e.getMessage());
        }
    }
}
