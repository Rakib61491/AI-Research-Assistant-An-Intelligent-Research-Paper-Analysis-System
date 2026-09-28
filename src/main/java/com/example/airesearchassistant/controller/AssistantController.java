package com.example.airesearchassistant.controller;

import com.example.airesearchassistant.model.AppSettings;
import com.example.airesearchassistant.model.ResearchPaper;
import com.example.airesearchassistant.service.ExportService;
import com.example.airesearchassistant.service.OllamaException;
import com.example.airesearchassistant.service.OllamaService;
import com.example.airesearchassistant.service.OllamaService.ChatMessage;
import com.example.airesearchassistant.service.PaperService;
import com.example.airesearchassistant.service.SettingsService;
import com.example.airesearchassistant.util.AlertUtil;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.sql.SQLException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * AssistantController — AI Research Assistant interface for local LLM inference.
 * Features structured 7-section summarization, information extraction, context-aware Q&A chat,
 * topic classification, background Task execution with cancel support, and offline instructions banner.
 */
public class AssistantController {

    // Offline Banner
    @FXML private HBox offlineBanner;
    @FXML private Label offlineBannerDetailLabel;

    // Left Panel: Paper Selection & Action Buttons
    @FXML private ComboBox<ResearchPaper> paperComboBox;
    @FXML private Button summarizeButton;
    @FXML private Button extractButton;
    @FXML private Button classifyButton;

    // Progress & Cancel Controls
    @FXML private HBox progressBox;
    @FXML private ProgressIndicator aiProgressIndicator;
    @FXML private Label progressStatusLabel;
    @FXML private Button cancelTaskButton;

    // Right Panel: Analysis TabPane
    @FXML private TabPane analysisTabPane;
    @FXML private Tab summaryTab;
    @FXML private Tab extractedTab;
    @FXML private Tab qaTab;

    // Tab 1: Summary Tab Controls
    @FXML private TextArea summaryTextArea;
    @FXML private Button copySummaryButton;
    @FXML private Button saveSummaryButton;
    @FXML private Label summaryStatusLabel;

    // Tab 2: Extracted Information Tab Controls
    @FXML private TextArea extractedAbstractArea;
    @FXML private TextArea extractedMethodologyArea;
    @FXML private TextArea extractedFindingsArea;
    @FXML private TextField extractedKeywordsField;
    @FXML private TextField extractedTopicField;
    @FXML private TextArea extractedContributionsArea;
    @FXML private TextArea extractedLimitationsArea;
    @FXML private Button saveExtractedButton;
    @FXML private Label extractedStatusLabel;

    // Tab 3: Q&A Chat Tab Controls
    @FXML private ListView<String> chatListView;
    @FXML private TextField chatInputField;
    @FXML private Button sendChatButton;

    private final PaperService paperService = new PaperService();
    private final SettingsService settingsService = new SettingsService();
    private final ExportService exportService = new ExportService();
    private final ObservableList<String> chatHistory = FXCollections.observableArrayList();
    private final List<ChatMessage> conversationMessages = new ArrayList<>();

    private Task<?> activeAiTask;

    @FXML
    public void initialize() {
        if (progressBox != null) progressBox.setVisible(false);
        if (chatListView != null) chatListView.setItems(chatHistory);

        loadPapersIntoDropdown();
        checkOllamaConnectionAsync();
    }

    private void loadPapersIntoDropdown() {
        List<ResearchPaper> papers = paperService.getAllPapers();
        if (paperComboBox != null) {
            paperComboBox.getItems().setAll(papers);
            if (!papers.isEmpty()) {
                paperComboBox.setValue(papers.get(0));
            }
        }
    }

