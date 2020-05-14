package com.torutk.spectrum.view;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.XYChart;

import java.io.File;
import java.util.logging.Logger;

/**
 * View Model of Spectrum Viewer Application.
 *
 * <ul>
 * <li>Singleton implementation by enum.</li>
 * <li>Holds all data to be displayed.</li>
 * <li>Mediate between view and domain data(Model).</li>
 * </ul>
 *
 */
enum SpectrumFileViewModel {
    INSTANCE;
    private static final Logger logger = Logger.getLogger(SpectrumFileViewModel.class.getName());

    private final DoubleProperty startFrequencyProperty = new SimpleDoubleProperty(950d);
    private final DoubleProperty stopFrequencyProperty = new SimpleDoubleProperty(1450d);
    private final DoubleProperty referenceLevelProperty = new SimpleDoubleProperty(0d);
    private final DoubleProperty scaleProperty = new SimpleDoubleProperty(5d);
    private final ObjectProperty<ObservableList<XYChart.Series<Float, Float>>> spectrumSeriesProperty =
            new SimpleObjectProperty<>(FXCollections.observableArrayList());
    private final ObjectProperty<File> lastOpenDirectoryProperty =
            new SimpleObjectProperty<>(new File(System.getProperty("user.dir")));

    /**
     * @return start frequency of display in MHz.
     */
    final double getStartFrequency() {
        return startFrequencyProperty.get();
    }

    /**
     * @param value start frequency of display in MHz.
     */
    final void setStartFrequency(double value) {
        startFrequencyProperty.set(value);
    }

    DoubleProperty startFrequencyProperty() {
        return startFrequencyProperty;
    }

    /**
     * @return stop frequency of display in MHz.
     */
    final double getStopFrequency() {
        return stopFrequencyProperty.get();
    }

    /**
     * @param value stop frequency of display in MHz.
     */
    final void setStopFrequency(double value) {
        stopFrequencyProperty.set(value);
    }

    DoubleProperty stopFrequencyProperty() {
        return stopFrequencyProperty;
    }

    /**
     * @return reference level(top value of Y-axis) in dBm.
     */
    final double getReferenceLevel() {
        return referenceLevelProperty.get();
    }

    /**
     * @param value reference level in dBm.
     */
    final void setReferenceLevel(double value) {
        referenceLevelProperty.set(value);
    }

    DoubleProperty referenceLevelProperty() {
        return referenceLevelProperty;
    }

    /**
     * @return scale of 1 div(1 of 10 division of Y-axis) in dBm.
     */
    final double getScale() {
        return scaleProperty.get();
    }

    /**
     * @param value scale of 1 div in dBm.
     */
    final void setScale(double value) {
        scaleProperty.set(value);
    }

    DoubleProperty scaleProperty() {
        return scaleProperty;
    }

    ObjectProperty<ObservableList<XYChart.Series<Float, Float>>> spectrumSeriesProperty() {
        return spectrumSeriesProperty;
    }

    ObjectProperty<File> lastOpenDirectoryProperty() {
        return lastOpenDirectoryProperty;
    }


}
