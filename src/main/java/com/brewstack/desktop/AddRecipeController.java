package com.brewstack.desktop;

import com.brewstack.desktop.api.BrewApiClient;
import com.brewstack.desktop.api.model.CreateRecipeRequest;
import com.brewstack.desktop.api.model.IngredientDTO;
import com.brewstack.desktop.api.model.IngredientRequest;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class AddRecipeController {

    @FXML private TextField nameField;
    @FXML private TextField priceField;
    @FXML private TextField xpField;
    @FXML private TextField imageUrlField;
    @FXML private VBox ingredientListBox;
    @FXML private Label ingredientsStatusLabel;
    @FXML private Label statusLabel;
    @FXML private Button saveBtn;

    private final BrewApiClient apiClient = new BrewApiClient();

    /** Each entry tracks: the ingredient, its checkbox, and its qty field. */
    private record IngredientRow(IngredientDTO ingredient, CheckBox checkBox, TextField qtyField) {}
    private final List<IngredientRow> ingredientRows = new ArrayList<>();

    @FXML
    public void initialize() {
        loadIngredients();
    }

    private void loadIngredients() {
        ingredientsStatusLabel.setText("Loading ingredients…");
        ingredientListBox.getChildren().clear();
        ingredientRows.clear();

        new Thread(() -> {
            try {
                List<IngredientDTO> ingredients = apiClient.getIngredients();
                Platform.runLater(() -> populateIngredients(ingredients));
            } catch (Exception e) {
                Platform.runLater(() -> ingredientsStatusLabel.setText("Could not load ingredients: " + e.getMessage()));
            }
        }).start();
    }

    private void populateIngredients(List<IngredientDTO> ingredients) {
        ingredientRows.clear();
        ingredientListBox.getChildren().clear();

        if (ingredients.isEmpty()) {
            ingredientsStatusLabel.setText("No ingredients found in inventory.");
            return;
        }

        ingredientsStatusLabel.setText("");

        for (IngredientDTO ing : ingredients) {
            CheckBox checkBox = new CheckBox(ing.getName() + " (" + ing.getUnit() + ")");
            checkBox.setStyle("-fx-text-fill: #3d2c1e; -fx-font-size: 13px;");
            HBox.setHgrow(checkBox, Priority.ALWAYS);

            TextField qtyField = new TextField();
            qtyField.setPromptText("qty");
            qtyField.setPrefWidth(80);
            qtyField.setDisable(true);
            qtyField.setStyle(fieldStyle());

            checkBox.selectedProperty().addListener((obs, wasSelected, isSelected) ->
                    qtyField.setDisable(!isSelected));

            HBox row = new HBox(12, checkBox, qtyField);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setStyle("-fx-padding: 4 0 4 0;");

            ingredientRows.add(new IngredientRow(ing, checkBox, qtyField));
            ingredientListBox.getChildren().add(row);
        }
    }

    @FXML
    private void onRefreshIngredients() {
        loadIngredients();
    }

    @FXML
    private void onSave() {
        statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #9a8470;");
        statusLabel.setText("Saving…");
        saveBtn.setDisable(true);

        String name = nameField.getText().trim();
        if (name.isEmpty()) { showError("Recipe name is required."); return; }

        BigDecimal price;
        try { price = new BigDecimal(priceField.getText().trim()); }
        catch (NumberFormatException e) { showError("Enter a valid price."); return; }

        int xp;
        try { xp = Integer.parseInt(xpField.getText().trim()); }
        catch (NumberFormatException e) { showError("Enter a valid XP value."); return; }

        List<IngredientRequest> ingredients = new ArrayList<>();
        for (IngredientRow row : ingredientRows) {
            if (!row.checkBox().isSelected()) continue;
            String qtyText = row.qtyField().getText().trim();
            if (qtyText.isEmpty()) { showError("Enter a quantity for: " + row.ingredient().getName()); return; }
            double qty;
            try { qty = Double.parseDouble(qtyText); }
            catch (NumberFormatException e) { showError("Invalid quantity for: " + row.ingredient().getName()); return; }
            ingredients.add(new IngredientRequest(row.ingredient().getId(), qty));
        }
        if (ingredients.isEmpty()) { showError("Select at least one ingredient."); return; }

        String imageUrl = imageUrlField.getText().trim();
        if (imageUrl.isEmpty()) imageUrl = null;

        CreateRecipeRequest request = new CreateRecipeRequest(name, xp, price, imageUrl, ingredients);

        new Thread(() -> {
            try {
                apiClient.createRecipe(request);
                Platform.runLater(() -> navigate("MainView.fxml"));
            } catch (Exception e) {
                Platform.runLater(() -> showError(e.getMessage()));
            }
        }).start();
    }

    @FXML
    private void onBack() {
        navigate("MainView.fxml");
    }

    private void navigate(String fxml) {
        try {
            FXMLLoader loader = new FXMLLoader(App.class.getResource(fxml));
            Stage stage = (Stage) nameField.getScene().getWindow();
            stage.setScene(new Scene(loader.load(), 1100, 720));
        } catch (Exception e) {
            showError("Navigation error: " + e.getMessage());
        }
    }

    private void showError(String message) {
        saveBtn.setDisable(false);
        statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #a05030;");
        statusLabel.setText(message);
    }

    private String fieldStyle() {
        return "-fx-background-color: #faf7f2; -fx-border-color: #e0d5c5; " +
               "-fx-border-radius: 8; -fx-background-radius: 8; " +
               "-fx-font-size: 13px; -fx-padding: 8 12 8 12;";
    }
}
