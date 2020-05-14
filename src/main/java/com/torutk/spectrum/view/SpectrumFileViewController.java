package com.torutk.spectrum.view;

import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Cursor;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Logger;

public class SpectrumFileViewController implements Initializable {
    private static final Logger logger = Logger.getLogger(SpectrumFileViewController.class.getName());
    private FileChooser fileChooser = new FileChooser();
    private DirectoryChooser directoryChooer = new DirectoryChooser();
    private ResourceBundle resources;
    private double chartDragPointX;
    private final SpectrumFileViewModel model = SpectrumFileViewModel.INSTANCE;

    @FXML private LineChart<Float, Float> chart;
    @FXML private NumberAxis xAxis;
    @FXML private NumberAxis yAxis;
    @FXML private TextField startFrequencyField;
    @FXML private TextField stopFrequencyField;
    @FXML private TextField referenceLevelField;
    @FXML private TextField scaleField;
    @FXML private Button updateButton;

    @FXML
    private void open(ActionEvent ev) {

    }

    @FXML
    private void export(ActionEvent ev) {

    }

    @FXML
    private void showHelp(ActionEvent ev) {

    }

    @FXML
    private void updateSettings(ActionEvent event) {

    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        resources = resourceBundle;
        fileChooser.setTitle(resources.getString("spectrum.view.filechooser.title"));
        fileChooser.initialDirectoryProperty().bindBidirectional(model.lastOpenDirectoryProperty());
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Spectrum Files", "*.dat")
        );
        directoryChooer.setTitle(resources.getString("spectrum.view.directorychooser.title"));
        directoryChooer.initialDirectoryProperty().bindBidirectional(model.lastOpenDirectoryProperty());
        initializeChart();
        initializeRightPane();
    }

    private void initializeChart() {
        xAxis.lowerBoundProperty().bind(model.startFrequencyProperty());
        xAxis.upperBoundProperty().bind(model.stopFrequencyProperty());
        xAxis.tickUnitProperty().bind(Bindings.divide(
                Bindings.subtract(model.stopFrequencyProperty(), model.startFrequencyProperty()), 10
        ));
        yAxis.lowerBoundProperty().bind(Bindings.subtract(
                model.referenceLevelProperty(), Bindings.multiply(model.scaleProperty(), 10)
        ));
        yAxis.upperBoundProperty().bind(model.referenceLevelProperty());
        yAxis.tickUnitProperty().bind(model.scaleProperty());
        chart.dataProperty().bind(model.spectrumSeriesProperty());
        chart.setOnMousePressed(event -> {
            chart.setCursor(Cursor.CLOSED_HAND);
            chartDragPointX = event.getX();
        });
        chart.setOnMouseDragged(event -> {
            double mouseMoveX = event.getX() - chartDragPointX;
            if (Math.abs(mouseMoveX) < 5) { // decimate number of refresh chart
                return;
            }
            double diff = (model.getStopFrequency() - model.getStartFrequency()) * mouseMoveX / xAxis.getWidth();
            diff = Math.floor(diff * 10) / 10;
            model.setStartFrequency(model.getStartFrequency() - diff);
            model.setStopFrequency(model.getStopFrequency() - diff);
            chartDragPointX = event.getX();
            refreshSettingFields();
        });
        chart.setOnMouseReleased(event -> chart.setCursor(Cursor.DEFAULT));
    }

    private void initializeRightPane() {

    }

    private void refreshSettingFields() {
        startFrequencyField.setText(String.format("%10.4f", model.getStartFrequency()));
        stopFrequencyField.setText(String.format("%10.4f", model.getStopFrequency()));
        referenceLevelField.setText(String.format("%5.1f", model.getReferenceLevel()));
        scaleField.setText(String.format("%4.1f", model.getScale()));
        updateButton.setDisable(true);
    }
}
