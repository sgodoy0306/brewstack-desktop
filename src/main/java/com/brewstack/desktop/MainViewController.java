package com.brewstack.desktop;

import com.brewstack.desktop.api.BrewApiClient;
import com.brewstack.desktop.api.model.Barista;
import com.brewstack.desktop.api.model.Recipe;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.util.Duration;

import java.util.List;

public class MainViewController {

    @FXML private FlowPane menuPane;
    @FXML private Label baristaNameLabel;
    @FXML private Label baristaLevelLabel;
    @FXML private ComboBox<Barista> baristaComboBox;
    @FXML private ProgressBar xpProgressBar;
    @FXML private Label xpLabel;

    private final BrewApiClient apiClient = new BrewApiClient();

    // Kept in sync after each load and each completed order
    private Map<String, Double> stockMap = new HashMap<>();
    private List<Recipe> loadedRecipes = new ArrayList<>();
    private final Map<Long, RecipeCard> recipeCards = new HashMap<>();

    @FXML
    public void initialize() {
        try {
            baristaComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) fetchAndDisplayBarista(newVal.getId());
            });
            loadBaristas();
            loadMenu();
        } catch (Exception e) {
            System.err.println("Failed to initialize MainViewController: " + e.getMessage());
        }
    }

    // ── Barista ──────────────────────────────────────────────────────────────

    private void loadBaristas() {
        new Thread(() -> {
            try {
                List<Barista> baristas = apiClient.getBaristas();
                Platform.runLater(() -> baristaComboBox.getItems().setAll(baristas));
            } catch (Exception e) {
                System.err.println("Could not load baristas: " + e.getMessage());
            }
        }).start();
    }

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

    private void updateBaristaPanel(Barista barista) {
        int level = computeLevel(barista.getTotalXp());
        long xpForCurrentLevel = xpRequiredForLevel(level);
        long xpForNextLevel    = xpRequiredForLevel(level + 1);
        long xpIntoLevel       = barista.getTotalXp() - xpForCurrentLevel;
        long xpNeeded          = xpForNextLevel - xpForCurrentLevel;
        double progress        = (double) xpIntoLevel / xpNeeded;

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
        btn.setOnAction(e -> handleSale(recipe, btn));
        return btn;
    }

    private void handleSale(Recipe recipe, Button btn) {
        btn.setDisable(true);
        new Thread(() -> {
            try {
                apiClient.processSale(recipe.getId());
                Platform.runLater(() -> flashButton(btn, true));
            } catch (Exception e) {
                System.err.println("Sale failed for " + recipe.getName() + ": " + e.getMessage());
                Platform.runLater(() -> flashButton(btn, false));
            }
        }).start();
    }

    private void flashButton(Button btn, boolean success) {
        String flashColor = success ? "#27ae60" : "#e74c3c";
        String baseColor  = "#2980b9";
        String baseStyle  =
            "-fx-text-fill: white; -fx-font-size: 13px;" +
            "-fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand;";

        btn.setStyle("-fx-background-color: " + flashColor + "; " + baseStyle);

        PauseTransition pause = new PauseTransition(Duration.millis(600));
        pause.setOnFinished(e -> {
            btn.setStyle("-fx-background-color: " + baseColor + "; " + baseStyle);
            btn.setDisable(false);
        });
        pause.play();
    }

    private void showMenuError(String message) {
        Label error = new Label("Could not load menu: " + message);
        error.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 13px;");
        menuPane.getChildren().add(error);
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
