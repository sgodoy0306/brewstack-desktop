package com.brewstack.desktop;

public class OrderItem {
    private final Recipe recipe;
    private int quantity;

    public OrderItem(Recipe recipe) {
        this.recipe = recipe;
        this.quantity = 1;
    }

    public Recipe getRecipe() { return recipe; }
    public int getQuantity() { return quantity; }
    public void increment() { quantity++; }
    public void decrement() { if (quantity > 0) quantity--; }
    public double lineTotal() { return recipe.getPrice() * quantity; }

    @Override
    public String toString() {
        if (quantity == 1) {
            return String.format("%s  —  $%.2f", recipe.getName(), recipe.getPrice());
        }
        return String.format("%s x%d  —  $%.2f", recipe.getName(), quantity, lineTotal());
    }
}
