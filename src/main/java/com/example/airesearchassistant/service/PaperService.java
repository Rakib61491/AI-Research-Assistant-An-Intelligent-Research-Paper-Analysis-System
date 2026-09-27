package com.example.airesearchassistant.service;

import com.example.airesearchassistant.model.ResearchPaper;
import com.example.airesearchassistant.repository.PaperRepository;
import com.example.airesearchassistant.service.OllamaService.ChatMessage;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

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

    // ----- Stage 6: AI Integration Helper Methods ----------------------------

    /**
     * Resolves the best available textual context for a paper.
     */
    public String resolvePaperContext(ResearchPaper paper) {
        if (paper == null) return "";

        // If file exists, try extracting full text
        if (paper.getFilePath() != null && !paper.getFilePath().trim().isEmpty()) {
            File file = new File(paper.getFilePath());
            if (file.exists() && file.isFile()) {
                try {
                    PdfExtractionService.ExtractedPaperData data = pdfExtractionService.extract(file);
                    if (data != null && data.fullText() != null && !data.fullText().trim().isEmpty()) {
                        return data.fullText();
                    }
                } catch (IOException e) {
                    System.err.println("Could not extract full text from file: " + e.getMessage());
                }
            }
        }

        // Fallback: build textual representation from database fields
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
     * Calls Ollama to produce a structured 7-section summary of the paper.
     */
    public String summarizePaper(ResearchPaper paper, OllamaService ollama, String model,
                                 double temperature, int maxTokens, Duration timeout) throws OllamaException {
        String context = resolvePaperContext(paper);
        return ollama.summarize(context, model, temperature, maxTokens, timeout);
    }

    /**
     * Calls Ollama to extract structured key-value information from the paper.
     */
    public String extractPaperInfo(ResearchPaper paper, OllamaService ollama, String model,
                                   double temperature, int maxTokens, Duration timeout) throws OllamaException {
        String context = resolvePaperContext(paper);
        return ollama.extractInfo(context, model, temperature, maxTokens, timeout);
    }

    /**
     * Calls Ollama for context-aware Q&A on the paper.
     */
    public String askPaperQuestion(ResearchPaper paper, String question, List<ChatMessage> history,
                                   OllamaService ollama, String model, double temperature, int maxTokens, Duration timeout) throws OllamaException {
        String context = resolvePaperContext(paper);
        return ollama.askQuestion(context, question, history, model, temperature, maxTokens, timeout);
    }

    /**
     * Classifies the paper topic using Ollama.
     */
    public String classifyPaperTopic(ResearchPaper paper, List<String> candidateTopics,
                                     OllamaService ollama, String model, Duration timeout) throws OllamaException {
        String context = resolvePaperContext(paper);
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
        String contextA = resolvePaperContext(paperA);
        String contextB = resolvePaperContext(paperB);
        return ollama.comparePapers(contextA, contextB, paperA.getTitle(), paperB.getTitle(),
                model, temperature, maxTokens, timeout);
    }
}

