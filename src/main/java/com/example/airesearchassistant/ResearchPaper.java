package com.example.airesearchassistant;

public class ResearchPaper {

    private int id;
    private String title;
    private String authors;
    private String abstractText;
    private String publicationYear;

    public ResearchPaper(
            int id,
            String title,
            String authors,
            String abstractText,
            String publicationYear
    ) {
        this.id = id;
        this.title = title;
        this.authors = authors;
        this.abstractText = abstractText;
        this.publicationYear = publicationYear;
    }

    public ResearchPaper(
            String title,
            String authors,
            String abstractText,
            String publicationYear
    ) {
        this(0, title, authors, abstractText, publicationYear);
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthors() {
        return authors;
    }

    public String getAbstractText() {
        return abstractText;
    }

    public String getPublicationYear() {
        return publicationYear;
    }
}