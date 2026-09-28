package com.example.airesearchassistant.service;

import com.example.airesearchassistant.model.ResearchPaper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PaperServiceTest {

    private PaperService paperService;

    @BeforeEach
    public void setUp() {
        paperService = new PaperService();
    }

    @Test
    public void testResolveSummaryContextUsesIntroAndAbstract() {
        ResearchPaper paper = new ResearchPaper();
        paper.setTitle("Deep Learning for Speech Recognition");
        paper.setAuthors("John Doe, Jane Smith");
        paper.setPublicationYear("2024");
        paper.setTopic("Speech Processing");
        paper.setAbstractText("This paper introduces a novel end-to-end speech model.");

        String sampleJson = """
            {
              "metadata": {
                "title": "Deep Learning for Speech Recognition",
                "authors": "John Doe, Jane Smith",
                "publicationYear": "2024",
                "topic": "Speech Processing"
              },
              "abstract": "This paper introduces a novel end-to-end speech model.",
              "methodology": "We train a 12-layer transformer encoder on 10,000 hours of audio.",
              "findings": "The proposed architecture achieves a 15% reduction in WER.",
              "sections": [
                {
                  "heading": "1. Introduction",
                  "content": "Speech recognition has made huge strides. In this work we explore on-device streaming."
                },
                {
                  "heading": "2. Methodology and Architecture",
                  "content": "Our encoder utilizes conformer blocks with causal self-attention."
                },
                {
                  "heading": "3. Experimental Results",
                  "content": "Extensive evaluations on LibriSpeech demonstrate state-of-the-art results."
                },
                {
                  "heading": "4. Conclusion and Future Work",
                  "content": "We presented an efficient acoustic model suitable for edge devices."
                }
              ]
            }
            """;
        paper.setExtractedJson(sampleJson);

        String summaryContext = paperService.resolveSummaryContext(paper);

        assertNotNull(summaryContext);
        assertTrue(summaryContext.contains("Deep Learning for Speech Recognition"));
        assertTrue(summaryContext.contains("ABSTRACT"));
        assertTrue(summaryContext.contains("This paper introduces a novel end-to-end speech model."));
        assertTrue(summaryContext.contains("1. Introduction"));
        assertTrue(summaryContext.contains("Conclusion and Future Work"));
    }

    @Test
    public void testResolveTargetedMethodologyContext() {
        ResearchPaper paper = new ResearchPaper();
        paper.setTitle("Quantum Neural Networks");
        String sampleJson = """
            {
              "metadata": { "title": "Quantum Neural Networks" },
              "methodology": "Parameterized quantum circuits executed on QPU.",
              "sections": [
                { "heading": "Introduction", "content": "Introductory remarks." },
                { "heading": "Proposed Methodology", "content": "Detailed quantum variational ansatz." },
                { "heading": "Results", "content": "85% accuracy on MNIST." }
              ]
            }
            """;
        paper.setExtractedJson(sampleJson);

        String context = paperService.resolveMethodologyContext(paper);

        assertTrue(context.contains("Quantum Neural Networks"));
        assertTrue(context.contains("Parameterized quantum circuits"));
        assertTrue(context.contains("Proposed Methodology"));
        assertFalse(context.contains("85% accuracy on MNIST")); // Results should not be in methodology
    }

    @Test
    public void testResolveTargetedFindingsContext() {
        ResearchPaper paper = new ResearchPaper();
        paper.setTitle("Quantum Neural Networks");
        String sampleJson = """
            {
              "metadata": { "title": "Quantum Neural Networks" },
              "findings": "Achieved 92.4% test accuracy with 50% fewer parameters.",
              "sections": [
                { "heading": "Introduction", "content": "Introductory remarks." },
                { "heading": "Methodology", "content": "Detailed method." },
                { "heading": "Experimental Results & Discussion", "content": "Significant latency improvements observed across all benchmarks." }
              ]
            }
            """;
        paper.setExtractedJson(sampleJson);

        String context = paperService.resolveFindingsContext(paper);

        assertTrue(context.contains("Quantum Neural Networks"));
        assertTrue(context.contains("92.4% test accuracy"));
        assertTrue(context.contains("Experimental Results & Discussion"));
        assertFalse(context.contains("Detailed method")); // Method should not be in findings
    }
}