    private void checkOllamaConnectionAsync() {
        AppSettings settings = settingsService.getSettings();
        String url = settings.getOllamaUrl();
        String model = settings.getDefaultModel();
        Task<Boolean> checkTask = new Task<>() {
            @Override
            protected Boolean call() {
                return settingsService.testOllamaConnection(url);
            }
        };

        checkTask.setOnSucceeded(e -> {
            boolean online = checkTask.getValue();
            if (offlineBanner != null) {
                offlineBanner.setVisible(!online);
                offlineBanner.setManaged(!online);
            }
            if (!online && offlineBannerDetailLabel != null) {
                offlineBannerDetailLabel.setText(
                    "URL: " + url + "  |  Model: " + model +
                    "  — Make sure Ollama is running and the model is pulled."
                );
            }
        });

        checkTask.setOnFailed(e -> {
            if (offlineBanner != null) {
                offlineBanner.setVisible(true);
                offlineBanner.setManaged(true);
            }
            if (offlineBannerDetailLabel != null) {
                offlineBannerDetailLabel.setText(
                    "URL: " + url + "  |  Model: " + model +
                    "  — Could not reach Ollama. Is it installed and running?"
                );
            }
        });

        Thread thread = new Thread(checkTask, "OllamaHealthCheckWorker");
        thread.setDaemon(true);
        thread.start();
    }

    // ----- Left Action 1: Summarize Paper ------------------------------------

