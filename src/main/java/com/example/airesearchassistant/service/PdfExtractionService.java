package com.example.airesearchassistant.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PdfExtractionService — Production-grade extractor for research papers using Apache PDFBox 3.x.
 *
 * Code-level improvements:
 * 1. Two-Column Layout Awareness: Correctly reads academic papers column-by-column rather than
 *    horizontally splicing adjacent text.
 * 2. Header / Footer Removal: Eliminates running headers (journal names, article IDs) and footers
 *    (page numbers, copyright notices) by geometric coordinates and pattern filtering.
 * 3. Table and Layout Extraction: Identifies table structures and outputs Markdown and CSV representations.
 * 4. Section Parsing: Segment papers into Introduction, Methodology, Findings, Conclusion, References.
 * 5. Metadata Handling: Extracts embedded PDF metadata (Title, Author, Subject) and parses paper heuristics
 *    (DOI, Year, Authors list, Keywords, Abstract).
 * 6. Structured JSON & Markdown: Emits a comprehensive structured JSON document ready for database
 *    storage and UI consumption.
 */
public class PdfExtractionService {

    public static final long MAX_FILE_SIZE_BYTES = 50 * 1024 * 1024; // 50 MB
    private static final Pattern YEAR_PATTERN = Pattern.compile("\\b(19|20)\\d{2}\\b");
    private static final Pattern DOI_PATTERN = Pattern.compile("\\b10\\.\\d{4,9}/[-._;()/:A-Za-z0-9]+\\b");
    private static final Pattern SECTION_HEADING_PATTERN = Pattern.compile(
            "^(\\d{1,2}\\.?\\s+|[IVXLCDM]+\\.?\\s+)?(Abstract|Introduction|Background|Related Work|System Design|Methodology|Methods|Proposed Method|Experimental Setup|Experiments|Results|Findings|Discussion|Limitations|Conclusion|Conclusions|References|Bibliography)",
            Pattern.CASE_INSENSITIVE
    );

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    /**
     * Represents an extracted table from the document with Markdown and CSV renderings.
     */
    public record ExtractedTable(
            String caption,
            List<String> headers,
            List<List<String>> rows,
            String markdown,
            String csv
    ) {}

    /**
     * Represents a structured section in the research paper.
     */
    public record ExtractedSection(
            String heading,
            String content
    ) {}

    /**
     * Rich DTO containing all extracted paper data, metadata, structured sections,
     * tables, references, markdown, and full JSON representation.
     */
    public record ExtractedPaperData(
            String title,
            String authors,
            List<String> authorsList,
            String publicationYear,
            String journal,
            String doi,
            String topic,
            String keywords,
            List<String> keywordsList,
            String abstractText,
            String methodology,
            String findings,
            String fullText,
            String markdown,
            String json,
            List<ExtractedTable> tables,
            List<ExtractedSection> sections,
            List<String> references,
            String filePath,
            int pageCount
    ) {
        // Backward-compatible 6-argument constructor
        public ExtractedPaperData(String title, String authors, String publicationYear,
                                  String abstractText, String fullText, String filePath) {
            this(
                    title,
                    authors,
                    splitAuthors(authors),
                    publicationYear,
                    "",
                    "",
                    "",
                    "",
                    Collections.emptyList(),
                    abstractText,
                    "",
                    "",
                    fullText,
                    fullText,
                    "",
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList(),
                    filePath,
                    1
            );
        }

        private static List<String> splitAuthors(String authors) {
            if (authors == null || authors.isBlank()) return Collections.emptyList();
            return Arrays.stream(authors.split("[,;]|\\band\\b"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        }
    }

    /**
     * Extracts text, structured sections, tables, and metadata from PDF or TXT document.
     *
     * @param file input document file
     * @return ExtractedPaperData with full structured JSON and metadata
     * @throws IllegalArgumentException on invalid file
     * @throws IOException on reading/parsing error
     */
    public ExtractedPaperData extract(File file) throws IOException {
        validateFile(file);

        String nameLower = file.getName().toLowerCase();
        if (nameLower.endsWith(".pdf")) {
            return extractFromPdf(file);
        } else if (nameLower.endsWith(".txt")) {
            return extractFromTxt(file);
        } else {
            throw new IllegalArgumentException("Unsupported file format: " + file.getName() + ". Only PDF and TXT are supported.");
        }
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
                    "File size (%.2f MB) exceeds maximum allowed limit of 50 MB.",
                    file.length() / (1024.0 * 1024.0)
            ));
        }

