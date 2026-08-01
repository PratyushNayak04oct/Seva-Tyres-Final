package com.sevatyres.model;

/** Buying / purchase price record — aligned with dealer price-list columns. */
public class PurchaseInfo {
    private int id;
    private Integer inventoryId;
    private String itemName;
    private String brand;
    private String rimSize;
    private String tyreSize;
    private String pattern;
    /** TL / TT etc. */
    private String tyreKind;
    private String productCode;
    private double buyingPrice;
    /** Recommended Consumer Price incl. GST */
    private double rcp;
    /** MRP incl. GST */
    private double mrp;
    private String notes;

    public PurchaseInfo() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Integer getInventoryId() { return inventoryId; }
    public void setInventoryId(Integer inventoryId) { this.inventoryId = inventoryId; }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

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

    public double getBuyingPrice() { return buyingPrice; }
    public void setBuyingPrice(double buyingPrice) { this.buyingPrice = buyingPrice; }

    public double getRcp() { return rcp; }
    public void setRcp(double rcp) { this.rcp = rcp; }

    public double getMrp() { return mrp; }
    public void setMrp(double mrp) { this.mrp = mrp; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
