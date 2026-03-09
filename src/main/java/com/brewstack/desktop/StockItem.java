package com.brewstack.desktop;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StockItem {
    private long id;
    private String name;
    private double currentStock;
    private double minimumThreshold;
    private String unit;

    public StockItem() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public double getCurrentStock() { return currentStock; }
    public void setCurrentStock(double currentStock) { this.currentStock = currentStock; }
    public double getMinimumThreshold() { return minimumThreshold; }
    public void setMinimumThreshold(double minimumThreshold) { this.minimumThreshold = minimumThreshold; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public boolean isLow() { return currentStock <= minimumThreshold; }
}
