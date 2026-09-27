package com.example.airesearchassistant.service;

/**
 * OllamaException — Thrown when an error occurs while communicating with the Ollama local AI server.
 * Provides user-friendly error messages suitable for display in UI alerts (Hard Rule 7).
 */
public class OllamaException extends Exception {

    public OllamaException(String message) {
        super(message);
    }

    public OllamaException(String message, Throwable cause) {
        super(message, cause);
    }
}
