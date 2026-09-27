package com.example.airesearchassistant;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.ArrayList;
import java.util.List;

public class Database {

    private static final String URL = "jdbc:sqlite:research.db";

    public static Connection connect() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    public static void initializeDatabase() {

        String sql = """
                CREATE TABLE IF NOT EXISTS papers (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    title TEXT NOT NULL,
                    authors TEXT,
                    abstract_text TEXT,
                    publication_year TEXT
                )
                """;

        try (
                Connection connection = connect();
                Statement statement = connection.createStatement()
        ) {
            statement.execute(sql);
            System.out.println("Database initialized successfully.");

        } catch (SQLException e) {
            System.out.println("Database initialization failed.");
            e.printStackTrace();
        }
    }

    public static void addPaper(ResearchPaper paper) throws SQLException {

        String sql = """
                INSERT INTO papers
                (title, authors, abstract_text, publication_year)
                VALUES (?, ?, ?, ?)
                """;

        try (
                Connection connection = connect();
                PreparedStatement statement = connection.prepareStatement(sql)
        ) {
            statement.setString(1, paper.getTitle());
            statement.setString(2, paper.getAuthors());
            statement.setString(3, paper.getAbstractText());
            statement.setString(4, paper.getPublicationYear());

            statement.executeUpdate();
        }
    }

    public static List<ResearchPaper> getAllPapers() throws SQLException {

        List<ResearchPaper> papers = new ArrayList<>();

        String sql = """
                SELECT id, title, authors, abstract_text, publication_year
                FROM papers
                ORDER BY id DESC
                """;

        try (
                Connection connection = connect();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(sql)
        ) {
            while (resultSet.next()) {

                ResearchPaper paper = new ResearchPaper(
                        resultSet.getInt("id"),
                        resultSet.getString("title"),
                        resultSet.getString("authors"),
                        resultSet.getString("abstract_text"),
                        resultSet.getString("publication_year")
                );

                papers.add(paper);
            }
        }

        return papers;
    }

    public static int getPaperCount() throws SQLException {

        String sql = "SELECT COUNT(*) FROM papers";

        try (
                Connection connection = connect();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(sql)
        ) {
            return resultSet.getInt(1);
        }
    }
}