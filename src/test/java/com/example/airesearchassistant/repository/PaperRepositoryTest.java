package com.example.airesearchassistant.repository;

import com.example.airesearchassistant.model.ResearchPaper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class PaperRepositoryTest {

    private PaperRepository paperRepository;

    @BeforeEach
    public void setUp() {
        paperRepository = new PaperRepository();
    }

    @Test
    public void testInsertAndFindById() throws SQLException {
        ResearchPaper paper = new ResearchPaper(
                "Test Paper " + System.currentTimeMillis(),
                "Jane Doe, John Smith",
                "2023",
                "IEEE Transactions on AI",
                "Machine Learning",
                "This paper discusses machine learning optimization."
        );

        int id = paperRepository.insert(paper);
        assertTrue(id > 0);

        Optional<ResearchPaper> found = paperRepository.findById(id);
        assertTrue(found.isPresent());
        assertEquals(paper.getTitle(), found.get().getTitle());
        assertEquals("2023", found.get().getPublicationYear());
        assertEquals("Machine Learning", found.get().getTopic());

        // Clean up
        boolean deleted = paperRepository.delete(id);
        assertTrue(deleted);
    }

    @Test
    public void testToggleFavorite() throws SQLException {
        ResearchPaper paper = new ResearchPaper(
                "Favorite Test Paper " + System.currentTimeMillis(),
                "Author A",
                "2022",
                "NeurIPS",
                "Computer Vision",
                "Vision transformers."
        );

        int id = paperRepository.insert(paper);
        assertTrue(id > 0);

        // Initially false
        assertFalse(paperRepository.findById(id).orElseThrow().isFavorite());

        // Toggle to true
        boolean nowFav = paperRepository.toggleFavorite(id);
        assertTrue(nowFav);
        assertTrue(paperRepository.findById(id).orElseThrow().isFavorite());

        // Toggle back to false
        boolean unFav = paperRepository.toggleFavorite(id);
        assertFalse(unFav);
        assertFalse(paperRepository.findById(id).orElseThrow().isFavorite());

        // Clean up
        paperRepository.delete(id);
    }

    @Test
    public void testSearch() throws SQLException {
        String uniqueTitle = "UniqueQuantumTransformer_" + System.currentTimeMillis();
        ResearchPaper paper = new ResearchPaper(
                uniqueTitle,
                "Quantum Physicist",
                "2024",
                "Nature Quantum",
                "Quantum Computing",
                "Novel quantum architecture."
        );

        int id = paperRepository.insert(paper);

        List<ResearchPaper> searchResults = paperRepository.search(
                "UniqueQuantum", "Quantum Computing", "2024", false
        );
        assertFalse(searchResults.isEmpty());
        assertTrue(searchResults.stream().anyMatch(p -> p.getId() == id));

        // Clean up
        paperRepository.delete(id);
    }
}
