module com.brewstack.desktop {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.fasterxml.jackson.databind;

    opens com.brewstack.desktop to javafx.fxml;
    exports com.brewstack.desktop;
}
