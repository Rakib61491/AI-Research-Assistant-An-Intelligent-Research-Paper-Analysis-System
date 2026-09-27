package com.example.airesearchassistant.controller;

import com.example.airesearchassistant.model.ResearchPaper;
import com.example.airesearchassistant.util.Constants;
import com.example.airesearchassistant.util.SceneManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;

/**
 * PaperDetailsController — Detailed view of an individual paper, abstract, and notes.
 */
public class PaperDetailsController {

    @FXML private Label paperTitleLabel;
    @FXML private Label paperAuthorsLabel;
    @FXML private Label paperMetaLabel;
    @FXML private TextArea paperAbstractArea;
    @FXML private TextArea paperNotesArea;

    private ResearchPaper currentPaper;

    @FXML
    public void initialize() {
        if (currentPaper == null) {
            if (paperTitleLabel != null) paperTitleLabel.setText("No Paper Selected");
            if (paperAuthorsLabel != null) paperAuthorsLabel.setText("Select a paper from the Library to view details.");
        }
    }

    /**
     * Injects the selected paper to display its details.
     */
    public void setPaper(ResearchPaper paper) {
        this.currentPaper = paper;
        if (paper == null) return;

        if (paperTitleLabel != null) {
            paperTitleLabel.setText(paper.getTitle());
        }
        if (paperAuthorsLabel != null) {
            paperAuthorsLabel.setText(paper.getAuthors() != null ? paper.getAuthors() : "Unknown Authors");
        }
        if (paperMetaLabel != null) {
            StringBuilder meta = new StringBuilder();
            if (paper.getPublicationYear() != null && !paper.getPublicationYear().isEmpty()) {
                meta.append("Year: ").append(paper.getPublicationYear()).append("   ");
            }
            if (paper.getJournal() != null && !paper.getJournal().isEmpty()) {
                meta.append("Venue: ").append(paper.getJournal()).append("   ");
            }
            if (paper.getTopic() != null && !paper.getTopic().isEmpty()) {
                meta.append("Topic: ").append(paper.getTopic()).append("   ");
            }
            if (paper.getDoi() != null && !paper.getDoi().isEmpty()) {
                meta.append("DOI: ").append(paper.getDoi());
            }
            paperMetaLabel.setText(meta.toString());
        }
        if (paperAbstractArea != null) {
            paperAbstractArea.setText(paper.getAbstractText() != null ? paper.getAbstractText() : "No abstract available.");
        }
        if (paperNotesArea != null) {
            paperNotesArea.setText(paper.getPersonalNotes() != null ? paper.getPersonalNotes() : "");
        }
    }

    @FXML
    private void handleBackToLibrary() {
        SceneManager.getInstance().load(Constants.FXML_LIBRARY);
    }

    @FXML
    private void handleAskAiAboutPaper() {
        SceneManager.getInstance().load(Constants.FXML_ASSISTANT);
    }

    @FXML
    private void handleOpenPdf() {
        if (currentPaper == null || currentPaper.getFilePath() == null || currentPaper.getFilePath().trim().isEmpty()) {
            return;
        }
        File file = new File(currentPaper.getFilePath());
        if (file.exists() && Desktop.isDesktopSupported()) {
            try {
                Desktop.getDesktop().open(file);
            } catch (IOException e) {
                System.err.println("Could not open file: " + e.getMessage());
            }
        }
    }
}
