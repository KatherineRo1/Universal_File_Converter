package controller;

import converter.image.ImageConverter;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Dragboard;
import javafx.scene.input.DragEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import model.ConversionHistory;
import model.DatabaseManager;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.*;

/**
 * Controller for handling TIFF to PNG/JPG conversions.
 * Supports drag-and-drop, file picking, format selection,
 * and preview display of selected files.
 */
public class TiffController {

    // Handles conversion logic
    private final ImageConverter converter = new ImageConverter();

    // Manages database insertions for history
    private final DatabaseManager dbManager = new DatabaseManager();
    public ImageView tiffImageView;

    // List of selected TIFF files for conversion
    private List<File> selectedTiffFiles = new ArrayList<>();

    // UI components from FXML
    @FXML private Button selectTiffFileButton;
    @FXML private Button convertTiffButton;
    @FXML private Label tiffStatusLabel;
    @FXML private ComboBox<String> tiffFormatComboBox;
    @FXML private StackPane tiffDropZone;
    @FXML private Label tiffDropLabel;
    @FXML private HBox tiffImagePreviewContainer;

    // Logger for tracking errors and events
    private static final Logger logger = Logger.getLogger(TiffController.class.getName());

    static {
        try {
            Handler fileHandler = new FileHandler("logs/log.txt", 1024 * 1024, 1, true);
            fileHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(fileHandler);
            logger.setLevel(Level.ALL);
        } catch (Exception e) {
            System.err.println("Failed to initialize logger in TiffController: " + e.getMessage());
        }
    }

    /**
     * Initializes the controller, sets up drag-and-drop, buttons, and format options.
     */
    @FXML
    public void initialize() {
        tiffFormatComboBox.getItems().addAll("png", "jpg");

        // File picker action
        selectTiffFileButton.setOnAction(e -> {
            try {
                FileChooser chooser = new FileChooser();
                chooser.getExtensionFilters().add(
                        new FileChooser.ExtensionFilter("TIFF Files", "*.tif", "*.tiff")
                );
                selectedTiffFiles = chooser.showOpenMultipleDialog(new Stage());
                if (selectedTiffFiles != null && !selectedTiffFiles.isEmpty()) {
                    if (validateTiffFiles(selectedTiffFiles)) return;
                    convertTiffButton.setDisable(false);
                    tiffDropLabel.setVisible(false);
                    showMultipleIcons(selectedTiffFiles);
                }
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "File selection failed", ex);
                tiffStatusLabel.setText("File selection failed: " + ex.getMessage());
            }
        });

        // Drag-over behavior
        tiffDropZone.setOnDragOver(e -> {
            if (e.getGestureSource() != tiffDropZone && e.getDragboard().hasFiles()) {
                e.acceptTransferModes(TransferMode.COPY);
            }
            e.consume();
        });

        // Handle file drop
        tiffDropZone.setOnDragDropped((DragEvent e) -> {
            try {
                Dragboard db = e.getDragboard();
                if (db.hasFiles()) {
                    selectedTiffFiles = db.getFiles();
                    if (validateTiffFiles(selectedTiffFiles)) {
                        convertTiffButton.setDisable(false);
                        tiffDropLabel.setVisible(false);
                        showMultipleIcons(selectedTiffFiles);
                        e.setDropCompleted(true);
                    } else {
                        e.setDropCompleted(false);
                    }

                } else {
                    e.setDropCompleted(false);
                }
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Drag & drop failed", ex);
                tiffStatusLabel.setText("Failed to process dropped files.");
            }
            e.consume();
        });

        convertTiffButton.setOnAction(e -> convertTiffFiles());
    }

    /**
     * Converts selected TIFF files into the chosen format and updates history.
     */
    private void convertTiffFiles() {
        String format = tiffFormatComboBox.getValue();
        if (selectedTiffFiles == null || selectedTiffFiles.isEmpty() || format == null || format.isEmpty()) {
            tiffStatusLabel.setText("Please select TIFF files and format.");
            return;
        }

        for (File file : selectedTiffFiles) {
            try {
                String baseName = file.getName().replaceAll("\\.[^.]+$", "");
                File outputFile = new File(file.getParent() + File.separator + baseName + "_converted." + format);
                converter.convert(file, format, outputFile, 1.0f);
                tiffStatusLabel.setText("Saved: " + outputFile.getName());

                dbManager.insertHistory(new ConversionHistory(
                        file.getName(), getFileExtension(file), format, "Success", LocalDateTime.now()
                ));
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "Conversion failed for: " + file.getName(), ex);
                tiffStatusLabel.setText("Failed: " + ex.getMessage());
            }
        }
    }

    /**
     * Validates that all selected files are valid TIFF files.
     */
    private boolean validateTiffFiles(List<File> files) {
        for (File file : files) {
            String ext = getFileExtension(file).toLowerCase();
            if (!(ext.equals("tif") || ext.equals("tiff"))) {
                tiffStatusLabel.setText("Only TIFF files are allowed.");
                return false;
            }
        }
        return true;
    }

    /**
     * Displays file type icons in the preview panel for the selected TIFF files.
     */
    private void showMultipleIcons(List<File> files) {
        tiffImagePreviewContainer.getChildren().clear();
        for (File file : files) {
            String ext = getFileExtension(file);
            String iconPath = switch (ext) {
                case "jpg", "jpeg" -> "/icons/icon_jpg_file.png";
                case "png" -> "/icons/icon_png_file.png";
                case "tif", "tiff" -> "/icons/icon_tiff_file.png";
                default -> null;
            };
            if (iconPath != null) {
                ImageView icon = new ImageView(new Image(Objects.requireNonNull(getClass().getResource(iconPath)).toExternalForm()));
                icon.setFitWidth(50);
                icon.setFitHeight(50);
                icon.setPreserveRatio(true);
                tiffImagePreviewContainer.getChildren().add(icon);
            }
        }
    }

    /**
     * Returns the file extension (without the dot) in lowercase.
     */
    private String getFileExtension(File file) {
        String name = file.getName();
        int dotIndex = name.lastIndexOf(".");
        return dotIndex > 0 ? name.substring(dotIndex + 1).toLowerCase() : "";
    }
}
