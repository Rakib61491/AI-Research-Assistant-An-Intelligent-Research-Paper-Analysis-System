package com.example.airesearchassistant.model;

/**
 * Collection — Domain model for research paper collections.
 * Contains no JavaFX imports (Hard Rule 2).
 */
public class Collection {

    private int id;
    private String name;
    private String description;
    private String dateCreated;
    private int paperCount;

    public Collection() {
    }

    public Collection(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public Collection(int id, String name, String description, String dateCreated) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.dateCreated = dateCreated;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDateCreated() { return dateCreated; }
    public void setDateCreated(String dateCreated) { this.dateCreated = dateCreated; }

    public int getPaperCount() { return paperCount; }
    public void setPaperCount(int paperCount) { this.paperCount = paperCount; }

    @Override
    public String toString() {
        return name;
    }
}
