package com.sevatyres.model;

/** One tax line applied to a sale transaction. */
public class SaleTaxLine {

    private int saleTaxId;
    private int saleId;
    private Integer taxId;
    private String taxName;
    private double taxRate;
    private double taxAmount;

    public SaleTaxLine() {}

    public SaleTaxLine(Integer taxId, String taxName, double taxRate, double taxAmount) {
        this.taxId = taxId;
        this.taxName = taxName;
        this.taxRate = taxRate;
        this.taxAmount = taxAmount;
    }

    public int getSaleTaxId() { return saleTaxId; }
    public void setSaleTaxId(int saleTaxId) { this.saleTaxId = saleTaxId; }

    public int getSaleId() { return saleId; }
    public void setSaleId(int saleId) { this.saleId = saleId; }

    public Integer getTaxId() { return taxId; }
    public void setTaxId(Integer taxId) { this.taxId = taxId; }

    public String getTaxName() { return taxName; }
    public void setTaxName(String taxName) { this.taxName = taxName; }

    public double getTaxRate() { return taxRate; }
    public void setTaxRate(double taxRate) { this.taxRate = taxRate; }

    public double getTaxAmount() { return taxAmount; }
    public void setTaxAmount(double taxAmount) { this.taxAmount = taxAmount; }

    public String getLabel() {
        return (taxName != null ? taxName : "Tax") + " (" + String.format("%.2f", taxRate) + "%)";
    }
}
