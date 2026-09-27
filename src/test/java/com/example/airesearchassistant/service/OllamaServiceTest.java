package com.example.airesearchassistant.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

public class OllamaServiceTest {

    private OllamaService service;

    @BeforeEach
    public void setUp() {
        // Point to an invalid local port to test graceful offline failure
        service = new OllamaService("http://127.0.0.1:59999");
    }

    @Test
    public void testBaseUrlSanitization() {
        service.setBaseUrl("http://localhost:11434/");
        assertEquals("http://localhost:11434", service.getBaseUrl());
    }

    @Test
    public void testOfflineConnectionReturnsFalse() {
        assertFalse(service.testConnection());
    }

    @Test
    public void testOfflineGenerateThrowsFriendlyException() {
        OllamaException ex = assertThrows(OllamaException.class, () -> {
            service.generate("llama3.2", "Hello", 0.4, 100, Duration.ofSeconds(1));
        });
        assertTrue(ex.getMessage().contains("Cannot connect") || ex.getMessage().contains("timed out") || ex.getMessage().contains("Error"));
    }

    @Test
    public void testOfflineListModelsThrowsFriendlyException() {
        OllamaException ex = assertThrows(OllamaException.class, () -> {
            service.listModels();
        });
        assertTrue(ex.getMessage().contains("Cannot connect") || ex.getMessage().contains("timed out") || ex.getMessage().contains("Failed"));
    }
}
