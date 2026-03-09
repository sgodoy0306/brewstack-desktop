module com.brewstack.desktop {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires java.net.http;
    requires com.fasterxml.jackson.databind;
    requires java.net.http;

    opens com.brewstack.desktop to javafx.fxml;
    opens com.brewstack.desktop.api.model to com.fasterxml.jackson.databind;
    exports com.brewstack.desktop;
    exports com.brewstack.desktop.api;
    exports com.brewstack.desktop.api.model;
}
