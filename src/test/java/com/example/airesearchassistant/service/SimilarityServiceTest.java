package com.example.airesearchassistant.service;

import com.example.airesearchassistant.model.ResearchPaper;
import com.example.airesearchassistant.service.SimilarityService.SimilarityResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SimilarityServiceTest {

    private SimilarityService similarityService;

    @BeforeEach
    public void setUp() {
        similarityService = new SimilarityService();
    }

    @Test
    public void testIdenticalPapersSimilarity() {
        ResearchPaper paperA = new ResearchPaper();
        paperA.setTitle("Deep Residual Learning for Image Recognition");
        paperA.setKeywords("deep learning, neural networks, image recognition, residual learning");
        paperA.setTopic("Computer Vision");
        paperA.setAbstractText("Deeper neural networks are more difficult to train. We present a residual learning framework to ease the training of networks that are substantially deeper than those used previously.");

        ResearchPaper paperB = new ResearchPaper();
        paperB.setTitle("Deep Residual Learning for Image Recognition");
        paperB.setKeywords("deep learning, neural networks, image recognition, residual learning");
        paperB.setTopic("Computer Vision");
        paperB.setAbstractText("Deeper neural networks are more difficult to train. We present a residual learning framework to ease the training of networks that are substantially deeper than those used previously.");

        SimilarityResult result = similarityService.computeSimilarity(paperA, paperB);

        assertNotNull(result);
        assertTrue(result.similarityPercent() > 95.0, "Identical papers should have close to 100% similarity");
        assertFalse(result.sharedTerms().isEmpty(), "Shared terms should not be empty");
    }

    @Test
    public void testCompletelyDisjointPapersSimilarity() {
        ResearchPaper paperA = new ResearchPaper();
        paperA.setTitle("Quantum Cryptography and Entanglement");
        paperA.setKeywords("quantum, photon, cryptography, physics, qubits");
        paperA.setTopic("Quantum Physics");
        paperA.setAbstractText("Quantum key distribution exploits fundamental laws of physics to guarantee unconditional security.");

        ResearchPaper paperB = new ResearchPaper();
        paperB.setTitle("Ancient Roman Agricultural Techniques");
        paperB.setKeywords("farming, crops, wheat, irrigation, mediterranean");
        paperB.setTopic("Archaeology");
        paperB.setAbstractText("Historical analysis of crop rotation and water management systems in ancient Roman agriculture.");

        SimilarityResult result = similarityService.computeSimilarity(paperA, paperB);

        assertNotNull(result);
        assertEquals(0.0, result.similarityPercent(), 0.001, "Disjoint papers should have 0% similarity");
        assertTrue(result.sharedTerms().isEmpty(), "Shared terms should be empty for disjoint topics");
    }

    @Test
    public void testPartiallySimilarPapers() {
        ResearchPaper paperA = new ResearchPaper();
        paperA.setTitle("Convolutional Neural Networks for Medical Image Classification");
        paperA.setKeywords("deep learning, medical imaging, CNN, diagnosis");
        paperA.setTopic("Healthcare AI");
        paperA.setAbstractText("We apply deep convolutional neural networks to classify tumor images with high precision.");

        ResearchPaper paperB = new ResearchPaper();
        paperB.setTitle("Graph Neural Networks for Molecular Property Prediction");
        paperB.setKeywords("deep learning, molecular graph, chemistry, neural networks");
        paperB.setTopic("Computational Chemistry");
        paperB.setAbstractText("We investigate graph neural networks and deep learning models for chemical molecular properties.");

        SimilarityResult result = similarityService.computeSimilarity(paperA, paperB);

        assertNotNull(result);
        assertTrue(result.similarityPercent() > 0.0, "Should have positive similarity due to neural network terms");
        assertTrue(result.similarityPercent() < 90.0, "Should not be identical");
        assertTrue(result.sharedTerms().contains("neural") || result.sharedTerms().contains("learning"),
                "Should find shared AI terminology");
    }

    @Test
    public void testEmptyFieldsGracefulHandling() {
        ResearchPaper paperA = new ResearchPaper();
        paperA.setTitle("Paper With Minimal Metadata");

        ResearchPaper paperB = new ResearchPaper();
        paperB.setTitle("Another Paper With Minimal Metadata");

        SimilarityResult result = similarityService.computeSimilarity(paperA, paperB);

        assertNotNull(result);
        assertDoesNotThrow(() -> result.toReport(paperA.getTitle(), paperB.getTitle()));
    }
}
