package com.example.airesearchassistant.service;

import com.example.airesearchassistant.service.PdfExtractionService.ExtractedPaperData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class PdfExtractionServiceTest {

    private PdfExtractionService service;

    @BeforeEach
    public void setUp() {
        service = new PdfExtractionService();
    }

    @Test
    public void testExtractFromTxtFile(@TempDir Path tempDir) throws IOException {
        Path txtPath = tempDir.resolve("sample_paper.txt");
        String content = """
                Deep Learning for Natural Language Processing
                Alice Johnson, Bob Martin
                Published in 2023.
                
                Abstract
                This paper provides a comprehensive overview of modern deep learning methods applied to NLP tasks.
                
                1. Introduction
                Recent advances in language modeling have revolutionized artificial intelligence.
                """;
        Files.writeString(txtPath, content);

        ExtractedPaperData data = service.extract(txtPath.toFile());
        assertNotNull(data);
        assertEquals("Deep Learning for Natural Language Processing", data.title());
        assertEquals("Alice Johnson, Bob Martin", data.authors());
        assertEquals("2023", data.publicationYear());
        assertTrue(data.abstractText().contains("comprehensive overview"));
        assertTrue(data.fullText().contains("Recent advances"));
    }

    @Test
    public void testEmptyFileValidation(@TempDir Path tempDir) throws IOException {
        Path emptyFile = tempDir.resolve("empty.txt");
        Files.createFile(emptyFile);

        Exception ex = assertThrows(IllegalArgumentException.class, () -> {
            service.extract(emptyFile.toFile());
        });
        assertTrue(ex.getMessage().contains("empty"));
    }

    @Test
    public void testUnsupportedFormat(@TempDir Path tempDir) throws IOException {
        Path unsupported = tempDir.resolve("image.png");
        Files.writeString(unsupported, "some content");

        Exception ex = assertThrows(IllegalArgumentException.class, () -> {
            service.extract(unsupported.toFile());
        });
        assertTrue(ex.getMessage().contains("Unsupported"));
    }
}
