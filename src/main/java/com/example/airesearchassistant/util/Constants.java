package com.example.airesearchassistant.util;

/**
 * Application-wide constants for AI Research Assistant.
 */
public final class Constants {

    private Constants() {
        // Prevent instantiation
    }

    public static final String APP_NAME = "AI Research Assistant";
    public static final String APP_VERSION = "1.0.0";
    public static final String APP_SUBTITLE = "An Intelligent Research Paper Analysis System";

    // FXML Resource Paths
    public static final String FXML_MAIN_SHELL    = "/com/example/airesearchassistant/fxml/main-shell.fxml";
    public static final String FXML_DASHBOARD     = "/com/example/airesearchassistant/fxml/dashboard.fxml";
    public static final String FXML_LIBRARY       = "/com/example/airesearchassistant/fxml/library.fxml";
    public static final String FXML_ADD_PAPER     = "/com/example/airesearchassistant/fxml/add-paper.fxml";
    public static final String FXML_PAPER_DETAILS = "/com/example/airesearchassistant/fxml/paper-details.fxml";
    public static final String FXML_ASSISTANT     = "/com/example/airesearchassistant/fxml/assistant.fxml";
    public static final String FXML_COMPARISON    = "/com/example/airesearchassistant/fxml/comparison.fxml";
    public static final String FXML_COLLECTIONS   = "/com/example/airesearchassistant/fxml/collections.fxml";
    public static final String FXML_SETTINGS      = "/com/example/airesearchassistant/fxml/settings.fxml";
    public static final String FXML_ABOUT         = "/com/example/airesearchassistant/fxml/about.fxml";
    public static final String FXML_LAB_VIEW      = "/com/example/airesearchassistant/lab/lab-view.fxml";

    // CSS Resource Paths
    public static final String CSS_STYLE = "/com/example/airesearchassistant/style.css";
    public static final String CSS_DARK  = "/com/example/airesearchassistant/dark.css";

    // Default Configuration
    public static final String DEFAULT_OLLAMA_URL = "http://localhost:11434";
    public static final String DEFAULT_OLLAMA_MODEL = "llama3.2";
    public static final int DEFAULT_TIMEOUT_SECONDS = 120;
}
