package com.brewstack.desktop;

import com.brewstack.desktop.api.BrewApiClient;
import com.brewstack.desktop.api.model.Recipe;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;

import java.math.BigDecimal;
import java.util.List;

public class MainViewController {

    @FXML private FlowPane menuPane;
    @FXML private Label baristaNameLabel;
    @FXML private Label baristaLevelLabel;

    private final BrewApiClient apiClient = new BrewApiClient();

    @FXML
    public void initialize() {
        setBaristaStatus("Barista", 1);
        loadMenu();
    }

    private void loadMenu() {
        Thread.ofVirtual().start(() -> {
            try {
                List<Recipe> recipes = apiClient.getMenu();
                Platform.runLater(() -> populateMenu(recipes));
            } catch (Exception e) {
                Platform.runLater(() -> showMenuError(e.getMessage()));
            }
        });
    }

    private void populateMenu(List<Recipe> recipes) {
        menuPane.getChildren().clear();
        for (Recipe recipe : recipes) {
            menuPane.getChildren().add(createRecipeButton(recipe));
        }
    }

    private Button createRecipeButton(Recipe recipe) {
        String price = recipe.getPrice() != null
                ? "$" + recipe.getPrice().setScale(2, java.math.RoundingMode.HALF_UP)
                : "N/A";

        Button btn = new Button(recipe.getName() + "\n" + price);
        btn.setPrefSize(160, 80);
        btn.setStyle(
            "-fx-background-color: #2980b9; -fx-text-fill: white;" +
            "-fx-font-size: 13px; -fx-font-weight: bold;" +
            "-fx-background-radius: 8; -fx-cursor: hand;"
        );
        btn.setOnMouseEntered(e -> btn.setStyle(
            "-fx-background-color: #3498db; -fx-text-fill: white;" +
            "-fx-font-size: 13px; -fx-font-weight: bold;" +
            "-fx-background-radius: 8; -fx-cursor: hand;"
        ));
        btn.setOnMouseExited(e -> btn.setStyle(
            "-fx-background-color: #2980b9; -fx-text-fill: white;" +
            "-fx-font-size: 13px; -fx-font-weight: bold;" +
            "-fx-background-radius: 8; -fx-cursor: hand;"
        ));
        btn.setOnAction(e -> handleSale(recipe));
        return btn;
    }

    private void handleSale(Recipe recipe) {
        Thread.ofVirtual().start(() -> {
            try {
                apiClient.processSale(recipe.getId());
            } catch (Exception e) {
                System.err.println("Sale failed for " + recipe.getName() + ": " + e.getMessage());
            }
        });
    }

    private void showMenuError(String message) {
        Label error = new Label("Could not load menu: " + message);
        error.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 13px;");
        menuPane.getChildren().add(error);
    }

    public void setBaristaStatus(String name, int level) {
        baristaNameLabel.setText(name);
        baristaLevelLabel.setText(String.valueOf(level));
    }
}
