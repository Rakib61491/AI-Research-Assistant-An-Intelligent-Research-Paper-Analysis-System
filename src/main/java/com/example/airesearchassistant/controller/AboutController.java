package com.example.airesearchassistant.controller;

import com.example.airesearchassistant.util.Constants;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

/**
 * AboutController — Project identity, version, and architecture details.
 */
public class AboutController {

    @FXML private Label appNameLabel;
    @FXML private Label versionLabel;
    @FXML private Label detailsLabel;

    @FXML
    public void initialize() {
        if (appNameLabel != null) {
            appNameLabel.setText(Constants.APP_NAME);
        }
        if (versionLabel != null) {
            versionLabel.setText("Version " + Constants.APP_VERSION);
        }
        if (detailsLabel != null) {
            detailsLabel.setText(Constants.APP_SUBTITLE + "\n\n" +
                    "Built with Java 21, JavaFX 21, SQLite JDBC, Apache PDFBox, and Ollama Local AI.\n" +
                    "Architected in clean MVC pattern.");
        }
    }
}
