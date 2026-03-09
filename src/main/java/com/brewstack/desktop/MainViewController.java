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
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
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

    /** Holds the visual components of a recipe card that change with stock state. */
    private record RecipeCard(StackPane root, Rectangle outOfStockOverlay, Label outOfStockBadge) {}

    private final ObservableList<OrderItem> currentOrder = FXCollections.observableArrayList();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    // Kept in sync after each load and each completed order
    private Map<String, Double> stockMap = new HashMap<>();
    private List<Recipe> loadedRecipes = new ArrayList<>();
    private final Map<Long, RecipeCard> recipeCards = new HashMap<>();

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
    private void onStock() {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource("StockView.fxml"));
            Stage stage = (Stage) recipePane.getScene().getWindow();
            stage.setScene(new Scene(loader.load(), 1100, 720));
        } catch (Exception e) {
            showError("Navigation Error", e.getMessage() != null ? e.getMessage() : e.getClass().getName());
        }
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
                HttpRequest stockReq = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8181/api/stock"))
                        .GET().build();
                HttpResponse<String> stockResp = httpClient.send(stockReq, HttpResponse.BodyHandlers.ofString());
                List<StockItem> stockItems = mapper.readValue(stockResp.body(), new TypeReference<>() {});
                Map<String, Double> stockMap = stockItems.stream()
                        .collect(Collectors.toMap(StockItem::getName, StockItem::getCurrentStock));

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

    private void populateButtons(List<Recipe> recipes, Map<String, Double> stock) {
        this.stockMap = stock;
        this.loadedRecipes = recipes;
        recipeCards.clear();

        for (Recipe recipe : recipes) {
            RecipeCard card = buildRecipeCard(recipe);
            recipeCards.put(recipe.getId(), card);
            recipePane.getChildren().add(card.root());
        }
        refreshRecipeButtons();
    }

    private RecipeCard buildRecipeCard(Recipe recipe) {
        StackPane card = new StackPane();
        card.setPrefWidth(170);
        card.setPrefHeight(215);

        // Clip to rounded rectangle
        Rectangle clip = new Rectangle(170, 215);
        clip.setArcWidth(16);
        clip.setArcHeight(16);
        card.setClip(clip);

        // Background: image or solid fallback
        Image image = loadImageForRecipe(recipe.getName());
        if (image != null) {
            ImageView iv = new ImageView(image);
            iv.setFitWidth(170);
            iv.setFitHeight(215);
            iv.setPreserveRatio(false);
            card.getChildren().add(iv);
        } else {
            Rectangle fallback = new Rectangle(170, 215, Color.web("#2c3e50"));
            card.getChildren().add(fallback);
        }

        // Gradient overlay — transparent at top, dark at bottom for text legibility
        LinearGradient gradient = new LinearGradient(
                0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0.3, Color.TRANSPARENT),
                new Stop(1.0, Color.rgb(0, 0, 0, 0.85))
        );
        Rectangle gradientRect = new Rectangle(170, 215, gradient);
        card.getChildren().add(gradientRect);

        // Out-of-stock dim overlay (hidden by default)
        Rectangle outOfStockOverlay = new Rectangle(170, 215, Color.rgb(10, 10, 10, 0.6));
        outOfStockOverlay.setVisible(false);
        card.getChildren().add(outOfStockOverlay);

        // Recipe name
        Label nameLabel = new Label(recipe.getName());
        nameLabel.setStyle(
            "-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-wrap-text: true;"
        );
        nameLabel.setMaxWidth(146);

        // Price
        Label priceLabel = new Label(String.format("$%.2f", recipe.getPrice()));
        priceLabel.setStyle(
            "-fx-text-fill: #f5c518; -fx-font-size: 12px; -fx-font-weight: bold;"
        );

        VBox textBox = new VBox(3, nameLabel, priceLabel);
        textBox.setAlignment(Pos.BOTTOM_LEFT);
        textBox.setPadding(new Insets(0, 12, 12, 12));
        StackPane.setAlignment(textBox, Pos.BOTTOM_LEFT);
        card.getChildren().add(textBox);

        // Out-of-stock badge (hidden by default)
        Label outOfStockBadge = new Label("Out of stock");
        outOfStockBadge.setStyle(
            "-fx-text-fill: #6a1a1a; -fx-font-size: 11px; -fx-font-weight: bold; " +
            "-fx-background-color: rgba(240,181,181,0.95); -fx-background-radius: 12; " +
            "-fx-padding: 4 12 4 12;"
        );
        outOfStockBadge.setVisible(false);
        StackPane.setAlignment(outOfStockBadge, Pos.CENTER);
        card.getChildren().add(outOfStockBadge);

        // Hover effect
        card.setOnMouseEntered(e -> { card.setScaleX(1.05); card.setScaleY(1.05); });
        card.setOnMouseExited(e ->  { card.setScaleX(1.0);  card.setScaleY(1.0);  });

        card.setStyle("-fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 10, 0, 0, 3);");

        return new RecipeCard(card, outOfStockOverlay, outOfStockBadge);
    }

    /** Tries to load an image from the images/ resource folder by normalizing the recipe name. */
    private Image loadImageForRecipe(String recipeName) {
        String normalized = recipeName.toLowerCase().replaceAll("[^a-z0-9]", "");
        for (String ext : new String[]{".jpg", ".jpeg", ".png"}) {
            var url = App.class.getResource("images/" + normalized + ext);
            if (url != null) return new Image(url.toExternalForm(), 170, 215, false, true);
        }
        return null;
    }

    /** Re-evaluates every recipe card against the current stockMap. */
    private void refreshRecipeButtons() {
        for (Recipe recipe : loadedRecipes) {
            RecipeCard card = recipeCards.get(recipe.getId());
            if (card == null) continue;
            boolean inStock = recipe.isInStock(stockMap);

            card.outOfStockOverlay().setVisible(!inStock);
            card.outOfStockBadge().setVisible(!inStock);

            if (inStock) {
                card.root().setStyle("-fx-cursor: hand; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 10, 0, 0, 3);");
                card.root().setOnMouseClicked(ev -> addToOrder(recipe));
                card.root().setOnMouseEntered(e -> { card.root().setScaleX(1.05); card.root().setScaleY(1.05); });
                card.root().setOnMouseExited(e ->  { card.root().setScaleX(1.0);  card.root().setScaleY(1.0);  });
            } else {
                card.root().setStyle("-fx-cursor: default; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 6, 0, 0, 2);");
                card.root().setOnMouseClicked(null);
                card.root().setOnMouseEntered(null);
                card.root().setOnMouseExited(null);
            }
        }
    }

    /**
     * Returns a copy of stockMap with the quantities already in the cart subtracted.
     * Used to prevent adding more items than physically available.
     */
    private Map<String, Double> computeVirtualStock() {
        Map<String, Double> virtual = new HashMap<>(stockMap);
        for (OrderItem item : currentOrder) {
            List<RecipeIngredient> ings = item.getRecipe().getIngredients();
            if (ings == null) continue;
            for (RecipeIngredient ing : ings) {
                virtual.merge(ing.getIngredientName(),
                        -(ing.getQuantityRequired() * item.getQuantity()),
                        Double::sum);
            }
        }
        return virtual;
    }

    private void addToOrder(Recipe recipe) {
        Map<String, Double> virtual = computeVirtualStock();
        List<RecipeIngredient> ings = recipe.getIngredients();
        boolean canAdd = ings == null || ings.isEmpty() ||
                ings.stream().allMatch(ing -> {
                    Double available = virtual.get(ing.getIngredientName());
                    return available != null && available >= ing.getQuantityRequired();
                });

        if (!canAdd) {
            showToast("Not enough stock available for " + recipe.getName() + ".");
            return;
        }

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
            syncStockAfterOrder();
        });

        task.setOnFailed(e -> {
            completeOrderBtn.setDisable(false);
            orderStatusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #c0392b;");
            Throwable ex = task.getException();
            orderStatusLabel.setText("Error: " + (ex != null ? ex.getMessage() : "Unknown error"));
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Re-fetches live stock after an order completes and refreshes all recipe cards.
     */
    private void syncStockAfterOrder() {
        Task<Map<String, Double>> task = new Task<>() {
            @Override
            protected Map<String, Double> call() throws Exception {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:8181/api/stock"))
                        .GET().build();
                HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
                List<StockItem> items = mapper.readValue(resp.body(), new TypeReference<>() {});
                return items.stream()
                        .collect(Collectors.toMap(StockItem::getName, StockItem::getCurrentStock));
            }
        };

        task.setOnSucceeded(e -> {
            stockMap = task.getValue();
            refreshRecipeButtons();
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

}
