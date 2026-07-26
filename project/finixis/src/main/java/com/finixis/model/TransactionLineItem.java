package com.finixis.model;

/**
 * One item row inside a Record Payment (Transaction_Credit_Item in DB).
 */
public class TransactionLineItem {
    private int    lineItemId;
    private int    transactionId;
    private int    itemId;
    private String itemName;
    private int    quantity;
    private double unitPriceSnapshot;
    private double lineTotal;

    public TransactionLineItem() {}

    public TransactionLineItem(int itemId, String itemName, int quantity, double unitPriceSnapshot) {
        this.itemId            = itemId;
        this.itemName          = itemName;
        this.quantity          = quantity;
        this.unitPriceSnapshot = unitPriceSnapshot;
        this.lineTotal         = quantity * unitPriceSnapshot;
    }

    public int    getLineItemId()         { return lineItemId; }
    public void   setLineItemId(int v)    { this.lineItemId = v; }
    public int    getTransactionId()      { return transactionId; }
    public void   setTransactionId(int v) { this.transactionId = v; }
    public int    getItemId()             { return itemId; }
    public void   setItemId(int v)        { this.itemId = v; }
    public String getItemName()           { return itemName; }
    public void   setItemName(String v)   { this.itemName = v; }
    public int    getQuantity()           { return quantity; }
    public void   setQuantity(int v)      { this.quantity = v; this.lineTotal = v * unitPriceSnapshot; }
    public double getUnitPriceSnapshot()  { return unitPriceSnapshot; }
    public void   setUnitPriceSnapshot(double v) { this.unitPriceSnapshot = v; this.lineTotal = quantity * v; }
    public double getLineTotal()          { return lineTotal; }
    public void   setLineTotal(double v)  { this.lineTotal = v; }
}
