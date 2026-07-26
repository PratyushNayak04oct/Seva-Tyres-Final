package com.sevatyres.model;

/**
 * Inventory item with stock status support.
 */
public class InventoryItem {
    private int id;
    private String name;
    private String sku;
    private String category;
    private int quantity;
    private int reorderLevel;
    private double unitPrice;
    private String barcode;

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

    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }

    /** @deprecated Use getStockStatus() instead */
    public boolean isLowStock() {
        return quantity > 0 && quantity < 10;
    }

    public enum StockStatus { OUT_OF_STOCK, LOW_STOCK, IN_STOCK }

    public StockStatus getStockStatus() {
        if (quantity == 0) return StockStatus.OUT_OF_STOCK;
        if (quantity < 10) return StockStatus.LOW_STOCK;
        return StockStatus.IN_STOCK;
    }

    @Override
    public String toString() {
        return name + "  (₹" + String.format("%.2f", unitPrice) + "/unit)";
    }
}
