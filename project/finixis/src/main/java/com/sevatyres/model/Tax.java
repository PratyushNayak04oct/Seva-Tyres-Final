package com.sevatyres.model;

/**
 * A tax definition that can be applied to sale transactions (e.g. GST 18%).
 */
public class Tax {

    private int id;
    private String name;
    private double rate;       // percentage, e.g. 18.0
    private String description;
    private boolean active = true;

    public Tax() {}

    public Tax(String name, double rate) {
        this.name = name;
        this.rate = rate;
        this.active = true;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getRate() { return rate; }
    public void setRate(double rate) { this.rate = rate; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    /** Display label e.g. "GST (18%)". */
    public String getDisplayLabel() {
        String n = name != null ? name : "Tax";
        return n + " (" + String.format("%.2f", rate) + "%)";
    }

    @Override
    public String toString() {
        return getDisplayLabel();
    }
}
