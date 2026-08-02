package com.sevatyres.model;

/**
 * Inventory item with stock status support.
 * unitPrice is tax-inclusive RCP / list price (CGST 9% + SGST 9%).
 * billingAmount is the manual selling price used on bills (falls back to unitPrice if unset).
 */
public class InventoryItem {
    private int id;
    private String name;
    private String brand;
    private String sku;
    private String category;
    private int quantity;
    private int reorderLevel;
    private double unitPrice;
    private String barcode;
    private String hsnSac;
    private String itemType; // PRODUCT, SERVICE, TYRE
    private String rimSize;
    /** Tyre size e.g. 165 65 R14 79H */
    private String tyreSize;
    private String pattern;
    /** TL / TT etc. */
    private String tyreKind;
    private String productCode;
    /** MRP including GST */
    private double mrp;
    /** Manual billing / selling amount (incl. tax). Entered in the app only. */
    private double billingAmount;
    /** Optional link to Purchase_Info */
    private Integer purchaseId;

    public InventoryItem() {}

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

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

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

    public String getHsnSac() { return hsnSac; }
    public void setHsnSac(String hsnSac) { this.hsnSac = hsnSac; }

    public String getItemType() { return itemType; }
    public void setItemType(String itemType) { this.itemType = itemType; }

    public String getRimSize() { return rimSize; }
    public void setRimSize(String rimSize) { this.rimSize = rimSize; }

    public String getTyreSize() { return tyreSize; }
    public void setTyreSize(String tyreSize) { this.tyreSize = tyreSize; }

    public String getPattern() { return pattern; }
    public void setPattern(String pattern) { this.pattern = pattern; }

    public String getTyreKind() { return tyreKind; }
    public void setTyreKind(String tyreKind) { this.tyreKind = tyreKind; }

    public String getProductCode() { return productCode; }
    public void setProductCode(String productCode) { this.productCode = productCode; }

    public double getMrp() { return mrp; }
    public void setMrp(double mrp) { this.mrp = mrp; }

    public double getBillingAmount() { return billingAmount; }
    public void setBillingAmount(double billingAmount) { this.billingAmount = billingAmount; }

    /**
     * Price used when selling: billing amount if set, otherwise RCP / unit price.
     */
    public double getSellingPrice() {
        return billingAmount > 0.009 ? billingAmount : unitPrice;
    }

    public Integer getPurchaseId() { return purchaseId; }
    public void setPurchaseId(Integer purchaseId) { this.purchaseId = purchaseId; }

    public boolean isTyre() {
        return itemType != null && itemType.equalsIgnoreCase("TYRE");
    }

    /** @deprecated Use {@link #getStockStatus()} instead */
    @Deprecated
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
        return name + "  (\u20b9" + String.format("%.2f", getSellingPrice()) + "/unit)";
    }
}
