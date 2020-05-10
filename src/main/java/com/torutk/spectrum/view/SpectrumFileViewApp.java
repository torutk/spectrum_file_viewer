package com.torutk.spectrum.view;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class SpectrumFileViewApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = FXMLLoader.load(SpectrumFileViewApp.class.getResource("SpectrumFileView.fxml"));
        var scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}
