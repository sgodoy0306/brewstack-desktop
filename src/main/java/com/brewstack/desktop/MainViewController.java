package com.brewstack.desktop;

import com.brewstack.desktop.api.BrewApiClient;
import com.brewstack.desktop.api.model.Barista;
import com.brewstack.desktop.api.model.OrderSummaryDTO;
import com.brewstack.desktop.api.model.Recipe;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.animation.PauseTransition;
import javafx.scene.control.Button;
import javafx.fxml.FXMLLoader;
import javafx.util.Duration;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.FlowPane;
import javafx.concurrent.Task;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MainViewController {

    @FXML private FlowPane menuPane;
    @FXML private Label baristaNameLabel;
    @FXML private Label baristaLevelLabel;
    @FXML private ProgressBar xpProgressBar;
    @FXML private Label xpLabel;
    @FXML private Label levelUpLabel;

    private int currentBaristaLevel = 0;

    @FXML private ListView<OrderItem> orderListView;
    @FXML private Button completeOrderBtn;
    @FXML private Label totalPriceLabel;
    @FXML private Label orderStatusLabel;

    private final BrewApiClient apiClient = new BrewApiClient();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    private Map<String, Double> stockMap = new HashMap<>();
    private List<Recipe> loadedRecipes = new ArrayList<>();
    private final Map<Long, RecipeCard> recipeCards = new HashMap<>();
    private final ObservableList<OrderItem> currentOrder = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        orderListView.setItems(currentOrder);
        orderListView.setCellFactory(lv -> new OrderItemCell(currentOrder, this::updateOrderTotal));

        com.brewstack.desktop.Barista current = AppState.getCurrentBarista();
        if (current != null) {
            fetchAndDisplayBarista(current.getId());
        }
        loadMenu();
    }

    // ── Barista ──────────────────────────────────────────────────────────────

    private void fetchAndDisplayBarista(Long id) {
        new Thread(() -> {
            try {
                Barista barista = apiClient.getBarista(id);
                Platform.runLater(() -> updateBaristaPanel(barista));
            } catch (Exception e) {
                System.err.println("Could not fetch barista: " + e.getMessage());
            }
        }).start();
    }

    private void updateBaristaFromSummary(OrderSummaryDTO summary) {
        long totalXp = summary.getBaristaXp();
        int level    = summary.getBaristaLevel();
        long xpForCurrentLevel = xpRequiredForLevel(level);
        long xpForNextLevel    = xpRequiredForLevel(level + 1);
        long xpIntoLevel       = totalXp - xpForCurrentLevel;
        long xpNeeded          = xpForNextLevel - xpForCurrentLevel;
        double progress        = xpNeeded > 0 ? (double) xpIntoLevel / xpNeeded : 1.0;

        boolean leveledUp = currentBaristaLevel > 0 && level > currentBaristaLevel;
        currentBaristaLevel = level;

        baristaLevelLabel.setText(String.valueOf(level));
        xpProgressBar.setProgress(progress);
        xpLabel.setText(xpIntoLevel + " / " + xpNeeded + " XP");

        if (leveledUp) {
            levelUpLabel.setVisible(true);
            PauseTransition hide = new PauseTransition(Duration.seconds(4));
            hide.setOnFinished(e -> levelUpLabel.setVisible(false));
            hide.play();
        }
    }

    private void updateBaristaPanel(Barista barista) {
        int level = computeLevel(barista.getTotalXp());
        long xpForCurrentLevel = xpRequiredForLevel(level);
        long xpForNextLevel    = xpRequiredForLevel(level + 1);
        long xpIntoLevel       = barista.getTotalXp() - xpForCurrentLevel;
        long xpNeeded          = xpForNextLevel - xpForCurrentLevel;
        double progress        = (double) xpIntoLevel / xpNeeded;

        currentBaristaLevel = level;
        baristaNameLabel.setText(barista.getName());
        baristaLevelLabel.setText(String.valueOf(level));
        xpProgressBar.setProgress(progress);
        xpLabel.setText(xpIntoLevel + " / " + xpNeeded + " XP");
    }

    /** Level formula: floor(sqrt(totalXp / 100)) + 1 */
    private int computeLevel(long totalXp) {
        return (int) Math.floor(Math.sqrt(totalXp / 100.0)) + 1;
    }

    /** Minimum totalXp to reach a given level: ((level - 1)^2) * 100 */
    private long xpRequiredForLevel(int level) {
        long l = level - 1;
        return l * l * 100;
    }

    // ── Menu ─────────────────────────────────────────────────────────────────

    private void loadMenu() {
        new Thread(() -> {
            try {
                List<Recipe> recipes = apiClient.getMenu();
                Platform.runLater(() -> populateMenu(recipes));
            } catch (Exception e) {
                Platform.runLater(() -> showMenuError(e.getMessage()));
            }
        }).start();
    }

    private void populateMenu(List<Recipe> recipes) {
        loadedRecipes = recipes;
        recipeCards.clear();
        menuPane.getChildren().clear();
        for (Recipe recipe : recipes) {
            RecipeCard card = new RecipeCard(recipe, () -> addToOrder(recipe));
            recipeCards.put(recipe.getId(), card);
            menuPane.getChildren().add(card.getRoot());
        }
    }

    private void showMenuError(String message) {
        Label error = new Label("Could not load menu: " + message);
        error.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 13px;");
        menuPane.getChildren().add(error);
    }

    // ── Order ─────────────────────────────────────────────────────────────────

    private void addToOrder(Recipe recipe) {
        for (OrderItem item : currentOrder) {
            if (item.getRecipe().getId().equals(recipe.getId())) {
                item.increment();
                int idx = currentOrder.indexOf(item);
                currentOrder.set(idx, item);
                updateOrderTotal();
                return;
            }
        }
        currentOrder.add(new OrderItem(recipe));
        updateOrderTotal();
    }

    private void updateOrderTotal() {
        double total = currentOrder.stream().mapToDouble(OrderItem::lineTotal).sum();
        totalPriceLabel.setText(String.format("Total:  $%.2f", total));
    }

    @FXML
    public void onCompleteOrder() {
        if (currentOrder.isEmpty()) return;
        completeOrderBtn.setDisable(true);
        orderStatusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #5a4a7a;");
        orderStatusLabel.setText("Processing…");

        com.brewstack.desktop.Barista current = AppState.getCurrentBarista();
        Long baristaId = current != null ? current.getId() : null;

        // Build flat list of recipe IDs, repeating for quantity
        List<Long> recipeIds = new ArrayList<>();
        for (OrderItem item : currentOrder) {
            for (int i = 0; i < item.getQuantity(); i++) {
                recipeIds.add(item.getRecipe().getId());
            }
        }
        List<OrderItem> snapshot = new ArrayList<>(currentOrder);

        new Thread(() -> {
            try {
                OrderSummaryDTO summary = apiClient.processOrder(recipeIds, baristaId);
                Platform.runLater(() -> {
                    currentOrder.clear();
                    updateOrderTotal();
                    orderStatusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #27ae60;");
                    orderStatusLabel.setText("Order completed!");
                    completeOrderBtn.setDisable(false);
                    syncStockAfterOrder();
                    updateBaristaFromSummary(summary);
                });
            } catch (Exception e) {
                System.err.println("Order failed: " + e.getMessage());
                Platform.runLater(() -> {
                    orderStatusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #c0392b;");
                    orderStatusLabel.setText("Order failed: " + e.getMessage());
                    completeOrderBtn.setDisable(false);
                });
            }
        }).start();
    }

    // ── Stock sync ────────────────────────────────────────────────────────────

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
            refreshRecipeCards();
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void refreshRecipeCards() {
        for (Recipe recipe : loadedRecipes) {
            RecipeCard card = recipeCards.get(recipe.getId());
            if (card != null) {
                card.setOutOfStock(!isInStock(recipe));
            }
        }
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    @FXML
    private void onAddRecipe() {
        navigate("AddRecipeView.fxml");
    }

    @FXML
    private void onChangeBarista() {
        navigate("BaristaSelection.fxml");
    }

    @FXML
    private void onViewStock() {
        navigate("StockView.fxml");
    }

    @FXML
    private void onViewHistory() {
        navigate("DailyHistoryView.fxml");
    }

    private void navigate(String fxml) {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource(fxml));
            Stage stage = (Stage) menuPane.getScene().getWindow();
            stage.setScene(new Scene(loader.load(), 1100, 720));
        } catch (Exception e) {
            System.err.println("Navigation error: " + e.getMessage());
        }
    }

    private boolean isInStock(Recipe recipe) {
        if (recipe.getIngredients() == null || recipe.getIngredients().isEmpty()) return true;
        return recipe.getIngredients().stream().allMatch(ing -> {
            Double available = stockMap.get(ing.getIngredientName());
            return available != null
                    && ing.getQuantityRequired() != null
                    && available >= ing.getQuantityRequired();
        });
    }
}
