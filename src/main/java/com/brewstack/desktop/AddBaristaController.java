package com.brewstack.desktop;

import com.brewstack.desktop.api.BrewApiClient;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class AddBaristaController {

    @FXML private TextField nameField;
    @FXML private Label statusLabel;
    @FXML private Button saveBtn;

    private final BrewApiClient apiClient = new BrewApiClient();

    @FXML
    private void onSave() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            showError("Please enter a name.");
            return;
        }

        saveBtn.setDisable(true);
        statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #9a8470;");
        statusLabel.setText("Creating…");

        new Thread(() -> {
            try {
                apiClient.createBarista(name);
                Platform.runLater(() -> navigate("BaristaSelection.fxml"));
            } catch (Exception e) {
                Platform.runLater(() -> showError(e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void onBack() {
        navigate("BaristaSelection.fxml");
    }

    private void navigate(String fxml) {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource(fxml));
            Stage stage = (Stage) nameField.getScene().getWindow();
            stage.setScene(new Scene(loader.load(), 1100, 720));
        } catch (Exception e) {
            showError("Navigation error: " + e.getMessage());
        }
    }

    private void showError(String message) {
        saveBtn.setDisable(false);
        statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #a05030;");
        statusLabel.setText(message);
    }
}
