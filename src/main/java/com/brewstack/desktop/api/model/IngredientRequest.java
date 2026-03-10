package com.brewstack.desktop.api.model;

public class IngredientRequest {
    private Long ingredientId;
    private Double quantity;

    public IngredientRequest(Long ingredientId, Double quantity) {
        this.ingredientId = ingredientId;
        this.quantity = quantity;
    }

    public Long getIngredientId() { return ingredientId; }
    public Double getQuantity() { return quantity; }
}
