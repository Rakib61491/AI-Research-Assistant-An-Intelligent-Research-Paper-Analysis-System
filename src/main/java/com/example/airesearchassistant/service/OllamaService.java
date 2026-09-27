package com.example.airesearchassistant.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * OllamaService — Client service for local Ollama HTTP API (http://localhost:11434).
 * Uses Java 21 HttpClient and Jackson Databind.
 * Contains no JavaFX/FXML references (Hard Rule 4).
 */
public class OllamaService {

    private String baseUrl = "http://localhost:11434";
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public record ChatMessage(String role, String content) {}

    public OllamaService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public OllamaService(String baseUrl) {
        this();
        setBaseUrl(baseUrl);
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        if (baseUrl != null && !baseUrl.trim().isEmpty()) {
            String url = baseUrl.trim();
            if (url.endsWith("/")) {
                url = url.substring(0, url.length() - 1);
            }
            this.baseUrl = url;
        }
    }

    /**
     * Tests connection to Ollama server (GET /).
     *
     * @return true if server is reachable and responds with "Ollama is running"
     */
    public boolean testConnection() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/"))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200 && response.body().contains("Ollama is running");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Fetches the list of locally available models (GET /api/tags).
     *
     * @return list of model names (e.g. ["llama3.2:latest", "mistral:latest"])
     * @throws OllamaException if unreachable or parsing fails
     */
    public List<String> listModels() throws OllamaException {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/tags"))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new OllamaException("Ollama server returned HTTP " + response.statusCode() + ": " + response.body());
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode modelsNode = root.get("models");
            List<String> modelNames = new ArrayList<>();
            if (modelsNode != null && modelsNode.isArray()) {
                for (JsonNode m : modelsNode) {
                    JsonNode nameNode = m.get("name");
                    if (nameNode != null) {
                        modelNames.add(nameNode.asText());
                    }
                }
            }
            return modelNames;
        } catch (ConnectException e) {
            throw new OllamaException("Cannot connect to Ollama at " + baseUrl + ". Please ensure Ollama is installed and running.", e);
        } catch (HttpTimeoutException e) {
            throw new OllamaException("Connection timed out while querying Ollama at " + baseUrl + ".", e);
        } catch (IOException | InterruptedException e) {
            throw new OllamaException("Failed to retrieve models from Ollama: " + e.getMessage(), e);
        }
    }

    /**
     * Generates a completion via POST /api/generate with stream: false.
     */
    public String generate(String model, String prompt, double temperature, int maxTokens, Duration timeout) throws OllamaException {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("model", model);
            root.put("prompt", prompt);
            root.put("stream", false);

            ObjectNode options = root.putObject("options");
            options.put("temperature", temperature);
            if (maxTokens > 0) {
                options.put("num_predict", maxTokens);
            }

            String requestBody = objectMapper.writeValueAsString(root);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/generate"))
                    .timeout(timeout != null ? timeout : Duration.ofSeconds(120))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new OllamaException("Ollama error (HTTP " + response.statusCode() + "): " + response.body());
            }

            JsonNode respNode = objectMapper.readTree(response.body());
            JsonNode textNode = respNode.get("response");
            return textNode != null ? textNode.asText() : "";
        } catch (ConnectException e) {
            throw new OllamaException("Cannot connect to Ollama at " + baseUrl + ". Please verify Ollama is running.", e);
        } catch (HttpTimeoutException e) {
            throw new OllamaException("Ollama generation timed out. Consider increasing timeout in Settings.", e);
        } catch (IOException | InterruptedException e) {
            throw new OllamaException("Error during Ollama generation: " + e.getMessage(), e);
        }
    }

    /**
     * Executes multi-turn conversation via POST /api/chat with stream: false.
     */
    public String chat(String model, List<ChatMessage> messages, double temperature, int maxTokens, Duration timeout) throws OllamaException {
        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("model", model);
            root.put("stream", false);

            ArrayNode messagesArray = root.putArray("messages");
            for (ChatMessage msg : messages) {
                ObjectNode mNode = messagesArray.addObject();
                mNode.put("role", msg.role());
                mNode.put("content", msg.content());
            }

            ObjectNode options = root.putObject("options");
            options.put("temperature", temperature);
            if (maxTokens > 0) {
                options.put("num_predict", maxTokens);
            }

            String requestBody = objectMapper.writeValueAsString(root);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/api/chat"))
                    .timeout(timeout != null ? timeout : Duration.ofSeconds(120))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new OllamaException("Ollama error (HTTP " + response.statusCode() + "): " + response.body());
            }

            JsonNode respNode = objectMapper.readTree(response.body());
            JsonNode msgNode = respNode.path("message").path("content");
            return msgNode.isMissingNode() ? "" : msgNode.asText();
        } catch (ConnectException e) {
            throw new OllamaException("Cannot connect to Ollama at " + baseUrl + ". Please verify Ollama is running.", e);
        } catch (HttpTimeoutException e) {
            throw new OllamaException("Ollama chat timed out. Consider increasing timeout in Settings.", e);
        } catch (IOException | InterruptedException e) {
            throw new OllamaException("Error during Ollama chat: " + e.getMessage(), e);
        }
    }

    // ----- Section 7 Contract: Specialized Research AI Methods ---------------

    /**
     * Generates a structured 7-section paper summary.
     */
    public String summarize(String paperText, String model, double temperature, int maxTokens, Duration timeout) throws OllamaException {
        String systemPrompt = "You are a research paper analysis assistant. Read the following paper text and produce " +
                "a structured summary with these sections: Research Problem, Objectives, Methodology, Main Findings, " +
                "Contributions, Limitations, Future Work. Use plain text. Do not invent facts not present in the text. " +
                "If a section is not present in the paper, write \"Not stated in the paper.\"";

        List<ChatMessage> messages = List.of(
                new ChatMessage("system", systemPrompt),
                new ChatMessage("user", "Paper text:\n" + paperText)
        );
        return chat(model, messages, temperature, maxTokens, timeout);
    }

    /**
     * Extracts structured key-value pairs (abstract, methodology, findings, keywords, topic, contributions, limitations).
     */
    public String extractInfo(String paperText, String model, double temperature, int maxTokens, Duration timeout) throws OllamaException {
        String prompt = "Extract from the paper: (1) Abstract (2) Methodology (3) Main Findings " +
                "(4) Keywords (up to 8) (5) Research Topic (single best category) (6) Contributions (7) Limitations. " +
                "Return as key: value pairs, one per line.\n\nPaper text:\n" + paperText;

        return generate(model, prompt, temperature, maxTokens, timeout);
    }

    /**
     * Context-aware Q&A on paper text.
     */
    public String askQuestion(String paperText, String question, List<ChatMessage> conversationHistory,
                              String model, double temperature, int maxTokens, Duration timeout) throws OllamaException {
        String systemPrompt = "You are answering questions about a specific paper. Use only the paper text below as context. " +
                "If the answer is not in the text, say so. Paper text: <<<" + paperText + ">>>";

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage("system", systemPrompt));
        if (conversationHistory != null) {
            messages.addAll(conversationHistory);
        }
        messages.add(new ChatMessage("user", question));

        return chat(model, messages, temperature, maxTokens, timeout);
    }

    /**
     * Topic classification from candidate topics.
     */
    public String classify(String paperText, List<String> candidateTopics, String model, Duration timeout) throws OllamaException {
        String topicList = candidateTopics != null && !candidateTopics.isEmpty()
                ? String.join(", ", candidateTopics)
                : "Machine Learning, Natural Language Processing, Computer Vision, Robotics, Software Engineering, Systems, Security";

        String prompt = "You are an academic classifier. Given the following research paper text, select the single best " +
                "matching research topic from this list: [" + topicList + "]. " +
                "Return only the exact topic name as a single line.\n\nPaper text:\n" + paperText;

        return generate(model, prompt, 0.1, 50, timeout).trim();
    }

    /**
     * Produces a structured comparison between two research papers covering:
     * Objectives, Methodology, Datasets, Findings, Contributions, Limitations,
     * Similarities, Differences, and Research Gaps.
     */
    public String comparePapers(String paperTextA, String paperTextB,
                                String titleA, String titleB,
                                String model, double temperature, int maxTokens, Duration timeout) throws OllamaException {
        String systemPrompt = "You are a senior research analyst. Your task is to compare two research papers " +
                "and produce a structured comparative analysis with the following clearly labelled sections:\n" +
                "1. Research Objectives\n2. Methodology\n3. Datasets & Evaluation\n" +
                "4. Main Findings\n5. Key Contributions\n6. Limitations\n" +
                "7. Similarities\n8. Differences\n9. Research Gaps & Future Work\n\n" +
                "Be objective and evidence-based. Do not invent facts not present in the texts. " +
                "If a section cannot be assessed for a paper, state \"Not stated in the paper.\"";

        String userPrompt = "Paper A: " + titleA + "\n---\n" + paperTextA +
                "\n\n===\n\nPaper B: " + titleB + "\n---\n" + paperTextB;

        List<ChatMessage> messages = List.of(
                new ChatMessage("system", systemPrompt),
                new ChatMessage("user", userPrompt)
        );
        return chat(model, messages, temperature, maxTokens, timeout);
    }
}
