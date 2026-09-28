package com.example.airesearchassistant.controller;

import com.example.airesearchassistant.model.ResearchPaper;
import com.example.airesearchassistant.service.PaperService;
import com.example.airesearchassistant.service.PdfExtractionService;
import com.example.airesearchassistant.service.PdfExtractionService.ExtractedPaperData;
import com.example.airesearchassistant.service.PdfExtractionService.ExtractedTable;
import com.example.airesearchassistant.util.AlertUtil;
import com.example.airesearchassistant.util.Constants;
import com.example.airesearchassistant.util.SceneManager;
import com.example.airesearchassistant.util.ValidationUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.sql.SQLException;

/**
 * AddPaperController — Form for manual entry, editing, PDF/TXT import, and JSON import/export.
 *
 * Fixed code-level issues:
 * 1. UI Threading: PDF extraction runs on a background Task with progress updates, cancel support,
 *    and thread safety without blocking the JavaFX Application Thread.
 * 2. Multi-Format Output: Displays structured JSON, clean Markdown, or extracted tabular data (CSV/Markdown).
 * 3. Structured Data Handling: Parses and saves metadata, abstract, methodology, findings, and complete
 *    structured JSON into SQLite database and exportable JSON files.
 */
public class AddPaperController {

    private static final String VIEW_MODE_MARKDOWN = "📄 Document Text / Markdown";
    private static final String VIEW_MODE_JSON = "🧩 Structured JSON";
    private static final String VIEW_MODE_TABLES = "📊 Extracted Tables (CSV/Markdown)";

    @FXML private TabPane addPaperTabPane;
    @FXML private Tab importTab;
    @FXML private Tab manualTab;

    // Import Tab Controls
    @FXML private Label selectedFileLabel;
    @FXML private ProgressBar importProgressBar;
    @FXML private ProgressIndicator importProgressIndicator;
    @FXML private Button cancelExtractionButton;
    @FXML private Label importStatusLabel;
    @FXML private ComboBox<String> previewFormatComboBox;
    @FXML private Button exportJsonButton;
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
    @FXML private TextArea methodologyArea;
    @FXML private TextArea findingsArea;
    @FXML private TextArea notesArea;

    private final PaperService paperService = new PaperService();
    private final PdfExtractionService extractionService = new PdfExtractionService();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private ResearchPaper paperToEdit;
    private ExtractedPaperData lastExtractedData;
    private Task<ExtractedPaperData> activeExtractionTask;

    @FXML
    public void initialize() {
        if (importProgressBar != null) importProgressBar.setVisible(false);
        if (importProgressIndicator != null) importProgressIndicator.setVisible(false);
        if (cancelExtractionButton != null) cancelExtractionButton.setVisible(false);

        if (previewFormatComboBox != null) {
            previewFormatComboBox.getItems().setAll(VIEW_MODE_MARKDOWN, VIEW_MODE_JSON, VIEW_MODE_TABLES);
            previewFormatComboBox.setValue(VIEW_MODE_MARKDOWN);
            previewFormatComboBox.valueProperty().addListener((obs, oldVal, newVal) -> updatePreviewView(newVal));
        }

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
        if (methodologyArea != null) methodologyArea.setText(paper.getMethodology());
        if (findingsArea != null) findingsArea.setText(paper.getFindings());
        if (notesArea != null) notesArea.setText(paper.getPersonalNotes());

        if (paper.getExtractedJson() != null && !paper.getExtractedJson().isBlank()) {
            if (previewTextArea != null) previewTextArea.setText(paper.getExtractedJson());
            if (exportJsonButton != null) exportJsonButton.setDisable(false);
        }

        if (formStatusLabel != null) {
            formStatusLabel.setText("Editing existing paper. Save when finished.");
        }

        if (addPaperTabPane != null && manualTab != null) {
            addPaperTabPane.getSelectionModel().select(manualTab);
        }
    }

    // =========================================================================
    // STAGE 5: FILE IMPORT & BACKGROUND EXTRACTION (UI THREADING & PARSING)
    // =========================================================================

