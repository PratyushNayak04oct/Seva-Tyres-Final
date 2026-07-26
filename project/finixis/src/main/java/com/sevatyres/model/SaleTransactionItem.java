package com.sevatyres.model;

/**
 * One line item inside a Sale_Transaction (multi-item bill).
 */
public class SaleTransactionItem {

    private int     saleItemId;
    private int     saleId;
    private Integer inventoryId;
    private String  itemName;
    private int     quantity;
    private double  unitPrice;
    private double  lineTotal;

    public SaleTransactionItem() {}

    public SaleTransactionItem(Integer inventoryId, String itemName, int quantity, double unitPrice) {
        this.inventoryId = inventoryId;
        this.itemName    = itemName;
        this.quantity    = quantity;
        this.unitPrice   = unitPrice;
        this.lineTotal   = quantity * unitPrice;
    }

    public int     getSaleItemId()              { return saleItemId; }
    public void    setSaleItemId(int v)         { this.saleItemId = v; }
    public int     getSaleId()                  { return saleId; }
    public void    setSaleId(int v)             { this.saleId = v; }
    public Integer getInventoryId()             { return inventoryId; }
    public void    setInventoryId(Integer v)    { this.inventoryId = v; }
    public String  getItemName()                { return itemName; }
    public void    setItemName(String v)        { this.itemName = v; }
    public int     getQuantity()                { return quantity; }
    public void    setQuantity(int v)           { this.quantity = v; recalc(); }
    public double  getUnitPrice()               { return unitPrice; }
    public void    setUnitPrice(double v)       { this.unitPrice = v; recalc(); }
    public double  getLineTotal()               { return lineTotal; }
    public void    setLineTotal(double v)       { this.lineTotal = v; }

    private void recalc() { this.lineTotal = quantity * unitPrice; }
}
