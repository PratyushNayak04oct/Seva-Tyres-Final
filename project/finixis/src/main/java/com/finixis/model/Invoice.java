package com.finixis.model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Invoice {
    private int id;
    private String number;
    private int customerId;
    private String customerName;
    private LocalDate issueDate;
    private LocalDate dueDate;
    private double subtotal;
    private double tax;
    private double total;
    private List<LineItem> lineItems = new ArrayList<>();

    public Invoice() {
    }

    public Invoice(int id, String number, int customerId, String customerName,
                   LocalDate issueDate, LocalDate dueDate, double subtotal, double tax,
                   double total, List<LineItem> lineItems) {
        this.id = id;
        this.number = number;
        this.customerId = customerId;
        this.customerName = customerName;
        this.issueDate = issueDate;
        this.dueDate = dueDate;
        this.subtotal = subtotal;
        this.tax = tax;
        this.total = total;
        this.lineItems = lineItems;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNumber() { return number; }
    public void setNumber(String number) { this.number = number; }

    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public LocalDate getIssueDate() { return issueDate; }
    public void setIssueDate(LocalDate issueDate) { this.issueDate = issueDate; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public double getSubtotal() { return subtotal; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }

    public double getTax() { return tax; }
    public void setTax(double tax) { this.tax = tax; }

    public double getTotal() { return total; }
    public void setTotal(double total) { this.total = total; }

    public List<LineItem> getLineItems() { return lineItems; }
    public void setLineItems(List<LineItem> lineItems) { this.lineItems = lineItems; }

    public static class LineItem {
        private final String description;
        private final int qty;
        private final double unitPrice;
        private final double lineTotal;

        public LineItem(String description, int qty, double unitPrice, double lineTotal) {
            this.description = description;
            this.qty = qty;
            this.unitPrice = unitPrice;
            this.lineTotal = lineTotal;
        }

        public String getDescription() { return description; }
        public int getQty() { return qty; }
        public double getUnitPrice() { return unitPrice; }
        public double getLineTotal() { return lineTotal; }
    }
}
