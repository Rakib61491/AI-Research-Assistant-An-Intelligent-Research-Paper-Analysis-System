package com.example.airesearchassistant.util;

/**
 * ValidationUtil — Input validation helpers for forms and data integrity.
 */
public final class ValidationUtil {

    private ValidationUtil() {
    }

    /**
     * Validates that title is not null and not blank.
     */
    public static boolean isValidTitle(String title) {
        return title != null && !title.trim().isEmpty();
    }

    /**
     * Validates publication year: if provided, must be a 4-digit number between 1800 and 2099.
     */
    public static boolean isValidYear(String year) {
        if (year == null || year.trim().isEmpty()) {
            return true; // Optional field
        }
        String clean = year.trim();
        if (!clean.matches("^\\d{4}$")) {
            return false;
        }
        int val = Integer.parseInt(clean);
        return val >= 1800 && val <= 2099;
    }

    /**
     * Validates that a string is present (not null and not blank).
     */
    public static boolean isRequired(String input) {
        return input != null && !input.trim().isEmpty();
    }

    /**
     * Validates DOI format: if provided, must start with 10. followed by registrant and suffix.
     * Optional/empty values return true.
     */
    public static boolean isValidDoi(String doi) {
        if (doi == null || doi.trim().isEmpty()) {
            return true; // Optional field
        }
        String clean = doi.trim();
        return clean.matches("^10\\.\\d{4,9}/[-._;()/:A-Za-z0-9]+$");
    }

    /**
     * Validates integer input: must be numeric and strictly positive (> 0).
     */
    public static boolean isPositiveInteger(String input) {
        if (input == null || input.trim().isEmpty()) {
            return false;
        }
        try {
            int val = Integer.parseInt(input.trim());
            return val > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
