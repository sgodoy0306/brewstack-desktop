package com.brewstack.desktop;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.stage.Stage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class StockViewController {

    @FXML private ListView<StockItem> stockListView;
    @FXML private Label statusLabel;
    @FXML private Label lowStockCountLabel;

    private final ObservableList<StockItem> stockItems = FXCollections.observableArrayList();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    @FXML
    public void initialize() {
        stockListView.setCellFactory(lv ->
                new StockCell(httpClient, mapper, this::refreshLowStockBadge));
        stockListView.setItems(stockItems);
        fetchStock();
    }

    private void fetchStock() {
        Task<List<StockItem>> task = new Task<>() {
            @Override
            protected List<StockItem> call() throws Exception {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8181/api/stock"))
                        .GET().build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                return mapper.readValue(response.body(), new TypeReference<>() {});
            }
        };

        task.setOnSucceeded(e -> {
            stockItems.setAll(task.getValue());
            refreshLowStockBadge();
        });

        task.setOnFailed(e ->
            statusLabel.setText("Could not load stock: " + task.getException().getMessage())
        );

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void refreshLowStockBadge() {
        long lowCount = stockItems.stream().filter(StockItem::isLow).count();
        if (lowCount == 0) {
            lowStockCountLabel.setText("All items sufficiently stocked.");
            lowStockCountLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-size: 13px;");
        } else {
            lowStockCountLabel.setText(lowCount + " item(s) at or below minimum threshold.");
            lowStockCountLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 13px;");
        }
    }

    @FXML
    private void onBack() {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("MainView.fxml"));
            Stage stage = (Stage) stockListView.getScene().getWindow();
            stage.setScene(new Scene(loader.load(), 1100, 720));
        } catch (Exception e) {
            statusLabel.setText("Navigation error: " + e.getMessage());
        }
    }
}
