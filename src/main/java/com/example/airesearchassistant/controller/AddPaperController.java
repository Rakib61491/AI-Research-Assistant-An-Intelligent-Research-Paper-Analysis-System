package com.example.airesearchassistant.controller;

import com.example.airesearchassistant.model.ResearchPaper;
import com.example.airesearchassistant.service.PaperService;
import com.example.airesearchassistant.service.PdfExtractionService;
import com.example.airesearchassistant.service.PdfExtractionService.ExtractedPaperData;
import com.example.airesearchassistant.util.AlertUtil;
import com.example.airesearchassistant.util.Constants;
import com.example.airesearchassistant.util.SceneManager;
import com.example.airesearchassistant.util.ValidationUtil;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.sql.SQLException;

/**
 * AddPaperController — Form for manual entry, editing, and PDF/TXT import.
 * Long-running text extraction runs asynchronously on a javafx.concurrent.Task (Hard Rule 6).
 */
public class AddPaperController {

    @FXML private TabPane addPaperTabPane;
    @FXML private Tab importTab;
    @FXML private Tab manualTab;

    // Import Tab Controls
    @FXML private Label selectedFileLabel;
    @FXML private ProgressBar importProgressBar;
    @FXML private ProgressIndicator importProgressIndicator;
    @FXML private Label importStatusLabel;
    @FXML private TextArea previewTextArea;
    @FXML private Button applyMetadataButton;

    // Metadata Form Controls
    @FXML private Label pageHeadlineLabel;
    @FXML private Label formStatusLabel;
    @FXML private TextField titleField;
    @FXML private TextField authorsField;
    @FXML private TextField yearField;
    @FXML private TextField venueField;
    @FXML private TextField doiField;
    @FXML private TextField topicField;
    @FXML private TextField keywordsField;
    @FXML private TextField filePathField;
    @FXML private TextArea abstractArea;
    @FXML private TextArea notesArea;

    private final PaperService paperService = new PaperService();
    private final PdfExtractionService extractionService = new PdfExtractionService();
    private ResearchPaper paperToEdit;
    private ExtractedPaperData lastExtractedData;

    @FXML
    public void initialize() {
        if (importProgressBar != null) importProgressBar.setVisible(false);
        if (importProgressIndicator != null) importProgressIndicator.setVisible(false);
        if (formStatusLabel != null) {
            formStatusLabel.setText("Fill in paper details or import a PDF/TXT document to autofill.");
        }
    }

    /**
     * Configures the form for editing an existing paper.
     */
    public void setPaperForEdit(ResearchPaper paper) {
        this.paperToEdit = paper;
        if (paper == null) return;

        if (pageHeadlineLabel != null) {
            pageHeadlineLabel.setText("Edit Research Paper (ID: " + paper.getId() + ")");
        }
        if (titleField != null) titleField.setText(paper.getTitle());
        if (authorsField != null) authorsField.setText(paper.getAuthors());
        if (yearField != null) yearField.setText(paper.getPublicationYear());
        if (venueField != null) venueField.setText(paper.getJournal());
        if (doiField != null) doiField.setText(paper.getDoi());
        if (topicField != null) topicField.setText(paper.getTopic());
        if (keywordsField != null) keywordsField.setText(paper.getKeywords());
        if (filePathField != null) filePathField.setText(paper.getFilePath());
        if (abstractArea != null) abstractArea.setText(paper.getAbstractText());
        if (notesArea != null) notesArea.setText(paper.getPersonalNotes());
        if (formStatusLabel != null) {
            formStatusLabel.setText("Editing existing paper. Save when finished.");
        }

        // Switch to the metadata form tab
        if (addPaperTabPane != null && manualTab != null) {
            addPaperTabPane.getSelectionModel().select(manualTab);
        }
    }

    // ----- Stage 5: File Import & Background Extraction ----------------------

