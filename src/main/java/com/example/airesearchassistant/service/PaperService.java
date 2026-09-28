package com.example.airesearchassistant.service;

import com.example.airesearchassistant.model.ResearchPaper;
import com.example.airesearchassistant.repository.PaperRepository;
import com.example.airesearchassistant.service.OllamaService.ChatMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * PaperService — Business logic layer for research papers and AI analytical workflows.
 * Never touches FXML or JavaFX controls (Hard Rule 4).
 */
public class PaperService {

    private final PaperRepository paperRepository;
    private final PdfExtractionService pdfExtractionService;

    public PaperService() {
        this.paperRepository = new PaperRepository();
        this.pdfExtractionService = new PdfExtractionService();
    }

    public PaperService(PaperRepository paperRepository, PdfExtractionService pdfExtractionService) {
        this.paperRepository = paperRepository;
        this.pdfExtractionService = pdfExtractionService;
    }

    public int createPaper(ResearchPaper paper) throws SQLException {
        if (paper == null) {
            throw new IllegalArgumentException("Paper cannot be null.");
        }
        if (paper.getTitle() == null || paper.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Paper title is required.");
        }
        return paperRepository.insert(paper);
    }

    public boolean updatePaper(ResearchPaper paper) throws SQLException {
        if (paper == null || paper.getId() <= 0) {
            throw new IllegalArgumentException("Valid paper entity with ID is required for update.");
        }
        return paperRepository.update(paper);
    }

    public boolean deletePaper(int id) throws SQLException {
        return paperRepository.delete(id);
    }

    public Optional<ResearchPaper> getPaperById(int id) throws SQLException {
        return paperRepository.findById(id);
    }

