package com.brewstack.desktop;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StockItem {
    private String name;
    private double currentStock;
    private double minimumThreshold;

    public StockItem() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public double getCurrentStock() { return currentStock; }
    public void setCurrentStock(double currentStock) { this.currentStock = currentStock; }
    public double getMinimumThreshold() { return minimumThreshold; }
    public void setMinimumThreshold(double minimumThreshold) { this.minimumThreshold = minimumThreshold; }
}
