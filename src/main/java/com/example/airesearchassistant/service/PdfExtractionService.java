package com.example.airesearchassistant.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PdfExtractionService — Extracts text and parses metadata from PDF and TXT documents.
 * Uses Apache PDFBox 3.x API (Loader.loadPDF).
 * Contains no JavaFX/FXML references (Hard Rule 4).
 */
public class PdfExtractionService {

    public static final long MAX_FILE_SIZE_BYTES = 50 * 1024 * 1024; // 50 MB
    private static final Pattern YEAR_PATTERN = Pattern.compile("\\b(19|20)\\d{2}\\b");

    /**
     * DTO containing extracted text and heuristic metadata.
     */
    public record ExtractedPaperData(
            String title,
            String authors,
            String publicationYear,
            String abstractText,
            String fullText,
            String filePath
    ) {}

    /**
     * Extracts text and heuristic metadata from the specified PDF or TXT file.
     *
     * @param file the document file
     * @return ExtractedPaperData with parsed fields
     * @throws IllegalArgumentException on validation failure (empty, > 50MB, unsupported format)
     * @throws IOException on parsing or reading failure
     */
    public ExtractedPaperData extract(File file) throws IOException {
        validateFile(file);

        String fullText;
        String nameLower = file.getName().toLowerCase();

        if (nameLower.endsWith(".pdf")) {
            fullText = extractTextFromPdf(file);
        } else if (nameLower.endsWith(".txt")) {
            fullText = Files.readString(file.toPath());
        } else {
            throw new IllegalArgumentException("Unsupported file format: " + file.getName() + ". Only PDF and TXT files are supported.");
        }

        if (fullText == null || fullText.trim().isEmpty()) {
            throw new IOException("The document does not contain readable text.");
        }

        return parseMetadata(fullText, file.getAbsolutePath());
    }

    private void validateFile(File file) {
        if (file == null || !file.exists()) {
            throw new IllegalArgumentException("The specified file does not exist.");
        }
        if (file.isDirectory()) {
            throw new IllegalArgumentException("Selected path is a directory, not a document file.");
        }
        if (file.length() == 0) {
            throw new IllegalArgumentException("The selected file is empty (0 bytes).");
        }
        if (file.length() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException(String.format(
                    "File size (%.2f MB) exceeds the maximum allowed limit of 50 MB.",
                    file.length() / (1024.0 * 1024.0)
            ));
        }

        String name = file.getName().toLowerCase();
        if (!name.endsWith(".pdf") && !name.endsWith(".txt")) {
            throw new IllegalArgumentException("Unsupported format. Please select a .pdf or .txt file.");
        }
    }

    private String extractTextFromPdf(File file) throws IOException {
        try (PDDocument document = Loader.loadPDF(file)) {
            if (document.isEncrypted()) {
                throw new IOException("PDF is password-protected or encrypted.");
            }
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(document);
        } catch (Exception e) {
            if (e instanceof IOException ioException) {
                throw ioException;
            }
            throw new IOException("Failed to parse PDF: " + e.getMessage(), e);
        }
    }

    private ExtractedPaperData parseMetadata(String fullText, String filePath) {
        String[] lines = fullText.split("\\r?\\n");

        String title = "";
        String authors = "";
        int lineIdx = 0;

        // Extract first non-empty line as Title
        while (lineIdx < lines.length) {
            String line = lines[lineIdx].trim();
            lineIdx++;
            if (!line.isEmpty()) {
                title = line;
                break;
            }
        }

        // Extract next non-empty line as Authors
        while (lineIdx < lines.length) {
            String line = lines[lineIdx].trim();
            lineIdx++;
            if (!line.isEmpty()) {
                authors = line;
                break;
            }
        }

        // Extract Year via regex from first 1500 characters
        String prefix = fullText.length() > 1500 ? fullText.substring(0, 1500) : fullText;
        String year = "";
        Matcher matcher = YEAR_PATTERN.matcher(prefix);
        if (matcher.find()) {
            year = matcher.group();
        }

        // Extract Abstract if section header is present
        String abstractText = "";
        Pattern abstractPattern = Pattern.compile("(?i)\\babstract\\b[:\\s]*(.+?)(?=(?i)\\b(1\\.?\\s+introduction|introduction|keywords|index terms)\\b|$)", Pattern.DOTALL);
        Matcher absMatcher = abstractPattern.matcher(fullText);
        if (absMatcher.find()) {
            abstractText = absMatcher.group(1).trim().replaceAll("\\s+", " ");
            if (abstractText.length() > 1500) {
                abstractText = abstractText.substring(0, 1500) + "...";
            }
        } else {
            // Fallback: take next 3 non-empty paragraphs
            StringBuilder sb = new StringBuilder();
            int count = 0;
            while (lineIdx < lines.length && count < 3) {
                String line = lines[lineIdx].trim();
                lineIdx++;
                if (!line.isEmpty()) {
                    sb.append(line).append(" ");
                    count++;
                }
            }
            abstractText = sb.toString().trim();
        }

        return new ExtractedPaperData(title, authors, year, abstractText, fullText, filePath);
    }
}
