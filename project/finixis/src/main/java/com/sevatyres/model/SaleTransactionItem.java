package com.sevatyres.model;

/**
 * One line item inside a Sale_Transaction (multi-item bill).
 * unitPrice is tax-inclusive; rate/cgst/sgst are segregated for invoice.
 */
public class SaleTransactionItem {

    private int     saleItemId;
    private int     saleId;
    private Integer inventoryId;
    private String  itemName;
    private int     quantity;
    private double  unitPrice;      // inclusive unit price
    private double  lineTotal;      // inclusive line total
    private String  hsnSac;
    private String  itemType;       // PRODUCT / SERVICE / TYRE
    private String  rimSize;
    private double  rate;           // taxable unit (exclusive)
    private double  cgstAmount;     // line CGST
    private double  sgstAmount;     // line SGST
    private double  taxableAmount;  // line taxable = rate * qty

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

    public String getHsnSac() { return hsnSac; }
    public void setHsnSac(String hsnSac) { this.hsnSac = hsnSac; }
    public String getItemType() { return itemType; }
    public void setItemType(String itemType) { this.itemType = itemType; }
    public String getRimSize() { return rimSize; }
    public void setRimSize(String rimSize) { this.rimSize = rimSize; }
    public double getRate() { return rate; }
    public void setRate(double rate) { this.rate = rate; }
    public double getCgstAmount() { return cgstAmount; }
    public void setCgstAmount(double cgstAmount) { this.cgstAmount = cgstAmount; }
    public double getSgstAmount() { return sgstAmount; }
    public void setSgstAmount(double sgstAmount) { this.sgstAmount = sgstAmount; }
    public double getTaxableAmount() { return taxableAmount; }
    public void setTaxableAmount(double taxableAmount) { this.taxableAmount = taxableAmount; }

    private void recalc() { this.lineTotal = quantity * unitPrice; }

    /** Description line for invoice (includes rim for tyres). */
    public String getInvoiceDescription() {
        if (rimSize != null && !rimSize.isBlank()) {
            return itemName + "\nRim: " + rimSize;
        }
        return itemName != null ? itemName : "";
    }
}
