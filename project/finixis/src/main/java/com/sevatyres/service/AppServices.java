package com.sevatyres.service;

import com.sevatyres.db.DatabaseConfig;

/**
 * Central service registry — initialised once at startup.
 */
public final class AppServices {

    private static CustomerService        customers;
    private static InventoryService       inventory;
    private static TransactionService     transactions;
    private static SaleTransactionService saleTransactions;
    private static ReportService          reports;
    private static EmailService           email;
    private static AlertService           alerts;

    private AppServices() {}

    /** Call once from App.start() before the first controller is loaded. */
    public static void init() {
        DatabaseConfig.init();
        customers        = new CustomerService();
        inventory        = new InventoryService();
        transactions     = new TransactionService();
        saleTransactions = new SaleTransactionService();
        reports          = new ReportService();
        email            = new EmailService();
        alerts           = new AlertService();
        email.startReminderScheduler(customers, transactions);
    }

    public static CustomerService       customers()        { return customers; }
    public static InventoryService      inventory()        { return inventory; }
    public static TransactionService    transactions()     { return transactions; }
    public static SaleTransactionService saleTransactions(){ return saleTransactions; }
    public static ReportService         reports()          { return reports; }
    public static EmailService          email()            { return email; }
    public static AlertService          alerts()           { return alerts; }

    public static void shutdown() {
        if (email != null) email.shutdown();
        DatabaseConfig.shutdown();
    }
}
