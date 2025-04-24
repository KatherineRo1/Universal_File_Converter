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

/**
 * Controller for handling PNG ⇄ JPG conversions.
 * Supports drag-and-drop, format selection, quality options, and status display.
 */
public class PngJpgController {

    // Image converter utility class
    private final ImageConverter converter = new ImageConverter();

    // Database manager for storing history entries
    private final DatabaseManager dbManager = new DatabaseManager();
    public ImageView imageView;

    // Selected files to convert
    private List<File> selectedFiles = new ArrayList<>();

    // UI components
    @FXML private ComboBox<String> formatComboBox;
    @FXML private ComboBox<String> qualityComboBox;
    @FXML private Button selectFileButton;
    @FXML private Button convertButton;
    @FXML private Label statusLabel;
    @FXML private StackPane dropZone;
    @FXML private Label dropLabel;
    @FXML private HBox imagePreviewContainer;

    // Logger for error tracking
    private static final Logger logger = Logger.getLogger(PngJpgController.class.getName());

    static {
        try {
            Handler fileHandler = new FileHandler("logs/log.txt", 1024 * 1024, 1, true);
            fileHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(fileHandler);
            logger.setLevel(Level.ALL);
        } catch (Exception e) {
            System.err.println("Failed to initialize logger in PngJpgController: " + e.getMessage());
        }
    }

    /**
     * Initializes the controller after the FXML is loaded.
     * Sets up dropdowns, drag-and-drop behavior, and button actions.
     */
    @FXML
    public void initialize() {
        formatComboBox.getItems().addAll("jpg", "png");
        qualityComboBox.getItems().addAll("Best (100%)", "High (90%)", "Medium (70%)", "Low (50%)");

        convertButton.setDisable(true);
        qualityComboBox.setVisible(false);
        qualityComboBox.setDisable(true);

        // Enable JPG quality options only when converting to JPG
        formatComboBox.setOnAction(e -> {
            String format = formatComboBox.getValue();
            boolean isJpg = "jpg".equalsIgnoreCase(format);
            qualityComboBox.setVisible(isJpg);
            qualityComboBox.setDisable(!isJpg);
        });

        // File chooser button
        selectFileButton.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Select PNG or JPG files");

            chooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
            );

            selectedFiles = chooser.showOpenMultipleDialog(new Stage());
            if (selectedFiles != null && !selectedFiles.isEmpty()) {
                convertButton.setDisable(false);
                dropLabel.setVisible(false);
                showMultipleIcons(selectedFiles);
            }
        });


        // Handle drag events for file drop zone
        dropZone.setOnDragOver(e -> {
            if (e.getGestureSource() != dropZone && e.getDragboard().hasFiles()) {
                e.acceptTransferModes(TransferMode.COPY);
            }
            e.consume();
        });

        dropZone.setOnDragDropped((DragEvent e) -> {
            Dragboard db = e.getDragboard();
            if (db.hasFiles()) {
                List<File> validFiles = new ArrayList<>();
                for (File file : db.getFiles()) {
                    String ext = getFileExtension(file);
                    if (ext.equals("png") || ext.equals("jpg") || ext.equals("jpeg")) {
                        validFiles.add(file);
                    }
                }

                if (!validFiles.isEmpty()) {
                    selectedFiles = validFiles;
                    convertButton.setDisable(false);
                    dropLabel.setVisible(false);
                    showMultipleIcons(selectedFiles);
                    statusLabel.setText("Loaded " + selectedFiles.size() + " image(s).");
                    e.setDropCompleted(true);
                } else {
                    statusLabel.setText("Please drop PNG or JPG image files.");
                    e.setDropCompleted(false);
                }
            }
            else {
                e.setDropCompleted(false);
            }
            e.consume();
        });

        convertButton.setOnAction(e -> convertSelectedFiles());
    }

    /**
     * Performs image conversion on the selected files
     * and updates history and UI status accordingly.
     */
    private void convertSelectedFiles() {
        String format = formatComboBox.getValue();
        float quality = 1.0f;
        int qualityInt = 100;
        String suffix;

        // Determine suffix and quality based on selected format
        if ("jpg".equalsIgnoreCase(format)) {
            String selected = qualityComboBox.getValue();
            if (selected == null) selected = "Best (100%)";
            switch (selected) {
                case "High (90%)" -> { quality = 0.9f; qualityInt = 90; }
                case "Medium (70%)" -> { quality = 0.7f; qualityInt = 70; }
                case "Low (50%)" -> { quality = 0.5f; qualityInt = 50; }
            }
            suffix = "_compressed" + qualityInt;
        } else {
            suffix = "_converted";
        }

        StringBuilder result = new StringBuilder("Files converted:\n");

        for (File file : selectedFiles) {
            try {
                String baseName = file.getName().replaceAll("\\.[^.]+$", "");
                File outputFile = new File(file.getParent() + File.separator + baseName + suffix + "." + format);
                converter.convert(file, format, outputFile, quality);

                result.append("• ").append(outputFile.getName()).append("\n");

                dbManager.insertHistory(new ConversionHistory(
                        file.getName(),
                        getFileExtension(file),
                        format,
                        "Success",
                        LocalDateTime.now()
                ));
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "Conversion failed for file: " + file.getName(), ex);
                result.append("× ").append(file.getName())
                        .append(" (Failed: ").append(ex.getMessage()).append(")\n");
            }
        }

        statusLabel.setText(result.toString());
    }


    /**
     * Displays icons for each selected file in the preview area.
     */
    private void showMultipleIcons(List<File> files) {
        imagePreviewContainer.getChildren().clear();
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
                imagePreviewContainer.getChildren().add(icon);
            }
        }
    }

    /**
     * Retrieves the file extension (without dot) from a file name.
     */
    private String getFileExtension(File file) {
        String name = file.getName();
        int dotIndex = name.lastIndexOf(".");
        return dotIndex > 0 ? name.substring(dotIndex + 1).toLowerCase() : "";
    }

}
