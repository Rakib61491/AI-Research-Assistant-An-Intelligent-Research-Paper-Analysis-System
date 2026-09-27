package com.example.airesearchassistant.controller;

import com.example.airesearchassistant.model.ResearchPaper;
import com.example.airesearchassistant.service.ExportService;
import com.example.airesearchassistant.service.OllamaService;
import com.example.airesearchassistant.service.PaperService;
import com.example.airesearchassistant.service.SettingsService;
import com.example.airesearchassistant.service.SimilarityService;
import com.example.airesearchassistant.service.SimilarityService.SimilarityResult;
import com.example.airesearchassistant.util.AlertUtil;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ComparisonController — Side-by-side local + AI comparative analysis of two research papers.
 * Hard Rule 6: long-running Ollama tasks use javafx.concurrent.Task.
 */
public class ComparisonController {

    @FXML private ComboBox<ResearchPaper> paperAComboBox;
    @FXML private ComboBox<ResearchPaper> paperBComboBox;
    @FXML private Label statusLabel;

    // Local similarity
    @FXML private Label similarityScoreLabel;
    @FXML private Label disclaimerLabel;
    @FXML private TextArea sharedTermsArea;
    @FXML private Label distinctiveALabel;
    @FXML private Label distinctiveBLabel;
    @FXML private TextArea distinctiveAArea;
    @FXML private TextArea distinctiveBArea;

    // AI comparison
    @FXML private TextArea aiOutputArea;
    @FXML private ProgressBar aiProgressBar;
    @FXML private Button aiCompareBtn;
    @FXML private Button localSimilarityBtn;

    private final PaperService paperService = new PaperService();
    private final SimilarityService similarityService = new SimilarityService();
    private final ExportService exportService = new ExportService();
    private final SettingsService settingsService = new SettingsService();

    private String lastExportContent = "";

    @FXML
    public void initialize() {
        loadPapers();
        if (disclaimerLabel != null) disclaimerLabel.setVisible(false);
    }

    private void loadPapers() {
        List<ResearchPaper> papers = paperService.getAllPapers();
        if (paperAComboBox != null) {
            paperAComboBox.setItems(FXCollections.observableArrayList(papers));
        }
        if (paperBComboBox != null) {
            paperBComboBox.setItems(FXCollections.observableArrayList(papers));
        }
        updateStatus();
    }

    private void updateStatus() {
        if (statusLabel != null) {
            int total = paperService.getTotalPaperCount();
            statusLabel.setText(total + " papers available. Select two to compare.");
        }
    }

    @FXML
    private void handleLocalSimilarity() {
        ResearchPaper a = paperAComboBox != null ? paperAComboBox.getValue() : null;
        ResearchPaper b = paperBComboBox != null ? paperBComboBox.getValue() : null;

        if (a == null || b == null) {
            AlertUtil.showWarning("Selection Required", "Please Select Two Papers",
                    "Both Paper A and Paper B must be selected before computing similarity.");
            return;
        }
        if (a.getId() == b.getId()) {
            AlertUtil.showWarning("Same Paper", "Identical Selection",
                    "Please select two different papers to compare.");
            return;
        }

        SimilarityResult result = similarityService.computeSimilarity(a, b);

        if (similarityScoreLabel != null) {
            similarityScoreLabel.setText(String.format("%.1f%%", result.similarityPercent()));
        }
        if (disclaimerLabel != null) disclaimerLabel.setVisible(true);

        if (sharedTermsArea != null) {
            sharedTermsArea.setText(result.sharedTerms().isEmpty()
                    ? "(no shared terms found)"
                    : String.join(", ", result.sharedTerms()));
        }

        if (distinctiveALabel != null) distinctiveALabel.setText("Distinctive to: " + a.getTitle());
        if (distinctiveBLabel != null) distinctiveBLabel.setText("Distinctive to: " + b.getTitle());

        if (distinctiveAArea != null) {
            distinctiveAArea.setText(result.distinctiveA().isEmpty()
                    ? "(none)"
                    : String.join(", ", result.distinctiveA()));
        }
        if (distinctiveBArea != null) {
            distinctiveBArea.setText(result.distinctiveB().isEmpty()
                    ? "(none)"
                    : String.join(", ", result.distinctiveB()));
        }

        // Build exportable local report
        lastExportContent = result.toReport(a.getTitle(), b.getTitle());

        if (statusLabel != null) {
            statusLabel.setText("Local similarity computed for: " + a.getTitle() + "  vs  " + b.getTitle());
        }
    }