    @FXML
    private void handleChooseFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Research Document (PDF or TXT)");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Supported Documents (*.pdf, *.txt)", "*.pdf", "*.txt"),
                new FileChooser.ExtensionFilter("PDF Documents (*.pdf)", "*.pdf"),
                new FileChooser.ExtensionFilter("Plain Text Files (*.txt)", "*.txt")
        );

        File file = fileChooser.showOpenDialog(titleField.getScene().getWindow());
        if (file == null) {
            return;
        }

        processFileAsync(file);
    }

    private void processFileAsync(File file) {
        if (selectedFileLabel != null) {
            selectedFileLabel.setText(file.getName() + " (" + (file.length() / 1024) + " KB)");
        }
        if (importStatusLabel != null) {
            importStatusLabel.setText("Extracting text and parsing metadata...");
        }
        if (importProgressBar != null) {
            importProgressBar.setVisible(true);
            importProgressBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        }
        if (importProgressIndicator != null) {
            importProgressIndicator.setVisible(true);
        }

        // Execute PDF/TXT parsing on a background task (Hard Rule 6)
        Task<ExtractedPaperData> extractionTask = new Task<>() {
            @Override
            protected ExtractedPaperData call() throws Exception {
                return extractionService.extract(file);
            }
        };

        extractionTask.setOnSucceeded(event -> {
            if (importProgressBar != null) importProgressBar.setVisible(false);
            if (importProgressIndicator != null) importProgressIndicator.setVisible(false);

            ExtractedPaperData data = extractionTask.getValue();
            this.lastExtractedData = data;

            if (previewTextArea != null) {
                previewTextArea.setText(data.fullText());
            }
            if (importStatusLabel != null) {
                importStatusLabel.setText("Extraction complete! Metadata pre-filled into form.");
            }
            if (applyMetadataButton != null) {
                applyMetadataButton.setDisable(false);
            }

            // Automatically apply extracted metadata
            applyExtractedMetadata(data);
        });

        extractionTask.setOnFailed(event -> {
            if (importProgressBar != null) importProgressBar.setVisible(false);
            if (importProgressIndicator != null) importProgressIndicator.setVisible(false);

            Throwable exception = extractionTask.getException();
            String message = (exception != null && exception.getMessage() != null)
                    ? exception.getMessage()
                    : "An unexpected error occurred while parsing the document.";

            if (importStatusLabel != null) {
                importStatusLabel.setText("Extraction failed: " + message);
            }

            // Display friendly alert without raw stack traces (Hard Rule 7)
            AlertUtil.showError("Document Import Error", "Failed to Process File", message);
        });

        Thread thread = new Thread(extractionTask, "PdfExtractionWorker");
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void handleApplyMetadata() {
        if (lastExtractedData != null) {
            applyExtractedMetadata(lastExtractedData);
            if (addPaperTabPane != null && manualTab != null) {
                addPaperTabPane.getSelectionModel().select(manualTab);
            }
        }
    }

    private void applyExtractedMetadata(ExtractedPaperData data) {
        if (titleField != null && (titleField.getText() == null || titleField.getText().trim().isEmpty())) {
            titleField.setText(data.title());
        }
        if (authorsField != null && (authorsField.getText() == null || authorsField.getText().trim().isEmpty())) {
            authorsField.setText(data.authors());
        }
        if (yearField != null && (yearField.getText() == null || yearField.getText().trim().isEmpty())) {
            yearField.setText(data.publicationYear());
        }
        if (abstractArea != null && (abstractArea.getText() == null || abstractArea.getText().trim().isEmpty())) {
            abstractArea.setText(data.abstractText());
        }
        if (filePathField != null) {
            filePathField.setText(data.filePath());
        }
    }

    // ----- Metadata Form Actions ---------------------------------------------

    @FXML
    private void handleSavePaper() {
        String title = titleField != null ? titleField.getText().trim() : "";
        if (!ValidationUtil.isValidTitle(title)) {
            AlertUtil.showWarning("Validation Error", "Missing Paper Title", "A paper title is required.");
            if (addPaperTabPane != null && manualTab != null) {
                addPaperTabPane.getSelectionModel().select(manualTab);
            }
            return;
        }

        String year = yearField != null ? yearField.getText().trim() : "";
        if (!ValidationUtil.isValidYear(year)) {
            AlertUtil.showWarning("Validation Error", "Invalid Publication Year",
                    "Publication year must be a 4-digit number (e.g. 2024).");
            if (addPaperTabPane != null && manualTab != null) {
                addPaperTabPane.getSelectionModel().select(manualTab);
            }
            return;
        }

        try {
            boolean isEdit = (paperToEdit != null);
            ResearchPaper paper = isEdit ? paperToEdit : new ResearchPaper();

            paper.setTitle(title);
            paper.setAuthors(authorsField != null ? authorsField.getText().trim() : "");
            paper.setPublicationYear(year);
            paper.setJournal(venueField != null ? venueField.getText().trim() : "");
            paper.setDoi(doiField != null ? doiField.getText().trim() : "");
            paper.setTopic(topicField != null ? topicField.getText().trim() : "");
            paper.setKeywords(keywordsField != null ? keywordsField.getText().trim() : "");
            paper.setFilePath(filePathField != null ? filePathField.getText().trim() : "");
            paper.setAbstractText(abstractArea != null ? abstractArea.getText().trim() : "");
            paper.setPersonalNotes(notesArea != null ? notesArea.getText().trim() : "");

            if (isEdit) {
                paperService.updatePaper(paper);
                AlertUtil.showInfo("Paper Updated", "Success", "Paper \"" + title + "\" has been updated.");
            } else {
                int id = paperService.createPaper(paper);
                AlertUtil.showInfo("Paper Saved", "Success", "Paper registered successfully with ID #" + id + ".");
            }

            // Return to Library view
            SceneManager.getInstance().load(Constants.FXML_LIBRARY);
        } catch (SQLException e) {
            AlertUtil.showError("Database Error", "Failed to save paper", e.getMessage());
        }
    }

    @FXML
    private void handleClearForm() {
        if (titleField != null) titleField.clear();
        if (authorsField != null) authorsField.clear();
        if (yearField != null) yearField.clear();
        if (venueField != null) venueField.clear();
        if (doiField != null) doiField.clear();
        if (topicField != null) topicField.clear();
        if (keywordsField != null) keywordsField.clear();
        if (filePathField != null) filePathField.clear();
        if (abstractArea != null) abstractArea.clear();
        if (notesArea != null) notesArea.clear();
        if (previewTextArea != null) previewTextArea.clear();
        if (selectedFileLabel != null) selectedFileLabel.setText("No file selected");
        if (importStatusLabel != null) importStatusLabel.setText("Ready to import PDF or TXT.");
        if (applyMetadataButton != null) applyMetadataButton.setDisable(true);
        lastExtractedData = null;
    }

    @FXML
    private void handleCancel() {
        SceneManager.getInstance().load(Constants.FXML_LIBRARY);
    }
}
