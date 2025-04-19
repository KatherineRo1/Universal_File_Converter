package controller.text;

import converter.text.CsvConverter;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import model.ConversionHistory;
import model.DatabaseManager;
import util.FileUtils;
import util.Logger;

import java.io.File;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Controller for converting CSV files to XLSX format.
 * Provides drag-and-drop and manual file selection,
 * delimiter configuration, and handles actual conversion via CsvConverter.
 */
public class CsvToXlsxController {

    private final CsvConverter converter = new CsvConverter();
    private final DatabaseManager dbManager = new DatabaseManager();
    private File selectedFile;

    @FXML private ComboBox<String> delimiterComboBox;
    @FXML private Button selectFileButton;
    @FXML private Button convertButton;
    @FXML private Label statusLabel;
    @FXML private StackPane dropZone;

    /**
     * Initializes UI components, populates the delimiter combo box,
     * and attaches event handlers for drag-and-drop and button clicks.
     */
    @FXML
    public void initialize() {
        // Populate the combo box with predefined delimiter options
        List<String> delimiters = Arrays.asList("Comma (,)", "Semicolon (;)", "Auto-detect");
        delimiterComboBox.getItems().addAll(delimiters);

        // Set up drag-and-drop events
        dropZone.setOnDragOver(this::handleDragOver);
        dropZone.setOnDragDropped(this::handleFileDrop);

        // Set up file selection and conversion button actions
        selectFileButton.setOnAction(e -> openFileChooser());
        convertButton.setOnAction(e -> convertFile());
    }

    /**
     * Allows dragging files over the drop zone.
     */
    private void handleDragOver(DragEvent event) {
        if (event.getGestureSource() != dropZone && event.getDragboard().hasFiles()) {
            event.acceptTransferModes(TransferMode.COPY);
        }
        event.consume();
    }

    /**
     * Handles dropped files. Accepts only .csv files, otherwise shows an error.
     */
    private void handleFileDrop(DragEvent event) {
        Dragboard db = event.getDragboard();
        boolean success = false;

        if (db.hasFiles()) {
            File file = db.getFiles().get(0);
            if (FileUtils.getExtension(file).equalsIgnoreCase("csv")) {
                selectedFile = file;
                convertButton.setDisable(false);
                statusLabel.setText("File loaded: " + file.getName());
                success = true;
            } else {
                statusLabel.setText("Please drop a .csv file");
            }
        }

        event.setDropCompleted(success);
        event.consume();
    }

    /**
     * Opens a file chooser to manually select a CSV file.
     */
    private void openFileChooser() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select CSV File");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv")
        );

        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            selectedFile = file;
            convertButton.setDisable(false);
            statusLabel.setText("File selected: " + file.getName());
        }
    }

    /**
     * Validates inputs, detects or resolves delimiter, and performs the CSV → XLSX conversion.
     * Also logs the result to the database and provides user feedback.
     */
    private void convertFile() {
        if (selectedFile == null) {
            statusLabel.setText("Please select a CSV file first.");
            return;
        }

        String selectedDelimiter = delimiterComboBox.getValue();
        if (selectedDelimiter == null) {
            statusLabel.setText("Please choose a delimiter.");
            return;
        }

        try {
            // Determine delimiter (auto or manual)
            String delimiter = selectedDelimiter.equals("Auto-detect")
                    ? CsvConverter.detectDelimiter(selectedFile)
                    : switch (selectedDelimiter) {
                case "Comma (,)" -> ",";
                case "Semicolon (;)" -> ";";
                default -> ","; // fallback
            };

            // Apply delimiter and execute conversion
            converter.setDelimiter(delimiter);
            File outputFile = FileUtils.generateOutputPath(selectedFile, "xlsx");
            converter.convertCsvToXlsx(selectedFile, outputFile);

            // Show confirmation and log history
            statusLabel.setText("File converted successfully:\n" + outputFile.getAbsolutePath());
            dbManager.insertHistory(new ConversionHistory(
                    selectedFile.getName(), "csv", "xlsx", "Success", LocalDateTime.now()
            ));
        } catch (Exception e) {
            statusLabel.setText("Conversion failed: " + e.getMessage());
            Logger.logError("CSV conversion failed: " + e.getMessage());
        }
    }
}
