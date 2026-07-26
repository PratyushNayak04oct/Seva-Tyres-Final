package com.finixis.service;

import com.finixis.db.DatabaseConfig;

/**
 * Central service registry — initialised once at startup.
 * Controllers obtain services via AppServices.customers(), .inventory(), etc.
 */
public final class AppServices {

    private static CustomerService    customers;
    private static InventoryService   inventory;
    private static TransactionService transactions;
    private static ReportService      reports;

    private AppServices() {}

    /** Call once from App.start() before the first controller is loaded. */
    public static void init() {
        DatabaseConfig.init();
        customers    = new CustomerService();
        inventory    = new InventoryService();
        transactions = new TransactionService();
        reports      = new ReportService();
    }

    public static CustomerService    customers()    { return customers; }
    public static InventoryService   inventory()    { return inventory; }
    public static TransactionService transactions() { return transactions; }
    public static ReportService      reports()      { return reports; }

    public static void shutdown() {
        DatabaseConfig.shutdown();
    }
}
