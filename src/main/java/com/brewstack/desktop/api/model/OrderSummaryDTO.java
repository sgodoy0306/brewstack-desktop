package com.brewstack.desktop.api.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderSummaryDTO {
    private List<String> brewedRecipes;
    private BigDecimal totalRevenue;
    private int totalOrders;
    private long baristaXp;
    private int baristaLevel;

    public List<String> getBrewedRecipes() { return brewedRecipes; }
    public void setBrewedRecipes(List<String> brewedRecipes) { this.brewedRecipes = brewedRecipes; }

    public BigDecimal getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }

    public int getTotalOrders() { return totalOrders; }
    public void setTotalOrders(int totalOrders) { this.totalOrders = totalOrders; }

    public long getBaristaXp() { return baristaXp; }
    public void setBaristaXp(long baristaXp) { this.baristaXp = baristaXp; }

    public int getBaristaLevel() { return baristaLevel; }
    public void setBaristaLevel(int baristaLevel) { this.baristaLevel = baristaLevel; }
}
