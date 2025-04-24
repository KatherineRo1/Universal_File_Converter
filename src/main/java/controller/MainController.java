package controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.File;
import java.io.IOException;
import java.util.logging.*;

/**
 * Controller for the main application layout.
 * Handles top-level navigation between categories (Images, Text, Audio, Video)
 * and dynamically loads corresponding views and subviews into the UI.
 */
public class MainController {

    public Button imagesButton;
    public Button textButton;
    public Button audioButton;
    public Button videoButton;
    public Button historyButton;
    public BorderPane mainLayout;
    // Main content area (center of the layout) where views are loaded
    @FXML
    private StackPane contentArea;

    // Sidebar container that holds subcategory buttons
    @FXML
    private VBox subcategoryMenu;

    // Internal content container used specifically in the ImageView for sub-views
    private StackPane imageContentArea;

    // Logger instance for tracking errors and important events
    private static final Logger logger = Logger.getLogger(MainController.class.getName());

    static {
        try {
            // Ensure "logs" directory exists
            File logDir = new File("logs");
            if (!logDir.exists()) {
                boolean created = logDir.mkdirs();
                if (!created) {
                    System.err.println("Failed to create logs directory.");
                }
            }

            // Configure logger to write logs to a file named "log.txt"
            Handler fileHandler = new FileHandler("logs/log.txt", 1024 * 1024, 1, true);
            fileHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(fileHandler);
            logger.setLevel(Level.ALL); // Log all levels from FINE to SEVERE
        } catch (IOException e) {
            System.err.println("Could not initialize file logging: " + e.getMessage());
        }
    }

    /**
     * Called automatically after the FXML file is loaded.
     * Initializes the controller by loading the default "Images" section.
     */
    @FXML
    public void initialize() {
        onImagesClick(); // Load the Images view by default
    }

    /**
     * Triggered when the user clicks the "Images" category button.
     * Loads the ImageView layout and sets up its subcategory buttons.
     */
    @FXML
    private void onImagesClick() {
        loadMainImageView();
        subcategoryMenu.getChildren().clear();

        // Add buttons for image format conversion subcategories
        addSubcategoryButton("PNG ⇄ JPG", () -> loadImageSubcategory("/fxml/image/png_jpg.fxml"));
        addSubcategoryButton("PNG/JPG → WEBP", () -> loadImageSubcategory("/fxml/image/webp.fxml"));
        addSubcategoryButton("TIFF → PNG/JPG", () -> loadImageSubcategory("/fxml/image/tiff.fxml"));

        loadImageSubcategory("/fxml/image/png_jpg.fxml");

        logger.info("Loaded image conversion view with subcategories.");
    }

    /**
     * Loads the main image conversion view (ImageView.fxml) and retrieves
     * its internal content area to load subcategory views later.
     */
    private void loadMainImageView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/image/ImageView.fxml"));
            Node view = loader.load();
            contentArea.getChildren().setAll(view);
            imageContentArea = (StackPane) view.lookup("#imageContentArea");

            logger.fine("ImageView.fxml loaded successfully.");

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to load ImageView.fxml", e);
            showError("Failed to load the image converter interface.");
        }
    }

    /**
     * Loads a specific subcategory FXML layout into the image content area.
     */
    private void loadImageSubcategory(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node subView = loader.load();

            if (imageContentArea != null) {
                imageContentArea.getChildren().setAll(subView);
                logger.fine("Loaded subcategory view: " + fxmlPath);
            } else {
                logger.warning("imageContentArea is null while loading subcategory: " + fxmlPath);
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to load subcategory FXML: " + fxmlPath, e);
            showError("Unable to load the selected subcategory.");
        }
    }
    /**
     * Loads a subcategory FXML view for text conversion into the center content area.
     */
    private void loadTextSubcategory(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Node subView = loader.load();
            contentArea.getChildren().setAll(subView);
            logger.fine("Loaded text subcategory view: " + fxmlPath);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to load text subcategory FXML: " + fxmlPath, e);
            showError("Unable to load the selected subcategory.");
        }
    }


    /**
     * Dynamically adds a button to the subcategory sidebar.
     */
    private void addSubcategoryButton(String title, Runnable onClick) {
        Button button = new Button(title);
        button.getStyleClass().add("subcategory-button");
        button.setMaxWidth(Double.MAX_VALUE); // Expand button to full width
        button.setOnAction(e -> onClick.run());
        subcategoryMenu.getChildren().add(button);
    }

    /**
     * Placeholder for loading the Text converter category.
     * To be implemented later.
     */
    @FXML
    private void onTextClick() {
        try {
            subcategoryMenu.getChildren().clear();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/text/txt.fxml"));
            Node view = loader.load();
            contentArea.getChildren().setAll(view);

            addSubcategoryButton("TXT → XLSX", () -> loadTextSubcategory("/fxml/text/txt.fxml"));
            addSubcategoryButton("CSV → XLSX", () -> loadTextSubcategory("/fxml/text/csv.fxml"));
            addSubcategoryButton("PNG → PDF", () -> loadTextSubcategory("/fxml/text/png_to_pdf.fxml"));


            logger.info("Loaded text converter view with subcategories.");

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to load txt.fxml", e);
            showError("Failed to load the text converter interface.");
        }
    }


    /**
     * Placeholder for loading the Audio converter category.
     * To be implemented later.
     */
    @FXML
    private void onAudioClick() {
        try {
            subcategoryMenu.getChildren().clear(); // Clear the side menu

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/audio/audio.fxml"));
            Node view = loader.load(); // Load the Audio Converter UI
            contentArea.getChildren().setAll(view); // Display it in the center pane

            logger.info("Audio converter view loaded.");
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to load audio.fxml", e);
            showError("Failed to load the audio converter interface.");
        }
    }


    /**
     * Placeholder for loading the Video converter category.
     */
    @FXML
    public void onVideoClick() {
        try {
            subcategoryMenu.getChildren().clear();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/video/video.fxml"));
            Node view = loader.load();
            contentArea.getChildren().setAll(view);

            logger.info("Video converter view loaded.");
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to load video.fxml", e);
            showError("Failed to load the video converter interface.");
        }
    }
    /**
     * Loads the conversion history view (history.fxml) into the main content area.
     */
    @FXML
    private void onHistoryClick() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/history.fxml"));
            Node historyView = loader.load();
            contentArea.getChildren().setAll(historyView);
            subcategoryMenu.getChildren().clear();

            logger.fine("History view loaded.");

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to load history.fxml", e);
            showError("Failed to load conversion history.");
        }
    }

    /**
     * Displays a popup error dialog with the provided message.
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("An error occurred");
        alert.setContentText(message);
        alert.showAndWait();
    }
}
