package com.brewstack.desktop.api.model;

import java.math.BigDecimal;
import java.util.List;

public class CreateRecipeRequest {
    private String name;
    private Integer baseXpReward;
    private BigDecimal price;
    private String imageUrl;
    private List<IngredientRequest> ingredients;

    public CreateRecipeRequest(String name, Integer baseXpReward, BigDecimal price, String imageUrl, List<IngredientRequest> ingredients) {
        this.name = name;
        this.baseXpReward = baseXpReward;
        this.price = price;
        this.imageUrl = imageUrl;
        this.ingredients = ingredients;
    }

    public String getName() { return name; }
    public Integer getBaseXpReward() { return baseXpReward; }
    public BigDecimal getPrice() { return price; }
    public String getImageUrl() { return imageUrl; }
    public List<IngredientRequest> getIngredients() { return ingredients; }
}
