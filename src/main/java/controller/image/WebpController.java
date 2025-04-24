package controller.image;

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
     * Initializes UI and event bindings.
     */
    @FXML
    public void initialize() {
        // File chooser button handler
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

        // Drag-and-drop support
        webpDropZone.setOnDragOver(e -> {
            if (e.getGestureSource() != webpDropZone && e.getDragboard().hasFiles()) {
                e.acceptTransferModes(TransferMode.COPY);
            }
            e.consume();
        });

        webpDropZone.setOnDragDropped((DragEvent e) -> {
            try {
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
                        convertWebpButton.setDisable(false);
                        webpDropLabel.setVisible(false);
                        showMultipleIcons(validFiles);
                        webpStatusLabel.setText("Loaded " + validFiles.size() + " image(s).\n");
                        e.setDropCompleted(true);
                    } else {
                        webpStatusLabel.setText("Please drop PNG or JPG image files.");
                        e.setDropCompleted(false);
                    }
                } else {
                    e.setDropCompleted(false);
                }
            } catch (Exception ex) {
                logger.log(Level.WARNING, "Drag & drop failed", ex);
                webpStatusLabel.setText("Failed to process dropped files.");
            }
            e.consume();
        });

        // Start conversion on click
        convertWebpButton.setOnAction(e -> convertToWebpWithFFmpeg(selectedFiles));
    }

    /**
     * Launches a background task to convert a list of files to WEBP using FFmpeg.
     */
    private void convertToWebpWithFFmpeg(List<File> inputFiles) {
        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                StringBuilder result = new StringBuilder("Files converted:\n");
                for (File inputFile : inputFiles) {
                    if (!checkImageSizeWithinWebpLimits(inputFile)) continue;

                    String baseName = inputFile.getName().replaceAll("\\.[^.]+$", "");
                    File outputFile = new File(inputFile.getParent(), baseName + ".webp");

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
                        result.append("× ").append(inputFile.getName()).append(" (Timed out)\n");
                    } else if (exitCode != 0) {
                        result.append("× ").append(inputFile.getName()).append(" (Failed: Exit code ").append(exitCode).append(")\n");
                    } else {
                        result.append("• ").append(outputFile.getName()).append("\n");

                        dbManager.insertHistory(new ConversionHistory(
                                inputFile.getName(), getFileExtension(inputFile), "webp", "Success", LocalDateTime.now()
                        ));
                    }
                }
                return result.toString();
            }
        };

        task.setOnSucceeded(e -> {
            webpStatusLabel.textProperty().unbind();
            webpStatusLabel.setText(task.getValue());
        });

        task.setOnFailed(e -> {
            webpStatusLabel.textProperty().unbind();
            webpStatusLabel.setText("Failed: " + task.getException().getMessage());
        });

        webpStatusLabel.textProperty().bind(task.messageProperty());
        new Thread(task).start();
    }

    /**
     * Validates that the image size does not exceed WebP maximum dimensions.
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
     * Displays preview icons in the UI based on file extension.
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
     * Returns the file extension in lowercase.
     */
    private String getFileExtension(File file) {
        String name = file.getName();
        int dotIndex = name.lastIndexOf(".");
        return dotIndex > 0 ? name.substring(dotIndex + 1).toLowerCase() : "";
    }
}
