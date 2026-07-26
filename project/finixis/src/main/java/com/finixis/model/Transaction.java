package com.finixis.model;

import java.time.LocalDate;

public class Transaction {
    public enum Type { CREDIT, DEBIT, PAYMENT }

    private int id;
    private int customerId;
    private String customerName;
    private Type type;
    private double amount;      // totalAmount for CREDIT/PAYMENT; full amount for DEBIT
    private double paidAmount;  // amount actually paid (CREDIT/PAYMENT only)
    private double balance;     // amount - paidAmount (CREDIT/PAYMENT); same as amount for DEBIT
    private String description;
    private LocalDate date;
    private boolean ongoing;    // false = settled / all cleared

    public Transaction() {}

    public Transaction(int id, int customerId, String customerName, Type type,
                       double amount, String description, LocalDate date, boolean ongoing) {
        this.id           = id;
        this.customerId   = customerId;
        this.customerName = customerName;
        this.type         = type;
        this.amount       = amount;
        this.paidAmount   = type == Type.DEBIT ? 0 : amount;
        this.balance      = 0;
        this.description  = description;
        this.date         = date;
        this.ongoing      = ongoing;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public double getPaidAmount() { return paidAmount; }
    public void setPaidAmount(double paidAmount) { this.paidAmount = paidAmount; }

    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public boolean isOngoing() { return ongoing; }
    public void setOngoing(boolean ongoing) { this.ongoing = ongoing; }
}
