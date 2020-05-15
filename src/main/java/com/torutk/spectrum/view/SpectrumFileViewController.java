package com.torutk.spectrum.view;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Cursor;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.converter.DoubleStringConverter;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class SpectrumFileViewController implements Initializable {
    private static final Logger logger = Logger.getLogger(SpectrumFileViewController.class.getName());
    private static final PseudoClass HAZE_PSEUDO_CLASS = PseudoClass.getPseudoClass("haze");

    private FileChooser fileChooser = new FileChooser();
    private DirectoryChooser directoryChooser = new DirectoryChooser();
    private ResourceBundle resources;
    private double chartDragPointX;
    private final SpectrumFileViewModel model = SpectrumFileViewModel.INSTANCE;

    @FXML private LineChart<Float, Float> chart;
    @FXML private NumberAxis xAxis;
    @FXML private NumberAxis yAxis;
    @FXML private ToggleButton rightPaneToggleButton;
    @FXML private Pane rightPane;
    @FXML private TextField startFrequencyField;
    @FXML private TextField stopFrequencyField;
    @FXML private TextField referenceLevelField;
    @FXML private TextField scaleField;
    @FXML private Button updateButton;
    @FXML private CheckBox detrendCheckBox;
    @FXML private Label detrendFileLabel;
    @FXML private CheckBox rcFilterCheckBox;

    @FXML
    private void open(ActionEvent ev) {
        logger.fine("User operation 'open' triggered.");
        List<File> files = fileChooser.showOpenMultipleDialog(getStage());
        if (files == null) {
            logger.fine("User operation 'open' cancelled.");
            return;
        }
        files.forEach(file -> {
            model.lastOpenDirectoryProperty().set(file.getParentFile());
            try {
                model.loadFromFile(file.toPath());
            } catch (IOException e) {
                logger.warning("could not parse file: " + file.getName());
            }
        });
        recreateAllSeries();
        refreshSettingFields();
    }

    @FXML
    private void export(ActionEvent ev) {
        logger.fine("User operation 'export' triggered.");
        File directory = directoryChooser.showDialog(getStage());
        if (directory == null) {
            logger.fine("User operation 'export' cancelled.");
            return;
        } else {
            model.lastOpenDirectoryProperty().set(directory.getParentFile());
        }
        try {
            model.exportTo(directory.toPath());
        } catch (IOException e) {
            logger.warning("could not export to the directory:" + directory);
        }
    }

    @FXML
    private void showHelp(ActionEvent ev) {

    }

    @FXML
    private void updateSettings(ActionEvent event) {
        logger.fine("User operation 'update' triggered.");
        double previousSpan = model.getSpan();
        model.setStartFrequency(Double.parseDouble(startFrequencyField.getText()));
        model.setStopFrequency(Double.parseDouble(stopFrequencyField.getText()));
        model.setReferenceLevel(Double.parseDouble(referenceLevelField.getText()));
        model.setScale(Double.parseDouble(scaleField.getText()));
        updateButton.setDisable(true);
        if (model.needsRecreate(previousSpan)) {
            recreateAllSeries();
        }
    }

    private void openDetrend() {
        logger.fine("User operation 'open detrend file' triggered.");
        File file = fileChooser.showOpenDialog(getStage());
        if (file == null) {
            logger.fine("User operation 'open detrended file' cancelled.");
            return;
        }
        try {
            model.loadDetrendFromFile(file.toPath());
            recreateAllSeries();
        } catch (IOException e) {
            logger.warning("could not parse detrend file:" + file.toString());
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        resources = resourceBundle;
        fileChooser.setTitle(resources.getString("spectrum.view.filechooser.title"));
        fileChooser.initialDirectoryProperty().bindBidirectional(model.lastOpenDirectoryProperty());
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Spectrum Files", "*.dat")
        );
        directoryChooser.setTitle(resources.getString("spectrum.view.directorychooser.title"));
        directoryChooser.initialDirectoryProperty().bindBidirectional(model.lastOpenDirectoryProperty());
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

    private void updateChartLegendItemsHandler() {
        chart.lookupAll(".chart-legend-item").stream()
                .filter(node -> node instanceof Label)
                .map(node -> (Label) node)
                .forEach(label -> {
                    label.setOnMouseClicked(ev -> {
                        if (ev.getButton() == MouseButton.PRIMARY) {
                            getSeriesByName(label.getText()).ifPresent(series -> {
                                boolean toBeInvisible = series.getNode().isVisible();
                                series.getNode().setVisible(!toBeInvisible);
                                label.pseudoClassStateChanged(HAZE_PSEUDO_CLASS, toBeInvisible);
                            });
                        }
                    });
                    label.setContextMenu(createLegendLabelContextMenu(label));
                });
    }

    private Optional<XYChart.Series<Float, Float>> getSeriesByName(String name) {
        return chart.getData().stream()
                .filter(series -> series.getName().equals(name))
                .findAny();
    }

    private ContextMenu createLegendLabelContextMenu(Label label) {
        var removeItem = new MenuItem(resources.getString("spectrum.view.chart.legend.menu.remove"));
        removeItem.setOnAction(event -> getSeriesByName(label.getText()).ifPresent(this::removeSeries));
        return new ContextMenu(removeItem);
    }

    private void initializeRightPane() {
        rightPane.managedProperty().bind(rightPaneToggleButton.selectedProperty());
        rightPane.visibleProperty().bind(rightPaneToggleButton.selectedProperty());

        rightPaneToggleButton.selectedProperty().addListener((obs, ov, nv) -> refreshSettingFields());
        Stream.of(startFrequencyField, stopFrequencyField, referenceLevelField, scaleField)
                .forEach(this::initializeSettingsTextFields);

        detrendFileLabel.disableProperty().bind(detrendCheckBox.selectedProperty().not());
        detrendFileLabel.setOnMouseClicked(event -> openDetrend());
        model.useDetrendProperty().bind(detrendCheckBox.selectedProperty());
        detrendCheckBox.selectedProperty().addListener((obs, ov, nv) -> recreateAllSeries());
        detrendFileLabel.textProperty().bind(Bindings.select(model.detrendProperty(), "name"));

        model.useRcFilterProperty().bind(rcFilterCheckBox.selectedProperty());
        rcFilterCheckBox.selectedProperty().addListener((obs, ov, nv) -> recreateAllSeries());
    }

    private void initializeSettingsTextFields(TextField field) {
        field.textProperty().addListener((obs, ov, nv) -> updateButton.setDisable(false));
        var tf = new TextFormatter<>(new DoubleStringConverter());
        field.setTextFormatter(tf);
        field.setOnScroll(ev ->
                tf.setValue(tf.getValue() + Math.signum(ev.getDeltaY()) * (tf.getValue() > 100 ? 1 : 0.5))
        );
    }

    /**
     * refresh values in settings panel.
     */
    private void refreshSettingFields() {
        startFrequencyField.setText(String.format("%10.4f", model.getStartFrequency()));
        stopFrequencyField.setText(String.format("%10.4f", model.getStopFrequency()));
        referenceLevelField.setText(String.format("%5.1f", model.getReferenceLevel()));
        scaleField.setText(String.format("%4.1f", model.getScale()));
        updateButton.setDisable(true);
    }

    private void recreateAllSeries() {
        model.recreateAllSeries();
        updateChartLegendItemsHandler();
    }

    // This method is called from context menu of legend label to be removed.
    // UpdateChartLegendItemHandler removes the legend label of context menu,
    // So it should not be called in this thread but in another thread later.
    private void removeSeries(XYChart.Series<Float, Float> series) {
        model.removeSpectrumData(series.getName());
        Platform.runLater(this::updateChartLegendItemsHandler);
    }

    private Stage getStage() {
        return (Stage) chart.getScene().getWindow();
    }
}
