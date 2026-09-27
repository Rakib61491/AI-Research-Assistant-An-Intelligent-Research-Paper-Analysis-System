package com.example.airesearchassistant.repository;

import com.example.airesearchassistant.model.ResearchPaper;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * PaperRepository — Data access object for ResearchPaper entities.
 * All queries strictly use PreparedStatement (Hard Rule 5).
 */
public class PaperRepository {

    private final DatabaseManager databaseManager;

    public PaperRepository() {
        this.databaseManager = DatabaseManager.getInstance();
    }

    public PaperRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    /**
     * Inserts a new ResearchPaper into the database.
     *
     * @param paper entity to insert
     * @return generated ID
     * @throws SQLException on database failure
     */
    public int insert(ResearchPaper paper) throws SQLException {
        String sql = """
                INSERT INTO papers (
                    title, authors, publication_year, journal, doi,
                    abstract_text, methodology, findings, keywords, topic,
                    file_path, ai_summary, personal_notes, is_favorite, date_analyzed
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
                """;

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, paper.getTitle());
            ps.setString(2, paper.getAuthors());
            ps.setString(3, paper.getPublicationYear());
            ps.setString(4, paper.getJournal());
            ps.setString(5, paper.getDoi());
            ps.setString(6, paper.getAbstractText());
            ps.setString(7, paper.getMethodology());
            ps.setString(8, paper.getFindings());
            ps.setString(9, paper.getKeywords());
            ps.setString(10, paper.getTopic());
            ps.setString(11, paper.getFilePath());
            ps.setString(12, paper.getAiSummary());
            ps.setString(13, paper.getPersonalNotes());
            ps.setInt(14, paper.isFavorite() ? 1 : 0);
            ps.setString(15, paper.getDateAnalyzed());

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    paper.setId(id);
                    return id;
                }
            }
        }
        return -1;
    }

    /**
     * Updates an existing ResearchPaper in the database.
     */
    public boolean update(ResearchPaper paper) throws SQLException {
        String sql = """
                UPDATE papers SET
                    title = ?, authors = ?, publication_year = ?, journal = ?, doi = ?,
                    abstract_text = ?, methodology = ?, findings = ?, keywords = ?, topic = ?,
                    file_path = ?, ai_summary = ?, personal_notes = ?, is_favorite = ?, date_analyzed = ?
                WHERE id = ?;
                """;

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, paper.getTitle());
            ps.setString(2, paper.getAuthors());
            ps.setString(3, paper.getPublicationYear());
            ps.setString(4, paper.getJournal());
            ps.setString(5, paper.getDoi());
            ps.setString(6, paper.getAbstractText());
            ps.setString(7, paper.getMethodology());
            ps.setString(8, paper.getFindings());
            ps.setString(9, paper.getKeywords());
            ps.setString(10, paper.getTopic());
            ps.setString(11, paper.getFilePath());
            ps.setString(12, paper.getAiSummary());
            ps.setString(13, paper.getPersonalNotes());
            ps.setInt(14, paper.isFavorite() ? 1 : 0);
            ps.setString(15, paper.getDateAnalyzed());
            ps.setInt(16, paper.getId());

            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Deletes a paper by ID.
     */
    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM papers WHERE id = ?;";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Finds a paper by ID.
     */
    public Optional<ResearchPaper> findById(int id) throws SQLException {
        String sql = "SELECT * FROM papers WHERE id = ?;";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToPaper(rs));
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Returns all papers ordered by date added descending.
     */
    public List<ResearchPaper> findAll() throws SQLException {
        List<ResearchPaper> list = new ArrayList<>();
        String sql = "SELECT * FROM papers ORDER BY id DESC;";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapResultSetToPaper(rs));
            }
        }
        return list;
    }

    /**
     * Searches papers with dynamic filters. All criteria bound safely via PreparedStatement.
     */
    public List<ResearchPaper> search(String query, String topic, String year, Boolean favoriteOnly) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT * FROM papers WHERE 1=1 ");
        List<Object> params = new ArrayList<>();

        if (query != null && !query.trim().isEmpty()) {
            sql.append(" AND (title LIKE ? OR authors LIKE ? OR keywords LIKE ? OR abstract_text LIKE ?)");
            String wildcard = "%" + query.trim() + "%";
            params.add(wildcard);
            params.add(wildcard);
            params.add(wildcard);
            params.add(wildcard);
        }

        if (topic != null && !topic.trim().isEmpty() && !"All Topics".equalsIgnoreCase(topic)) {
            sql.append(" AND topic = ?");
            params.add(topic.trim());
        }

        if (year != null && !year.trim().isEmpty() && !"All Years".equalsIgnoreCase(year)) {
            sql.append(" AND publication_year = ?");
            params.add(year.trim());
        }

        if (favoriteOnly != null && favoriteOnly) {
            sql.append(" AND is_favorite = 1");
        }

        sql.append(" ORDER BY id DESC;");

        List<ResearchPaper> list = new ArrayList<>();
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToPaper(rs));
                }
            }
        }
        return list;
    }

    /**
     * Toggles the favorite status for a paper.
     */
    public boolean toggleFavorite(int id) throws SQLException {
        String querySql = "SELECT is_favorite FROM papers WHERE id = ?;";
        String updateSql = "UPDATE papers SET is_favorite = ? WHERE id = ?;";

        try (Connection conn = databaseManager.getConnection()) {
            int currentState = 0;
            try (PreparedStatement psQuery = conn.prepareStatement(querySql)) {
                psQuery.setInt(1, id);
                try (ResultSet rs = psQuery.executeQuery()) {
                    if (rs.next()) {
                        currentState = rs.getInt("is_favorite");
                    } else {
                        return false;
                    }
                }
            }

            int newState = (currentState == 1) ? 0 : 1;
            try (PreparedStatement psUpdate = conn.prepareStatement(updateSql)) {
                psUpdate.setInt(1, newState);
                psUpdate.setInt(2, id);
                psUpdate.executeUpdate();
            }
            return newState == 1;
        }
    }

    public int countAll() throws SQLException {
        return executeCount("SELECT COUNT(*) FROM papers;");
    }

    public int countAnalyzed() throws SQLException {
        return executeCount("SELECT COUNT(*) FROM papers WHERE date_analyzed IS NOT NULL AND date_analyzed != '';");
    }

    public int countFavorites() throws SQLException {
        return executeCount("SELECT COUNT(*) FROM papers WHERE is_favorite = 1;");
    }

    public List<String> getDistinctTopics() throws SQLException {
        List<String> topics = new ArrayList<>();
        String sql = "SELECT DISTINCT topic FROM papers WHERE topic IS NOT NULL AND topic != '' ORDER BY topic ASC;";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                topics.add(rs.getString("topic"));
            }
        }
        return topics;
    }

    public List<String> getDistinctYears() throws SQLException {
        List<String> years = new ArrayList<>();
        String sql = "SELECT DISTINCT publication_year FROM papers WHERE publication_year IS NOT NULL AND publication_year != '' ORDER BY publication_year DESC;";
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                years.add(rs.getString("publication_year"));
            }
        }
        return years;
    }

    private int executeCount(String sql) throws SQLException {
        try (Connection conn = databaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    private ResearchPaper mapResultSetToPaper(ResultSet rs) throws SQLException {
        ResearchPaper paper = new ResearchPaper();
        paper.setId(rs.getInt("id"));
        paper.setTitle(rs.getString("title"));
        paper.setAuthors(rs.getString("authors"));
        paper.setPublicationYear(rs.getString("publication_year"));
        paper.setJournal(rs.getString("journal"));
        paper.setDoi(rs.getString("doi"));
        paper.setAbstractText(rs.getString("abstract_text"));
        paper.setMethodology(rs.getString("methodology"));
        paper.setFindings(rs.getString("findings"));
        paper.setKeywords(rs.getString("keywords"));
        paper.setTopic(rs.getString("topic"));
        paper.setFilePath(rs.getString("file_path"));
        paper.setAiSummary(rs.getString("ai_summary"));
        paper.setPersonalNotes(rs.getString("personal_notes"));
        paper.setFavorite(rs.getInt("is_favorite") == 1);
        paper.setDateAdded(rs.getString("date_added"));
        paper.setDateAnalyzed(rs.getString("date_analyzed"));
        return paper;
    }
}
