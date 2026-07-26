package com.finixis.model;

import java.time.LocalDate;

public class Credit {
    private int id;
    private int customerId;
    private String customerName;
    private double amount;
    private String description;
    private LocalDate dateIssued;
    private LocalDate dueDate;
    private boolean settled;

    public Credit() {
    }

    public Credit(int id, int customerId, String customerName, double amount,
                  String description, LocalDate dateIssued, LocalDate dueDate, boolean settled) {
        this.id = id;
        this.customerId = customerId;
        this.customerName = customerName;
        this.amount = amount;
        this.description = description;
        this.dateIssued = dateIssued;
        this.dueDate = dueDate;
        this.settled = settled;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getDateIssued() { return dateIssued; }
    public void setDateIssued(LocalDate dateIssued) { this.dateIssued = dateIssued; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public boolean isSettled() { return settled; }
    public void setSettled(boolean settled) { this.settled = settled; }
}
