package controller.text;

import converter.text.TextConverter;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.input.*;
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
 * Controller for converting TXT files to XLSX format using a custom converter.
 * Supports drag-and-drop, manual file selection, and configurable delimiter options.
 */
public class TextConverterController {

    private final TextConverter converter = new TextConverter();              // Converter logic for TXT → XLSX
    private final DatabaseManager dbManager = new DatabaseManager();          // Handles conversion history saving
    private File selectedFile;                                                // Currently selected input file

    @FXML private ComboBox<String> delimiterComboBox;                         // Drop-down to choose delimiter
    @FXML private TextField customDelimiterField;                             // Field for custom delimiter input
    @FXML private Button selectFileButton;                                    // Button to open file chooser
    @FXML private Button convertButton;                                       // Button to start conversion
    @FXML private Label statusLabel;                                          // Label to display status messages
    @FXML private StackPane dropZone;                                         // Drag-and-drop file input area
    @FXML private Button txtToXlsxButton;                                     // Subcategory navigation (not active here)
    @FXML private Button csvToXlsxButton;
    @FXML private Button pdfToPngButton;

    /**
     * Initializes UI components and sets up all event listeners.
     */
    @FXML
    public void initialize() {
        // Populate delimiter options
        List<String> delimiters = Arrays.asList("Comma (,)", "Semicolon (;)", "Tab (\\t)", "Custom");
        delimiterComboBox.getItems().addAll(delimiters);

        // Show custom input field only if "Custom" is selected
        delimiterComboBox.setOnAction(event -> {
            String selected = delimiterComboBox.getValue();
            boolean isCustom = selected != null && selected.equals("Custom");
            customDelimiterField.setVisible(isCustom);
        });

        // Set up drag-and-drop zone events
        dropZone.setOnDragOver(this::handleDragOver);
        dropZone.setOnDragDropped(this::handleFileDrop);

        // Button actions
        selectFileButton.setOnAction(e -> openFileChooser());
        convertButton.setOnAction(e -> convertFile());
    }

    /**
     * Allows copy mode drag if files are present.
     */
    private void handleDragOver(DragEvent event) {
        if (event.getGestureSource() != dropZone && event.getDragboard().hasFiles()) {
            event.acceptTransferModes(TransferMode.COPY);
        }
        event.consume();
    }

    /**
     * Processes a dropped file and accepts only .txt files.
     */
    private void handleFileDrop(DragEvent event) {
        Dragboard db = event.getDragboard();
        boolean success = false;

        if (db.hasFiles()) {
            File file = db.getFiles().get(0);
            if (FileUtils.getExtension(file).equalsIgnoreCase("txt")) {
                selectedFile = file;
                convertButton.setDisable(false);
                statusLabel.setText("File loaded: " + file.getName());
                success = true;
            } else {
                statusLabel.setText("Please drop a .txt file");
            }
        }

        event.setDropCompleted(success);
        event.consume();
    }

    /**
     * Opens a file chooser dialog to manually select a .txt file.
     */
    private void openFileChooser() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select TXT File");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Text Files", "*.txt")
        );

        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            selectedFile = file;
            convertButton.setDisable(false);
            statusLabel.setText("File selected: " + file.getName());
        }
    }

    /**
     * Performs the TXT → XLSX conversion using internal logic.
     * Validates inputs and delimiter settings, then writes output to a file.
     */
    private void convertFile() {
        if (selectedFile == null) {
            statusLabel.setText("Please select a text file first.");
            return;
        }

        String selectedDelimiter = delimiterComboBox.getValue();
        if (selectedDelimiter == null) {
            statusLabel.setText("Please choose a delimiter.");
            return;
        }

        // Resolve delimiter based on dropdown selection
        String delimiter = switch (selectedDelimiter) {
            case "Comma (,)" -> ",";
            case "Semicolon (;)" -> ";";
            case "Tab (\\t)" -> "\t";
            case "Custom" -> customDelimiterField.getText();
            default -> null;
        };

        if (delimiter == null || delimiter.isEmpty()) {
            statusLabel.setText("Please enter a valid delimiter.");
            return;
        }

        try {
            // Prepare output file path and trigger conversion
            File outputFile = FileUtils.generateOutputPath(selectedFile, "xlsx");
            converter.setDelimiter(delimiter);
            converter.convertTxtToExcel(selectedFile, outputFile);

            statusLabel.setText("File converted successfully:\n" + outputFile.getAbsolutePath());

            dbManager.insertHistory(new ConversionHistory(
                    selectedFile.getName(),
                    "txt",
                    "xlsx",
                    "Success",
                    LocalDateTime.now()
            ));
        } catch (Exception e) {
            statusLabel.setText("Conversion failed: " + e.getMessage());
            Logger.logError("Text conversion failed: " + e.getMessage());
        }
    }
}
