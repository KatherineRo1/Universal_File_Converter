/**
 * Entry point for the Universal File Converter application.
 * This class launches the JavaFX UI and loads the initial layout.
 */

package app;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Main application class that initializes and displays the main window.
 */
public class Main extends Application {

    /**
     * Starts the JavaFX application by loading the main layout and showing the UI.
     */
    @Override
    public void start(Stage primaryStage) {
        try {
            // Load the FXML layout for the main view
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainView.fxml"));
            BorderPane root = loader.load();

            // Create the scene and apply it to the stage
            Scene scene = new Scene(root, 1000, 700);
            primaryStage.setTitle("Universal File Converter");
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (IOException e) {
            System.err.println("Failed to load main UI: " + e.getMessage());
            e.printStackTrace();

            // Optional: show a user-friendly alert instead of just printing the error
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle("Startup Error");
            alert.setHeaderText("Unable to launch the application");
            alert.setContentText("The interface could not be loaded. Please check the application files.");
            alert.showAndWait();
        }
    }

    /**
     * Launches the JavaFX application.
     */
    public static void main(String[] args) {
        launch(args);
    }
}