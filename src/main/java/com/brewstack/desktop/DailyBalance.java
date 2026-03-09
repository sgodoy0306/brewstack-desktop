package com.brewstack.desktop;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DailyBalance {
    private String date;
    private BigDecimal totalRevenue;
    private int totalOrders;

    public DailyBalance() {}

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public BigDecimal getTotalRevenue() { return totalRevenue; }
    public void setTotalRevenue(BigDecimal totalRevenue) { this.totalRevenue = totalRevenue; }
    public int getTotalOrders() { return totalOrders; }
    public void setTotalOrders(int totalOrders) { this.totalOrders = totalOrders; }

    public String getFormattedRevenue() {
        return totalRevenue == null ? "$0.00" : String.format("$%.2f", totalRevenue);
    }
}
