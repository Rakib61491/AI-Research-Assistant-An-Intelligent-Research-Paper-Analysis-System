package com.example.airesearchassistant.model;

/**
 * ResearchPaper — Domain model representing a scientific research paper.
 * Contains no JavaFX imports (Hard Rule 2).
 */
public class ResearchPaper {

    private int id;
    private String title;
    private String authors;
    private String publicationYear;
    private String journal;
    private String doi;
    private String abstractText;
    private String methodology;
    private String findings;
    private String keywords;
    private String topic;
    private String filePath;
    private String aiSummary;
    private String personalNotes;
    private String extractedJson;
    private boolean isFavorite;
    private String dateAdded;
    private String dateAnalyzed;

    public ResearchPaper() {
    }

    public ResearchPaper(String title, String authors, String publicationYear, String journal,
                         String topic, String abstractText) {
        this.title = title;
        this.authors = authors;
        this.publicationYear = publicationYear;
        this.journal = journal;
        this.topic = topic;
        this.abstractText = abstractText;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthors() { return authors; }
    public void setAuthors(String authors) { this.authors = authors; }

    public String getPublicationYear() { return publicationYear; }
    public void setPublicationYear(String publicationYear) { this.publicationYear = publicationYear; }

    public String getJournal() { return journal; }
    public void setJournal(String journal) { this.journal = journal; }

    public String getDoi() { return doi; }
    public void setDoi(String doi) { this.doi = doi; }

    public String getAbstractText() { return abstractText; }
    public void setAbstractText(String abstractText) { this.abstractText = abstractText; }

    public String getMethodology() { return methodology; }
    public void setMethodology(String methodology) { this.methodology = methodology; }

    public String getFindings() { return findings; }
    public void setFindings(String findings) { this.findings = findings; }

    public String getKeywords() { return keywords; }
    public void setKeywords(String keywords) { this.keywords = keywords; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public String getAiSummary() { return aiSummary; }
    public void setAiSummary(String aiSummary) { this.aiSummary = aiSummary; }

    public String getPersonalNotes() { return personalNotes; }
    public void setPersonalNotes(String personalNotes) { this.personalNotes = personalNotes; }

    public String getExtractedJson() { return extractedJson; }
    public void setExtractedJson(String extractedJson) { this.extractedJson = extractedJson; }

    public boolean isFavorite() { return isFavorite; }
    public void setFavorite(boolean favorite) { isFavorite = favorite; }

    public String getDateAdded() { return dateAdded; }
    public void setDateAdded(String dateAdded) { this.dateAdded = dateAdded; }

    public String getDateAnalyzed() { return dateAnalyzed; }
    public void setDateAnalyzed(String dateAnalyzed) { this.dateAnalyzed = dateAnalyzed; }

    public boolean isAnalyzed() {
        return dateAnalyzed != null && !dateAnalyzed.trim().isEmpty();
    }

    @Override
    public String toString() {
        return title != null ? title : "Untitled Paper (ID: " + id + ")";
    }
}
