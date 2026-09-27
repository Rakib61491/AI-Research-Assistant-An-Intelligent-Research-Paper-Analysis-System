package com.example.airesearchassistant.service;

import com.example.airesearchassistant.model.Collection;
import com.example.airesearchassistant.repository.CollectionRepository;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * CollectionService — Business logic layer for paper collections.
 * Never touches FXML or JavaFX controls (Hard Rule 4).
 */
public class CollectionService {

    private final CollectionRepository collectionRepository;

    public CollectionService() {
        this.collectionRepository = new CollectionRepository();
    }

    public CollectionService(CollectionRepository collectionRepository) {
        this.collectionRepository = collectionRepository;
    }

    public int createCollection(String name, String description) throws SQLException {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Collection name is required.");
        }
        Collection col = new Collection(name.trim(), description != null ? description.trim() : "");
        return collectionRepository.insert(col);
    }

    public boolean updateCollection(Collection collection) throws SQLException {
        if (collection == null || collection.getId() <= 0) {
            throw new IllegalArgumentException("Valid collection entity with ID is required.");
        }
        return collectionRepository.update(collection);
    }

    public boolean deleteCollection(int id) throws SQLException {
        return collectionRepository.delete(id);
    }

    public Optional<Collection> getCollectionById(int id) throws SQLException {
        return collectionRepository.findById(id);
    }

    public List<Collection> getAllCollections() {
        try {
            return collectionRepository.findAll();
        } catch (SQLException e) {
            System.err.println("Error fetching collections: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public boolean addPaperToCollection(int paperId, int collectionId) throws SQLException {
        return collectionRepository.addPaperToCollection(paperId, collectionId);
    }

    public boolean removePaperFromCollection(int paperId, int collectionId) throws SQLException {
        return collectionRepository.removePaperFromCollection(paperId, collectionId);
    }

    public List<com.example.airesearchassistant.model.ResearchPaper> getPapersInCollection(int collectionId) {
        try {
            return collectionRepository.findPapersInCollection(collectionId);
        } catch (java.sql.SQLException e) {
            System.err.println("Error fetching papers in collection: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public int getTotalCollectionCount() {
        try {
            return collectionRepository.countAll();
        } catch (SQLException e) {
            return 0;
        }
    }
}
