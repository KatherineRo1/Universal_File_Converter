package controller.audio;

import javafx.concurrent.Task;
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
import model.ConversionHistory;
import model.DatabaseManager;
import util.FileUtils;
import util.Logger;

import java.io.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Controller for converting supported audio files (WAV, FLAC, AAC, OGG) to MP3.
 * Supports drag-and-drop, manual file selection, and FFmpeg-based conversion.
 */
public class AudioController {

    // Database manager instance used to record conversion history
    private final DatabaseManager dbManager = new DatabaseManager();

    // List to hold selected audio files for conversion
    private final List<File> selectedFiles = new ArrayList<>();

    // UI controls injected from FXML
    @FXML private StackPane dropZone;
    @FXML private Label dropLabel;
    @FXML private HBox previewContainer;
    @FXML private Button selectFileButton;
    @FXML private Button convertButton;
    @FXML private Label statusLabel;

    // Set of allowed audio file extensions
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("wav", "flac", "aac", "ogg");

    /**
     * Called automatically when the FXML view is loaded.
     * Sets up drag-and-drop and button click handlers.
     */
    @FXML
    public void initialize() {
        dropZone.setOnDragOver(this::handleDragOver);
        dropZone.setOnDragDropped(this::handleFileDrop);

        selectFileButton.setOnAction(e -> openFileChooser());
        convertButton.setOnAction(e -> convertToMp3());
    }

    /**
     * Handles drag-over event to allow file copy transfer if audio files are present.
     */
    private void handleDragOver(DragEvent event) {
        if (event.getGestureSource() != dropZone && event.getDragboard().hasFiles()) {
            event.acceptTransferModes(TransferMode.COPY);
        }
        event.consume();
    }

    /**
     * Handles dropped files and processes them if valid audio types.
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
     * Opens a file chooser for the user to select audio files manually.
     * Filters to only show supported extensions.
     */
    private void openFileChooser() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Audio Files");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Audio Files", "*.wav", "*.flac", "*.aac", "*.ogg")
        );
        List<File> files = fileChooser.showOpenMultipleDialog(null);
        if (files != null && !files.isEmpty()) {
            // Filter only supported extensions from manually selected files
            List<File> validFiles = files.stream()
                    .filter(f -> SUPPORTED_EXTENSIONS.contains(FileUtils.getExtension(f).toLowerCase()))
                    .toList();
            if (!validFiles.isEmpty()) {
                processFiles(validFiles);
            } else {
                statusLabel.setText("Please select supported audio files.");
            }
        }
    }

    /**
     * Validates selected files, displays their icons, and enables conversion.
     */
    private void processFiles(List<File> files) {
        selectedFiles.clear();
        previewContainer.getChildren().clear();

        boolean anyValid = false;

        for (File file : files) {
            String ext = FileUtils.getExtension(file).toLowerCase();
            if (SUPPORTED_EXTENSIONS.contains(ext)) {
                anyValid = true;
                selectedFiles.add(file);

                // Choose the appropriate icon for each file extension
                String iconPath = switch (ext) {
                    case "wav" -> "/icons/icon_wav_file.png";
                    case "flac" -> "/icons/icon_flac_file.png";
                    case "ogg" -> "/icons/icon_ogg_file.png";
                    case "aac" -> "/icons/icon_aac_file.png";
                    default -> null;
                };

                // Display icon in the preview container
                if (iconPath != null) {
                    try {
                        Image image = new Image(Objects.requireNonNull(getClass().getResource(iconPath)).toExternalForm());
                        ImageView icon = new ImageView(image);
                        icon.setFitWidth(50);
                        icon.setFitHeight(50);
                        icon.setPreserveRatio(true);
                        previewContainer.getChildren().add(icon);
                    } catch (Exception e) {
                        statusLabel.setText("Error loading icon: " + e.getMessage());
                    }
                }
            }
        }

        dropLabel.setVisible(selectedFiles.isEmpty());
        convertButton.setDisable(selectedFiles.isEmpty());

        if (anyValid) {
            statusLabel.setText("Loaded " + selectedFiles.size() + " files.");
        } else {
            statusLabel.setText("Please select supported audio files.");
        }
    }

    /**
     * Converts selected audio files to MP3 using FFmpeg and logs each conversion.
     */
    private void convertToMp3() {
        if (selectedFiles.isEmpty()) {
            statusLabel.setText("No audio files selected.");
            return;
        }

        // Build conversion summary
        StringBuilder result = new StringBuilder("Files converted:\n");

        for (File inputFile : selectedFiles) {
            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    String baseName = inputFile.getName().replaceAll("\\.[^.]+$", "");
                    int index = selectedFiles.indexOf(inputFile) + 1;
                    File outputFile = new File(inputFile.getParent(), baseName + "_converted_" + index + ".mp3");

                    // FFmpeg command to convert input file to MP3
                    List<String> command = Arrays.asList(
                            "ffmpeg", "-y",
                            "-i", inputFile.getAbsolutePath(),
                            "-codec:a", "libmp3lame",
                            outputFile.getAbsolutePath()
                    );

                    ProcessBuilder builder = new ProcessBuilder(command);
                    builder.redirectErrorStream(true);
                    Process process = builder.start();

                    // Log FFmpeg output line by line
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            Logger.logError("[FFmpeg] " + line);
                        }
                    }

                    int exitCode = process.waitFor();
                    if (exitCode != 0) {
                        throw new IOException("FFmpeg exited with code: " + exitCode);
                    }

                    // Store success result in the history DB
                    dbManager.insertHistory(new ConversionHistory(
                            inputFile.getName(),
                            FileUtils.getExtension(inputFile),
                            "mp3",
                            "Success",
                            LocalDateTime.now()
                    ));

                    // Append the name of the converted file to the result message
                    synchronized (result) {
                        result.append("• ").append(outputFile.getName()).append("\n");
                    }

                    return null;
                }
            };

            // Update status label after conversion or on failure
            task.setOnSucceeded(e -> statusLabel.setText(result.toString()));
            task.setOnFailed(e -> statusLabel.setText("Failed: " + task.getException().getMessage()));

            new Thread(task).start();
        }
    }
}