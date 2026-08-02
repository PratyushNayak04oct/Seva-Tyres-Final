package com.sevatyres.model;

import java.time.LocalDate;

/**
 * Unified row for the Transactions page — either a receivable (sale) or payable.
 */
public class TxnListEntry {

    public enum Type { RECEIVABLE, PAYABLE }

    private final Type type;
    private final LocalDate date;
    private final String number;
    private final String party;
    private final String details;
    private final String brand;
    private final Integer quantity;
    private final String payment;
    private final double amount;
    private final Double netProfit;
    private final SaleTransaction sale;
    private final PayableTransaction payable;

    private TxnListEntry(Type type, LocalDate date, String number, String party, String details,
                         String brand, Integer quantity, String payment, double amount,
                         Double netProfit, SaleTransaction sale, PayableTransaction payable) {
        this.type = type;
        this.date = date;
        this.number = number;
        this.party = party;
        this.details = details;
        this.brand = brand;
        this.quantity = quantity;
        this.payment = payment;
        this.amount = amount;
        this.netProfit = netProfit;
        this.sale = sale;
        this.payable = payable;
    }

    public static TxnListEntry fromSale(SaleTransaction s) {
        return new TxnListEntry(
                Type.RECEIVABLE,
                s.getSaleDate(),
                s.getBillNo(),
                s.getCustomerName() != null && !s.getCustomerName().isBlank() ? s.getCustomerName() : "—",
                s.getParticulars() != null ? s.getParticulars() : "",
                s.getBrand(),
                s.getQuantity(),
                s.getPaymentSummary(),
                s.getTotal(),
                s.getNetProfit(),
                s,
                null
        );
    }

    public static TxnListEntry fromPayable(PayableTransaction p) {
        return new TxnListEntry(
                Type.PAYABLE,
                p.getTxnDate(),
                p.getTxnNumber(),
                p.getPaidTo() != null ? p.getPaidTo() : "—",
                p.getNotes() != null ? p.getNotes() : "",
                null,
                null,
                "Paid out",
                p.getAmount(),
                null,
                null,
                p
        );
    }

    public Type getType() { return type; }
    public boolean isReceivable() { return type == Type.RECEIVABLE; }
    public boolean isPayable() { return type == Type.PAYABLE; }
    public LocalDate getDate() { return date; }
    public String getNumber() { return number; }
    public String getParty() { return party; }
    public String getDetails() { return details; }
    public String getBrand() { return brand; }
    public Integer getQuantity() { return quantity; }
    public String getPayment() { return payment; }
    public double getAmount() { return amount; }
    public Double getNetProfit() { return netProfit; }
    public SaleTransaction getSale() { return sale; }
    public PayableTransaction getPayable() { return payable; }

    public String getTypeLabel() {
        return isPayable() ? "Payable" : "Receivable";
    }
}
