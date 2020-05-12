package com.torutk.spectrum.view;

import com.torutk.spectrum.data.FileFinder;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ResourceBundle;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * Spectrum analyzer's data viewer.
 * Read a data file from Glowlink's Model 1030/8000, and view.
 */
public class SpectrumFileViewApp extends Application {
    private static final Logger logger = Logger.getLogger(SpectrumFileViewApp.class.getName());
    private static final String CSS_FILENAME = "SpectrumFileView.css";

    @Override
    public void start(Stage primaryStage) throws Exception {
        var resource = ResourceBundle.getBundle("com.torutk.spectrum.view.SpectrumFileViewApp");
        Parent root = FXMLLoader.load(
                SpectrumFileViewApp.class.getResource("SpectrumFileView.fxml"), resource
        );
        var scene = new Scene(root);
        setupStylesheet(scene);
        primaryStage.setScene(scene);
        primaryStage.setTitle(String.format("%s %s",
                resource.getString("spectrum.view.title"),
                resource.getString("spectrum.view.version")
        ));
        primaryStage.show();
        logger.info("Spectrum File Viewer started.");
    }

    /**
     *  Load css file and apply to the specified scene.
     *  At 1st, read from the classpath of this class.
     *  2nd, if exist in current directory, read from there.
     *
     * @param scene to be applied css file
     */
    private void setupStylesheet(Scene scene) {
        scene.getStylesheets().add(getClass().getResource(CSS_FILENAME).toExternalForm());
        try {
            Path cssFile = FileFinder.find(Paths.get(".."), CSS_FILENAME, 3);
            scene.getStylesheets().add(cssFile.toUri().toString());
        } catch (IOException ex) {
            logger.config("No custom css file found.");
        }
    }

    /**
     * Initialize log configuration.
     *
     * Logging configuration can be applied from file or class specified by system property.
     * If system property is not specified, then read file named 'logging.properties'
     * from the directory of this application class.
     */
    private static void setupLogging() {
        if (System.getProperty("java.util.logging.config.file") == null
                && System.getProperty("java.util.logging.config.class") == null) {
            try (InputStream resource = SpectrumFileViewApp.class.getResourceAsStream("logging.properties")) {
                if (resource != null) {
                    LogManager.getLogManager().readConfiguration(resource);
                }
            } catch (IOException ex) {
                logger.config("no logging.properties read from classpath.");
            }
        }
    }

    private static void logConfig() {
        logger.config("Java VM Version = " + Runtime.version());
        logger.config("OS = " + System.getProperty("os.name"));
        logger.config("Current directory = " + System.getProperty("user.dir"));
    }

    /**
     * Entry point of this application.
     *
     * @param args command-line options
     */
    public static void main(String[] args) {
        setupLogging();
        logConfig();
        launch(args);
    }
}
