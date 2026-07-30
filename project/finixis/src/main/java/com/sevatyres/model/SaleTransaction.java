package com.sevatyres.model;

import java.time.LocalDate;

/**
 * Represents a sales transaction at Seva Tyres.
 * A single transaction can have multiple payment methods.
 */
public class SaleTransaction {

    private int id;
    private String billNo;
    private LocalDate saleDate;
    private String particulars;
    private String brand;
    private int quantity;
    private double unitPrice;       // price per unit (from inventory or manual)
    private Integer inventoryItemId; // FK to Inventory (null if not linked)

    // Payment methods
    private double phonePe;         // UPI payment
    private double accountTransfer; // bank account transfer
    private double cardSwipe;       // card payment
    private double bajajFinance;    // Bajaj Finance EMI
    private double cash;            // cash payment
    private double cheque;          // cheque payment
    private double creditAmount;    // amount on credit (customer owes)

    // Tax (applied on item subtotal)
    private double subtotal;        // sum of line items before tax
    private double taxAmount;       // total tax amount
    private String taxLabel;        // e.g. "GST (18%)" or "CGST + SGST"

    // Computed total = subtotal + taxAmount
    private double total;

    // Optional customer info (for invoice; mandatory if credit > 0)
    private Integer customerId;     // null if not registered
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String customerAddress;

    public SaleTransaction() {}

    // ─── Getters / Setters ─────────────────────────────────────────────

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getBillNo() { return billNo; }
    public void setBillNo(String billNo) { this.billNo = billNo; }

    public LocalDate getSaleDate() { return saleDate; }
    public void setSaleDate(LocalDate saleDate) { this.saleDate = saleDate; }

    public String getParticulars() { return particulars; }
    public void setParticulars(String particulars) { this.particulars = particulars; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }

    public Integer getInventoryItemId() { return inventoryItemId; }
    public void setInventoryItemId(Integer inventoryItemId) { this.inventoryItemId = inventoryItemId; }

    public double getPhonePe() { return phonePe; }
    public void setPhonePe(double phonePe) { this.phonePe = phonePe; }

    public double getAccountTransfer() { return accountTransfer; }
    public void setAccountTransfer(double accountTransfer) { this.accountTransfer = accountTransfer; }

    public double getCardSwipe() { return cardSwipe; }
    public void setCardSwipe(double cardSwipe) { this.cardSwipe = cardSwipe; }

    public double getBajajFinance() { return bajajFinance; }
    public void setBajajFinance(double bajajFinance) { this.bajajFinance = bajajFinance; }

    public double getCash() { return cash; }
    public void setCash(double cash) { this.cash = cash; }

    public double getCheque() { return cheque; }
    public void setCheque(double cheque) { this.cheque = cheque; }

    public double getCreditAmount() { return creditAmount; }
    public void setCreditAmount(double creditAmount) { this.creditAmount = creditAmount; }

    public double getSubtotal() { return subtotal; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }

    public double getTaxAmount() { return taxAmount; }
    public void setTaxAmount(double taxAmount) { this.taxAmount = taxAmount; }

    public String getTaxLabel() { return taxLabel; }
    public void setTaxLabel(String taxLabel) { this.taxLabel = taxLabel; }

    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }

    /**
     * Recomputes total and credit amount.
     * Prefer explicit subtotal + tax when set; else unitPrice × qty; else payment sum.
     */
    public double computeTotal() {
        if (subtotal > 0 || taxAmount > 0) {
            total = subtotal + taxAmount;
        } else if (unitPrice > 0 && quantity > 0) {
            subtotal = unitPrice * quantity;
            total = subtotal + taxAmount;
        } else {
            total = phonePe + accountTransfer + cardSwipe + bajajFinance + cash + cheque + creditAmount;
        }
        double paid = getPaidAmount();
        creditAmount = Math.max(0, total - paid);
        return total;
    }

    /** Item subtotal before tax (legacy helper). */
    public double getItemTotal() {
        return subtotal > 0 ? subtotal : unitPrice * quantity;
    }

    /** Amount paid so far (all methods except credit). */
    public double getPaidAmount() {
        return phonePe + accountTransfer + cardSwipe + bajajFinance + cash + cheque;
    }

    /** Remaining unpaid balance against grand total (incl. tax). */
    public double getRemaining() { return Math.max(0, getTotal() - getPaidAmount()); }

    public Integer getCustomerId() { return customerId; }
    public void setCustomerId(Integer customerId) { this.customerId = customerId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }

    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

    public String getCustomerAddress() { return customerAddress; }
    public void setCustomerAddress(String customerAddress) { this.customerAddress = customerAddress; }

    /** True if this transaction has credit (customer owes money). */
    public boolean hasCredit() { return creditAmount > 0; }

    /** Readable summary of payment methods used. */
    public String getPaymentSummary() {
        StringBuilder sb = new StringBuilder();
        if (phonePe > 0)         append(sb, "PhonePe");
        if (accountTransfer > 0) append(sb, "A/C Transfer");
        if (cardSwipe > 0)       append(sb, "Card");
        if (bajajFinance > 0)    append(sb, "Bajaj Finance");
        if (cash > 0)            append(sb, "Cash");
        if (cheque > 0)          append(sb, "Cheque");
        if (creditAmount > 0)    append(sb, "Credit");
        return sb.isEmpty() ? "—" : sb.toString();
    }

    private static void append(StringBuilder sb, String s) {
        if (!sb.isEmpty()) sb.append(", ");
        sb.append(s);
    }
}
