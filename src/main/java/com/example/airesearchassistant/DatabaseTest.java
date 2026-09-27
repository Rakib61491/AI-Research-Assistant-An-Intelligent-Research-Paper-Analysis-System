package com.example.airesearchassistant;

import java.sql.SQLException;
import java.util.List;

public class DatabaseTest {

    public static void main(String[] args) {

        Database.initializeDatabase();

        ResearchPaper paper = new ResearchPaper(
                "Introduction to Machine Learning",
                "John Smith, Alice Brown",
                "This paper introduces fundamental concepts of machine learning.",
                "2024"
        );

        try {
            Database.addPaper(paper);
            System.out.println("Paper inserted successfully.");

            List<ResearchPaper> papers = Database.getAllPapers();

            for (ResearchPaper p : papers) {
                System.out.println("ID: " + p.getId());
                System.out.println("Title: " + p.getTitle());
                System.out.println("Authors: " + p.getAuthors());
                System.out.println("Year: " + p.getPublicationYear());
                System.out.println("-------------------------");
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}