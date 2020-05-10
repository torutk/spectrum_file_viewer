module com.torutk.spectrum {
    requires javafx.controls;
    requires javafx.fxml;
    opens com.torutk.spectrum.view to javafx.graphics, javafx.fxml;
}