    @FXML
    private void handleSummarize() {
        ResearchPaper paper = getSelectedPaper();
        if (paper == null) return;

        AppSettings settings = settingsService.getSettings();
        OllamaService ollama = settingsService.getOllamaService();

        // Switch to summary tab immediately so user sees incoming tokens
        if (analysisTabPane != null && summaryTab != null) {
            analysisTabPane.getSelectionModel().select(summaryTab);
        }
        if (summaryTextArea != null) {
            summaryTextArea.clear();
        }
        if (summaryStatusLabel != null) {
            summaryStatusLabel.setText("Connecting to " + settings.getDefaultModel() + " and streaming summary...");
        }

        showProgress("Streaming 7-section paper summary using " + settings.getDefaultModel() + "...");

        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                return paperService.summarizePaperStream(
                        paper, ollama, settings.getDefaultModel(),
                        settings.getTemperature(), settings.getMaxTokens(),
                        Duration.ofSeconds(settings.getTimeoutSeconds()),
                        token -> Platform.runLater(() -> {
                            if (summaryTextArea != null) {
                                summaryTextArea.appendText(token);
                            }
                        })
                );
            }
        };

        task.setOnSucceeded(e -> {
            hideProgress();
            if (summaryStatusLabel != null) {
                summaryStatusLabel.setText("✓ Summary generated successfully via structured JSON context. Ready to save.");
            }
        });

        task.setOnFailed(e -> handleTaskFailure(task.getException(), "Summarization Failed"));

        startAiTask(task);
    }

    // ----- Left Action 2: Extract Information (Targeted JSON sections with streaming) ----

    @FXML
    private void handleExtractInfo() {
        ResearchPaper paper = getSelectedPaper();
        if (paper == null) return;

        AppSettings settings = settingsService.getSettings();
        OllamaService ollama = settingsService.getOllamaService();
        String model = settings.getDefaultModel();
        double temp = settings.getTemperature();
        int maxTokens = settings.getMaxTokens();
        Duration timeout = Duration.ofSeconds(settings.getTimeoutSeconds());

        // Switch to extracted tab immediately
        if (analysisTabPane != null && extractedTab != null) {
            analysisTabPane.getSelectionModel().select(extractedTab);
        }

        // Clear existing fields
        if (extractedAbstractArea != null) extractedAbstractArea.clear();
        if (extractedMethodologyArea != null) extractedMethodologyArea.clear();
        if (extractedFindingsArea != null) extractedFindingsArea.clear();
        if (extractedKeywordsField != null) extractedKeywordsField.clear();
        if (extractedTopicField != null) extractedTopicField.clear();
        if (extractedContributionsArea != null) extractedContributionsArea.clear();
        if (extractedLimitationsArea != null) extractedLimitationsArea.clear();

        showProgress("Extracting structured info from JSON sections using " + model + "...");

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                // Ensure paper JSON is ready
                paperService.ensurePaperJson(paper);

                // Step 1: Abstract & Metadata (Keywords, Topic)
                if (isCancelled()) return null;
                Platform.runLater(() -> {
                    if (progressStatusLabel != null) {
                        progressStatusLabel.setText("Extracting Abstract & Metadata from JSON...");
                    }
                    if (extractedStatusLabel != null) {
                        extractedStatusLabel.setText("Extracting abstract and metadata from JSON...");
                    }
                });

                String abstractCtx = (paper.getAbstractText() != null && !paper.getAbstractText().isBlank())
                        ? paper.getAbstractText()
                        : paper.getTitle();

                String metaPrompt = "Given this paper title: \"" + paper.getTitle() + "\" and abstract/context:\n" +
                        abstractCtx + "\n\n" +
                        "Extract the following 3 items strictly in this format:\n" +
                        "TOPIC: (single concise research field, e.g. Natural Language Processing, Machine Learning, Computer Vision)\n" +
                        "KEYWORDS: (up to 8 comma-separated keywords)\n" +
                        "ABSTRACT:\n(clean, concise abstract paragraph)\n";

                String metaResponse = ollama.generate(model, metaPrompt, temp, maxTokens, timeout);
                if (isCancelled()) return null;
                Platform.runLater(() -> parseAndPopulateMetaFields(metaResponse, paper));

                // Step 2: Methodology (using ONLY methodology JSON section)
                if (isCancelled()) return null;
                Platform.runLater(() -> {
                    if (progressStatusLabel != null) {
                        progressStatusLabel.setText("Streaming Methodology from JSON methodology section...");
                    }
                    if (extractedStatusLabel != null) {
                        extractedStatusLabel.setText("Streaming methodology from JSON...");
                    }
                });

                String methContext = paperService.resolveMethodologyContext(paper);
                String methPrompt = "You are a research analyst. Based strictly on the methodology and approach section below from the research paper, provide a structured summary of the methodology, models/techniques, and experimental setup:\n\n" + methContext;
                ollama.extractInfoStream(methPrompt, model, temp, maxTokens, timeout, token -> {
                    Platform.runLater(() -> {
                        if (extractedMethodologyArea != null) {
                            extractedMethodologyArea.appendText(token);
                        }
                    });
                });

                // Step 3: Main Findings (using ONLY results/findings JSON section)
                if (isCancelled()) return null;
                Platform.runLater(() -> {
                    if (progressStatusLabel != null) {
                        progressStatusLabel.setText("Streaming Findings from JSON results section...");
                    }
                    if (extractedStatusLabel != null) {
                        extractedStatusLabel.setText("Streaming main findings from JSON...");
                    }
                });

                String findContext = paperService.resolveFindingsContext(paper);
                String findPrompt = "You are a research analyst. Based strictly on the results and findings section below from the research paper, summarize the main empirical findings, performance results, and key takeaways:\n\n" + findContext;
                ollama.extractInfoStream(findPrompt, model, temp, maxTokens, timeout, token -> {
                    Platform.runLater(() -> {
                        if (extractedFindingsArea != null) {
                            extractedFindingsArea.appendText(token);
                        }
                    });
                });

                // Step 4: Contributions (using ONLY intro & conclusion JSON sections)
                if (isCancelled()) return null;
                Platform.runLater(() -> {
                    if (progressStatusLabel != null) {
                        progressStatusLabel.setText("Streaming Contributions from JSON intro & conclusion...");
                    }
                    if (extractedStatusLabel != null) {
                        extractedStatusLabel.setText("Streaming contributions from JSON...");
                    }
                });

                String contContext = paperService.resolveContributionsContext(paper);
                String contPrompt = "You are a research analyst. Based strictly on the introduction and conclusion sections below from the research paper, list the key novel contributions of this research in bullet points:\n\n" + contContext;
                ollama.extractInfoStream(contPrompt, model, temp, maxTokens, timeout, token -> {
                    Platform.runLater(() -> {
                        if (extractedContributionsArea != null) {
                            extractedContributionsArea.appendText(token);
                        }
                    });
                });

                // Step 5: Limitations (using ONLY discussion & conclusion JSON sections)
                if (isCancelled()) return null;
                Platform.runLater(() -> {
                    if (progressStatusLabel != null) {
                        progressStatusLabel.setText("Streaming Limitations from JSON discussion & conclusion...");
                    }
                    if (extractedStatusLabel != null) {
                        extractedStatusLabel.setText("Streaming limitations from JSON...");
                    }
                });

                String limContext = paperService.resolveLimitationsContext(paper);
                String limPrompt = "You are a research analyst. Based strictly on the discussion, limitations, and conclusion sections below from the research paper, list the limitations, constraints, assumptions, and threats to validity in bullet points:\n\n" + limContext;
                ollama.extractInfoStream(limPrompt, model, temp, maxTokens, timeout, token -> {
                    Platform.runLater(() -> {
                        if (extractedLimitationsArea != null) {
                            extractedLimitationsArea.appendText(token);
                        }
                    });
                });

                return null;
            }
        };

        task.setOnSucceeded(e -> {
            hideProgress();
            if (extractedStatusLabel != null) {
                extractedStatusLabel.setText("✓ All fields extracted from targeted JSON sections. Ready to save.");
            }
        });

        task.setOnFailed(e -> handleTaskFailure(task.getException(), "Information Extraction Failed"));

        startAiTask(task);
    }

    private void parseAndPopulateMetaFields(String text, ResearchPaper paper) {
        if (text == null || text.isBlank()) {
            if (paper.getAbstractText() != null && extractedAbstractArea != null) {
                extractedAbstractArea.setText(paper.getAbstractText());
            }
            if (paper.getTopic() != null && extractedTopicField != null) {
                extractedTopicField.setText(paper.getTopic());
            }
            if (paper.getKeywords() != null && extractedKeywordsField != null) {
                extractedKeywordsField.setText(paper.getKeywords());
            }
            return;
        }

        String[] lines = text.split("\\r?\\n");
        StringBuilder abstractBuilder = new StringBuilder();
        boolean inAbstract = false;

        for (String line : lines) {
            String trimmed = line.trim();
            String lower = trimmed.toLowerCase();
            if (lower.startsWith("topic:")) {
                String topic = trimmed.substring(6).trim();
                if (extractedTopicField != null && !topic.isEmpty()) {
                    extractedTopicField.setText(topic);
                }
            } else if (lower.startsWith("keywords:")) {
                String kw = trimmed.substring(9).trim();
                if (extractedKeywordsField != null && !kw.isEmpty()) {
                    extractedKeywordsField.setText(kw);
                }
            } else if (lower.startsWith("abstract:")) {
                inAbstract = true;
                String rest = trimmed.substring(9).trim();
                if (!rest.isEmpty()) abstractBuilder.append(rest).append("\n");
            } else if (inAbstract) {
                abstractBuilder.append(line).append("\n");
            }
        }

        if (extractedAbstractArea != null) {
            String finalAbstract = abstractBuilder.toString().trim();
            if (finalAbstract.isEmpty() && paper.getAbstractText() != null) {
                finalAbstract = paper.getAbstractText();
            }
            extractedAbstractArea.setText(finalAbstract);
        }
    }

    // ----- Left Action 3: Classify Topic -------------------------------------

    @FXML
    private void handleClassifyTopic() {
        ResearchPaper paper = getSelectedPaper();
        if (paper == null) return;

        AppSettings settings = settingsService.getSettings();
        OllamaService ollama = settingsService.getOllamaService();
        List<String> topics = paperService.getAvailableTopics();

        showProgress("Classifying research topic using " + settings.getDefaultModel() + "...");

        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                return paperService.classifyPaperTopic(paper, topics, ollama, settings.getDefaultModel(), Duration.ofSeconds(settings.getTimeoutSeconds()));
            }
        };

        task.setOnSucceeded(e -> {
            hideProgress();
            String classifiedTopic = task.getValue();
            paper.setTopic(classifiedTopic);
            try {
                paperService.updatePaper(paper);
                if (extractedTopicField != null) extractedTopicField.setText(classifiedTopic);
                AlertUtil.showInfo("Topic Classified", "Classification Complete",
                        "Paper \"" + paper.getTitle() + "\" has been classified as:\n\n" + classifiedTopic);
            } catch (SQLException ex) {
                AlertUtil.showError("Database Error", "Failed to update paper topic", ex.getMessage());
            }
        });

        task.setOnFailed(e -> handleTaskFailure(task.getException(), "Classification Failed"));

        startAiTask(task);
    }

    // ----- Tab 1 Actions: Copy & Save Summary --------------------------------

    @FXML
    private void handleCopySummary() {
        if (summaryTextArea != null && !summaryTextArea.getText().isEmpty()) {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(summaryTextArea.getText());
            clipboard.setContent(content);
            if (summaryStatusLabel != null) {
                summaryStatusLabel.setText("✓ Summary copied to system clipboard!");
            }
        }
    }

    @FXML
    private void handleSaveSummary() {
        ResearchPaper paper = getSelectedPaper();
        if (paper == null || summaryTextArea == null || summaryTextArea.getText().trim().isEmpty()) {
            AlertUtil.showWarning("Missing Summary", "No Summary Content", "Please generate a summary before saving.");
            return;
        }

        try {
            paperService.saveAiSummary(paper.getId(), summaryTextArea.getText().trim());
            paper.setAiSummary(summaryTextArea.getText().trim());
            if (summaryStatusLabel != null) {
                summaryStatusLabel.setText("✓ AI Summary saved to paper record and marked as analyzed.");
            }
            AlertUtil.showInfo("Summary Saved", "Record Updated", "AI Summary has been attached to paper record.");
        } catch (SQLException e) {
            AlertUtil.showError("Database Error", "Failed to save AI summary", e.getMessage());
        }
    }

    // ----- Tab 2 Actions: Save Extracted Fields ------------------------------

    @FXML
    private void handleSaveExtracted() {
        ResearchPaper paper = getSelectedPaper();
        if (paper == null) return;

        if (extractedAbstractArea != null && !extractedAbstractArea.getText().trim().isEmpty()) {
            paper.setAbstractText(extractedAbstractArea.getText().trim());
        }
        if (extractedMethodologyArea != null && !extractedMethodologyArea.getText().trim().isEmpty()) {
            paper.setMethodology(extractedMethodologyArea.getText().trim());
        }
        if (extractedFindingsArea != null && !extractedFindingsArea.getText().trim().isEmpty()) {
            paper.setFindings(extractedFindingsArea.getText().trim());
        }
        if (extractedKeywordsField != null && !extractedKeywordsField.getText().trim().isEmpty()) {
            paper.setKeywords(extractedKeywordsField.getText().trim());
        }
        if (extractedTopicField != null && !extractedTopicField.getText().trim().isEmpty()) {
            paper.setTopic(extractedTopicField.getText().trim());
        }

        try {
            paperService.updatePaper(paper);
            if (extractedStatusLabel != null) {
                extractedStatusLabel.setText("✓ Extracted fields updated in database!");
            }
            AlertUtil.showInfo("Paper Updated", "Success", "Extracted metadata saved to paper record.");
        } catch (SQLException e) {
            AlertUtil.showError("Database Error", "Failed to save extracted information", e.getMessage());
        }
    }

    // ----- Tab 3 Actions: Context-Aware Q&A ----------------------------------

    @FXML
    private void handleSendChat() {
        ResearchPaper paper = getSelectedPaper();
        if (paper == null) {
            AlertUtil.showWarning("Selection Required", "No Paper Selected", "Please select a paper from the dropdown first.");
            return;
        }

        String question = chatInputField != null ? chatInputField.getText().trim() : "";
        if (question.isEmpty()) return;

        chatInputField.clear();
        chatHistory.add("You: " + question);

        AppSettings settings = settingsService.getSettings();
        OllamaService ollama = settingsService.getOllamaService();

        showProgress("Consulting Ollama (" + settings.getDefaultModel() + ")...");

        // Add assistant placeholder for streaming
        int assistantIndex = chatHistory.size();
        chatHistory.add("Assistant: ");
        if (chatListView != null) {
            chatListView.scrollTo(assistantIndex);
        }

        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                return paperService.askPaperQuestionStream(
                        paper, question, conversationMessages, ollama,
                        settings.getDefaultModel(), settings.getTemperature(), settings.getMaxTokens(),
                        Duration.ofSeconds(settings.getTimeoutSeconds()),
                        token -> Platform.runLater(() -> {
                            if (assistantIndex < chatHistory.size()) {
                                String current = chatHistory.get(assistantIndex);
                                chatHistory.set(assistantIndex, current + token);
                                if (chatListView != null) {
                                    chatListView.scrollTo(assistantIndex);
                                }
                            }
                        })
                );
            }
        };

        task.setOnSucceeded(e -> {
            hideProgress();
            String answer = task.getValue();
            conversationMessages.add(new ChatMessage("user", question));
            conversationMessages.add(new ChatMessage("assistant", answer));
            if (chatListView != null) {
                chatListView.scrollTo(chatHistory.size() - 1);
            }
        });

        task.setOnFailed(e -> handleTaskFailure(task.getException(), "Q&A Request Failed"));

        startAiTask(task);
    }

    // ----- Task Management Helpers ------------------------------------------

    private void startAiTask(Task<?> task) {
        this.activeAiTask = task;
        Thread thread = new Thread(task, "OllamaInferenceWorker");
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void handleCancelTask() {
        if (activeAiTask != null && activeAiTask.isRunning()) {
            activeAiTask.cancel();
            hideProgress();
            AlertUtil.showInfo("Operation Cancelled", "Inference Aborted", "The background AI task was cancelled.");
        }
    }

    private void showProgress(String message) {
        if (progressBox != null) progressBox.setVisible(true);
        if (progressStatusLabel != null) progressStatusLabel.setText(message);
        setButtonsDisabled(true);
    }

    private void hideProgress() {
        if (progressBox != null) progressBox.setVisible(false);
        setButtonsDisabled(false);
        activeAiTask = null;
    }

    private void setButtonsDisabled(boolean disabled) {
        if (summarizeButton != null) summarizeButton.setDisable(disabled);
        if (extractButton != null) extractButton.setDisable(disabled);
        if (classifyButton != null) classifyButton.setDisable(disabled);
        if (sendChatButton != null) sendChatButton.setDisable(disabled);
    }

    private void handleTaskFailure(Throwable ex, String title) {
        hideProgress();
        AppSettings settings = settingsService.getSettings();
        String configInfo = "URL: " + settings.getOllamaUrl() + ", Model: " + settings.getDefaultModel();
        String message = (ex instanceof OllamaException)
                ? ex.getMessage() + "\n\n" + configInfo
                : (ex != null && ex.getMessage() != null)
                ? ex.getMessage() + "\n\n" + configInfo
                : "An error occurred while communicating with Ollama.\n\n" + configInfo;

        // If offline, ensure banner is displayed with details
        if (offlineBanner != null) {
            offlineBanner.setVisible(true);
            offlineBanner.setManaged(true);
        }
        if (offlineBannerDetailLabel != null) {
            offlineBannerDetailLabel.setText(
                configInfo + "  — Click Reconnect to retry connection."
            );
        }

        AlertUtil.showError(title, "Local AI Execution Error", message);
    }

    /** Re-tests the Ollama connection and hides/shows the banner accordingly. */
    @FXML
    private void handleReconnect() {
        if (offlineBannerDetailLabel != null) {
            offlineBannerDetailLabel.setText("Testing connection…");
        }
        checkOllamaConnectionAsync();
    }

    private ResearchPaper getSelectedPaper() {
        if (paperComboBox == null || paperComboBox.getValue() == null) {
            AlertUtil.showWarning("Paper Selection Required", "No Paper Selected",
                    "Please select a research paper from the dropdown before requesting analysis.");
            return null;
        }
        return paperComboBox.getValue();
    }

    @FXML
    private void handleExport() {
        ResearchPaper paper = paperComboBox != null ? paperComboBox.getValue() : null;
        String title = (paper != null) ? paper.getTitle() : "AI_Assistant_Output";

        String exportContent = "";
        Tab selectedTab = analysisTabPane != null ? analysisTabPane.getSelectionModel().getSelectedItem() : null;

        if (selectedTab == summaryTab || (selectedTab != null && "📑 7-Section Summary".equals(selectedTab.getText()))) {
            exportContent = summaryTextArea != null ? summaryTextArea.getText() : "";
        } else if (selectedTab == extractedTab || (selectedTab != null && "🔍 Extracted Info".equals(selectedTab.getText()))) {
            StringBuilder sb = new StringBuilder();
            sb.append("Extracted Information for: ").append(title).append("\n\n");
            if (extractedAbstractArea != null && !extractedAbstractArea.getText().isBlank()) {
                sb.append("Abstract:\n").append(extractedAbstractArea.getText()).append("\n\n");
            }
            if (extractedMethodologyArea != null && !extractedMethodologyArea.getText().isBlank()) {
                sb.append("Methodology:\n").append(extractedMethodologyArea.getText()).append("\n\n");
            }
            if (extractedFindingsArea != null && !extractedFindingsArea.getText().isBlank()) {
                sb.append("Findings:\n").append(extractedFindingsArea.getText()).append("\n\n");
            }
            if (extractedKeywordsField != null && !extractedKeywordsField.getText().isBlank()) {
                sb.append("Keywords: ").append(extractedKeywordsField.getText()).append("\n\n");
            }
            if (extractedTopicField != null && !extractedTopicField.getText().isBlank()) {
                sb.append("Topic: ").append(extractedTopicField.getText()).append("\n\n");
            }
            if (extractedContributionsArea != null && !extractedContributionsArea.getText().isBlank()) {
                sb.append("Contributions:\n").append(extractedContributionsArea.getText()).append("\n\n");
            }
            if (extractedLimitationsArea != null && !extractedLimitationsArea.getText().isBlank()) {
                sb.append("Limitations:\n").append(extractedLimitationsArea.getText()).append("\n\n");
            }
            exportContent = sb.toString();
        } else if (selectedTab == qaTab || (selectedTab != null && selectedTab.getText().contains("Q&A"))) {
            StringBuilder sb = new StringBuilder();
            sb.append("Q&A Chat History for: ").append(title).append("\n\n");
            if (conversationMessages != null) {
                for (ChatMessage msg : conversationMessages) {
                    sb.append(msg.role().toUpperCase()).append(":\n").append(msg.content()).append("\n\n");
                }
            }
            exportContent = sb.toString();
        }

        if (exportContent == null || exportContent.trim().isEmpty()) {
            AlertUtil.showWarning("Nothing to Export", "No Content Available",
                    "The active tab has no content to export. Run an analysis or chat first.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export AI Assistant Output");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Markdown (*.md)", "*.md"),
                new FileChooser.ExtensionFilter("Plain Text (*.txt)", "*.txt")
        );
        String safeName = title.replaceAll("[^a-zA-Z0-9_-]", "_");
        if (safeName.length() > 30) safeName = safeName.substring(0, 30);
        chooser.setInitialFileName("ai_analysis_" + safeName);

        File file = chooser.showSaveDialog(null);
        if (file == null) return;

        boolean asMarkdown = file.getName().endsWith(".md");
        try {
            exportService.exportTextContent(exportContent, "AI Analysis - " + title, file.toPath(), asMarkdown);
            AlertUtil.showInfo("Export Successful", "File Saved",
                    "Output successfully exported to:\n" + file.getAbsolutePath());
        } catch (Exception ex) {
            AlertUtil.showError("Export Failed", "Could not write file", ex.getMessage());
        }
    }
}
