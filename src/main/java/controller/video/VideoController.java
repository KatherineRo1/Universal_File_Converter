// VideoConverterController.java
package controller.video;

import converter.video.VideoConverter;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import model.ConversionHistory;
import model.DatabaseManager;
import util.FileUtils;
import util.Logger;

import java.io.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Controller for handling video conversion logic and UI interaction.
 */
public class VideoController {

    // Handles actual conversion operations via FFmpeg
    private final VideoConverter converter = new VideoConverter();

    // Used for writing conversion history to the database
    private final DatabaseManager dbManager = new DatabaseManager();

    // Holds the list of user-selected video files for conversion
    private final List<File> selectedFiles = new ArrayList<>();

    // FXML UI controls
    @FXML private ComboBox<String> formatComboBox;
    @FXML private StackPane dropZone;
    @FXML private Button convertButton;
    @FXML private Label statusLabel;
    @FXML private Button selectFileButton;
    @FXML private HBox previewContainer;
    @FXML private Label dropLabel;

    /**
     * Initializes UI elements and configures drag-and-drop events.
     */
    @FXML
    public void initialize() {
        // Populate format dropdown with supported output formats
        formatComboBox.getItems().addAll(converter.getSupportedFormats());

        selectFileButton.setOnAction(e -> onSelectFileClick());

        // Allow drag-and-drop of files
        dropZone.setOnDragOver(event -> {
            if (event.getGestureSource() != dropZone && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });

        // Handle drop action to load files into UI
        dropZone.setOnDragDropped(this::handleFileDrop);
    }

    /**
     * Accepts and processes files dropped into the UI.
     */
    private void handleFileDrop(DragEvent event) {
        Dragboard db = event.getDragboard();
        boolean success = false;
        if (db.hasFiles()) {
            selectedFiles.clear();
            previewContainer.getChildren().clear();

            for (File file : db.getFiles()) {
                String ext = FileUtils.getExtension(file);
                if (converter.getSupportedFormats().contains(ext)) {
                    selectedFiles.add(file);
                    previewContainer.getChildren().add(createPreview(ext));
                } else {
                    statusLabel.setText("Unsupported format: " + ext);
                    return;
                }
            }

            statusLabel.setText(selectedFiles.size() + " file(s) loaded.");
            convertButton.setDisable(false);
            dropLabel.setVisible(false);
            success = true;
        }
        event.setDropCompleted(success);
        event.consume();
    }

    /**
     * Starts the conversion of selected files in separate background threads.
     */
    @FXML
    public void onConvertClick() {
        String outputFormat = formatComboBox.getValue();
        if (outputFormat == null || selectedFiles.isEmpty()) {
            statusLabel.setText("Please select format.");
            return;
        }

        convertButton.setDisable(true);
        statusLabel.setText("Converting..."); // Show progress immediately

        StringBuilder result = new StringBuilder("Files converted:\n");
        AtomicInteger completedCount = new AtomicInteger(0);
        int totalFiles = selectedFiles.size();

        // Launch a task per file
        for (int i = 0; i < totalFiles; i++) {
            final File originalFile = selectedFiles.get(i);
            final int index = i + 1;

            Task<Boolean> task = new Task<>() {
                @Override
                protected Boolean call() {
                    // Generate unique output name
                    String baseName = originalFile.getName().replaceAll("\\.[^.]+$", "");
                    File outputFile = new File(originalFile.getParent(), baseName + "_converted_" + index + "." + outputFormat);

                    boolean success = converter.convert(originalFile, outputFormat, outputFile);

                    // Log conversion result to DB
                    dbManager.insertHistory(new ConversionHistory(
                            originalFile.getName(),
                            FileUtils.getExtension(originalFile),
                            outputFormat,
                            success ? "Success" : "Failed",
                            LocalDateTime.now()
                    ));

                    // Update status label once all are done
                    if (success) {
                        synchronized (result) {
                            result.append("• ").append(outputFile.getName()).append("\n");
                        }
                    }
                    return success;
                }
            };

            // Track successful completion
            task.setOnSucceeded(e -> {
                if (completedCount.incrementAndGet() == totalFiles) {
                    statusLabel.setText(result.toString());
                    convertButton.setDisable(false);
                }
            });

            // Handle task failures
            task.setOnFailed(e -> {
                statusLabel.setText("Conversion error: " + task.getException().getMessage());
                convertButton.setDisable(false);
            });

            new Thread(task).start();
        }
    }

    /**
     * Opens the file dialog and loads valid video files.
     */
    @FXML
    public void onSelectFileClick() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Video Files");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "Video Files", "*.mp4", "*.avi", "*.mkv", "*.mov", "*.webm"));

        List<File> files = fileChooser.showOpenMultipleDialog(dropZone.getScene().getWindow());

        if (files != null && !files.isEmpty()) {
            selectedFiles.clear();
            previewContainer.getChildren().clear();

            for (File file : files) {
                selectedFiles.add(file);
                String ext = FileUtils.getExtension(file);
                previewContainer.getChildren().add(createPreview(ext));
            }

            convertButton.setDisable(false);
            statusLabel.setText(selectedFiles.size() + " file(s) selected.");
            dropLabel.setVisible(false);
        }
    }

    /**
     * Generates a preview icon for a file based on its extension.
     */
    private HBox createPreview(String extension) {
        ImageView icon = new ImageView();
        icon.setFitHeight(50);
        icon.setFitWidth(50);
        icon.setPreserveRatio(true);

        try {
            Image img = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/icons/icon_" + extension + "_file.png")));
            icon.setImage(img);
        } catch (Exception e) {
            Logger.logError("Icon not found for extension: " + extension);
        }

        return new HBox(icon);
    }
}