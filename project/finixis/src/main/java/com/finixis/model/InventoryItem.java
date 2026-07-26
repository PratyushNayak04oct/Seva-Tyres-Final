package com.finixis.model;

public class InventoryItem {
    private int id;
    private String name;
    private String sku;
    private String category;
    private int quantity;
    private int reorderLevel;
    private double unitPrice;

    public InventoryItem() {
    }

    public InventoryItem(int id, String name, String sku, String category,
                         int quantity, int reorderLevel, double unitPrice) {
        this.id = id;
        this.name = name;
        this.sku = sku;
        this.category = category;
        this.quantity = quantity;
        this.reorderLevel = reorderLevel;
        this.unitPrice = unitPrice;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public int getReorderLevel() { return reorderLevel; }
    public void setReorderLevel(int reorderLevel) { this.reorderLevel = reorderLevel; }

    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }

    public boolean isLowStock() {
        return quantity <= reorderLevel;
    }
}
