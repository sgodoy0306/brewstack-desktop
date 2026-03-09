package com.brewstack.desktop;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Recipe {
    private long id;
    private String name;
    private double price;
    private List<RecipeIngredient> ingredients = Collections.emptyList();

    public Recipe() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
    public List<RecipeIngredient> getIngredients() { return ingredients; }
    public void setIngredients(List<RecipeIngredient> ingredients) { this.ingredients = ingredients; }

    /** Returns true if all required ingredients have enough stock. */
    public boolean isInStock(Map<String, Double> stockMap) {
        return ingredients.stream().allMatch(ing -> {
            Double available = stockMap.get(ing.getIngredientName());
            return available != null && available >= ing.getQuantityRequired();
        });
    }

    @Override
    public String toString() {
        return String.format("%s  —  $%.2f", name, price);
    }
}
