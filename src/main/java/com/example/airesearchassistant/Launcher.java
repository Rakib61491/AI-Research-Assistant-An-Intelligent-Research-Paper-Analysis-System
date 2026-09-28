package com.example.airesearchassistant;

public class paperAnalyzingMaterial {
    private int paper_count;
    private String paper_name;
    private String author;
    private int year;

    public paperAnalyzingMaterial(int paper_count,
                                  String paper_name,
                                  String author,
                                  int year) {
        this.paper_count = paper_count;
        this.paper_name = paper_name;
        this.author = author;
        this.year = year;
    }

    public class paperAnalyzingMaterial {
        private int paper_count;
        private String paper_name;
        private String author;
        private int year;

        public paperAnalyzingMaterial(int paper_count,
                                      String paper_name,
                                      String author,
                                      int year) {
            this.paper_count = paper_count;
            this.paper_name = paper_name;
            this.author = author;
            this.year = year;
        }

        public int getPaper_count() {
            return paper_count;
        }

        public void setPaper_count(int paper_count) {
            this.paper_count = paper_count;
        }

        public String getPaper_name() {
            return paper_name;
        }

        public void setPaper_name(String paper_name) {
            this.paper_name = paper_name;
        }

        public String getAuthor() {
            return author;
        }

        public void setAuthor(String author) {
            this.author = author;
        }

        public int getYear() {
            return year;
        }

        public void setYear(int year) {
            this.year = year;
        }
    }
}