package controller.text;

import converter.text.TextConverter;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import model.ConversionHistory;
import model.DatabaseManager;
import util.FileUtils;
import util.Logger;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Controller for converting TXT files to XLSX format using a custom converter.
 * Supports drag-and-drop, manual file selection, and configurable delimiter options.
 */
public class TxtToXlsxController {

    private final TextConverter converter = new TextConverter();              // Converter logic for TXT → XLSX
    private final DatabaseManager dbManager = new DatabaseManager();          // Handles conversion history saving

    private final List<File> selectedFiles = new ArrayList<>();               // Currently selected input files
    public VBox txtToXlsxPane;

    @FXML private ComboBox<String> delimiterComboBox;                         // Drop-down to choose delimiter
    @FXML private TextField customDelimiterField;                             // Field for custom delimiter input
    @FXML private Button selectFileButton;                                    // Button to open file chooser
    @FXML private Button convertButton;                                       // Button to start conversion
    @FXML private Label statusLabel;                                          // Label to display status messages
    @FXML private StackPane dropZone;                                         // Drag-and-drop file input area
    @FXML private Label dropLabel;                                            // Label inside drop zone
    @FXML private HBox fileIconsContainer;                                    // Container for file icons
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
            customDelimiterField.setVisible("Custom".equals(selected));
        });

        // Set up drag-and-drop zone events
        dropZone.setOnDragOver(this::handleDragOver);
        dropZone.setOnDragDropped(this::handleFileDrop);

        // Button actions
        selectFileButton.setOnAction(e -> openFileChooser());
        convertButton.setOnAction(e -> convertFiles());
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
     * Processes dropped files, accepting only the first .txt file.
     * Only one file is allowed.
     */
    private void handleFileDrop(DragEvent event) {
        Dragboard db = event.getDragboard();

        if (db.hasFiles()) {
            selectedFiles.clear();
            fileIconsContainer.getChildren().clear();

            File file = db.getFiles().get(0); // Accept only the first file
            if ("txt".equalsIgnoreCase(FileUtils.getExtension(file))) {
                selectedFiles.add(file);
                addFileIcon();
                convertButton.setDisable(false);
                dropLabel.setVisible(false);
                statusLabel.setText("1 file loaded");
            } else {
                statusLabel.setText("Only .txt files are accepted");
            }
        }

        event.setDropCompleted(true);
        event.consume();
    }

    /**
     * Opens a file chooser dialog to manually select a .txt file.
     * Multiple selection disabled.
     */
    private void openFileChooser() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select TXT File");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Text Files", "*.txt")
        );

        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            selectedFiles.clear();
            fileIconsContainer.getChildren().clear();
            selectedFiles.add(file);
            addFileIcon();
            convertButton.setDisable(false);
            dropLabel.setVisible(false);
            statusLabel.setText("1 file selected");
        }
    }

    /**
     * Performs the TXT → XLSX conversion using internal logic.
     */
    private void convertFiles() {
        if (selectedFiles.isEmpty()) {
            statusLabel.setText("Please select text file first.");
            return;
        }

        String selectedDelimiter = delimiterComboBox.getValue();
        if (selectedDelimiter == null) {
            statusLabel.setText("Please choose a delimiter.");
            return;
        }

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
            File inputFile = selectedFiles.get(0);
            File outputFile = FileUtils.generateOutputPath(inputFile, "xlsx");
            converter.setDelimiter(delimiter);
            converter.convertTxtToExcel(inputFile, outputFile);

            dbManager.insertHistory(new ConversionHistory(
                    inputFile.getName(), "txt", "xlsx", "Success", LocalDateTime.now()
            ));

            statusLabel.setText("File converted: " + outputFile.getName());
        } catch (Exception e) {
            statusLabel.setText("Conversion failed: " + e.getMessage());
            Logger.logError("Text conversion failed: " + e.getMessage());
        }
    }

    /**
     * Adds an icon to the display for the selected TXT file.
     */
    private void addFileIcon() {
        ImageView icon = new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream("/icons/icon_txt_file.png"))));
        icon.setFitWidth(50);
        icon.setFitHeight(50);
        icon.setPreserveRatio(true);
        fileIconsContainer.getChildren().add(icon);
    }
}
