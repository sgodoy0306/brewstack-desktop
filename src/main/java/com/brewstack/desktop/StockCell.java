package com.brewstack.desktop;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.Map;

public class StockCell extends ListCell<StockItem> {

    private final HttpClient httpClient;
    private final ObjectMapper mapper;
    private final Runnable onRestocked;

    // Layout nodes — built once, reused across cell recycling
    private final HBox container = new HBox(12);
    private final VBox infoBox = new VBox(3);
    private final Label nameLabel = new Label();
    private final Label stockLabel = new Label();
    private final Region spacer = new Region();
    private final TextField amountField = new TextField();
    private final Button restockBtn = new Button("+ Restock");
    private final Label feedbackLabel = new Label();

    public StockCell(HttpClient httpClient, ObjectMapper mapper, Runnable onRestocked) {
        this.httpClient = httpClient;
        this.mapper = mapper;
        this.onRestocked = onRestocked;

        HBox.setHgrow(spacer, Priority.ALWAYS);

        nameLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #3d2c1e;");
        stockLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #9a8470;");

        amountField.setPromptText("Amount");
        amountField.setPrefWidth(80);
        amountField.setStyle("-fx-font-size: 12px; -fx-background-radius: 8; -fx-border-radius: 8;");

        restockBtn.setStyle(
            "-fx-background-color: #6aaa82; -fx-text-fill: #0f2e1a; -fx-font-size: 12px; " +
            "-fx-font-weight: bold; -fx-background-radius: 16; -fx-cursor: hand; -fx-padding: 5 14 5 14;"
        );

        feedbackLabel.setStyle("-fx-font-size: 11px;");
        feedbackLabel.setVisible(false);

        restockBtn.setOnAction(e -> handleRestock());

        infoBox.setAlignment(Pos.CENTER_LEFT);
        infoBox.getChildren().addAll(nameLabel, stockLabel);

        container.setAlignment(Pos.CENTER_LEFT);
        container.setStyle("-fx-background-color: #faf7f2; -fx-padding: 10 14 10 14; " +
                "-fx-border-color: #e0d5c5; -fx-border-width: 0 0 1 0;");
        container.getChildren().addAll(infoBox, spacer, feedbackLabel, amountField, restockBtn);
    }

    private void handleRestock() {
        StockItem item = getItem();
        if (item == null) return;

        String text = amountField.getText().trim();
        double amount;
        try {
            amount = Double.parseDouble(text);
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            showFeedback("Enter a valid amount.", false);
            return;
        }

        restockBtn.setDisable(true);
        double finalAmount = amount;

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("amount", finalAmount);

        Thread thread = new Thread(() -> {
            try {
                String body = mapper.writeValueAsString(payload);
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8181/api/stock/" + item.getId() + "/restock"))
                        .header("Content-Type", "application/json")
                        .method("PATCH", HttpRequest.BodyPublishers.ofString(body))
                        .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    StockItem updated = mapper.readValue(response.body(), StockItem.class);
                    Platform.runLater(() -> {
                        item.setCurrentStock(updated.getCurrentStock());
                        updateStockLabel(item);
                        amountField.clear();
                        showFeedback("Restocked!", true);
                        restockBtn.setDisable(false);
                        onRestocked.run();
                    });
                } else {
                    Platform.runLater(() -> {
                        showFeedback("Failed.", false);
                        restockBtn.setDisable(false);
                    });
                }
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    showFeedback("Error: " + ex.getMessage(), false);
                    restockBtn.setDisable(false);
                });
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private void showFeedback(String message, boolean success) {
        feedbackLabel.setText(message);
        feedbackLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " +
                (success ? "#3a9e6a" : "#d47070") + ";");
        feedbackLabel.setVisible(true);
    }

    private void updateStockLabel(StockItem item) {
        String stockText = String.format("%.1f %s  (min: %.1f)",
                item.getCurrentStock(), item.getUnit() != null ? item.getUnit() : "",
                item.getMinimumThreshold());
        stockLabel.setText(stockText);
        stockLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " +
                (item.isLow() ? "#d47070" : "#3a9e6a") + ";");
    }

    @Override
    protected void updateItem(StockItem item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || item == null) {
            setGraphic(null);
        } else {
            nameLabel.setText(item.getName());
            updateStockLabel(item);
            feedbackLabel.setVisible(false);
            amountField.clear();
            restockBtn.setDisable(false);
            setGraphic(container);
        }
    }
}
