package com.example.airesearchassistant.repository;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * DatabaseManager — Manages SQLite database connection and schema bootstrap.
 * Stores database at System.getProperty("user.home") + "/.ai-research-assistant/research.db".
 * Ensures PRAGMA foreign_keys = ON on every connection.
 */
public final class DatabaseManager {

    private static DatabaseManager instance;
    private final String dbUrl;

    private DatabaseManager() {
        String userHome = System.getProperty("user.home");
        File dir = new File(userHome, ".ai-research-assistant");
        if (!dir.exists()) {
            boolean created = dir.mkdirs();
            if (created) {
                System.out.println("Created database directory: " + dir.getAbsolutePath());
            }
        }
        File dbFile = new File(dir, "research.db");
        this.dbUrl = "jdbc:sqlite:" + dbFile.getAbsolutePath();

        initializeSchema();
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    /**
     * Obtains a connection to SQLite with foreign keys enabled.
     *
     * @return active Connection
     * @throws SQLException on database error
     */
    public Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(dbUrl);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
        }
        return conn;
    }

    /**
     * Bootstraps database schema if tables do not exist.
     */
    private void initializeSchema() {
        String createPapersTable = """
                CREATE TABLE IF NOT EXISTS papers (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    title TEXT NOT NULL,
                    authors TEXT,
                    publication_year TEXT,
                    journal TEXT,
                    doi TEXT,
                    abstract_text TEXT,
                    methodology TEXT,
                    findings TEXT,
                    keywords TEXT,
                    topic TEXT,
                    file_path TEXT,
                    ai_summary TEXT,
                    personal_notes TEXT,
                    is_favorite INTEGER NOT NULL DEFAULT 0,
                    date_added TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    date_analyzed TEXT,
                    extracted_json TEXT
                );
                """;

        String createCollectionsTable = """
                CREATE TABLE IF NOT EXISTS collections (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL UNIQUE,
                    description TEXT,
                    date_created TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
                );
                """;

        String createPaperCollectionsTable = """
                CREATE TABLE IF NOT EXISTS paper_collections (
                    paper_id INTEGER NOT NULL,
                    collection_id INTEGER NOT NULL,
                    PRIMARY KEY (paper_id, collection_id),
                    FOREIGN KEY (paper_id) REFERENCES papers(id) ON DELETE CASCADE,
                    FOREIGN KEY (collection_id) REFERENCES collections(id) ON DELETE CASCADE
                );
                """;

        String createSettingsTable = """
                CREATE TABLE IF NOT EXISTS settings (
                    key TEXT PRIMARY KEY,
                    value TEXT
                );
                """;

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(createPapersTable);
            stmt.execute(createCollectionsTable);
            stmt.execute(createPaperCollectionsTable);
            stmt.execute(createSettingsTable);

            // Migration: ensure extracted_json column exists in legacy databases
            try {
                stmt.execute("ALTER TABLE papers ADD COLUMN extracted_json TEXT;");
            } catch (SQLException ignored) {
                // Column already exists
            }

            System.out.println("SQLite database schema initialized at " + dbUrl);
        } catch (SQLException e) {
            System.err.println("Failed to initialize database schema: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public String getDbUrl() {
        return dbUrl;
    }
}
