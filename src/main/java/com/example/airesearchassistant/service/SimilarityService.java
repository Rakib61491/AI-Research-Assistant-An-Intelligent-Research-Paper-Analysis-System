package com.example.airesearchassistant.service;

import com.example.airesearchassistant.model.ResearchPaper;

import java.util.*;
import java.util.stream.Collectors;

/**
 * SimilarityService — Pure-Java TF-IDF + cosine similarity over paper text fields.
 * Returns similarity percentage, shared terms, and distinctive terms per paper.
 * Contains no JavaFX imports (Hard Rule 2). Never touches FXML or controls (Hard Rule 4).
 */
public class SimilarityService {

    private static final Set<String> STOP_WORDS = Set.of(
            "a", "an", "the", "and", "or", "but", "in", "on", "at", "to", "for", "of", "with",
            "by", "from", "is", "are", "was", "were", "be", "been", "being", "have", "has", "had",
            "do", "does", "did", "will", "would", "could", "should", "may", "might", "shall",
            "this", "that", "these", "those", "it", "its", "we", "our", "they", "their", "he", "she",
            "as", "if", "then", "than", "also", "can", "not", "no", "more", "such", "about",
            "which", "so", "each", "into", "between", "through", "during", "before", "after"
    );

    private static final int MIN_TERM_LENGTH = 3;

    // -----------------------------------------------------------------------
    // Public API
    // -----------------------------------------------------------------------

    public SimilarityResult computeSimilarity(ResearchPaper paperA, ResearchPaper paperB) {
        Objects.requireNonNull(paperA, "Paper A cannot be null");
        Objects.requireNonNull(paperB, "Paper B cannot be null");

        String textA = buildText(paperA);
        String textB = buildText(paperB);

        Map<String, Integer> freqA = termFrequency(textA);
        Map<String, Integer> freqB = termFrequency(textB);

        Set<String> vocabulary = new HashSet<>();
        vocabulary.addAll(freqA.keySet());
        vocabulary.addAll(freqB.keySet());

        if (vocabulary.isEmpty()) {
            return new SimilarityResult(0.0, Collections.emptyList(),
                    Collections.emptyList(), Collections.emptyList());
        }

        Map<String, Double> idf = computeIdf(vocabulary, freqA, freqB);
        Map<String, Double> tfidfA = computeTfIdf(freqA, idf, textA);
        Map<String, Double> tfidfB = computeTfIdf(freqB, idf, textB);

        double similarity = cosineSimilarity(tfidfA, tfidfB, vocabulary);
        double percentage = Math.round(similarity * 1000.0) / 10.0;

        List<String> sharedTerms = vocabulary.stream()
                .filter(t -> freqA.containsKey(t) && freqB.containsKey(t))
                .sorted(Comparator.comparingDouble((String t) ->
                        -(tfidfA.getOrDefault(t, 0.0) + tfidfB.getOrDefault(t, 0.0))))
                .limit(15)
                .collect(Collectors.toList());

        List<String> distinctiveA = tfidfA.entrySet().stream()
                .filter(e -> !freqB.containsKey(e.getKey()))
                .sorted(Comparator.comparingDouble(e -> -e.getValue()))
                .limit(10)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        List<String> distinctiveB = tfidfB.entrySet().stream()
                .filter(e -> !freqA.containsKey(e.getKey()))
                .sorted(Comparator.comparingDouble(e -> -e.getValue()))
                .limit(10)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        return new SimilarityResult(percentage, sharedTerms, distinctiveA, distinctiveB);
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private String buildText(ResearchPaper p) {
        StringBuilder sb = new StringBuilder();
        appendField(sb, p.getTitle(), 3);
        appendField(sb, p.getKeywords(), 2);
        appendField(sb, p.getAbstractText(), 1);
        appendField(sb, p.getMethodology(), 1);
        appendField(sb, p.getFindings(), 1);
        appendField(sb, p.getTopic(), 2);
        return sb.toString().toLowerCase();
    }

    private void appendField(StringBuilder sb, String field, int weight) {
        if (field == null || field.isBlank()) return;
        for (int i = 0; i < weight; i++) {
            sb.append(' ').append(field);
        }
    }

    private Map<String, Integer> termFrequency(String text) {
        Map<String, Integer> freq = new HashMap<>();
        String[] tokens = text.split("[^a-z0-9]+");
        for (String token : tokens) {
            if (token.length() >= MIN_TERM_LENGTH && !STOP_WORDS.contains(token)) {
                freq.merge(token, 1, Integer::sum);
            }
        }
        return freq;
    }

    private Map<String, Double> computeIdf(Set<String> vocab,
                                           Map<String, Integer> freqA,
                                           Map<String, Integer> freqB) {
        Map<String, Double> idf = new HashMap<>();
        for (String term : vocab) {
            int df = 0;
            if (freqA.containsKey(term)) df++;
            if (freqB.containsKey(term)) df++;
            idf.put(term, Math.log(3.0 / (df + 1)) + 1.0);
        }
        return idf;
    }

    private Map<String, Double> computeTfIdf(Map<String, Integer> freq,
                                              Map<String, Double> idf,
                                              String rawText) {
        long totalTerms = freq.values().stream().mapToLong(Integer::longValue).sum();
        if (totalTerms == 0) return Collections.emptyMap();
        Map<String, Double> tfidf = new HashMap<>();
        for (Map.Entry<String, Integer> e : freq.entrySet()) {
            double tf = (double) e.getValue() / totalTerms;
            tfidf.put(e.getKey(), tf * idf.getOrDefault(e.getKey(), 1.0));
        }
        return tfidf;
    }

    private double cosineSimilarity(Map<String, Double> a, Map<String, Double> b, Set<String> vocab) {
        double dot = 0.0, normA = 0.0, normB = 0.0;
        for (String term : vocab) {
            double va = a.getOrDefault(term, 0.0);
            double vb = b.getOrDefault(term, 0.0);
            dot += va * vb;
            normA += va * va;
            normB += vb * vb;
        }
        if (normA == 0.0 || normB == 0.0) return 0.0;
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    // -----------------------------------------------------------------------
    // Result record
    // -----------------------------------------------------------------------

    public record SimilarityResult(
            double similarityPercent,
            List<String> sharedTerms,
            List<String> distinctiveA,
            List<String> distinctiveB
    ) {
        public String toReport(String titleA, String titleB) {
            StringBuilder sb = new StringBuilder();
            sb.append("=== Lexical Similarity Report ===\n\n");
            sb.append("Paper A: ").append(titleA).append("\n");
            sb.append("Paper B: ").append(titleB).append("\n\n");
            sb.append(String.format("Similarity Score:  %.1f%%\n\n", similarityPercent));

            sb.append("Shared Key Terms (").append(sharedTerms.size()).append("):\n");
            sb.append(sharedTerms.isEmpty() ? "  (none)\n" : "  " + String.join(", ", sharedTerms) + "\n");

            sb.append("\nDistinctive to Paper A (").append(distinctiveA.size()).append("):\n");
            sb.append(distinctiveA.isEmpty() ? "  (none)\n" : "  " + String.join(", ", distinctiveA) + "\n");

            sb.append("\nDistinctive to Paper B (").append(distinctiveB.size()).append("):\n");
            sb.append(distinctiveB.isEmpty() ? "  (none)\n" : "  " + String.join(", ", distinctiveB) + "\n");

            sb.append("\n⚠ Disclaimer: This is a lexical similarity measure only. ");
            sb.append("It is NOT proof of plagiarism or identical contributions.\n");
            return sb.toString();
        }
    }
}
