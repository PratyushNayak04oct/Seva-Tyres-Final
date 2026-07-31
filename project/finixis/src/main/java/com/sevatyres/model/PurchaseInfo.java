package com.sevatyres.model;

/** Buying / purchase price record for an inventory item. */
public class PurchaseInfo {
    private int id;
    private Integer inventoryId;
    private String itemName;
    private double buyingPrice;
    private String notes;

    public PurchaseInfo() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Integer getInventoryId() { return inventoryId; }
    public void setInventoryId(Integer inventoryId) { this.inventoryId = inventoryId; }

    public String getItemName() { return itemName; }
    public void setItemName(String itemName) { this.itemName = itemName; }

    public double getBuyingPrice() { return buyingPrice; }
    public void setBuyingPrice(double buyingPrice) { this.buyingPrice = buyingPrice; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
