package com.brewstack.desktop;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class DailyHistoryController {

    @FXML private Label dateLabel;
    @FXML private Label revenueLabel;
    @FXML private Label ordersLabel;
    @FXML private Label statusLabel;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    @FXML
    public void initialize() {
        fetchDailyReport();
    }

    private void fetchDailyReport() {
        Task<DailyBalance> task = new Task<>() {
            @Override
            protected DailyBalance call() throws Exception {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8181/api/finance/daily-report"))
                        .GET()
                        .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                return parseBalance(mapper.readTree(response.body()));
            }
        };

        task.setOnSucceeded(e -> {
            DailyBalance balance = task.getValue();
            dateLabel.setText(balance.getDate());
            revenueLabel.setText(balance.getFormattedRevenue());
            ordersLabel.setText(String.valueOf(balance.getTotalOrders()));
        });

        task.setOnFailed(e ->
            statusLabel.setText("Could not load report: " + task.getException().getMessage())
        );

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    /** Handles both string ("2026-03-09") and array ([2026,3,9]) date formats from Jackson. */
    static DailyBalance parseBalance(JsonNode node) {
        DailyBalance b = new DailyBalance();

        JsonNode dateNode = node.get("date");
        if (dateNode != null) {
            if (dateNode.isArray()) {
                b.setDate(String.format("%04d-%02d-%02d",
                        dateNode.get(0).asInt(),
                        dateNode.get(1).asInt(),
                        dateNode.get(2).asInt()));
            } else {
                b.setDate(dateNode.asText());
            }
        }

        JsonNode revNode = node.get("totalRevenue");
        b.setTotalRevenue(revNode != null ? new BigDecimal(revNode.asText()) : BigDecimal.ZERO);

        JsonNode ordNode = node.get("totalOrders");
        b.setTotalOrders(ordNode != null ? ordNode.asInt() : 0);

        return b;
    }

    @FXML
    private void onBack() {
        navigate("MainView.fxml");
    }

    @FXML
    private void onFullHistory() {
        navigate("FullHistoryView.fxml");
    }

    private void navigate(String fxml) {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource(fxml));
            Stage stage = (Stage) dateLabel.getScene().getWindow();
            stage.setScene(new Scene(loader.load(), 1100, 720));
        } catch (Exception e) {
            statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #e74c3c;");
            statusLabel.setText("Navigation error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