    @FXML
    private void handleAiComparison() {
        ResearchPaper a = paperAComboBox != null ? paperAComboBox.getValue() : null;
        ResearchPaper b = paperBComboBox != null ? paperBComboBox.getValue() : null;

        if (a == null || b == null) {
            AlertUtil.showWarning("Selection Required", "Please Select Two Papers",
                    "Both Paper A and Paper B must be selected for AI comparison.");
            return;
        }
        if (a.getId() == b.getId()) {
            AlertUtil.showWarning("Same Paper", "Identical Selection",
                    "Please select two different papers to compare.");
            return;
        }

        com.example.airesearchassistant.model.AppSettings settings = settingsService.getSettings();
        String model = settings.getDefaultModel();
        double temperature = settings.getTemperature();
        int maxTokens = settings.getMaxTokens();
        int timeoutSecs = settings.getTimeoutSeconds();
        OllamaService ollama = new OllamaService(settings.getOllamaUrl());

        if (aiProgressBar != null) aiProgressBar.setVisible(true);
        if (aiCompareBtn != null) aiCompareBtn.setDisable(true);
        if (aiOutputArea != null) aiOutputArea.setText("Running AI comparative analysis…\nThis may take 30–120 seconds.");
        if (statusLabel != null) statusLabel.setText("⏳ Asking AI to compare: " + a.getTitle() + "  vs  " + b.getTitle() + "…");

        final ResearchPaper finalA = a;
        final ResearchPaper finalB = b;

        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                return paperService.comparePapers(finalA, finalB, ollama, model,
                        temperature, maxTokens, Duration.ofSeconds(timeoutSecs));
            }
        };

        task.setOnSucceeded(e -> {
            String result = task.getValue();
            if (aiOutputArea != null) aiOutputArea.setText(result);
            lastExportContent = "=== AI Comparison ===\n\nPaper A: " + finalA.getTitle() +
                    "\nPaper B: " + finalB.getTitle() + "\n\n" + result;
            if (statusLabel != null) statusLabel.setText("AI comparison complete.");
            if (aiProgressBar != null) aiProgressBar.setVisible(false);
            if (aiCompareBtn != null) aiCompareBtn.setDisable(false);
        });

        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            if (aiOutputArea != null) aiOutputArea.setText("AI comparison failed:\n" + ex.getMessage());
            if (statusLabel != null) statusLabel.setText("AI comparison failed.");
            if (aiProgressBar != null) aiProgressBar.setVisible(false);
            if (aiCompareBtn != null) aiCompareBtn.setDisable(false);
            AlertUtil.showError("AI Comparison Failed", "Could not complete comparison", ex.getMessage());
        });

        new Thread(task, "ai-comparison-thread").start();
    }

    @FXML
    private void handleExport() {
        if (lastExportContent == null || lastExportContent.isBlank()) {
            AlertUtil.showWarning("Nothing to Export", "No Content",
                    "Please run a local similarity or AI comparison first.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Comparison Report");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Markdown (*.md)", "*.md"),
                new FileChooser.ExtensionFilter("Plain Text (*.txt)", "*.txt")
        );
        chooser.setInitialFileName("comparison_report");

        File file = chooser.showSaveDialog(null);
        if (file == null) return;

        boolean asMarkdown = file.getName().endsWith(".md");
        try {
            exportService.exportTextContent(lastExportContent, "Comparison Report",
                    file.toPath(), asMarkdown);
            AlertUtil.showInfo("Export Successful", "File Saved",
                    "Report exported to:\n" + file.getAbsolutePath());
        } catch (Exception ex) {
            AlertUtil.showError("Export Failed", "Could not write file", ex.getMessage());
        }
    }
}
