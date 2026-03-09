package com.brewstack.desktop;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.Stage;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class FullHistoryController {

    @FXML private TableView<DailyBalance> historyTable;
    @FXML private TableColumn<DailyBalance, String> dateCol;
    @FXML private TableColumn<DailyBalance, String> revenueCol;
    @FXML private TableColumn<DailyBalance, Number> ordersCol;
    @FXML private Label statusLabel;

    private final ObservableList<DailyBalance> data = FXCollections.observableArrayList();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    @FXML
    public void initialize() {
        dateCol.setCellValueFactory(row -> new SimpleStringProperty(row.getValue().getDate()));
        revenueCol.setCellValueFactory(row -> new SimpleStringProperty(row.getValue().getFormattedRevenue()));
        ordersCol.setCellValueFactory(row -> new SimpleIntegerProperty(row.getValue().getTotalOrders()));
        historyTable.setItems(data);
        fetchHistory();
    }

    private void fetchHistory() {
        Task<ObservableList<DailyBalance>> task = new Task<>() {
            @Override
            protected ObservableList<DailyBalance> call() throws Exception {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8181/api/finance/history"))
                        .GET()
                        .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                JsonNode array = mapper.readTree(response.body());
                ObservableList<DailyBalance> list = FXCollections.observableArrayList();
                for (JsonNode node : array) {
                    list.add(DailyHistoryController.parseBalance(node));
                }
                return list;
            }
        };

        task.setOnSucceeded(e -> {
            data.setAll(task.getValue());
            if (data.isEmpty()) {
                statusLabel.setText("No history records found.");
            }
        });

        task.setOnFailed(e ->
            statusLabel.setText("Could not load history: " + task.getException().getMessage())
        );

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void onBack() {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("DailyHistoryView.fxml"));
            Stage stage = (Stage) historyTable.getScene().getWindow();
            stage.setScene(new Scene(loader.load(), 1100, 720));
        } catch (Exception e) {
            statusLabel.setText("Navigation error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
