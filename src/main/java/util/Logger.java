package util;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;

public class Logger {

    private static final String LOG_PATH = "logs/log.txt";

    /**
     * Logs error message with timestamp to log file.
     */
    public static void logError(String message) {
        writeToLog("ERROR", message);
    }

    /**
     * Logs informational message with timestamp to log file.
     */
    public static void logInfo(String message) {
        writeToLog("INFO", message);
    }

    /**
     * Generic method to log messages with a specific level.
     */
    private static void writeToLog(String level, String message) {
        try (FileWriter fw = new FileWriter(LOG_PATH, true)) {
            fw.write("[" + LocalDateTime.now() + "] " + level + ": " + message + "\n");
        } catch (IOException e) {
            System.err.println("Failed to write to log: " + e.getMessage());
        }
    }
}