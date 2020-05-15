package com.torutk.spectrum.view;

import com.torutk.spectrum.data.SpectrumData;
import com.torutk.spectrum.data.SpectrumDataExporter;
import com.torutk.spectrum.data.SpectrumDataParser;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.XYChart;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collector;
import java.util.stream.IntStream;

/**
 * View Model of Spectrum Viewer Application.
 *
 * <ul>
 * <li>Singleton implementation by enum.</li>
 * <li>Holds all data to be displayed.</li>
 * <li>Mediate between view and domain data(Model).</li>
 * </ul>
 */
enum SpectrumFileViewModel {
    INSTANCE;

    private static final int SPECTRUM_DISPLAY_PIXELS = 1024; // temporary assume 1k display pixel
    private static final Logger logger = Logger.getLogger(SpectrumFileViewModel.class.getName());

    private final DoubleProperty startFrequencyProperty = new SimpleDoubleProperty(950d);
    private final DoubleProperty stopFrequencyProperty = new SimpleDoubleProperty(1450d);
    private final DoubleProperty referenceLevelProperty = new SimpleDoubleProperty(0d);
    private final DoubleProperty scaleProperty = new SimpleDoubleProperty(5d);
    private final ObjectProperty<ObservableList<XYChart.Series<Float, Float>>> spectrumSeriesProperty =
            new SimpleObjectProperty<>(FXCollections.observableArrayList());
    private final BooleanProperty useDetrendProperty = new SimpleBooleanProperty();
    private final ObjectProperty<SpectrumData> detrendProperty = new SimpleObjectProperty<>();
    private final BooleanProperty useRcFilterProperty = new SimpleBooleanProperty();

    private final ObjectProperty<File> lastOpenDirectoryProperty =
            new SimpleObjectProperty<>(new File(System.getProperty("user.dir")));


    private final List<SpectrumData> spectrumDataList = new ArrayList<>();

    /**
     * Loads spectrum data from the specified file, then holds in the spectrum list to be displayed.
     *
     * @param path the file path to be loaded.
     * @throws IOException if the specified file cannot be read.
     */
    public void loadFromFile(Path path) throws IOException {
        SpectrumData spectrum = SpectrumDataParser.parse(path);
        if (spectrumDataList.contains(spectrum)) {
            logger.info(() -> String.format("Already loaded %s from file %s ", spectrum, path));
            return;
        }
        logger.info(() -> String.format("Loaded %s from file %s", spectrum, path));
        load(spectrum);
    }

    private void load(SpectrumData spectrum) {
        spectrumDataList.add(spectrum);
        setStartFrequency(spectrum.getStartFrequency());
        setStopFrequency(spectrum.getStopFrequency());
        setReferenceLevel(spectrum.getReferenceLevel());
        setScale(spectrum.getScale());
    }

    /**
     * Loads spectrum data from the specified file, then holds as the detrend.
     *
     * @param path the file path to be loaded.
     * @throws IOException if the specified file cannot be read.
     */
    void loadDetrendFromFile(Path path) throws IOException {
        detrendProperty.set(SpectrumDataParser.parse(path));
        logger.info(String.format("Loaded Detrend %s from file %s", detrendProperty.get(), path));
    }

    /**
     * Make a decision the rate to be decimate for display.
     *
     * @param samplingRate data's sampling rate.(means the number of points in the display frequency)
     * @return decimation ratio.(3 means 1 of 3 point is displayed)
     */
    private int decimation(double samplingRate) {
        int numPointsInData = (int) ((getStopFrequency() - getStartFrequency()) / samplingRate);
        int decimation = numPointsInData / SPECTRUM_DISPLAY_PIXELS;
        logger.fine(String.format("arg %f, full points %d, decimation %d", samplingRate, numPointsInData, decimation));
        return decimation == 0 ? 1 : decimation;
    }

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

    BooleanProperty useDetrendProperty() {
        return useDetrendProperty;
    }

    ObjectProperty<SpectrumData> detrendProperty() {
        return detrendProperty;
    }

    BooleanProperty useRcFilterProperty() {
        return useRcFilterProperty;
    }

    ObjectProperty<File> lastOpenDirectoryProperty() {
        return lastOpenDirectoryProperty;
    }

    void removeSpectrumData(String name) {
        spectrumDataList.removeIf(data -> data.getName().equals(name));
        spectrumSeriesProperty.get().removeIf(series -> series.getName().equals(name));
    }

