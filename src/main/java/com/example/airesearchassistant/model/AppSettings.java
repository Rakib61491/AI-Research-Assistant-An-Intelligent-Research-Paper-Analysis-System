package com.example.airesearchassistant.model;

/**
 * AppSettings — Application configuration model.
 * Contains no JavaFX imports (Hard Rule 2).
 */
public class AppSettings {

    private String ollamaUrl = "http://localhost:11434";
    private String defaultModel = "llama3.2";
    private double temperature = 0.4;
    private int maxTokens = 800;
    private int timeoutSeconds = 120;
    private String theme = "dark";

    public AppSettings() {
    }

    public AppSettings(String ollamaUrl, String defaultModel, double temperature, int maxTokens, int timeoutSeconds, String theme) {
        this.ollamaUrl = ollamaUrl;
        this.defaultModel = defaultModel;
        this.temperature = temperature;
        this.maxTokens = maxTokens;
        this.timeoutSeconds = timeoutSeconds;
        this.theme = theme;
    }

    public String getOllamaUrl() { return ollamaUrl; }
    public void setOllamaUrl(String ollamaUrl) { this.ollamaUrl = ollamaUrl; }

    public String getDefaultModel() { return defaultModel; }
    public void setDefaultModel(String defaultModel) { this.defaultModel = defaultModel; }

    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }

    public int getMaxTokens() { return maxTokens; }
    public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }

    public int getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }

    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme; }
}
