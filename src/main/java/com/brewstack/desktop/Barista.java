package com.brewstack.desktop;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Barista {
    private long id;
    private String name;
    private int level;
    private int totalXp;

    public Barista() {}

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public int getTotalXp() { return totalXp; }
    public void setTotalXp(int totalXp) { this.totalXp = totalXp; }

    /**
     * XP required to complete the current level.
     * Backend formula: level = floor(sqrt(totalXp / 100)) + 1
     * → XP needed for level n = (2n - 1) * 100
     */
    public int xpForCurrentLevel() {
        return (2 * level - 1) * 100;
    }

    /**
     * Cumulative XP at the start of the current level.
     * → XP at level n start = (n - 1)² * 100
     */
    private int xpAtLevelStart() {
        return (level - 1) * (level - 1) * 100;
    }

    /** XP earned within the current level. */
    public int xpInCurrentLevel() {
        return Math.max(0, totalXp - xpAtLevelStart());
    }

    /** Progress ratio [0.0 – 1.0] within the current level. */
    public double xpProgress() {
        return Math.min(1.0, (double) xpInCurrentLevel() / xpForCurrentLevel());
    }
}
