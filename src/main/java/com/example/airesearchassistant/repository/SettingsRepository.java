package com.example.airesearchassistant.repository;

import com.example.airesearchassistant.model.AppSettings;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * SettingsRepository — Key-value store for application settings.
 * All queries strictly use PreparedStatement (Hard Rule 5).
 */
public class SettingsRepository {

    private final DatabaseManager databaseManager;

    public SettingsRepository() {
        this.databaseManager = DatabaseManager.getInstance();
    }

    public SettingsRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public String get(String key, String defaultValue) throws SQLException {
        String sql = "SELECT value FROM settings WHERE key = ?;";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String val = rs.getString("value");
                    return val != null ? val : defaultValue;
                }
            }
        }
        return defaultValue;
    }

    public void set(String key, String value) throws SQLException {
        String sql = "INSERT INTO settings (key, value) VALUES (?, ?) ON CONFLICT(key) DO UPDATE SET value = excluded.value;";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();
        }
    }

    public Map<String, String> getAll() throws SQLException {
        Map<String, String> map = new HashMap<>();
        String sql = "SELECT key, value FROM settings;";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                map.put(rs.getString("key"), rs.getString("value"));
            }
        }
        return map;
    }

    public AppSettings loadSettings() {
        AppSettings settings = new AppSettings();
        // 1. Try to load from project-level app-settings.json first
        File projectConfigFile = new File("app-settings.json");
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        if (projectConfigFile.exists() && projectConfigFile.isFile() && projectConfigFile.length() > 0) {
            try {
                AppSettings fileSettings = mapper.readValue(projectConfigFile, AppSettings.class);
                if (fileSettings != null && fileSettings.getDefaultModel() != null && !fileSettings.getDefaultModel().isBlank()) {
                    settings = fileSettings;
                }
            } catch (Exception e) {
                System.err.println("Could not parse project app-settings.json: " + e.getMessage());
            }
        }

        // 2. Read / overlay from SQLite settings table
        try {
            String dbOllamaUrl = get("ollama_url", null);
            if (dbOllamaUrl != null && !dbOllamaUrl.isBlank()) settings.setOllamaUrl(dbOllamaUrl);

            String dbModel = get("default_model", null);
            if (dbModel != null && !dbModel.isBlank()) settings.setDefaultModel(dbModel);

            String tempStr = get("temperature", null);
            if (tempStr != null) {
                try { settings.setTemperature(Double.parseDouble(tempStr)); } catch (NumberFormatException ignored) {}
            }

            String maxTokensStr = get("max_tokens", null);
            if (maxTokensStr != null) {
                try { settings.setMaxTokens(Integer.parseInt(maxTokensStr)); } catch (NumberFormatException ignored) {}
            }

            String timeout = get("timeout_seconds", null);
            if (timeout != null) {
                try { settings.setTimeoutSeconds(Integer.parseInt(timeout)); } catch (NumberFormatException ignored) {}
            }

            String theme = get("theme", null);
            if (theme != null && !theme.isBlank()) settings.setTheme(theme);
        } catch (SQLException e) {
            System.err.println("Could not load settings from database: " + e.getMessage());
        }
        return settings;
    }

    public void saveSettings(AppSettings settings) throws SQLException {
        if (settings == null) return;

        // Persist to database
        set("ollama_url", settings.getOllamaUrl());
        set("default_model", settings.getDefaultModel());
        set("temperature", String.valueOf(settings.getTemperature()));
        set("max_tokens", String.valueOf(settings.getMaxTokens()));
        set("timeout_seconds", String.valueOf(settings.getTimeoutSeconds()));
        set("theme", settings.getTheme());

        // Also permanently save to project-level app-settings.json
        try {
            File projectConfigFile = new File("app-settings.json");
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.writerWithDefaultPrettyPrinter().writeValue(projectConfigFile, settings);
        } catch (Exception e) {
            System.err.println("Could not write project app-settings.json: " + e.getMessage());
        }
    }
}
