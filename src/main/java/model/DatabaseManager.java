package model;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.*;

/**
 * Manages connection to the SQLite database and handles saving/retrieving history records.
 */
public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:converter_history.db";

    // Logger for logging DB operations and errors
    private static final Logger logger = Logger.getLogger(DatabaseManager.class.getName());

    static {
        try {
            Handler fileHandler = new FileHandler("logs/log.txt", 1024 * 1024, 1, true);
            fileHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(fileHandler);
            logger.setLevel(Level.ALL);
        } catch (IOException e) {
            System.err.println("Failed to initialize logging for DatabaseManager: " + e.getMessage());
        }
    }

    /**
     * Initializes the database and creates the history table if it does not exist.
     */
    public DatabaseManager() {
        createTableIfNeeded();
    }

    /**
     * Creates the conversion_history table if it is not already present in the database.
     */
    private void createTableIfNeeded() {
        String sql = "CREATE TABLE IF NOT EXISTS conversion_history (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "file_name TEXT," +
                "source_format TEXT," +
                "target_format TEXT," +
                "status TEXT," +
                "timestamp TEXT)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            logger.info("Verified or created conversion_history table.");
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error creating database table", e);
        }
    }

    /**
     * Inserts a conversion history record into the database.
     */
    public void insertHistory(ConversionHistory history) {
        String sql = "INSERT INTO conversion_history " +
                "(file_name, source_format, target_format, status, timestamp) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, history.fileName());
            pstmt.setString(2, history.sourceFormat());
            pstmt.setString(3, history.targetFormat());
            pstmt.setString(4, history.status());
            pstmt.setString(5, history.timestamp().toString());
            pstmt.executeUpdate();
            logger.info("Inserted history: " + history);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error inserting history record", e);
        }
    }

    /**
     * Retrieves all conversion history records from the database.
     */
    public List<ConversionHistory> getAllHistory() {
        List<ConversionHistory> list = new ArrayList<>();
        String sql = "SELECT file_name, source_format, target_format, status, timestamp " +
                "FROM conversion_history ORDER BY timestamp DESC";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String fileName = rs.getString("file_name");
                String sourceFormat = rs.getString("source_format");
                String targetFormat = rs.getString("target_format");
                String status = rs.getString("status");
                LocalDateTime timestamp = LocalDateTime.parse(rs.getString("timestamp"));

                list.add(new ConversionHistory(fileName, sourceFormat, targetFormat, status, timestamp));
            }

            logger.info("Loaded " + list.size() + " history entries from database.");

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error retrieving history", e);
        }

        return list;
    }

    /**
     * Deletes all history records from the database.
     */
    public void clearHistory() {
        String sql = "DELETE FROM conversion_history";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            int deleted = stmt.executeUpdate(sql);
            logger.info("Cleared conversion history. Rows deleted: " + deleted);
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Error clearing history", e);
        }
    }
}