    @FXML
    private void handleChooseFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Research Document (PDF or TXT)");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Supported Documents (*.pdf, *.txt)", "*.pdf", "*.txt"),
                new FileChooser.ExtensionFilter("PDF Documents (*.pdf)", "*.pdf"),
                new FileChooser.ExtensionFilter("Plain Text Files (*.txt)", "*.txt")
        );

        File file = fileChooser.showOpenDialog(titleField != null && titleField.getScene() != null
                ? titleField.getScene().getWindow() : null);
        if (file == null) return;

        processFileAsync(file);
    }

    /**
     * Imports paper details from a structured JSON file.
     */
    @FXML
    private void handleLoadJson() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Import Paper from JSON");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files (*.json)", "*.json"));

        File file = fileChooser.showOpenDialog(titleField != null && titleField.getScene() != null
                ? titleField.getScene().getWindow() : null);
        if (file == null) return;

        try {
            String jsonContent = Files.readString(file.toPath());
            JsonNode root = objectMapper.readTree(jsonContent);

            JsonNode meta = root.has("metadata") ? root.get("metadata") : root;
            if (titleField != null && meta.has("title")) titleField.setText(meta.get("title").asText());
            if (authorsField != null && meta.has("authors")) authorsField.setText(meta.get("authors").asText());
            if (yearField != null && meta.has("publicationYear")) yearField.setText(meta.get("publicationYear").asText());
            if (venueField != null && meta.has("journal")) venueField.setText(meta.get("journal").asText());
            if (doiField != null && meta.has("doi")) doiField.setText(meta.get("doi").asText());
            if (topicField != null && meta.has("topic")) topicField.setText(meta.get("topic").asText());
            if (keywordsField != null && meta.has("keywords")) keywordsField.setText(meta.get("keywords").asText());
            if (filePathField != null && meta.has("filePath")) filePathField.setText(meta.get("filePath").asText());

            if (abstractArea != null && root.has("abstract")) abstractArea.setText(root.get("abstract").asText());
            if (methodologyArea != null && root.has("methodology")) methodologyArea.setText(root.get("methodology").asText());
            if (findingsArea != null && root.has("findings")) findingsArea.setText(root.get("findings").asText());

            if (previewTextArea != null) previewTextArea.setText(jsonContent);
            if (previewFormatComboBox != null) previewFormatComboBox.setValue(VIEW_MODE_JSON);
            if (selectedFileLabel != null) selectedFileLabel.setText(file.getName());
            if (importStatusLabel != null) importStatusLabel.setText("JSON imported successfully. Review details in Tab 2.");
            if (exportJsonButton != null) exportJsonButton.setDisable(false);
            if (applyMetadataButton != null) applyMetadataButton.setDisable(false);

            AlertUtil.showInfo("JSON Import", "Import Successful", "Metadata and paper details populated from " + file.getName());
        } catch (Exception e) {
            AlertUtil.showError("JSON Parse Error", "Failed to Load JSON", e.getMessage());
        }
    }

    /**
     * Exports extracted structured JSON to a user-specified file.
     */
    @FXML
    private void handleExportJson() {
        String jsonToExport = "";
        if (lastExtractedData != null && lastExtractedData.json() != null && !lastExtractedData.json().isBlank()) {
            jsonToExport = lastExtractedData.json();
        } else if (previewTextArea != null && !previewTextArea.getText().isBlank()) {
            jsonToExport = previewTextArea.getText();
        }

        if (jsonToExport.isBlank()) {
            AlertUtil.showWarning("Export Notice", "No JSON Available", "Please import a PDF or document first.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Extracted Paper JSON");
        String defName = (titleField != null && !titleField.getText().isBlank())
                ? titleField.getText().trim().replaceAll("[^a-zA-Z0-9.-]", "_") + ".json"
                : "extracted_paper.json";
        fileChooser.setInitialFileName(defName);
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files (*.json)", "*.json"));

        File saveFile = fileChooser.showSaveDialog(previewTextArea.getScene().getWindow());
        if (saveFile != null) {
            try {
                Files.writeString(saveFile.toPath(), jsonToExport);
                AlertUtil.showInfo("Export Complete", "JSON Saved", "Structured paper JSON saved to:\n" + saveFile.getAbsolutePath());
            } catch (IOException e) {
                AlertUtil.showError("Export Failed", "Could not write JSON file", e.getMessage());
            }
        }
    }

    @FXML
    private void handleCancelExtraction() {
        if (activeExtractionTask != null && activeExtractionTask.isRunning()) {
            activeExtractionTask.cancel();
            if (importStatusLabel != null) importStatusLabel.setText("Extraction cancelled by user.");
            hideLoadingUi();
        }
    }

    private void processFileAsync(File file) {
        if (selectedFileLabel != null) {
            selectedFileLabel.setText(file.getName() + " (" + (file.length() / 1024) + " KB)");
        }
        if (importStatusLabel != null) {
            importStatusLabel.setText("Extracting layout, two-column text, tables, and metadata...");
        }

        showLoadingUi();

        // Background Task for non-blocking UI threading (Hard Rule 6)
        activeExtractionTask = new Task<>() {
            @Override
            protected ExtractedPaperData call() throws Exception {
                updateMessage("Analyzing document structure...");
                return extractionService.extract(file);
            }
        };

        activeExtractionTask.setOnSucceeded(event -> {
            hideLoadingUi();
            ExtractedPaperData data = activeExtractionTask.getValue();
            this.lastExtractedData = data;

            updatePreviewView(previewFormatComboBox != null ? previewFormatComboBox.getValue() : VIEW_MODE_MARKDOWN);

            if (importStatusLabel != null) {
                importStatusLabel.setText("✓ Extraction complete (" + data.pageCount() + " pages, "
                        + data.sections().size() + " sections, " + data.tables().size() + " tables). Metadata auto-filled!");
            }
            if (applyMetadataButton != null) applyMetadataButton.setDisable(false);
            if (exportJsonButton != null) exportJsonButton.setDisable(false);

            // Automatically autofill extracted metadata into form fields
            applyExtractedMetadata(data);
        });

        activeExtractionTask.setOnFailed(event -> {
            hideLoadingUi();
            Throwable exception = activeExtractionTask.getException();
            String message = (exception != null && exception.getMessage() != null)
                    ? exception.getMessage()
                    : "An unexpected error occurred while parsing the document.";

            if (importStatusLabel != null) {
                importStatusLabel.setText("Extraction failed: " + message);
            }
            AlertUtil.showError("Document Import Error", "Failed to Process File", message);
        });

        activeExtractionTask.setOnCancelled(event -> {
            hideLoadingUi();
            if (importStatusLabel != null) {
                importStatusLabel.setText("Extraction cancelled.");
            }
        });

        Thread thread = new Thread(activeExtractionTask, "PdfExtractionWorker");
        thread.setDaemon(true);
        thread.start();
    }

    private void showLoadingUi() {
        if (importProgressBar != null) {
            importProgressBar.setVisible(true);
            importProgressBar.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        }
        if (importProgressIndicator != null) importProgressIndicator.setVisible(true);
        if (cancelExtractionButton != null) cancelExtractionButton.setVisible(true);
    }

    private void hideLoadingUi() {
        if (importProgressBar != null) importProgressBar.setVisible(false);
        if (importProgressIndicator != null) importProgressIndicator.setVisible(false);
        if (cancelExtractionButton != null) cancelExtractionButton.setVisible(false);
    }

    private void updatePreviewView(String mode) {
        if (previewTextArea == null || lastExtractedData == null) return;

        if (VIEW_MODE_JSON.equals(mode)) {
            previewTextArea.setText(lastExtractedData.json());
        } else if (VIEW_MODE_TABLES.equals(mode)) {
            if (lastExtractedData.tables().isEmpty()) {
                previewTextArea.setText("No explicit tables were detected in this document.\n\nRaw Text Summary:\n"
                        + lastExtractedData.markdown());
            } else {
                StringBuilder sb = new StringBuilder("# Extracted Tables\n\n");
                for (ExtractedTable tbl : lastExtractedData.tables()) {
                    sb.append("### ").append(tbl.caption()).append("\n\n");
                    sb.append(tbl.markdown()).append("\n\n");
                    sb.append("CSV Format:\n```csv\n").append(tbl.csv()).append("```\n\n---\n\n");
                }
                previewTextArea.setText(sb.toString());
            }
        } else {
            // Default: Clean Markdown representation
            previewTextArea.setText(lastExtractedData.markdown() != null && !lastExtractedData.markdown().isBlank()
                    ? lastExtractedData.markdown()
                    : lastExtractedData.fullText());
        }
    }

    @FXML
    private void handleApplyMetadata() {
        if (lastExtractedData != null) {
            applyExtractedMetadata(lastExtractedData);
        }
        if (addPaperTabPane != null && manualTab != null) {
            addPaperTabPane.getSelectionModel().select(manualTab);
        }
    }

    private void applyExtractedMetadata(ExtractedPaperData data) {
        if (titleField != null) titleField.setText(data.title());
        if (authorsField != null) authorsField.setText(data.authors());
        if (yearField != null) yearField.setText(data.publicationYear());
        if (venueField != null && !data.journal().isEmpty()) venueField.setText(data.journal());
        if (doiField != null && !data.doi().isEmpty()) doiField.setText(data.doi());
        if (topicField != null && !data.topic().isEmpty()) topicField.setText(data.topic());
        if (keywordsField != null && !data.keywords().isEmpty()) keywordsField.setText(data.keywords());
        if (abstractArea != null) abstractArea.setText(data.abstractText());
        if (filePathField != null) filePathField.setText(data.filePath());

        if (methodologyArea != null && !data.methodology().isEmpty()) {
            methodologyArea.setText(data.methodology());
        }
        if (findingsArea != null && !data.findings().isEmpty()) {
            findingsArea.setText(data.findings());
        }
    }

    // =========================================================================
    // METADATA FORM ACTIONS & DATABASE PERSISTENCE
    // =========================================================================

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
            paper.setMethodology(methodologyArea != null ? methodologyArea.getText().trim() : "");
            paper.setFindings(findingsArea != null ? findingsArea.getText().trim() : "");
            paper.setPersonalNotes(notesArea != null ? notesArea.getText().trim() : "");

            // Save the structured JSON representation
            if (lastExtractedData != null && lastExtractedData.json() != null) {
                paper.setExtractedJson(lastExtractedData.json());
            }

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
        if (methodologyArea != null) methodologyArea.clear();
        if (findingsArea != null) findingsArea.clear();
        if (notesArea != null) notesArea.clear();
        if (previewTextArea != null) previewTextArea.clear();
        if (selectedFileLabel != null) selectedFileLabel.setText("No file selected");
        if (importStatusLabel != null) importStatusLabel.setText("Ready to import PDF or TXT.");
        if (applyMetadataButton != null) applyMetadataButton.setDisable(true);
        if (exportJsonButton != null) exportJsonButton.setDisable(true);
        lastExtractedData = null;
    }

    @FXML
    private void handleCancel() {
        SceneManager.getInstance().load(Constants.FXML_LIBRARY);
    }
}
