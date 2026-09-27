package com.example.airesearchassistant.service;

import com.example.airesearchassistant.model.Collection;
import com.example.airesearchassistant.model.ResearchPaper;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * ExportService — Writes Markdown or plain-text reports to disk.
 * Contains no JavaFX imports (Hard Rule 2). Never touches FXML or controls (Hard Rule 4).
 */
public class ExportService {

    private static final DateTimeFormatter STAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // -----------------------------------------------------------------------
    // Library export
    // -----------------------------------------------------------------------

    public void exportLibrary(List<ResearchPaper> papers, Path destination, boolean asMarkdown) throws IOException {
        try (PrintWriter pw = openWriter(destination)) {
            if (asMarkdown) {
                pw.println("# Research Library Export");
                pw.println();
                pw.println("Generated: " + now());
                pw.println();
                pw.println("---");
                pw.println();
                for (ResearchPaper p : papers) {
                    pw.println("## " + safe(p.getTitle()));
                    pw.println();
                    pw.println("| Field | Value |");
                    pw.println("|-------|-------|");
                    mdRow(pw, "Authors", p.getAuthors());
                    mdRow(pw, "Year", p.getPublicationYear());
                    mdRow(pw, "Journal", p.getJournal());
                    mdRow(pw, "DOI", p.getDoi());
                    mdRow(pw, "Topic", p.getTopic());
                    mdRow(pw, "Keywords", p.getKeywords());
                    mdRow(pw, "Favorite", p.isFavorite() ? "★ Yes" : "No");
                    mdRow(pw, "Analyzed", p.isAnalyzed() ? "✓ Yes (" + p.getDateAnalyzed() + ")" : "No");
                    pw.println();
                    if (hasText(p.getAbstractText())) {
                        pw.println("**Abstract:**");
                        pw.println();
                        pw.println(p.getAbstractText());
                        pw.println();
                    }
                    if (hasText(p.getAiSummary())) {
                        pw.println("**AI Summary:**");
                        pw.println();
                        pw.println(p.getAiSummary());
                        pw.println();
                    }
                    if (hasText(p.getPersonalNotes())) {
                        pw.println("**Personal Notes:**");
                        pw.println();
                        pw.println(p.getPersonalNotes());
                        pw.println();
                    }
                    pw.println("---");
                    pw.println();
                }
            } else {
                pw.println("RESEARCH LIBRARY EXPORT");
                pw.println("Generated: " + now());
                pw.println("=".repeat(60));
                pw.println();
                for (ResearchPaper p : papers) {
                    pw.println("TITLE:     " + safe(p.getTitle()));
                    pw.println("AUTHORS:   " + safe(p.getAuthors()));
                    pw.println("YEAR:      " + safe(p.getPublicationYear()));
                    pw.println("JOURNAL:   " + safe(p.getJournal()));
                    pw.println("DOI:       " + safe(p.getDoi()));
                    pw.println("TOPIC:     " + safe(p.getTopic()));
                    pw.println("KEYWORDS:  " + safe(p.getKeywords()));
                    pw.println("FAVORITE:  " + (p.isFavorite() ? "Yes" : "No"));
                    pw.println("ANALYZED:  " + (p.isAnalyzed() ? "Yes (" + p.getDateAnalyzed() + ")" : "No"));
                    if (hasText(p.getAbstractText())) {
                        pw.println();
                        pw.println("ABSTRACT:");
                        pw.println(p.getAbstractText());
                    }
                    if (hasText(p.getAiSummary())) {
                        pw.println();
                        pw.println("AI SUMMARY:");
                        pw.println(p.getAiSummary());
                    }
                    if (hasText(p.getPersonalNotes())) {
                        pw.println();
                        pw.println("PERSONAL NOTES:");
                        pw.println(p.getPersonalNotes());
                    }
                    pw.println("-".repeat(60));
                    pw.println();
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    // AI Assistant / comparison export
    // -----------------------------------------------------------------------

    public void exportTextContent(String content, String title, Path destination, boolean asMarkdown) throws IOException {
        try (PrintWriter pw = openWriter(destination)) {
            if (asMarkdown) {
                pw.println("# " + title);
                pw.println();
                pw.println("Generated: " + now());
                pw.println();
                pw.println("---");
                pw.println();
                pw.println(content);
            } else {
                pw.println(title.toUpperCase());
                pw.println("Generated: " + now());
                pw.println("=".repeat(60));
                pw.println();
                pw.println(content);
            }
        }
    }

    // -----------------------------------------------------------------------
    // Collections export
    // -----------------------------------------------------------------------

    public void exportCollection(Collection collection, List<ResearchPaper> papers,
                                 Path destination, boolean asMarkdown) throws IOException {
        try (PrintWriter pw = openWriter(destination)) {
            if (asMarkdown) {
                pw.println("# Collection: " + safe(collection.getName()));
                pw.println();
                pw.println("Generated: " + now());
                if (hasText(collection.getDescription())) {
                    pw.println();
                    pw.println("**Description:** " + collection.getDescription());
                }
                pw.println();
                pw.println("**Papers:** " + papers.size());
                pw.println();
                pw.println("---");
                pw.println();
                for (ResearchPaper p : papers) {
                    pw.println("## " + safe(p.getTitle()));
                    pw.println();
                    mdRow(pw, "Authors", p.getAuthors());
                    mdRow(pw, "Year", p.getPublicationYear());
                    mdRow(pw, "Topic", p.getTopic());
                    pw.println();
                    if (hasText(p.getAbstractText())) {
                        pw.println("**Abstract:** " + p.getAbstractText());
                        pw.println();
                    }
                    pw.println("---");
                    pw.println();
                }
            } else {
                pw.println("COLLECTION: " + safe(collection.getName()));
                pw.println("Generated: " + now());
                pw.println("=".repeat(60));
                pw.println();
                for (ResearchPaper p : papers) {
                    pw.println("TITLE:   " + safe(p.getTitle()));
                    pw.println("AUTHORS: " + safe(p.getAuthors()));
                    pw.println("YEAR:    " + safe(p.getPublicationYear()));
                    if (hasText(p.getAbstractText())) {
                        pw.println("ABSTRACT: " + p.getAbstractText());
                    }
                    pw.println("-".repeat(60));
                    pw.println();
                }
            }
        }
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private PrintWriter openWriter(Path path) throws IOException {
        return new PrintWriter(path.toFile(), StandardCharsets.UTF_8);
    }

    private String now() {
        return LocalDateTime.now().format(STAMP);
    }

    private String safe(String s) {
        return (s != null && !s.isBlank()) ? s : "—";
    }

    private boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    private void mdRow(PrintWriter pw, String field, String value) {
        pw.println("| " + field + " | " + safe(value) + " |");
    }
}