    public List<ResearchPaper> getAllPapers() {
        try {
            return paperRepository.findAll();
        } catch (SQLException e) {
            System.err.println("Error fetching all papers: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public List<ResearchPaper> searchPapers(String query, String topic, String year, Boolean favoriteOnly) {
        try {
            return paperRepository.search(query, topic, year, favoriteOnly);
        } catch (SQLException e) {
            System.err.println("Error searching papers: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public boolean toggleFavorite(int id) throws SQLException {
        return paperRepository.toggleFavorite(id);
    }

    public int getTotalPaperCount() {
        try {
            return paperRepository.countAll();
        } catch (SQLException e) {
            return 0;
        }
    }

    public int getAnalyzedPaperCount() {
        try {
            return paperRepository.countAnalyzed();
        } catch (SQLException e) {
            return 0;
        }
    }

    public int getFavoritePaperCount() {
        try {
            return paperRepository.countFavorites();
        } catch (SQLException e) {
            return 0;
        }
    }

    public List<String> getAvailableTopics() {
        try {
            return paperRepository.getDistinctTopics();
        } catch (SQLException e) {
            return Collections.emptyList();
        }
    }

    public List<String> getAvailableYears() {
        try {
            return paperRepository.getDistinctYears();
        } catch (SQLException e) {
            return Collections.emptyList();
        }
    }


    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // ----- Stage 6: AI Integration Helper Methods ----------------------------

    /**
     * Ensures that structured JSON representation is loaded or extracted for the paper.
     */
    public JsonNode ensurePaperJson(ResearchPaper paper) {
        if (paper == null) return null;

        // 1. Try parsing existing extractedJson
        if (paper.getExtractedJson() != null && !paper.getExtractedJson().trim().isEmpty()) {
            try {
                return OBJECT_MAPPER.readTree(paper.getExtractedJson());
            } catch (Exception e) {
                System.err.println("Could not parse existing extracted_json: " + e.getMessage());
            }
        }

        // 2. If file exists, extract structured data and cache JSON
        if (paper.getFilePath() != null && !paper.getFilePath().trim().isEmpty()) {
            File file = new File(paper.getFilePath());
            if (file.exists() && file.isFile()) {
                try {
                    PdfExtractionService.ExtractedPaperData data = pdfExtractionService.extract(file);
                    if (data != null && data.json() != null && !data.json().trim().isEmpty()) {
                        paper.setExtractedJson(data.json());
                        try {
                            paperRepository.update(paper);
                        } catch (SQLException ex) {
                            System.err.println("Could not cache extracted JSON to DB: " + ex.getMessage());
                        }
                        return OBJECT_MAPPER.readTree(data.json());
                    }
                } catch (Exception e) {
                    System.err.println("Could not extract from file: " + e.getMessage());
                }
            }
        }

        // 3. Fallback: synthesize minimal JSON from paper fields
        try {
            com.fasterxml.jackson.databind.node.ObjectNode root = OBJECT_MAPPER.createObjectNode();
            com.fasterxml.jackson.databind.node.ObjectNode meta = root.putObject("metadata");
            meta.put("title", paper.getTitle() != null ? paper.getTitle() : "");
            meta.put("authors", paper.getAuthors() != null ? paper.getAuthors() : "");
            meta.put("publicationYear", paper.getPublicationYear() != null ? paper.getPublicationYear() : "");
            meta.put("topic", paper.getTopic() != null ? paper.getTopic() : "");
            meta.put("keywords", paper.getKeywords() != null ? paper.getKeywords() : "");

            root.put("abstract", paper.getAbstractText() != null ? paper.getAbstractText() : "");
            root.put("methodology", paper.getMethodology() != null ? paper.getMethodology() : "");
            root.put("findings", paper.getFindings() != null ? paper.getFindings() : "");
            root.putArray("sections");
            return root;
        } catch (Exception e) {
            return null;
        }
    }

    private String truncate(String text, int maxChars) {
        if (text == null) return "";
        String trimmed = text.trim();
        if (trimmed.length() <= maxChars) return trimmed;
        return trimmed.substring(0, maxChars) + "\n...[truncated]...";
    }

    /**
     * Resolves targeted context for summarization using Introduction, Abstract, and core sections from the JSON.
     * Prevents context window overflows and huge prompt latency.
     */
    public String resolveSummaryContext(ResearchPaper paper) {
        if (paper == null) return "";
        JsonNode root = ensurePaperJson(paper);
        if (root == null) return resolvePaperContext(paper);

        StringBuilder sb = new StringBuilder();
        JsonNode meta = root.path("metadata");
        sb.append("=== PAPER METADATA ===\n");
        sb.append("Title: ").append(meta.path("title").asText(paper.getTitle())).append("\n");
        if (!meta.path("authors").asText("").isBlank()) {
            sb.append("Authors: ").append(meta.path("authors").asText()).append("\n");
        }
        if (!meta.path("publicationYear").asText("").isBlank()) {
            sb.append("Year: ").append(meta.path("publicationYear").asText()).append("\n");
        }
        if (!meta.path("topic").asText("").isBlank()) {
            sb.append("Topic: ").append(meta.path("topic").asText()).append("\n");
        }
        sb.append("\n");

        // 1. Abstract
        String abstractText = root.path("abstract").asText("").trim();
        if (abstractText.isEmpty() && paper.getAbstractText() != null) {
            abstractText = paper.getAbstractText().trim();
        }
        if (!abstractText.isEmpty()) {
            sb.append("=== ABSTRACT ===\n").append(truncate(abstractText, 1500)).append("\n\n");
        }

        // 2. Sections: extract Introduction, Methodology, Results/Findings, Conclusion
        JsonNode sections = root.path("sections");
        if (sections.isArray() && sections.size() > 0) {
            for (JsonNode sec : sections) {
                String heading = sec.path("heading").asText("").trim();
                String content = sec.path("content").asText("").trim();
                if (content.isEmpty()) continue;

                String lower = heading.toLowerCase();
                if (lower.matches(".*(intro|motivation|background|overview).*")) {
                    sb.append("=== SECTION: ").append(heading).append(" ===\n")
                      .append(truncate(content, 2000)).append("\n\n");
                } else if (lower.matches(".*(methodology|methods|proposed|architecture|approach).*")) {
                    sb.append("=== SECTION: ").append(heading).append(" ===\n")
                      .append(truncate(content, 2000)).append("\n\n");
                } else if (lower.matches(".*(result|finding|experiment|evaluation).*")) {
                    sb.append("=== SECTION: ").append(heading).append(" ===\n")
                      .append(truncate(content, 2000)).append("\n\n");
                } else if (lower.matches(".*(conclusion|concluding|discussion|summary|future work).*")) {
                    sb.append("=== SECTION: ").append(heading).append(" ===\n")
                      .append(truncate(content, 2000)).append("\n\n");
                }
            }
        }

        // Fallbacks from root fields if sections did not cover methodology or findings
        if (!sb.toString().contains("=== SECTION:") && !root.path("methodology").asText("").isBlank()) {
            sb.append("=== METHODOLOGY ===\n")
              .append(truncate(root.path("methodology").asText(), 2000)).append("\n\n");
        }
        if (!sb.toString().contains("=== SECTION:") && !root.path("findings").asText("").isBlank()) {
            sb.append("=== FINDINGS ===\n")
              .append(truncate(root.path("findings").asText(), 2000)).append("\n\n");
        }

        return sb.toString();
    }

    /**
     * Resolves targeted context for methodology extraction using only methodology parts of JSON.
     */
    public String resolveMethodologyContext(ResearchPaper paper) {
        if (paper == null) return "";
        JsonNode root = ensurePaperJson(paper);
        if (root == null) return resolvePaperContext(paper);

        StringBuilder sb = new StringBuilder();
        sb.append("Paper Title: ").append(root.path("metadata").path("title").asText(paper.getTitle())).append("\n\n");

        String methodology = root.path("methodology").asText("").trim();
        if (!methodology.isEmpty()) {
            sb.append("Extracted Methodology:\n").append(truncate(methodology, 2500)).append("\n\n");
        }

        JsonNode sections = root.path("sections");
        if (sections.isArray()) {
            for (JsonNode sec : sections) {
                String heading = sec.path("heading").asText("").trim();
                String content = sec.path("content").asText("").trim();
                if (heading.toLowerCase().matches(".*(method|proposed|approach|architecture|system|design|setup|algorithm).*")) {
                    sb.append("Section (").append(heading).append("):\n")
                      .append(truncate(content, 2000)).append("\n\n");
                }
            }
        }
        return sb.toString();
    }

    /**
     * Resolves targeted context for findings extraction using only results/findings parts of JSON.
     */
    public String resolveFindingsContext(ResearchPaper paper) {
        if (paper == null) return "";
        JsonNode root = ensurePaperJson(paper);
        if (root == null) return resolvePaperContext(paper);

        StringBuilder sb = new StringBuilder();
        sb.append("Paper Title: ").append(root.path("metadata").path("title").asText(paper.getTitle())).append("\n\n");

        String findings = root.path("findings").asText("").trim();
        if (!findings.isEmpty()) {
            sb.append("Extracted Findings:\n").append(truncate(findings, 2500)).append("\n\n");
        }

        JsonNode sections = root.path("sections");
        if (sections.isArray()) {
            for (JsonNode sec : sections) {
                String heading = sec.path("heading").asText("").trim();
                String content = sec.path("content").asText("").trim();
                if (heading.toLowerCase().matches(".*(result|finding|experiment|evaluation|performance|discussion).*")) {
                    sb.append("Section (").append(heading).append("):\n")
                      .append(truncate(content, 2000)).append("\n\n");
                }
            }
        }
        return sb.toString();
    }

    /**
     * Resolves targeted context for contributions extraction using Introduction & Conclusion parts of JSON.
     */
    public String resolveContributionsContext(ResearchPaper paper) {
        if (paper == null) return "";
        JsonNode root = ensurePaperJson(paper);
        if (root == null) return resolvePaperContext(paper);

        StringBuilder sb = new StringBuilder();
        sb.append("Paper Title: ").append(root.path("metadata").path("title").asText(paper.getTitle())).append("\n");
        String abstractText = root.path("abstract").asText("").trim();
        if (!abstractText.isEmpty()) {
            sb.append("Abstract:\n").append(truncate(abstractText, 1000)).append("\n\n");
        }

        JsonNode sections = root.path("sections");
        if (sections.isArray()) {
            for (JsonNode sec : sections) {
                String heading = sec.path("heading").asText("").trim();
                String content = sec.path("content").asText("").trim();
                if (heading.toLowerCase().matches(".*(intro|contribution|novelty|conclusion|discussion).*")) {
                    sb.append("Section (").append(heading).append("):\n")
                      .append(truncate(content, 2000)).append("\n\n");
                }
            }
        }
        return sb.toString();
    }

    /**
     * Resolves targeted context for limitations extraction using Discussion & Conclusion parts of JSON.
     */
    public String resolveLimitationsContext(ResearchPaper paper) {
        if (paper == null) return "";
        JsonNode root = ensurePaperJson(paper);
        if (root == null) return resolvePaperContext(paper);

        StringBuilder sb = new StringBuilder();
        sb.append("Paper Title: ").append(root.path("metadata").path("title").asText(paper.getTitle())).append("\n\n");

        JsonNode sections = root.path("sections");
        if (sections.isArray()) {
            for (JsonNode sec : sections) {
                String heading = sec.path("heading").asText("").trim();
                String content = sec.path("content").asText("").trim();
                if (heading.toLowerCase().matches(".*(limitation|threat|constraint|discussion|conclusion|future work).*")) {
                    sb.append("Section (").append(heading).append("):\n")
                      .append(truncate(content, 2000)).append("\n\n");
                }
            }
        }
        return sb.toString();
    }

    /**
     * Resolves context for context-aware Q&A using structured overview from JSON.
     */
    public String resolveQnAContext(ResearchPaper paper) {
        if (paper == null) return "";
        return resolveSummaryContext(paper);
    }

    /**
     * Resolves the best available textual context for a paper (fallback).
     */
    public String resolvePaperContext(ResearchPaper paper) {
        if (paper == null) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("Title: ").append(paper.getTitle()).append("\n");
        if (paper.getAuthors() != null) sb.append("Authors: ").append(paper.getAuthors()).append("\n");
        if (paper.getPublicationYear() != null) sb.append("Year: ").append(paper.getPublicationYear()).append("\n");
        if (paper.getTopic() != null) sb.append("Topic: ").append(paper.getTopic()).append("\n");
        if (paper.getAbstractText() != null) sb.append("Abstract:\n").append(paper.getAbstractText()).append("\n");
        if (paper.getMethodology() != null) sb.append("Methodology:\n").append(paper.getMethodology()).append("\n");
        if (paper.getFindings() != null) sb.append("Findings:\n").append(paper.getFindings()).append("\n");
        return sb.toString();
    }

    /**
     * Calls Ollama to produce a structured 7-section summary of the paper with token streaming.
     */
    public String summarizePaperStream(ResearchPaper paper, OllamaService ollama, String model,
                                       double temperature, int maxTokens, Duration timeout,
                                       Consumer<String> onToken) throws OllamaException {
        String context = resolveSummaryContext(paper);
        return ollama.summarizeStream(context, model, temperature, maxTokens, timeout, onToken);
    }

    /**
     * Calls Ollama to produce a structured 7-section summary of the paper.
     */
    public String summarizePaper(ResearchPaper paper, OllamaService ollama, String model,
                                 double temperature, int maxTokens, Duration timeout) throws OllamaException {
        return summarizePaperStream(paper, ollama, model, temperature, maxTokens, timeout, null);
    }

    /**
     * Calls Ollama to extract structured key-value information from the paper.
     */
    public String extractPaperInfo(ResearchPaper paper, OllamaService ollama, String model,
                                   double temperature, int maxTokens, Duration timeout) throws OllamaException {
        String context = resolveSummaryContext(paper);
        return ollama.extractInfo(context, model, temperature, maxTokens, timeout);
    }

    /**
     * Calls Ollama for context-aware Q&A on the paper with token streaming.
     */
    public String askPaperQuestionStream(ResearchPaper paper, String question, List<ChatMessage> history,
                                         OllamaService ollama, String model, double temperature, int maxTokens,
                                         Duration timeout, Consumer<String> onToken) throws OllamaException {
        String context = resolveQnAContext(paper);
        return ollama.askQuestionStream(context, question, history, model, temperature, maxTokens, timeout, onToken);
    }

    /**
     * Calls Ollama for context-aware Q&A on the paper.
     */
    public String askPaperQuestion(ResearchPaper paper, String question, List<ChatMessage> history,
                                   OllamaService ollama, String model, double temperature, int maxTokens, Duration timeout) throws OllamaException {
        return askPaperQuestionStream(paper, question, history, ollama, model, temperature, maxTokens, timeout, null);
    }

    /**
     * Classifies the paper topic using Ollama.
     */
    public String classifyPaperTopic(ResearchPaper paper, List<String> candidateTopics,
                                     OllamaService ollama, String model, Duration timeout) throws OllamaException {
        String context = resolveSummaryContext(paper);
        return ollama.classify(context, candidateTopics, model, timeout);
    }

    /**
     * Saves AI summary and marks paper as analyzed.
     */
    public void saveAiSummary(int paperId, String summary) throws SQLException {
        Optional<ResearchPaper> opt = paperRepository.findById(paperId);
        if (opt.isPresent()) {
            ResearchPaper paper = opt.get();
            paper.setAiSummary(summary);
            String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            paper.setDateAnalyzed(now);
            paperRepository.update(paper);
        }
    }

    /**
     * Calls Ollama to produce a structured comparison between two papers.
     */
    public String comparePapers(ResearchPaper paperA, ResearchPaper paperB,
                                OllamaService ollama, String model,
                                double temperature, int maxTokens, Duration timeout) throws OllamaException {
        String contextA = resolveSummaryContext(paperA);
        String contextB = resolveSummaryContext(paperB);
        return ollama.comparePapers(contextA, contextB, paperA.getTitle(), paperB.getTitle(),
                model, temperature, maxTokens, timeout);
    }
}

