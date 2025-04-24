package controller.text;

import converter.text.CsvConverter;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import model.ConversionHistory;
import model.DatabaseManager;
import util.FileUtils;
import util.Logger;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Controller for converting CSV files to XLSX format.
 * Provides drag-and-drop and manual file selection,
 * and handles actual conversion via CsvConverter (auto delimiter detection).
 */
public class CsvToXlsxController {

    private final CsvConverter converter = new CsvConverter();                // Converter logic
    private final DatabaseManager dbManager = new DatabaseManager();         // Tracks conversion history
    private final List<File> selectedFiles = new ArrayList<>();              // Single file is used

    @FXML private Button selectFileButton;                                    // File chooser button
    @FXML private Button convertButton;                                       // Convert action trigger
    @FXML private Label statusLabel;                                          // Displays status messages
    @FXML private StackPane dropZone;                                         // Drag-and-drop area
    @FXML private Label dropLabel;                                            // Drop zone label
    @FXML private HBox previewContainer;                                      // Displays file icon

    /**
     * Initializes UI components and attaches event handlers for drag-and-drop and button clicks.
     */
    @FXML
    public void initialize() {
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
     * Handles dropped files. Accepts only a single .csv file.
     */
    private void handleFileDrop(DragEvent event) {
        Dragboard db = event.getDragboard();

        if (db.hasFiles()) {
            selectedFiles.clear();
            previewContainer.getChildren().clear();

            File file = db.getFiles().get(0); // Take only first file
            if (FileUtils.getExtension(file).equalsIgnoreCase("csv")) {
                selectedFiles.add(file);
                displayFileIcon();
                convertButton.setDisable(false);
                statusLabel.setText("1 file loaded.");
                dropLabel.setVisible(false);
            } else {
                statusLabel.setText("Only CSV files are allowed.");
            }
        }

        event.setDropCompleted(true);
        event.consume();
    }

    /**
     * Opens a file chooser to manually select a single CSV file.
     */
    private void openFileChooser() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select CSV File");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv")
        );

        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            selectedFiles.clear();
            selectedFiles.add(file);
            previewContainer.getChildren().clear();
            displayFileIcon();
            convertButton.setDisable(false);
            statusLabel.setText("1 file selected.");
            dropLabel.setVisible(false);
        }
    }

    /**
     * Always detects delimiter automatically and performs the CSV → XLSX conversion.
     * Also logs the result to the database and provides user feedback.
     */
    private void convertFile() {
        if (selectedFiles.isEmpty()) {
            statusLabel.setText("Please select CSV file first.");
            return;
        }

        try {
            File selectedFile = selectedFiles.get(0);
            String delimiter = CsvConverter.detectDelimiter(selectedFile);

            converter.setDelimiter(delimiter);
            File outputFile = FileUtils.generateOutputPath(selectedFile, "xlsx");
            converter.convertCsvToXlsx(selectedFile, outputFile);

            dbManager.insertHistory(new ConversionHistory(
                    selectedFile.getName(), "csv", "xlsx", "Success", LocalDateTime.now()
            ));

            statusLabel.setText("File converted: " + outputFile.getName());
        } catch (Exception e) {
            statusLabel.setText("Conversion failed: " + e.getMessage());
            Logger.logError("CSV conversion failed: " + e.getMessage());
        }
    }

    /**
     * Displays the icon of the selected CSV file.
     */
    private void displayFileIcon() {
        ImageView icon = new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/icons/icon_csv_file.png"))));
        icon.setFitWidth(50);
        icon.setFitHeight(50);
        icon.setPreserveRatio(true);
        previewContainer.getChildren().add(icon);
    }
}
