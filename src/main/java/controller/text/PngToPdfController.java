package controller.text;

import converter.text.PngPdfConverter;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
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
import java.util.*;

/**
 * Controller for converting multiple PNG or JPEG images into a single PDF.
 * Allows drag-and-drop and manual selection, includes optional compression and rotation per file.
 */
public class PngToPdfController {

    private final DatabaseManager dbManager = new DatabaseManager();       // Handles writing to conversion history
    private final List<File> selectedFiles = new ArrayList<>();            // Stores image files selected for conversion
    private final Map<File, Integer> rotationMap = new HashMap<>();        // Stores rotation angle (in degrees) for each image

    @FXML private StackPane dropZone;               // Drop area for drag-and-drop
    @FXML private HBox imagePreviewContainer;       // Displays file icons + rotation buttons
    @FXML private Button selectFileButton;          // Manual file selection
    @FXML private Button convertButton;             // Starts conversion to PDF
    @FXML private Label statusLabel;                // Displays messages for the user
    @FXML private CheckBox compressCheckbox;        // Toggles image compression
    @FXML private Label dropLabel;                  // Instructional label inside drop zone

    /**
     * Initializes drag-and-drop zone and button handlers.
     */
    @FXML
    public void initialize() {
        dropZone.setOnDragOver(this::handleDragOver);
        dropZone.setOnDragDropped(this::handleFileDrop);
        selectFileButton.setOnAction(e -> openFileChooser());
        convertButton.setOnAction(e -> convertToPdf());
    }

    /**
     * Accepts dragged files if present.
     */
    private void handleDragOver(DragEvent event) {
        if (event.getGestureSource() != dropZone && event.getDragboard().hasFiles()) {
            event.acceptTransferModes(TransferMode.COPY);
        }
        event.consume();
    }

    /**
     * Handles files dropped into the drop zone.
     * Filters only PNG and JPG images, updates UI preview.
     */
    private void handleFileDrop(DragEvent event) {
        Dragboard db = event.getDragboard();
        if (db.hasFiles()) {
            processFiles(db.getFiles());
        }
        event.setDropCompleted(true);
        event.consume();
    }

    /**
     * Opens a file chooser to allow manual selection of PNG/JPG files.
     */
    private void openFileChooser() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select PNG or JPG Files");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );
        List<File> files = fileChooser.showOpenMultipleDialog(null);
        if (files != null && !files.isEmpty()) {
            processFiles(files);
        }
    }

    /**
     * Filters and processes valid image files.
     * Adds each to the preview container with a rotation button.
     */
    private void processFiles(List<File> files) {
        selectedFiles.clear();
        rotationMap.clear();
        imagePreviewContainer.getChildren().clear();

        for (File file : files) {
            String ext = FileUtils.getExtension(file).toLowerCase();
            if (ext.equals("png") || ext.equals("jpg") || ext.equals("jpeg")) {
                selectedFiles.add(file);
                rotationMap.put(file, 0); // Default rotation angle
                imagePreviewContainer.getChildren().add(createPreviewWithRotateButton(file, ext));
            }
        }

        // Update UI based on whether valid files were added
        boolean hasFiles = !selectedFiles.isEmpty();
        convertButton.setDisable(!hasFiles);
        dropLabel.setVisible(!hasFiles);
        statusLabel.setText(hasFiles
                ? "Files loaded: " + selectedFiles.size()
                : "Please drop a PNG or JPG file.");
    }

    /**
     * Creates a vertical preview node showing an icon and rotation button.
     */
    private VBox createPreviewWithRotateButton(File file, String ext) {
        ImageView icon = new ImageView();
        icon.setFitWidth(50);
        icon.setFitHeight(50);
        icon.setPreserveRatio(true);

        String iconPath = ext.equals("jpg") || ext.equals("jpeg")
                ? "/icons/icon_jpg_file.png"
                : "/icons/icon_png_file.png";
        icon.setImage(new Image(iconPath));

        // Rotate button for rotating the image 90° clockwise each click
        Button rotateBtn = new Button("↻");
        rotateBtn.setOnAction(e -> {
            int current = rotationMap.get(file);
            int newRotation = (current + 90) % 360;
            rotationMap.put(file, newRotation);
            rotateBtn.setText(newRotation + "°");
        });
        rotateBtn.setMinWidth(40);
        rotateBtn.setMaxWidth(40);

        VBox box = new VBox(5, icon, rotateBtn);
        box.setStyle("-fx-alignment: center");
        return box;
    }

    /**
     * Starts the conversion process using the selected files and their respective rotation states.
     * Supports optional compression.
     */
    private void convertToPdf() {
        if (selectedFiles.isEmpty()) {
            statusLabel.setText("Please select image files first.");
            return;
        }

        boolean compress = compressCheckbox.isSelected();

        try {
            File outputFile = FileUtils.generateOutputPath(selectedFiles.get(0), "pdf");
            PngPdfConverter.convertMultipleImagesToPdf(selectedFiles, rotationMap, outputFile, compress);

            statusLabel.setText("PDF created successfully: " + outputFile.getAbsolutePath());

            dbManager.insertHistory(new ConversionHistory(
                    outputFile.getName(), "png/jpg", "pdf", "Success", LocalDateTime.now()
            ));
        } catch (Exception e) {
            statusLabel.setText("Conversion failed: " + e.getMessage());
            Logger.logError("Image to PDF conversion failed: " + e.getMessage());
        }
    }
}
