package com.example.airesearchassistant.repository;

import com.example.airesearchassistant.model.AppSettings;

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
        try {
            settings.setOllamaUrl(get("ollama_url", "http://localhost:11434"));
            settings.setDefaultModel(get("default_model", "llama3.2"));
            String tempStr = get("temperature", "0.4");
            try {
                settings.setTemperature(Double.parseDouble(tempStr));
            } catch (NumberFormatException ignored) {}

            String maxTokensStr = get("max_tokens", "800");
            try {
                settings.setMaxTokens(Integer.parseInt(maxTokensStr));
            } catch (NumberFormatException ignored) {}

            String timeout = get("timeout_seconds", "120");
            try {
                settings.setTimeoutSeconds(Integer.parseInt(timeout));
            } catch (NumberFormatException ignored) {}
            settings.setTheme(get("theme", "dark"));
        } catch (SQLException e) {
            System.err.println("Could not load settings from database: " + e.getMessage());
        }
        return settings;
    }

    public void saveSettings(AppSettings settings) throws SQLException {
        set("ollama_url", settings.getOllamaUrl());
        set("default_model", settings.getDefaultModel());
        set("temperature", String.valueOf(settings.getTemperature()));
        set("max_tokens", String.valueOf(settings.getMaxTokens()));
        set("timeout_seconds", String.valueOf(settings.getTimeoutSeconds()));
        set("theme", settings.getTheme());
    }
}
