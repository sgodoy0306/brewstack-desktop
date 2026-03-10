package com.brewstack.desktop;

import com.brewstack.desktop.api.model.Recipe;
import javafx.animation.PauseTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/**
 * A 170×215 image-based recipe card for the BrewStack POS menu.
 *
 * Visual layers (bottom to top):
 *   1. Drink photo (or dark-gray fallback)
 *   2. Gradient overlay  (transparent → 85% black)
 *   3. Name + price labels pinned to bottom-left
 *   4. Out-of-stock dim overlay (60% black)
 *   5. Out-of-stock badge (pastel rose)
 */
public class RecipeCard {

    private static final double WIDTH  = 170;
    private static final double HEIGHT = 215;
    private static final double ARC    = 32; // diameter → 16 px corner radius

    private static final String SHADOW_NORMAL  =
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.35), 10, 0, 0, 3);";
    private static final String SHADOW_DIMMED  =
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.20),  6, 0, 0, 3);";

    private final StackPane root;
    private final Rectangle outOfStockOverlay;
    private final Label     outOfStockBadge;

    private Runnable onBrewAction;

    // ── Constructor ────────────────────────────────────────────────────────────

    public RecipeCard(Recipe recipe, Runnable onBrewAction) {
        this.onBrewAction = onBrewAction;

        // 1. Background: drink photo or solid fallback
        StackPane background = buildBackground(recipe.getImageUrl(), recipe.getName());

        // 2. Gradient overlay
        Rectangle gradient = buildGradientOverlay();

        // 3. Name + price text
        VBox textBox = buildTextBox(recipe);

        // 4. Out-of-stock dim overlay (hidden by default)
        outOfStockOverlay = new Rectangle(WIDTH, HEIGHT);
        outOfStockOverlay.setFill(Color.rgb(0, 0, 0, 0.60));
        outOfStockOverlay.setVisible(false);

        // 5. Out-of-stock badge (hidden by default)
        outOfStockBadge = new Label("Out of stock");
        outOfStockBadge.setStyle(
                "-fx-background-color: #f0b5b5; -fx-text-fill: #8b0000;" +
                "-fx-font-size: 11px; -fx-font-weight: bold;" +
                "-fx-background-radius: 8; -fx-padding: 4 8 4 8;");
        outOfStockBadge.setVisible(false);

        // Root
        root = new StackPane(background, gradient, textBox, outOfStockOverlay, outOfStockBadge);
        root.setPrefSize(WIDTH, HEIGHT);
        root.setMaxSize(WIDTH, HEIGHT);

        // Rounded clip
        Rectangle clip = new Rectangle(WIDTH, HEIGHT);
        clip.setArcWidth(ARC);
        clip.setArcHeight(ARC);
        root.setClip(clip);

        root.setStyle(SHADOW_NORMAL + " -fx-cursor: hand;");

        attachHoverAnimation();
        attachClickHandler();
    }

    // ── Public API ─────────────────────────────────────────────────────────────

    /** Returns the JavaFX node to add to a parent container. */
    public StackPane getRoot() { return root; }

    /**
     * Shows or hides the out-of-stock overlay and disables/re-enables click.
     */
    public void setOutOfStock(boolean outOfStock) {
        outOfStockOverlay.setVisible(outOfStock);
        outOfStockBadge.setVisible(outOfStock);

        if (outOfStock) {
            root.setStyle(SHADOW_DIMMED);
            root.setCursor(Cursor.DEFAULT);
            root.setOnMouseClicked(null);
        } else {
            root.setStyle(SHADOW_NORMAL + " -fx-cursor: hand;");
            root.setCursor(Cursor.HAND);
            attachClickHandler();
        }
    }

    /**
     * Temporarily enables or disables interaction (e.g. while a sale is in progress).
     */
    public void setEnabled(boolean enabled) {
        if (enabled) {
            root.setCursor(Cursor.HAND);
            attachClickHandler();
        } else {
            root.setCursor(Cursor.DEFAULT);
            root.setOnMouseClicked(null);
        }
    }

    /**
     * Briefly flashes a green (success) or red (failure) tint on the card,
     * then restores the normal state.
     */
    public void flash(boolean success) {
        Rectangle flashRect = new Rectangle(WIDTH, HEIGHT);
        flashRect.setFill(success ? Color.rgb(39, 174, 96, 0.55)
                                  : Color.rgb(231, 76,  60, 0.55));
        root.getChildren().add(flashRect);

        PauseTransition pause = new PauseTransition(Duration.millis(600));
        pause.setOnFinished(e -> root.getChildren().remove(flashRect));
        pause.play();
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private StackPane buildBackground(String imageUrl, String recipeName) {
        StackPane pane = new StackPane();
        pane.setPrefSize(WIDTH, HEIGHT);

        Image img = loadImageFromUrl(imageUrl);
        if (img == null) img = loadImageFromResources(recipeName);

        if (img != null) {
            ImageView iv = new ImageView(img);
            iv.setFitWidth(WIDTH);
            iv.setFitHeight(HEIGHT);
            iv.setPreserveRatio(false);
            pane.getChildren().add(iv);
        } else {
            pane.setStyle("-fx-background-color: #2c3e50;");
        }
        return pane;
    }

    private Rectangle buildGradientOverlay() {
        Rectangle rect = new Rectangle(WIDTH, HEIGHT);
        rect.setFill(new LinearGradient(
                0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0.00, Color.TRANSPARENT),
                new Stop(0.50, Color.rgb(0, 0, 0, 0.20)),
                new Stop(1.00, Color.rgb(0, 0, 0, 0.85))
        ));
        return rect;
    }

    private VBox buildTextBox(Recipe recipe) {
        Label nameLabel = new Label(recipe.getName());
        nameLabel.setStyle(
                "-fx-text-fill: white; -fx-font-size: 14px; -fx-font-weight: bold; -fx-wrap-text: true;");
        nameLabel.setMaxWidth(WIDTH - 16);

        String priceText = recipe.getPrice() != null
                ? "$" + recipe.getPrice().setScale(2, java.math.RoundingMode.HALF_UP)
                : "N/A";
        Label priceLabel = new Label(priceText);
        priceLabel.setStyle(
                "-fx-text-fill: #f5c518; -fx-font-size: 12px; -fx-font-weight: bold;");

        VBox box = new VBox(2, nameLabel, priceLabel);
        box.setAlignment(Pos.BOTTOM_LEFT);
        StackPane.setAlignment(box, Pos.BOTTOM_LEFT);
        StackPane.setMargin(box, new Insets(0, 0, 10, 10));
        return box;
    }

    private void attachHoverAnimation() {
        ScaleTransition hoverIn = new ScaleTransition(Duration.millis(150), root);
        hoverIn.setToX(1.05);
        hoverIn.setToY(1.05);

        ScaleTransition hoverOut = new ScaleTransition(Duration.millis(150), root);
        hoverOut.setToX(1.0);
        hoverOut.setToY(1.0);

        root.setOnMouseEntered(e -> hoverIn.playFromStart());
        root.setOnMouseExited(e -> hoverOut.playFromStart());
    }

    private void attachClickHandler() {
        root.setOnMouseClicked(e -> { if (onBrewAction != null) onBrewAction.run(); });
    }

    private Image loadImageFromUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) return null;
        try {
            Image img = new Image(imageUrl, WIDTH, HEIGHT, false, true, false);
            if (img.isError()) return null;
            return img;
        } catch (Exception e) {
            return null;
        }
    }

    private Image loadImageFromResources(String recipeName) {
        if (recipeName == null) return null;
        String key = recipeName.toLowerCase().replaceAll("[^a-z0-9]", "");
        for (String ext : new String[]{".jpg", ".jpeg", ".png"}) {
            var url = RecipeCard.class.getResource("images/" + key + ext);
            if (url != null) return new Image(url.toExternalForm(), WIDTH, HEIGHT, false, true);
        }
        return null;
    }
}
