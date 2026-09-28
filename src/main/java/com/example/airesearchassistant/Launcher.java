package com.example.airesearchassistant;

/**
 * Launcher — plain main class that bootstraps JavaFX.
 * Keeping this separate from MainApp avoids the "JavaFX runtime components are missing"
 * error when running via the javafx-maven-plugin on the classpath (no module-info.java).
 */
public class Launcher {

    public static void main(String[] args) {
        MainApp.main(args);
    }
}