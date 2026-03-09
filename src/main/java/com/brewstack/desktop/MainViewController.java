package com.brewstack.desktop;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MainViewController {

    @FXML private Label baristaNameLabel;
    @FXML private ProgressBar xpBar;
    @FXML private Label xpLabel;
    @FXML private FlowPane recipePane;
    @FXML private ListView<OrderItem> orderListView;
    @FXML private Label totalPriceLabel;
    @FXML private Button completeOrderBtn;
    @FXML private Label orderStatusLabel;
    @FXML private Label levelUpLabel;

    private final ObservableList<OrderItem> currentOrder = FXCollections.observableArrayList();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    @FXML
    public void initialize() {
        orderListView.setItems(currentOrder);
        updateTotal();
        loadBaristaHeader();
        fetchRecipes();
    }

    private void loadBaristaHeader() {
        Barista b = AppState.getCurrentBarista();
        if (b == null) return;
        baristaNameLabel.setText(b.getName() + "  •  Level " + b.getLevel());
        xpBar.setProgress(b.xpProgress());
        xpLabel.setText(b.xpInCurrentLevel() + " / " + b.xpForCurrentLevel() + " XP");
    }

    @FXML
    private void onDailyHistory() {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("DailyHistoryView.fxml"));
            Stage stage = (Stage) recipePane.getScene().getWindow();
            stage.setScene(new Scene(loader.load(), 1100, 720));
        } catch (Exception e) {
            showError("Navigation Error", e.getMessage() != null ? e.getMessage() : e.getClass().getName());
        }
    }

    private void showLevelUp(int newLevel) {
        levelUpLabel.setText("Level Up!  →  Level " + newLevel);
        levelUpLabel.setOpacity(1.0);
        levelUpLabel.setVisible(true);

        PauseTransition hold = new PauseTransition(Duration.seconds(1.5));
        FadeTransition fade = new FadeTransition(Duration.seconds(1.5), levelUpLabel);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);
        fade.setOnFinished(ev -> levelUpLabel.setVisible(false));

        new SequentialTransition(hold, fade).play();
    }

    private void showError(String title, String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void onSwitchBarista() {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("BaristaSelection.fxml"));
            Scene scene = new Scene(loader.load(), 800, 500);
            Stage stage = (Stage) recipePane.getScene().getWindow();
            stage.setScene(scene);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void fetchRecipes() {
        Task<List<Recipe>> task = new Task<>() {
            @Override
            protected List<Recipe> call() throws Exception {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8181/api/recipes"))
                        .GET()
                        .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                return mapper.readValue(response.body(), new TypeReference<List<Recipe>>() {});
            }
        };

        task.setOnSucceeded(e -> populateButtons(task.getValue()));
        task.setOnFailed(e -> {
            Label error = new Label("Could not load recipes: " + task.getException().getMessage());
            error.setStyle("-fx-text-fill: red; -fx-font-size: 13px;");
            recipePane.getChildren().add(error);
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void populateButtons(List<Recipe> recipes) {
        for (Recipe recipe : recipes) {
            Button btn = new Button(recipe.getName() + "\n$" + String.format("%.2f", recipe.getPrice()));
            btn.setPrefWidth(130);
            btn.setPrefHeight(80);
            btn.setStyle(
                "-fx-font-size: 13px; -fx-background-color: #3498db; " +
                "-fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand;"
            );
            btn.setOnAction(ev -> addToOrder(recipe));
            recipePane.getChildren().add(btn);
        }
    }

    private void addToOrder(Recipe recipe) {
        for (int i = 0; i < currentOrder.size(); i++) {
            if (currentOrder.get(i).getRecipe().getId() == recipe.getId()) {
                OrderItem item = currentOrder.get(i);
                item.increment();
                currentOrder.set(i, item);
                updateTotal();
                return;
            }
        }
        currentOrder.add(new OrderItem(recipe));
        updateTotal();
    }

    private void updateTotal() {
        double total = currentOrder.stream().mapToDouble(OrderItem::lineTotal).sum();
        totalPriceLabel.setText(String.format("Total:  $%.2f", total));
    }

    @FXML
    private void onCompleteOrder() {
        if (currentOrder.isEmpty()) {
            orderStatusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #c0392b;");
            orderStatusLabel.setText("Add items before completing.");
            return;
        }

        Barista barista = AppState.getCurrentBarista();
        if (barista == null) return;

        // Build flat recipeIds list — repeat ID once per quantity
        List<Long> recipeIds = new ArrayList<>();
        for (OrderItem item : currentOrder) {
            for (int i = 0; i < item.getQuantity(); i++) {
                recipeIds.add(item.getRecipe().getId());
            }
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("recipeIds", recipeIds);
        payload.put("baristaId", barista.getId());

        completeOrderBtn.setDisable(true);
        orderStatusLabel.setText("");

        Task<long[]> task = new Task<>() {
            @Override
            protected long[] call() throws Exception {
                String body = mapper.writeValueAsString(payload);
                HttpRequest postRequest = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8181/api/brew/order"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();
                HttpResponse<String> response = httpClient.send(postRequest, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    throw new RuntimeException("Order failed: HTTP " + response.statusCode() + " — " + response.body());
                }
                // Extract baristaXp and baristaLevel directly from the response
                var node = mapper.readTree(response.body());
                long xp = node.get("baristaXp").asLong();
                int level = node.get("baristaLevel").asInt();
                return new long[]{xp, level};
            }
        };

        task.setOnSucceeded(e -> {
            long[] result = task.getValue();
            int oldLevel = barista.getLevel();
            int newLevel = (int) result[1];
            barista.setTotalXp((int) result[0]);
            barista.setLevel(newLevel);
            AppState.setCurrentBarista(barista);
            loadBaristaHeader();
            currentOrder.clear();
            updateTotal();
            completeOrderBtn.setDisable(false);
            orderStatusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #27ae60;");
            orderStatusLabel.setText("Order completed!");
            if (newLevel > oldLevel) {
                showLevelUp(newLevel);
            }
        });

        task.setOnFailed(e -> {
            completeOrderBtn.setDisable(false);
            orderStatusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #c0392b;");
            orderStatusLabel.setText("Error: " + task.getException().getMessage());
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }
}
