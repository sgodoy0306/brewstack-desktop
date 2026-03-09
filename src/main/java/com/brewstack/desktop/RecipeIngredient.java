package com.brewstack.desktop;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RecipeIngredient {
    private String ingredientName;
    private String unit;
    private double quantityRequired;

    public RecipeIngredient() {}

    public String getIngredientName() { return ingredientName; }
    public void setIngredientName(String ingredientName) { this.ingredientName = ingredientName; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public double getQuantityRequired() { return quantityRequired; }
    public void setQuantityRequired(double quantityRequired) { this.quantityRequired = quantityRequired; }
}
