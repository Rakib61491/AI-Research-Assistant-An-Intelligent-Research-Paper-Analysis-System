package com.example.airesearchassistant.repository;

import com.example.airesearchassistant.model.Collection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * CollectionRepository — Data access object for Collection entities and paper mappings.
 * All queries strictly use PreparedStatement (Hard Rule 5).
 */
public class CollectionRepository {

    private final DatabaseManager databaseManager;

    public CollectionRepository() {
        this.databaseManager = DatabaseManager.getInstance();
    }

    public CollectionRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public int insert(Collection collection) throws SQLException {
        String sql = "INSERT INTO collections (name, description) VALUES (?, ?);";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, collection.getName());
            ps.setString(2, collection.getDescription());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    collection.setId(id);
                    return id;
                }
            }
        }
        return -1;
    }

    public boolean update(Collection collection) throws SQLException {
        String sql = "UPDATE collections SET name = ?, description = ? WHERE id = ?;";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, collection.getName());
            ps.setString(2, collection.getDescription());
            ps.setInt(3, collection.getId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM collections WHERE id = ?;";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    public Optional<Collection> findById(int id) throws SQLException {
        String sql = "SELECT * FROM collections WHERE id = ?;";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToCollection(rs));
                }
            }
        }
        return Optional.empty();
    }

    public List<Collection> findAll() throws SQLException {
        List<Collection> list = new ArrayList<>();
        String sql = """
                SELECT c.*, COUNT(pc.paper_id) AS paper_count
                FROM collections c
                LEFT JOIN paper_collections pc ON c.id = pc.collection_id
                GROUP BY c.id
                ORDER BY c.name ASC;
                """;

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Collection col = mapResultSetToCollection(rs);
                col.setPaperCount(rs.getInt("paper_count"));
                list.add(col);
            }
        }
        return list;
    }

    public boolean addPaperToCollection(int paperId, int collectionId) throws SQLException {
        String sql = "INSERT OR IGNORE INTO paper_collections (paper_id, collection_id) VALUES (?, ?);";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, paperId);
            ps.setInt(2, collectionId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean removePaperFromCollection(int paperId, int collectionId) throws SQLException {
        String sql = "DELETE FROM paper_collections WHERE paper_id = ? AND collection_id = ?;";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, paperId);
            ps.setInt(2, collectionId);
            return ps.executeUpdate() > 0;
        }
    }

    public int countAll() throws SQLException {
        String sql = "SELECT COUNT(*) FROM collections;";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    private Collection mapResultSetToCollection(ResultSet rs) throws SQLException {
        Collection collection = new Collection();
        collection.setId(rs.getInt("id"));
        collection.setName(rs.getString("name"));
        collection.setDescription(rs.getString("description"));
        collection.setDateCreated(rs.getString("date_created"));
        return collection;
    }

    public List<com.example.airesearchassistant.model.ResearchPaper> findPapersInCollection(int collectionId) throws SQLException {
        List<com.example.airesearchassistant.model.ResearchPaper> papers = new ArrayList<>();
        String sql = """
                SELECT p.* FROM papers p
                JOIN paper_collections pc ON p.id = pc.paper_id
                WHERE pc.collection_id = ?
                ORDER BY p.title ASC;
                """;
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, collectionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    papers.add(mapResultSetToPaper(rs));
                }
            }
        }
        return papers;
    }

    private com.example.airesearchassistant.model.ResearchPaper mapResultSetToPaper(ResultSet rs) throws SQLException {
        com.example.airesearchassistant.model.ResearchPaper p = new com.example.airesearchassistant.model.ResearchPaper();
        p.setId(rs.getInt("id"));
        p.setTitle(rs.getString("title"));
        p.setAuthors(rs.getString("authors"));
        p.setPublicationYear(rs.getString("publication_year"));
        p.setJournal(rs.getString("journal"));
        p.setDoi(rs.getString("doi"));
        p.setAbstractText(rs.getString("abstract_text"));
        p.setMethodology(rs.getString("methodology"));
        p.setFindings(rs.getString("findings"));
        p.setKeywords(rs.getString("keywords"));
        p.setTopic(rs.getString("topic"));
        p.setFilePath(rs.getString("file_path"));
        p.setAiSummary(rs.getString("ai_summary"));
        p.setPersonalNotes(rs.getString("personal_notes"));
        p.setFavorite(rs.getInt("is_favorite") == 1);
        p.setDateAdded(rs.getString("date_added"));
        p.setDateAnalyzed(rs.getString("date_analyzed"));
        return p;
    }
}
