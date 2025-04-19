package controller.image;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import model.ConversionHistory;
import model.DatabaseManager;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.*;

/**
 * Controller for managing the conversion history view.
 * Handles initialization and logic for displaying and clearing past conversion records.
 */
public class HistoryController {

    // Manages interaction with the SQLite database
    private final DatabaseManager dbManager = new DatabaseManager();

    // UI components mapped via FXML
    @FXML private TableView<ConversionHistory> historyTable;
    @FXML private TableColumn<ConversionHistory, String> fileNameColumn;
    @FXML private TableColumn<ConversionHistory, String> sourceFormatColumn;
    @FXML private TableColumn<ConversionHistory, String> targetFormatColumn;
    @FXML private TableColumn<ConversionHistory, String> statusColumn;
    @FXML private TableColumn<ConversionHistory, String> timestampColumn;

    @FXML private Button refreshHistoryButton;
    @FXML private Button clearHistoryButton;

    // Logger for tracking actions and errors
    private static final Logger logger = Logger.getLogger(HistoryController.class.getName());

    static {
        try {
            // Ensure "logs" directory exists
            File logDir = new File("logs");
            if (!logDir.exists()) {
                boolean created = logDir.mkdirs();
                if (!created) {
                    System.err.println("⚠ Failed to create logs directory.");
                }
            }

            // Configure logger to write to log.txt in "logs" folder
            Handler fileHandler = new FileHandler("logs/log.txt", 1024 * 1024, 1, true);
            fileHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(fileHandler);
            logger.setLevel(Level.ALL);

        } catch (IOException e) {
            System.err.println("Could not initialize logger for HistoryController: " + e.getMessage());
        }
    }

    /**
     * Initializes the controller after FXML is loaded.
     * Sets up column mappings and button behavior.
     */
    @FXML
    public void initialize() {
        // Map table columns to fields in ConversionHistory model
        fileNameColumn.setCellValueFactory(new PropertyValueFactory<>("fileName"));
        sourceFormatColumn.setCellValueFactory(new PropertyValueFactory<>("sourceFormat"));
        targetFormatColumn.setCellValueFactory(new PropertyValueFactory<>("targetFormat"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        timestampColumn.setCellValueFactory(new PropertyValueFactory<>("timestamp"));

        // Load existing history
        loadHistoryFromDatabase();

        // Button: Refresh history from database
        refreshHistoryButton.setOnAction(e -> loadHistoryFromDatabase());

        // Button: Clear all history and reload table
        clearHistoryButton.setOnAction(e -> {
            try {
                dbManager.clearHistory();
                loadHistoryFromDatabase();

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("History");
                alert.setHeaderText(null);
                alert.setContentText("Conversion history cleared.");
                alert.showAndWait();

                logger.info("Conversion history cleared by user.");

            } catch (Exception ex) {
                logger.log(Level.SEVERE, "Failed to clear conversion history", ex);
                showError("Failed to clear conversion history.");
            }
        });
    }

    /**
     * Loads all conversion history entries from the database
     * and populates the TableView.
     */
    private void loadHistoryFromDatabase() {
        try {
            List<ConversionHistory> entries = dbManager.getAllHistory();
            historyTable.setItems(FXCollections.observableArrayList(entries));
            logger.fine("History loaded: " + entries.size() + " entries.");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to load conversion history", e);
            showError("Could not load conversion history.");
        }
    }

    /**
     * Shows a standard error dialog with a provided message.
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Something went wrong");
        alert.setContentText(message);
        alert.showAndWait();
    }
}
