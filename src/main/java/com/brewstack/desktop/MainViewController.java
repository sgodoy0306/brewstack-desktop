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
import javafx.scene.control.Tooltip;
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
import java.util.stream.Collectors;

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
    @FXML private Label toastLabel;

    /** Carrier for a completed order result — success or structured error. */
    private record OrderResult(boolean success, long baristaXp, int baristaLevel,
                                String errorType, String errorMessage) {}

    private final ObservableList<OrderItem> currentOrder = FXCollections.observableArrayList();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    @FXML
    public void initialize() {
        orderListView.setItems(currentOrder);
        orderListView.setCellFactory(lv -> new OrderItemCell(currentOrder, this::updateTotal));
        updateTotal();
        loadBaristaHeader();
        fetchRecipesAndStock();
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

    private void showToast(String message) {
        toastLabel.setText(message);
        toastLabel.setOpacity(1.0);
        toastLabel.setVisible(true);

        PauseTransition hold = new PauseTransition(Duration.seconds(2.5));
        FadeTransition fade = new FadeTransition(Duration.seconds(0.7), toastLabel);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);
        fade.setOnFinished(ev -> toastLabel.setVisible(false));

        new SequentialTransition(hold, fade).play();
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

    private record RecipeData(List<Recipe> recipes, Map<String, Double> stockMap) {}

    private void fetchRecipesAndStock() {
        Task<RecipeData> task = new Task<>() {
            @Override
            protected RecipeData call() throws Exception {
                // Fetch stock levels
                HttpRequest stockReq = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8181/api/stock"))
                        .GET().build();
                HttpResponse<String> stockResp = httpClient.send(stockReq, HttpResponse.BodyHandlers.ofString());
                List<StockItem> stockItems = mapper.readValue(stockResp.body(), new TypeReference<>() {});
                Map<String, Double> stockMap = stockItems.stream()
                        .collect(Collectors.toMap(StockItem::getName, StockItem::getCurrentStock));

                // Fetch recipes (includes ingredients list)
                HttpRequest recipeReq = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8181/api/recipes"))
                        .GET().build();
                HttpResponse<String> recipeResp = httpClient.send(recipeReq, HttpResponse.BodyHandlers.ofString());
                List<Recipe> recipes = mapper.readValue(recipeResp.body(), new TypeReference<>() {});

                return new RecipeData(recipes, stockMap);
            }
        };

        task.setOnSucceeded(e -> populateButtons(task.getValue().recipes(), task.getValue().stockMap()));
        task.setOnFailed(e -> {
            Label error = new Label("Could not load recipes: " + task.getException().getMessage());
            error.setStyle("-fx-text-fill: red; -fx-font-size: 13px;");
            recipePane.getChildren().add(error);
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void populateButtons(List<Recipe> recipes, Map<String, Double> stockMap) {
        for (Recipe recipe : recipes) {
            boolean inStock = recipe.isInStock(stockMap);
            Button btn = new Button(recipe.getName() + "\n$" + String.format("%.2f", recipe.getPrice()));
            btn.setPrefWidth(130);
            btn.setPrefHeight(80);

            if (inStock) {
                btn.setStyle(
                    "-fx-font-size: 13px; -fx-background-color: #3498db; " +
                    "-fx-text-fill: white; -fx-background-radius: 8; -fx-cursor: hand;"
                );
                btn.setOnAction(ev -> addToOrder(recipe));
            } else {
                btn.setDisable(true);
                btn.setStyle(
                    "-fx-font-size: 13px; -fx-background-color: #bdc3c7; " +
                    "-fx-text-fill: #7f8c8d; -fx-background-radius: 8;"
                );
                btn.setTooltip(new Tooltip("Out of stock"));
            }

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

        Task<OrderResult> task = new Task<>() {
            @Override
            protected OrderResult call() throws Exception {
                String body = mapper.writeValueAsString(payload);
                HttpRequest postRequest = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8181/api/brew/order"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build();
                HttpResponse<String> response = httpClient.send(postRequest, HttpResponse.BodyHandlers.ofString());

                var node = mapper.readTree(response.body());

                if (response.statusCode() >= 200 && response.statusCode() < 300) {
                    return new OrderResult(true,
                            node.get("baristaXp").asLong(),
                            node.get("baristaLevel").asInt(),
                            null, null);
                }

                // Parse structured error from backend ErrorResponse
                return new OrderResult(false, 0, 0,
                        node.path("error").asText("Error"),
                        node.path("message").asText("Something went wrong."));
            }
        };

        task.setOnSucceeded(e -> {
            OrderResult result = task.getValue();
            completeOrderBtn.setDisable(false);

            if (!result.success()) {
                if ("Insufficient Stock".equals(result.errorType())) {
                    String ingredient = result.errorMessage().replace("Not enough stock for ", "");
                    showToast("Out of stock: " + ingredient + ". Please choose another option.");
                } else {
                    orderStatusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #c0392b;");
                    orderStatusLabel.setText("Error: " + result.errorMessage());
                }
                return;
            }

            int oldLevel = barista.getLevel();
            int newLevel = result.baristaLevel();
            barista.setTotalXp((int) result.baristaXp());
            barista.setLevel(newLevel);
            AppState.setCurrentBarista(barista);
            loadBaristaHeader();
            currentOrder.clear();
            updateTotal();
            orderStatusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #27ae60;");
            orderStatusLabel.setText("Order completed!");
            if (newLevel > oldLevel) {
                showLevelUp(newLevel);
            }
        });

        task.setOnFailed(e -> {
            completeOrderBtn.setDisable(false);
            orderStatusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #c0392b;");
            orderStatusLabel.setText("Connection error. Check the server.");
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }
}
