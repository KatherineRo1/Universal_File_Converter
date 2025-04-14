package controller;

import converter.image.ImageConverter;
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
import javafx.stage.Stage;
import model.ConversionHistory;
import model.DatabaseManager;

import java.io.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.logging.*;

/**
 * Controller for converting PNG/JPG images to WEBP format using FFmpeg.
 * Supports drag & drop, file chooser, real-time status, and conversion history.
 */
public class WebpController {

    private final DatabaseManager dbManager = new DatabaseManager();
    public ImageView webpImageView;
    private List<File> selectedFiles = new ArrayList<>();

    @FXML private Button selectWebpFileButton;
    @FXML private Button convertWebpButton;
    @FXML private Label webpStatusLabel;
    @FXML private StackPane webpDropZone;
    @FXML private Label webpDropLabel;
    @FXML private HBox webpImagePreviewContainer;

    private static final Logger logger = Logger.getLogger(WebpController.class.getName());

    static {
        try {
            Handler fileHandler = new FileHandler("logs/log.txt", 1024 * 1024, 1, true);
            fileHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(fileHandler);
            logger.setLevel(Level.ALL);
        } catch (IOException e) {
            System.err.println("Logger init failed: " + e.getMessage());
        }
    }

    /**
     * Sets up the UI components and event handlers after the FXML is loaded.
     */
    @FXML
    public void initialize() {
        // File chooser button
        selectWebpFileButton.setOnAction(e -> {
            try {
                FileChooser chooser = new FileChooser();
                chooser.getExtensionFilters().add(
                        new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
                );
                selectedFiles = chooser.showOpenMultipleDialog(new Stage());
                if (selectedFiles != null && !selectedFiles.isEmpty()) {
                    convertWebpButton.setDisable(false);
                    webpDropLabel.setVisible(false);
                    showMultipleIcons(selectedFiles);
                }
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "File selection failed", ex);
                webpStatusLabel.setText("File selection failed: " + ex.getMessage());
            }
        });

        // Drag-over for drop zone
        webpDropZone.setOnDragOver(e -> {
            if (e.getGestureSource() != webpDropZone && e.getDragboard().hasFiles()) {
                e.acceptTransferModes(TransferMode.COPY);
            }
            e.consume();
        });

        // Handle drop
        webpDropZone.setOnDragDropped((DragEvent e) -> {
            try {
                Dragboard db = e.getDragboard();
                if (db.hasFiles()) {
                    selectedFiles = db.getFiles();
                    convertWebpButton.setDisable(false);
                    webpDropLabel.setVisible(false);
                    showMultipleIcons(selectedFiles);
                    e.setDropCompleted(true);
                } else {
                    e.setDropCompleted(false);
                }
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Drag & drop failed", ex);
                webpStatusLabel.setText("Failed to process dropped files.");
            }
            e.consume();
        });

        convertWebpButton.setOnAction(e -> handleWebpConversion());
    }

    /**
     * Starts WEBP conversion for selected files.
     */
    private void handleWebpConversion() {
        for (File file : selectedFiles) {
            try {
                if (!checkImageSizeWithinWebpLimits(file)) continue;

                String baseName = file.getName().replaceAll("\\.[^.]+$", "");
                File outputFile = new File(file.getParent() + File.separator + baseName + ".webp");

                convertToWebpWithFFmpeg(file, outputFile);

                dbManager.insertHistory(new ConversionHistory(
                        file.getName(), getFileExtension(file), "webp", "Success", LocalDateTime.now()
                ));
            } catch (Exception ex) {
                logger.log(Level.SEVERE, "Conversion failed for file: " + file.getName(), ex);
                webpStatusLabel.setText("Failed: " + ex.getMessage());
            }
        }
    }

    /**
     * Uses FFmpeg via ProcessBuilder to convert input image to WEBP format in a background thread.
     */
    private void convertToWebpWithFFmpeg(File inputFile, File outputFile) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                updateMessage("Converting...");

                String[] command = {
                        "ffmpeg", "-y",
                        "-i", inputFile.getAbsolutePath(),
                        "-c:v", "libwebp",
                        outputFile.getAbsolutePath()
                };

                ProcessBuilder builder = new ProcessBuilder(command);
                builder.redirectErrorStream(true);
                Process process = builder.start();

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        logger.fine("[FFmpeg] " + line);
                    }
                }

                boolean finished = process.waitFor(15, TimeUnit.SECONDS);
                int exitCode = process.exitValue();

                if (!finished) {
                    process.destroyForcibly();
                    throw new Exception("FFmpeg timed out (exit code: " + exitCode + ")");
                }

                if (exitCode != 0) {
                    throw new Exception("FFmpeg failed (exit code: " + exitCode + ")");
                }

                return null;
            }
        };

        task.setOnSucceeded(e -> {
            webpStatusLabel.textProperty().unbind();
            webpStatusLabel.setText("Saved: " + outputFile.getName());
        });

        task.setOnFailed(e -> {
            webpStatusLabel.textProperty().unbind();
            webpStatusLabel.setText("Failed: " + task.getException().getMessage());
        });

        webpStatusLabel.textProperty().bind(task.messageProperty());
        new Thread(task).start();
    }

    /**
     * Checks image size to ensure it meets WebP format constraints.
     */
    private boolean checkImageSizeWithinWebpLimits(File file) {
        try {
            Image image = new Image(file.toURI().toString());
            if (image.getWidth() > 16383 || image.getHeight() > 16383) {
                webpStatusLabel.setText("Image too large for WebP (max 16383x16383)");
                return false;
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to read image: " + file.getName(), e);
            webpStatusLabel.setText("Failed to read image: " + e.getMessage());
            return false;
        }
        return true;
    }

    /**
     * Displays icons for selected image files in the preview container.
     */
    private void showMultipleIcons(List<File> files) {
        webpImagePreviewContainer.getChildren().clear();
        for (File file : files) {
            String ext = getFileExtension(file);
            String iconPath = switch (ext) {
                case "jpg", "jpeg" -> "/icons/icon_jpg_file.png";
                case "png" -> "/icons/icon_png_file.png";
                default -> null;
            };
            if (iconPath != null) {
                ImageView icon = new ImageView(new Image(Objects.requireNonNull(getClass().getResource(iconPath)).toExternalForm()));
                icon.setFitWidth(50);
                icon.setFitHeight(50);
                icon.setPreserveRatio(true);
                webpImagePreviewContainer.getChildren().add(icon);
            }
        }
    }

    /**
     * Returns the file extension of the given file, in lowercase.
     */
    private String getFileExtension(File file) {
        String name = file.getName();
        int dotIndex = name.lastIndexOf(".");
        return dotIndex > 0 ? name.substring(dotIndex + 1).toLowerCase() : "";
    }
}