        String name = file.getName().toLowerCase();
        if (!name.endsWith(".pdf") && !name.endsWith(".txt")) {
            throw new IllegalArgumentException("Unsupported format. Please select a .pdf or .txt file.");
        }
    }

    // =========================================================================
    // PDF EXTRACTION IMPLEMENTATION
    // =========================================================================

    private ExtractedPaperData extractFromPdf(File file) throws IOException {
        try (PDDocument document = Loader.loadPDF(file)) {
            if (document.isEncrypted()) {
                throw new IOException("PDF is password-protected or encrypted.");
            }

            int pageCount = document.getNumberOfPages();
            PDDocumentInformation info = document.getDocumentInformation();

            // Extract text using Two-Column Aware Stripper
            TwoColumnAwareStripper stripper = new TwoColumnAwareStripper();
            stripper.strip(document);

            String fullText = stripper.getFullText();
            List<ExtractedTable> tables = stripper.getExtractedTables();

            // Parse metadata and structure
            return buildStructuredData(fullText, info, file.getAbsolutePath(), pageCount, tables);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException("Failed to parse PDF document: " + e.getMessage(), e);
        }
    }

    private ExtractedPaperData extractFromTxt(File file) throws IOException {
        String fullText = Files.readString(file.toPath());
        if (fullText == null || fullText.trim().isEmpty()) {
            throw new IOException("The document does not contain readable text.");
        }
        return buildStructuredData(fullText, null, file.getAbsolutePath(), 1, Collections.emptyList());
    }

    /**
     * Builds structured data from text, PDF info, and tables.
     */
    private ExtractedPaperData buildStructuredData(String fullText, PDDocumentInformation pdfInfo,
                                                   String filePath, int pageCount,
                                                   List<ExtractedTable> tables) {
        String[] lines = fullText.split("\\r?\\n");

        // 1. Extract embedded PDF metadata
        String pdfTitle = (pdfInfo != null && pdfInfo.getTitle() != null) ? pdfInfo.getTitle().trim() : "";
        String pdfAuthor = (pdfInfo != null && pdfInfo.getAuthor() != null) ? pdfInfo.getAuthor().trim() : "";
        String pdfKeywords = (pdfInfo != null && pdfInfo.getKeywords() != null) ? pdfInfo.getKeywords().trim() : "";
        String pdfSubject = (pdfInfo != null && pdfInfo.getSubject() != null) ? pdfInfo.getSubject().trim() : "";

        // 2. Parse Title
        String title = "";
        int lineIdx = 0;
        // Skip common running headers at line 0 (e.g. "IEEE Transactions on...", "arXiv:...")
        while (lineIdx < lines.length && lineIdx < 5) {
            String l = lines[lineIdx].trim();
            if (!l.isEmpty() && !isHeaderFooterLine(l)) {
                title = l;
                lineIdx++;
                // Check if title wraps to next line (common in research papers)
                if (lineIdx < lines.length) {
                    String next = lines[lineIdx].trim();
                    if (!next.isEmpty() && !isAuthorLine(next) && !next.toLowerCase().startsWith("abstract")
                            && next.length() < 120 && Character.isUpperCase(next.charAt(0))) {
                        title += " " + next;
                        lineIdx++;
                    }
                }
                break;
            }
            lineIdx++;
        }

        // If title still empty or looks like PDF metadata title is cleaner, use it
        if ((title.isEmpty() || title.length() < 5) && !pdfTitle.isEmpty() && !pdfTitle.equalsIgnoreCase("untitled")) {
            title = pdfTitle;
        }
        if (title.isEmpty()) {
            title = new File(filePath).getName().replaceFirst("\\.[^.]+$", "");
        }

        // 3. Parse Authors
        String authors = "";
        while (lineIdx < lines.length && lineIdx < 12) {
            String l = lines[lineIdx].trim();
            lineIdx++;
            if (!l.isEmpty() && !isHeaderFooterLine(l) && !l.equalsIgnoreCase(title)) {
                if (l.toLowerCase().startsWith("abstract") || l.toLowerCase().startsWith("1. introduction")) {
                    break;
                }
                // Strip email addresses and affiliation markers
                String cleaned = l.replaceAll("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}", "")
                        .replaceAll("\\b(Department|University|Institute|School|College|Faculty|Lab|Laboratory|Inc\\.|Corp\\.|Center)\\b.*", "")
                        .replaceAll("[0-9*†‡§]", "")
                        .trim();
                if (!cleaned.isEmpty() && cleaned.length() > 2) {
                    authors = cleaned;
                    break;
                }
            }
        }
        if (authors.isEmpty() && !pdfAuthor.isEmpty()) {
            authors = pdfAuthor;
        }

        // 4. Parse Publication Year
        String publicationYear = "";
        String prefix = fullText.length() > 2500 ? fullText.substring(0, 2500) : fullText;
        Matcher yearMatcher = YEAR_PATTERN.matcher(prefix);
        while (yearMatcher.find()) {
            String y = yearMatcher.group();
            int val = Integer.parseInt(y);
            if (val >= 1950 && val <= 2030) {
                publicationYear = y;
                break;
            }
        }
        if (publicationYear.isEmpty()) {
            publicationYear = String.valueOf(java.time.Year.now().getValue());
        }

        // 5. Parse DOI
        String doi = "";
        Matcher doiMatcher = DOI_PATTERN.matcher(fullText);
        if (doiMatcher.find()) {
            doi = doiMatcher.group();
        }

        // 6. Parse Abstract
        String abstractText = "";
        Pattern absPattern = Pattern.compile("(?i)\\babstract\\b[:\\s]*(.+?)(?=(?i)\\b(1\\.?\\s+introduction|introduction|keywords|index terms)\\b|$)", Pattern.DOTALL);
        Matcher absMatcher = absPattern.matcher(fullText);
        if (absMatcher.find()) {
            abstractText = absMatcher.group(1).trim().replaceAll("\\s+", " ");
            if (abstractText.length() > 2500) {
                abstractText = abstractText.substring(0, 2500) + "...";
            }
        }

        // 7. Parse Keywords
        String keywords = "";
        Pattern kwPattern = Pattern.compile("(?i)\\b(keywords|index terms)\\b[:\\s]*(.+?)(?=(?i)\\b(1\\.?\\s+introduction|introduction|\\n\\n)|$)", Pattern.DOTALL);
        Matcher kwMatcher = kwPattern.matcher(fullText);
        if (kwMatcher.find()) {
            keywords = kwMatcher.group(2).trim().replaceAll("\\s+", " ");
            if (keywords.endsWith(".")) keywords = keywords.substring(0, keywords.length() - 1);
        } else if (!pdfKeywords.isEmpty()) {
            keywords = pdfKeywords;
        }

        List<String> keywordsList = new ArrayList<>();
        if (!keywords.isEmpty()) {
            for (String kw : keywords.split("[,;•]")) {
                String k = kw.trim();
                if (!k.isEmpty()) keywordsList.add(k);
            }
        }

        // 8. Extract Venue / Journal
        String journal = "";
        for (int i = 0; i < Math.min(lines.length, 6); i++) {
            String l = lines[i].trim();
            if (l.toLowerCase().contains("journal") || l.toLowerCase().contains("proceedings")
                    || l.toLowerCase().contains("conference") || l.toLowerCase().contains("ieee")
                    || l.toLowerCase().contains("acm") || l.toLowerCase().contains("arxiv")) {
                journal = l;
                break;
            }
        }
        if (journal.isEmpty() && !pdfSubject.isEmpty()) {
            journal = pdfSubject;
        }

        // 9. Parse Structured Sections
        List<ExtractedSection> sections = extractSections(fullText);
        String methodology = "";
        String findings = "";
        for (ExtractedSection sec : sections) {
            String h = sec.heading().toLowerCase();
            if (h.contains("method") || h.contains("proposed") || h.contains("approach") || h.contains("design")) {
                if (methodology.isEmpty()) methodology = sec.content();
            } else if (h.contains("result") || h.contains("finding") || h.contains("experiment") || h.contains("evaluation")) {
                if (findings.isEmpty()) findings = sec.content();
            }
        }

        // 10. Parse References
        List<String> references = extractReferences(fullText);

        // 11. Infer Topic
        String topic = inferTopic(title + " " + abstractText + " " + keywords);

        // 12. Authors List
        List<String> authorsList = ExtractedPaperData.splitAuthors(authors);

        // 13. Generate Clean Markdown representation
        String markdown = generateMarkdown(title, authors, publicationYear, journal, doi, topic,
                keywords, abstractText, sections, tables, references);

        // 14. Generate Structured JSON
        String json = generateJson(title, authors, authorsList, publicationYear, journal, doi,
                topic, keywords, keywordsList, abstractText, methodology, findings,
                sections, tables, references, filePath, pageCount, pdfTitle, pdfAuthor);

        return new ExtractedPaperData(
                title, authors, authorsList, publicationYear, journal, doi,
                topic, keywords, keywordsList, abstractText, methodology, findings,
                fullText, markdown, json, tables, sections, references, filePath, pageCount
        );
    }

    private boolean isHeaderFooterLine(String line) {
        String l = line.toLowerCase();
        if (l.matches("^\\d+$")) return true; // Standalone page number
        if (l.contains("ieee transactions") || l.contains("all rights reserved") || l.contains("copyright")) return true;
        if (l.matches(".*page \\d+ of \\d+.*")) return true;
        return false;
    }

    private boolean isAuthorLine(String line) {
        return line.contains(",") || line.contains("and") || line.matches("^[A-Z][a-z]+ [A-Z][a-z]+.*");
    }

    private List<ExtractedSection> extractSections(String fullText) {
        List<ExtractedSection> sections = new ArrayList<>();
        String[] lines = fullText.split("\\r?\\n");

        String currentHeading = "Preamble";
        StringBuilder currentContent = new StringBuilder();

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;

            Matcher matcher = SECTION_HEADING_PATTERN.matcher(trimmed);
            if (matcher.find() && trimmed.length() < 70) {
                if (currentContent.length() > 0) {
                    sections.add(new ExtractedSection(currentHeading, currentContent.toString().trim()));
                    currentContent.setLength(0);
                }
                currentHeading = trimmed;
            } else {
                if (currentContent.length() > 0) currentContent.append(" ");
                currentContent.append(trimmed);
            }
        }

        if (currentContent.length() > 0) {
            sections.add(new ExtractedSection(currentHeading, currentContent.toString().trim()));
        }

        return sections;
    }

    private List<String> extractReferences(String fullText) {
        List<String> refs = new ArrayList<>();
        int refIdx = fullText.toLowerCase().lastIndexOf("references");
        if (refIdx == -1) {
            refIdx = fullText.toLowerCase().lastIndexOf("bibliography");
        }
        if (refIdx != -1) {
            String refSection = fullText.substring(refIdx);
            String[] lines = refSection.split("\\r?\\n");
            StringBuilder currentRef = new StringBuilder();
            for (String line : lines) {
                String t = line.trim();
                if (t.matches("^\\[\\d+\\].*") || t.matches("^\\d+\\..*")) {
                    if (currentRef.length() > 0) {
                        refs.add(currentRef.toString().trim());
                        currentRef.setLength(0);
                    }
                    currentRef.append(t);
                } else if (currentRef.length() > 0 && !t.isEmpty()) {
                    currentRef.append(" ").append(t);
                }
            }
            if (currentRef.length() > 0) {
                refs.add(currentRef.toString().trim());
            }
        }
        return refs;
    }

    private String inferTopic(String text) {
        String lower = text.toLowerCase();
        if (lower.contains("transformer") || lower.contains("nlp") || lower.contains("language model") || lower.contains("text")) {
            return "Natural Language Processing";
        } else if (lower.contains("vision") || lower.contains("image") || lower.contains("object detection") || lower.contains("cnn")) {
            return "Computer Vision";
        } else if (lower.contains("reinforcement") || lower.contains("policy") || lower.contains("reward")) {
            return "Reinforcement Learning";
        } else if (lower.contains("security") || lower.contains("crypt") || lower.contains("vulnerability")) {
            return "Cybersecurity";
        } else if (lower.contains("robot") || lower.contains("control") || lower.contains("autonomous")) {
            return "Robotics";
        } else if (lower.contains("network") || lower.contains("distributed") || lower.contains("cloud")) {
            return "Systems & Networking";
        } else if (lower.contains("quantum")) {
            return "Quantum Computing";
        }
        return "Artificial Intelligence / Machine Learning";
    }

    private String generateMarkdown(String title, String authors, String year, String journal,
                                    String doi, String topic, String keywords, String abstractText,
                                    List<ExtractedSection> sections, List<ExtractedTable> tables,
                                    List<String> references) {
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(title).append("\n\n");
        if (!authors.isEmpty()) sb.append("**Authors:** ").append(authors).append("\n\n");
        if (!year.isEmpty() || !journal.isEmpty()) {
            sb.append("**Published:** ").append(year);
            if (!journal.isEmpty()) sb.append(" in *").append(journal).append("*");
            sb.append("\n\n");
        }
        if (!doi.isEmpty()) sb.append("**DOI:** [").append(doi).append("](https://doi.org/").append(doi).append(")\n\n");
        if (!topic.isEmpty()) sb.append("**Topic:** `").append(topic).append("`\n\n");
        if (!keywords.isEmpty()) sb.append("**Keywords:** ").append(keywords).append("\n\n");

        if (!abstractText.isEmpty()) {
            sb.append("## Abstract\n\n").append(abstractText).append("\n\n");
        }

        for (ExtractedSection sec : sections) {
            if (!sec.heading().equalsIgnoreCase("Preamble") && !sec.heading().toLowerCase().startsWith("abstract")) {
                sb.append("## ").append(sec.heading()).append("\n\n");
                sb.append(sec.content()).append("\n\n");
            }
        }

        if (tables != null && !tables.isEmpty()) {
            sb.append("## Extracted Tables\n\n");
            for (ExtractedTable t : tables) {
                if (t.caption() != null && !t.caption().isEmpty()) {
                    sb.append("### ").append(t.caption()).append("\n\n");
                }
                sb.append(t.markdown()).append("\n\n");
            }
        }

        if (references != null && !references.isEmpty()) {
            sb.append("## References\n\n");
            for (String ref : references) {
                sb.append("- ").append(ref).append("\n");
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    private String generateJson(String title, String authors, List<String> authorsList,
                                String year, String journal, String doi, String topic,
                                String keywords, List<String> keywordsList, String abstractText,
                                String methodology, String findings, List<ExtractedSection> sections,
                                List<ExtractedTable> tables, List<String> references,
                                String filePath, int pageCount, String pdfTitle, String pdfAuthor) {
        try {
            ObjectNode root = OBJECT_MAPPER.createObjectNode();

            // Metadata Node
            ObjectNode meta = root.putObject("metadata");
            meta.put("title", title);
            meta.put("authors", authors);

            ArrayNode authArr = meta.putArray("authorsList");
            for (String a : authorsList) authArr.add(a);

            meta.put("publicationYear", year);
            meta.put("journal", journal);
            meta.put("doi", doi);
            meta.put("topic", topic);
            meta.put("keywords", keywords);

            ArrayNode kwArr = meta.putArray("keywordsList");
            for (String k : keywordsList) kwArr.add(k);

            meta.put("pageCount", pageCount);
            meta.put("filePath", filePath);

            ObjectNode pdfInfoNode = meta.putObject("pdfEmbeddedInfo");
            pdfInfoNode.put("pdfTitle", pdfTitle);
            pdfInfoNode.put("pdfAuthor", pdfAuthor);

            // Core Content Nodes
            root.put("abstract", abstractText);
            root.put("methodology", methodology);
            root.put("findings", findings);

            // Sections Array
            ArrayNode secArr = root.putArray("sections");
            for (ExtractedSection sec : sections) {
                ObjectNode sNode = secArr.addObject();
                sNode.put("heading", sec.heading());
                sNode.put("content", sec.content());
            }

            // Tables Array
            ArrayNode tblArr = root.putArray("tables");
            for (ExtractedTable tbl : tables) {
                ObjectNode tNode = tblArr.addObject();
                tNode.put("caption", tbl.caption());
                tNode.put("markdown", tbl.markdown());
                tNode.put("csv", tbl.csv());
            }

            // References Array
            ArrayNode refArr = root.putArray("references");
            for (String ref : references) refArr.add(ref);

            return OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(root);
        } catch (Exception e) {
            return "{}";
        }
    }

    // =========================================================================
    // TWO-COLUMN AWARE PDF STRIPPER (Custom PDFBox Subclass)
    // =========================================================================

    /**
     * TwoColumnAwareStripper — Layout-aware text stripper that handles academic two-column papers,
     * strips running headers and footers, and extracts structured tables.
     */
    private static class TwoColumnAwareStripper extends PDFTextStripper {

        private static class TextFragment {
            final String text;
            final float x;
            final float y;
            final float width;
            final float height;

            TextFragment(String text, float x, float y, float width, float height) {
                this.text = text;
                this.x = x;
                this.y = y;
                this.width = width;
                this.height = height;
            }
        }

        private static class LineFragment {
            final float y;
            final float minX;
            final float maxX;
            final StringBuilder text = new StringBuilder();

            LineFragment(float y, float minX, float maxX, String initial) {
                this.y = y;
                this.minX = minX;
                this.maxX = maxX;
                this.text.append(initial);
            }
        }

        private final StringBuilder fullDocText = new StringBuilder();
        private final List<ExtractedTable> extractedTables = new ArrayList<>();
        private final List<TextFragment> pageFragments = new ArrayList<>();

        public TwoColumnAwareStripper() throws IOException {
            super();
            setSortByPosition(true);
        }

        public String getFullText() {
            return fullDocText.toString();
        }

        public List<ExtractedTable> getExtractedTables() {
            return extractedTables;
        }

        public void strip(PDDocument doc) throws IOException {
            for (int p = 1; p <= doc.getNumberOfPages(); p++) {
                setStartPage(p);
                setEndPage(p);
                pageFragments.clear();

                StringWriter dummy = new StringWriter();
                writeText(doc, dummy);

                PDPage page = doc.getPage(p - 1);
                PDRectangle box = page.getMediaBox();
                float width = box.getWidth();
                float height = box.getHeight();

                processPageLayout(pageFragments, width, height, p);
            }
        }

        @Override
        protected void processTextPosition(TextPosition text) {
            String unicode = text.getUnicode();
            if (unicode != null && !unicode.isEmpty()) {
                pageFragments.add(new TextFragment(
                        unicode,
                        text.getXDirAdj(),
                        text.getYDirAdj(),
                        text.getWidthDirAdj(),
                        text.getHeightDir()
                ));
            }
        }

        private void processPageLayout(List<TextFragment> fragments, float pageWidth, float pageHeight, int pageNum) {
            if (fragments.isEmpty()) return;

            // Header & Footer margins (50 points top and bottom)
            float topMargin = 45.0f;
            float bottomMargin = pageHeight - 45.0f;

            // Filter out running header and footer zones
            List<TextFragment> bodyFragments = new ArrayList<>();
            for (TextFragment tf : fragments) {
                if (tf.y >= topMargin && tf.y <= bottomMargin) {
                    bodyFragments.add(tf);
                }
            }

            if (bodyFragments.isEmpty()) {
                bodyFragments = fragments; // Fallback if margins too tight
            }

            // Group fragments into lines based on Y coordinate tolerance
            List<LineFragment> lines = groupIntoLines(bodyFragments);

            // Detect if page has two-column layout
            float midX = pageWidth / 2.0f;
            int leftCount = 0;
            int rightCount = 0;
            int spanCount = 0;

            for (LineFragment line : lines) {
                float lineWidth = line.maxX - line.minX;
                if (lineWidth > (pageWidth * 0.65f)) {
                    spanCount++;
                } else if (line.maxX < (midX + 15.0f)) {
                    leftCount++;
                } else if (line.minX > (midX - 15.0f)) {
                    rightCount++;
                }
            }

            boolean isTwoColumn = (leftCount > 5 && rightCount > 5);

            if (!isTwoColumn) {
                // Single column: simply output lines top to bottom
                for (LineFragment line : lines) {
                    fullDocText.append(line.text.toString().trim()).append("\n");
                }
            } else {
                // Two-column layout:
                // 1. Full-width spans (e.g. Title, Authors, Abstract at top of page 1)
                // 2. Left column (x < midX) top to bottom
                // 3. Right column (x >= midX) top to bottom
                List<LineFragment> topSpans = new ArrayList<>();
                List<LineFragment> leftCol = new ArrayList<>();
                List<LineFragment> rightCol = new ArrayList<>();
                List<LineFragment> bottomSpans = new ArrayList<>();

                float twoColStartY = 0;
                if (pageNum == 1 && spanCount > 0) {
                    // Find where two-column text starts
                    for (LineFragment line : lines) {
                        if (line.maxX < (midX + 15.0f) || line.minX > (midX - 15.0f)) {
                            twoColStartY = line.y;
                            break;
                        }
                    }
                }

                for (LineFragment line : lines) {
                    float lineWidth = line.maxX - line.minX;
                    if (pageNum == 1 && line.y < twoColStartY) {
                        topSpans.add(line);
                    } else if (lineWidth > (pageWidth * 0.7f)) {
                        bottomSpans.add(line);
                    } else if (line.maxX <= (midX + 20.0f)) {
                        leftCol.add(line);
                    } else {
                        rightCol.add(line);
                    }
                }

                // Output top spans
                for (LineFragment l : topSpans) fullDocText.append(l.text.toString().trim()).append("\n");
                // Output Column 1
                for (LineFragment l : leftCol) fullDocText.append(l.text.toString().trim()).append("\n");
                // Output Column 2
                for (LineFragment l : rightCol) fullDocText.append(l.text.toString().trim()).append("\n");
                // Output bottom spans
                for (LineFragment l : bottomSpans) fullDocText.append(l.text.toString().trim()).append("\n");
            }

            fullDocText.append("\n");

            // Detect table candidates on this page
            detectTables(lines);
        }

        private List<LineFragment> groupIntoLines(List<TextFragment> fragments) {
            // Sort by Y, then X
            fragments.sort((a, b) -> {
                int yComp = Float.compare(a.y, b.y);
                return (yComp != 0) ? yComp : Float.compare(a.x, b.x);
            });

            List<LineFragment> lines = new ArrayList<>();
            float yTolerance = 4.0f;

            for (TextFragment tf : fragments) {
                boolean placed = false;
                for (LineFragment line : lines) {
                    if (Math.abs(line.y - tf.y) <= yTolerance) {
                        // Check spacing
                        if (tf.x - line.maxX > 3.0f) {
                            line.text.append(" ");
                        }
                        line.text.append(tf.text);
                        placed = true;
                        break;
                    }
                }
                if (!placed) {
                    lines.add(new LineFragment(tf.y, tf.x, tf.x + tf.width, tf.text));
                }
            }

            return lines;
        }

        private void detectTables(List<LineFragment> lines) {
            for (int i = 0; i < lines.size(); i++) {
                String lineText = lines.get(i).text.toString().trim();
                if (lineText.matches("(?i)^(Table\\s+\\d+|TABLE\\s+[IVXLCDM]+)[:\\s].*")) {
                    String caption = lineText;
                    List<String> tableLines = new ArrayList<>();
                    int j = i + 1;
                    while (j < lines.size() && j < i + 15) {
                        String tLine = lines.get(j).text.toString().trim();
                        if (tLine.isEmpty() || tLine.startsWith("Figure") || tLine.startsWith("Fig.")) break;
                        // Table rows often have multiple spaces or columns
                        if (tLine.split("\\s{2,}").length >= 2 || tLine.contains("\t")) {
                            tableLines.add(tLine);
                        }
                        j++;
                    }
                    if (tableLines.size() >= 2) {
                        formatAndAddTable(caption, tableLines);
                    }
                }
            }
        }

        private void formatAndAddTable(String caption, List<String> rawLines) {
            List<String> headers = new ArrayList<>();
            List<List<String>> rows = new ArrayList<>();

            StringBuilder md = new StringBuilder();
            StringBuilder csv = new StringBuilder();

            if (!rawLines.isEmpty()) {
                String[] hCols = rawLines.get(0).split("\\s{2,}|\t");
                for (String c : hCols) headers.add(c.trim());

                // Markdown Header
                md.append("| ").append(String.join(" | ", headers)).append(" |\n");
                md.append("|").append("---|".repeat(headers.size())).append("\n");

                // CSV Header
                csv.append(String.join(",", headers.stream().map(this::escapeCsv).toList())).append("\n");

                // Rows
                for (int r = 1; r < rawLines.size(); r++) {
                    String[] cols = rawLines.get(r).split("\\s{2,}|\t");
                    List<String> rowList = new ArrayList<>();
                    for (String c : cols) rowList.add(c.trim());
                    rows.add(rowList);

                    md.append("| ").append(String.join(" | ", rowList)).append(" |\n");
                    csv.append(String.join(",", rowList.stream().map(this::escapeCsv).toList())).append("\n");
                }

                extractedTables.add(new ExtractedTable(caption, headers, rows, md.toString(), csv.toString()));
            }
        }

        private String escapeCsv(String val) {
            if (val.contains(",") || val.contains("\"") || val.contains("\n")) {
                return "\"" + val.replace("\"", "\"\"") + "\"";
            }
            return val;
        }
    }
}
