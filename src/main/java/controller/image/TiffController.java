// TiffController.java — conversion of TIFF to PNG/JPG with full inline comments
package controller.image;

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

public class TiffController {

    // Converter instance responsible for transforming image files
    private final ImageConverter converter = new ImageConverter();

    // Handles conversion history database operations
    private final DatabaseManager dbManager = new DatabaseManager();

    // Image preview container in the UI (optional use)
    public ImageView tiffImageView;

    // List of TIFF files selected for conversion
    private List<File> selectedTiffFiles = new ArrayList<>();

    // UI controls injected from FXML
    @FXML private Button selectTiffFileButton;
    @FXML private Button convertTiffButton;
    @FXML private Label tiffStatusLabel;
    @FXML private ComboBox<String> tiffFormatComboBox;
    @FXML private StackPane tiffDropZone;
    @FXML private Label tiffDropLabel;
    @FXML private HBox tiffImagePreviewContainer;

    // Logger setup for writing logs to a file
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
     * Initializes the controller after FXML loading: sets up format selection,
     * drag-and-drop behavior, and file selection buttons.
     */
    @FXML
    public void initialize() {
        // Add target formats for conversion
        tiffFormatComboBox.getItems().addAll("png", "jpg");

        // Handle manual file selection via FileChooser
        selectTiffFileButton.setOnAction(e -> {
            try {
                FileChooser chooser = new FileChooser();
                chooser.getExtensionFilters().add(
                        new FileChooser.ExtensionFilter("TIFF Files", "*.tif", "*.tiff")
                );
                selectedTiffFiles = chooser.showOpenMultipleDialog(new Stage());
                if (selectedTiffFiles != null && !selectedTiffFiles.isEmpty()) {
                    if (!validateTiffFiles(selectedTiffFiles)) return;
                    convertTiffButton.setDisable(false);
                    tiffDropLabel.setVisible(false);
                    showMultipleIcons(selectedTiffFiles);
                }
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "File selection failed", ex);
                tiffStatusLabel.setText("File selection failed: " + ex.getMessage());
            }
        });

        // Handle drag-over event to indicate drop zone is ready
        tiffDropZone.setOnDragOver(e -> {
            if (e.getGestureSource() != tiffDropZone && e.getDragboard().hasFiles()) {
                e.acceptTransferModes(TransferMode.COPY);
            }
            e.consume();
        });

        // Handle actual drop of files
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
                        tiffStatusLabel.setText("Only TIFF files are allowed.");
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

        // Start conversion process when button is clicked
        convertTiffButton.setOnAction(e -> convertTiffFiles());
    }

    /**
     * Converts all selected TIFF files to the chosen format.
     * Builds a status message listing successful and failed conversions.
     */
    private void convertTiffFiles() {
        String format = tiffFormatComboBox.getValue();
        if (selectedTiffFiles == null || selectedTiffFiles.isEmpty() || format == null || format.isEmpty()) {
            tiffStatusLabel.setText("Please select TIFF files and format.");
            return;
        }

        StringBuilder result = new StringBuilder("Conversion results:\n");

        for (File file : selectedTiffFiles) {
            try {
                String baseName = file.getName().replaceAll("\\.[^.]+$", "");
                File outputFile = new File(file.getParent() + File.separator + baseName + "_converted." + format);

                // Perform the conversion using the image converter class
                converter.convert(file, format, outputFile, 1.0f);

                // Append success info to result message
                result.append("• ").append(outputFile.getName()).append("\n");

                // Save conversion to history
                dbManager.insertHistory(new ConversionHistory(
                        file.getName(), getFileExtension(file), format, "Success", LocalDateTime.now()
                ));
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "Conversion failed for: " + file.getName(), ex);
                result.append("× ").append(file.getName()).append(" (Failed: ").append(ex.getMessage()).append(")\n");
            }
        }

        // Show results to the user
        tiffStatusLabel.setText(result.toString());
    }

    /**
     * Ensures that all files in the list are valid TIFF files.
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
     * Displays icon previews for each selected file in the preview area.
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
     * Returns the extension of a file, in lowercase, without the dot.
     */
    private String getFileExtension(File file) {
        String name = file.getName();
        int dotIndex = name.lastIndexOf(".");
        return dotIndex > 0 ? name.substring(dotIndex + 1).toLowerCase() : "";
    }
}
