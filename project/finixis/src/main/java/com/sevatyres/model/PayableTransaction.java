package com.sevatyres.model;

import java.time.LocalDate;

/** Money paid out by the shop (payables). */
public class PayableTransaction {
    private int id;
    private String txnNumber;   // 6-digit unique
    private LocalDate txnDate;
    private String paidTo;
    private double amount;
    private String notes;

    public PayableTransaction() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTxnNumber() { return txnNumber; }
    public void setTxnNumber(String txnNumber) { this.txnNumber = txnNumber; }

    public LocalDate getTxnDate() { return txnDate; }
    public void setTxnDate(LocalDate txnDate) { this.txnDate = txnDate; }

    public String getPaidTo() { return paidTo; }
    public void setPaidTo(String paidTo) { this.paidTo = paidTo; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