    private XYChart.Series<Float, Float> createDecimatedSeries(String name, float[] xArray, float[] yArray, int decimation) {
        assert xArray.length == yArray.length;
        Collector<XYChart.Data<Float, Float>, ObservableList<XYChart.Data<Float, Float>>,
                XYChart.Series<Float, Float>> collector;
        if (useRcFilterProperty().get()) {
            collector = toSeriesWithRcFilter(name);
        } else {
            collector = toSeries(name);
        }
        return IntStream.range(0, xArray.length)
                .filter(i -> i % decimation == 0)
                .mapToObj(i -> new XYChart.Data<Float, Float>(xArray[i], maxWithin(yArray, i, i + decimation)))
                .collect(collector);
    }

    /**
     * Search max value from specified data array between from index to to index.
     *
     * @param data whole data to be searched
     * @param fromIndex start index to be searched from the data
     * @param toIndexExclude stop index (not include this value) to be searched from the data
     * @return the max value
     */
    private float maxWithin(float[] data, int fromIndex, int toIndexExclude) {
        return (float) IntStream.range(fromIndex, Math.min(toIndexExclude, data.length))
                .mapToDouble(i -> data[i])
                .max()
                .orElse(data[fromIndex]);
    }

    /**
     * Collector collect the each element of {@code XYChart.Data<Float, Float>} to the list of
     * {@code ObservableList<XYChart.Data<Float, Float>>} with applying RC filter, then output
     * {@code XYChart.Series<Float, Float>}.
     *
     * @param name set to the return object of XYChart.Series.
     * @return XYChart.Series to be displayed.
     */
    private static Collector<XYChart.Data<Float, Float>, ObservableList<XYChart.Data<Float, Float>>,
            XYChart.Series<Float, Float>> toSeriesWithRcFilter(String name)
    {
        return Collector.of(
                FXCollections::observableArrayList,
                (l, e) -> {
                    e.setYValue((float) SpectrumData.applyRcFilter(
                            l.size() > 0 ? l.get(l.size() - 1).getYValue() : e.getYValue(), e.getYValue()
                    ));
                    l.add(e);
                },
                (left, right) -> {
                    left.addAll(right);
                    return left;
                },
                l -> new XYChart.Series<>(name, l)
        );
    }

    /**
     * Collector collect the each element of {@code XYChart.Data<Float, Float>} to the list of
     * {@code ObservableList<XYChart.Data<Float, Float>>}, then output {@code XYChart.Series<Float, Float>}.
     *
     * @param name set to the return object of XYChart.Series.
     * @return XYChart.Series to be displayed.
     */
    private static Collector<XYChart.Data<Float, Float>, ObservableList<XYChart.Data<Float, Float>>,
            XYChart.Series<Float, Float>> toSeries(String name)
    {
        return Collector.of(
                FXCollections::observableArrayList,
                ObservableList::add,
                (l, r) -> {l.addAll(r); return l;},
                l -> new XYChart.Series<>(name, l)
        );
    }

    /**
     * Export SpectrumData in list to the specified directory with CSV format.
     *
     * @param toPath the directory to be exported
     * @throws IOException error in file I/O
     */
    public void exportTo(Path toPath) throws IOException {
        for (SpectrumData data: spectrumDataList) {
            SpectrumDataExporter.exportAsCsv(toPath, data);
            logger.info(String.format("exported data %s to file %s", data, toPath));
        }
    }

    /**
     * Recreate all XYChart.Series.
     * This is needed when start/stop frequency, reference level, or scale is changed
     * and re decimated with new value.
     */
    public void recreateAllSeries() {
        ObservableList<XYChart.Series<Float, Float>> list = spectrumDataList.stream()
                .map(data -> createDecimatedSeries(
                        data.getName(), data.getFrequencies(), getPowersDetrend(data),
                        decimation(data.getSamplingRate())
                ))
                .collect(FXCollections::observableArrayList, ObservableList::add, ObservableList::addAll);
        spectrumSeriesProperty.set(list);
    }

    private float[] getPowersDetrend(SpectrumData data) {
        if (useDetrendProperty.get() && detrendProperty.get() != null) {
            return data.getPowersDetrend(detrendProperty.get());
        } else {
            return data.getPowers();
        }
    }

    double getSpan() {
        return getStopFrequency() - getStartFrequency();
    }

    boolean needsRecreate(double previousSpan) {
        double span = getSpan();
        return Math.max(span, previousSpan) / Math.min(span, previousSpan) > 2;
    }
}
