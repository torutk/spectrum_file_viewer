module com.torutk.spectrum {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.logging;
    opens com.torutk.spectrum.view to javafx.graphics, javafx.fxml;
}
