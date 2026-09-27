package com.example.airesearchassistant.service;

import com.example.airesearchassistant.model.AppSettings;
import com.example.airesearchassistant.repository.SettingsRepository;

import java.sql.SQLException;
import java.util.List;

/**
 * SettingsService — Business service for application preferences and Ollama connection settings.
 * Contains no JavaFX/FXML references (Hard Rule 4).
 */
public class SettingsService {

    private final SettingsRepository settingsRepository;
    private final OllamaService ollamaService;

    public SettingsService() {
        this.settingsRepository = new SettingsRepository();
        this.ollamaService = new OllamaService();
    }

    public SettingsService(SettingsRepository settingsRepository, OllamaService ollamaService) {
        this.settingsRepository = settingsRepository;
        this.ollamaService = ollamaService;
    }

    public AppSettings getSettings() {
        return settingsRepository.loadSettings();
    }

    public void saveSettings(AppSettings settings) throws SQLException {
        if (settings == null) return;
        settingsRepository.saveSettings(settings);
        ollamaService.setBaseUrl(settings.getOllamaUrl());
    }

    public boolean testOllamaConnection(String url) {
        OllamaService testClient = new OllamaService(url);
        return testClient.testConnection();
    }

    public List<String> fetchAvailableModels(String url) throws OllamaException {
        OllamaService client = new OllamaService(url);
        return client.listModels();
    }

    public OllamaService getOllamaService() {
        AppSettings current = getSettings();
        ollamaService.setBaseUrl(current.getOllamaUrl());
        return ollamaService;
    }
}
