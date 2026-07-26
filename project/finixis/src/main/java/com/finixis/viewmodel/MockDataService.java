package com.finixis.viewmodel;

import com.finixis.model.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates realistic in-memory mock data at startup. This is static/mock only —
 * NOT a persistence layer and NOT a stand-in for a future DB design.
 */
public class MockDataService {

    private final List<User> users = new ArrayList<>();
    private final List<Customer> customers = new ArrayList<>();
    private final List<InventoryItem> inventory = new ArrayList<>();
    private final List<Transaction> transactions = new ArrayList<>();
    private final List<Credit> credits = new ArrayList<>();
    private final List<Invoice> invoices = new ArrayList<>();
    private final List<GeneratedFile> generatedFiles = new ArrayList<>();

    private int customerIdSeq = 100;
    private int creditIdSeq = 200;
    private int txnIdSeq = 300;
    private int itemIdSeq = 400;
    private int invoiceIdSeq = 600;

    public MockDataService() {
        seed();
    }

    private void seed() {
        // --- Staff users (for profile page) ---
        users.add(new User(501, "Amelia Hart",  "amelia.hart@finixis.com",  "+1 (415) 555-0142", Role.ADMIN,    true));
        users.add(new User(502, "Daniel Osei",  "daniel.osei@finixis.com",  "+1 (415) 555-0177", Role.MANAGER,  true));
        users.add(new User(503, "Priya Raman",  "priya.raman@finixis.com",  "+1 (415) 555-0193", Role.EMPLOYEE, true));

        // --- Customers (~12, varied balances + customer-since dates) ---
        addCustomer("Atlas Building Supplies", "+1 (212) 555-0110", "billing@atlasbuild.com",
                "84 Whitestone Blvd, Bronx, NY", LocalDate.of(2019, 3, 12), 4850.00);
        addCustomer("Cedar & Co. Cafe", "+1 (503) 555-0144", "owner@cedarco.cafe",
                "17 SE Hawthorne Blvd, Portland, OR", LocalDate.of(2021, 6, 28), -1230.50);
        addCustomer("Marisol Vega", "+1 (305) 555-0188", "marisol.vega@gmail.com",
                "410 Ocean Dr, Miami, FL", LocalDate.of(2023, 1, 19), 320.75);
        addCustomer("Northwind Logistics", "+1 (312) 555-0121", "accounts@northwind.co",
                "9 Dockyard Rd, Chicago, IL", LocalDate.of(2018, 11, 5), -6720.00);
        addCustomer("Pinecrest Garden Center", "+1 (206) 555-0156", "info@pinecrestgc.com",
                "62 Greenway Ln, Seattle, WA", LocalDate.of(2022, 4, 14), 0.00);
        addCustomer("Riverside Auto Parts", "+1 (614) 555-0133", "ap@riversideauto.com",
                "3 Industrial Pkwy, Columbus, OH", LocalDate.of(2020, 9, 2), 2150.25);
        addCustomer("Sienna Bakery", "+1 (718) 555-0167", "hello@siennabakery.com",
                "28 Smith St, Brooklyn, NY", LocalDate.of(2024, 2, 8), -540.00);
        addCustomer("Thompson & Sons Hardware", "+1 (617) 555-0119", "sales@thompsonhardware.com",
                "55 Main St, Worcester, MA", LocalDate.of(2017, 7, 21), 9875.40);
        addCustomer("Urban Bloom Florist", "+1 (415) 555-0150", "orders@urbanbloom.co",
                "120 Valencia St, San Francisco, CA", LocalDate.of(2023, 8, 30), -312.80);
        addCustomer("Vertex Construction", "+1 (404) 555-0172", "finance@vertexconst.com",
                "440 Peachtree St, Atlanta, GA", LocalDate.of(2019, 12, 11), 12400.00);
        addCustomer("Wavelength Records", "+1 (512) 555-0185", "shop@wavelengthrecords.com",
                "7 E 6th St, Austin, TX", LocalDate.of(2022, 10, 17), 180.00);
        addCustomer("Yusuf Al-Rashid", "+1 (702) 555-0148", "yusuf.rashid@outlook.com",
                "910 Sahara Ave, Las Vegas, NV", LocalDate.of(2024, 5, 3), -8940.00);

        // --- Inventory (~12 items, a few low-stock) ---
        addItem("Cement Mix 25kg", "CMT-025", "Building Materials", 140, 30, 12.50);
        addItem("Plywood Sheet 8x4", "PLY-084", "Building Materials", 22, 25, 34.00);
        addItem("Brick Paver Red", "BPR-RED", "Building Materials", 600, 100, 1.20);
        addItem("Galvanized Nail 2in", "NGL-2IN", "Fasteners", 1500, 500, 0.08);
        addItem("Pipe PVC 20mm", "PVC-020", "Plumbing", 18, 20, 4.75);
        addItem("Copper Fitting 90deg", "CFI-90D", "Plumbing", 320, 80, 2.40);
        addItem("LED Bulb 9W", "LED-009", "Electrical", 440, 100, 3.10);
        addItem("Wire Roll 14AWG 50m", "WIR-14A", "Electrical", 12, 15, 21.00);
        addItem("Paint Primer White 5L", "PRM-5LW", "Paint & Finish", 86, 20, 28.90);
        addItem("Brush Set 6pc", "BRS-6PC", "Tools", 9, 12, 15.75);
        addItem("Tape Measure 5m", "TMS-005", "Tools", 130, 30, 8.40);
        addItem("Safety Goggles", "GOG-001", "Safety Gear", 8, 25, 6.25);

        // --- Credits (some settled, some pending) ---
        addCredit(1, "Cedar & Co. Cafe", 1230.50, "Bulk coffee beans — 60kg",
                LocalDate.of(2024, 8, 15), LocalDate.of(2024, 9, 15), false);
        addCredit(2, "Northwind Logistics", 6720.00, "Forklift parts & service",
                LocalDate.of(2024, 9, 1), LocalDate.of(2024, 10, 1), false);
        addCredit(3, "Sienna Bakery", 540.00, "Commercial oven refurb",
                LocalDate.of(2024, 9, 20), LocalDate.of(2024, 10, 20), false);
        addCredit(4, "Urban Bloom Florist", 312.80, "Vase & ribbon bulk order",
                LocalDate.of(2024, 10, 5), LocalDate.of(2024, 11, 5), false);
        addCredit(5, "Yusuf Al-Rashid", 8940.00, "Home renovation materials",
                LocalDate.of(2024, 6, 10), LocalDate.of(2024, 7, 10), false);
        addCredit(6, "Atlas Building Supplies", 1500.00, "Roofing tiles — partial",
                LocalDate.of(2024, 5, 12), LocalDate.of(2024, 6, 12), true);
        addCredit(7, "Vertex Construction", 2200.00, "Scaffolding rental Q1",
                LocalDate.of(2024, 3, 18), LocalDate.of(2024, 4, 18), true);

        // --- Transactions (~24, spread across dates, some ongoing) ---
        addTxn(1, "Atlas Building Supplies", Transaction.Type.CREDIT, 4850.00,
                "Cement & aggregates — Q3 order", LocalDate.now().minusDays(1), true);
        addTxn(1, "Atlas Building Supplies", Transaction.Type.PAYMENT, 3350.00,
                "Partial payment — bank transfer", LocalDate.now().minusDays(8), false);
        addTxn(1, "Atlas Building Supplies", Transaction.Type.CREDIT, 1500.00,
                "Roofing tiles — partial", LocalDate.of(2024, 5, 12), false);

        addTxn(2, "Cedar & Co. Cafe", Transaction.Type.CREDIT, 1230.50,
                "Bulk coffee beans — 60kg", LocalDate.now().minusDays(2), true);
        addTxn(2, "Cedar & Co. Cafe", Transaction.Type.PAYMENT, 400.00,
                "Card payment", LocalDate.now().minusDays(14), false);

        addTxn(3, "Marisol Vega", Transaction.Type.PAYMENT, 320.75,
                "Garden supplies settlement", LocalDate.now().minusDays(3), false);

        addTxn(4, "Northwind Logistics", Transaction.Type.CREDIT, 6720.00,
                "Forklift parts & service", LocalDate.now().minusDays(6), true);
        addTxn(4, "Northwind Logistics", Transaction.Type.PAYMENT, 2000.00,
                "Cheque deposit", LocalDate.now().minusDays(20), false);

        addTxn(5, "Pinecrest Garden Center", Transaction.Type.PAYMENT, 180.00,
                "Potting soil — settled", LocalDate.now().minusDays(4), false);

        addTxn(6, "Riverside Auto Parts", Transaction.Type.CREDIT, 2150.25,
                "Brake pads & rotors — 50pc", LocalDate.now().minusDays(2), true);
        addTxn(6, "Riverside Auto Parts", Transaction.Type.PAYMENT, 1200.00,
                "Partial payment", LocalDate.now().minusDays(25), false);

        addTxn(7, "Sienna Bakery", Transaction.Type.CREDIT, 540.00,
                "Commercial oven refurb", LocalDate.now().minusDays(9), true);

        addTxn(8, "Thompson & Sons Hardware", Transaction.Type.CREDIT, 4875.40,
                "Fastener & tool restock", LocalDate.now().minusDays(5), true);
        addTxn(8, "Thompson & Sons Hardware", Transaction.Type.PAYMENT, 5000.00,
                "Full settlement — wire", LocalDate.now().minusDays(35), false);
        addTxn(8, "Thompson & Sons Hardware", Transaction.Type.CREDIT, 5000.00,
                "Seasonal inventory replenish", LocalDate.of(2024, 4, 10), false);

        addTxn(9, "Urban Bloom Florist", Transaction.Type.CREDIT, 312.80,
                "Vase & ribbon bulk order", LocalDate.now().minusDays(11), true);

        addTxn(10, "Vertex Construction", Transaction.Type.CREDIT, 5400.00,
                "Concrete — 40 cubic meters", LocalDate.now().minusDays(1), true);
        addTxn(10, "Vertex Construction", Transaction.Type.PAYMENT, 7000.00,
                "Advance payment", LocalDate.now().minusDays(16), false);
        addTxn(10, "Vertex Construction", Transaction.Type.CREDIT, 2200.00,
                "Scaffolding rental Q1", LocalDate.of(2024, 3, 18), false);

        addTxn(11, "Wavelength Records", Transaction.Type.PAYMENT, 180.00,
                "Vinyl display shelves", LocalDate.now().minusDays(6), false);

        addTxn(12, "Yusuf Al-Rashid", Transaction.Type.CREDIT, 8940.00,
                "Home renovation materials", LocalDate.now().minusDays(3), true);
        addTxn(12, "Yusuf Al-Rashid", Transaction.Type.PAYMENT, 1000.00,
                "Cash deposit", LocalDate.now().minusDays(28), false);

        addTxn(8, "Thompson & Sons Hardware", Transaction.Type.DEBIT, 320.00,
                "Returned goods credit", LocalDate.now().minusDays(40), false);

        // --- Invoices (2 samples) ---
        invoices.add(new Invoice(601, "INV-2024-0142", 10, "Vertex Construction",
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(29),
                5400.00, 432.00, 5832.00,
                java.util.List.of(
                        new Invoice.LineItem("Concrete — 40 cubic meters", 40, 135.00, 5400.00)
                )));
        invoices.add(new Invoice(602, "INV-2024-0143", 1, "Atlas Building Supplies",
                LocalDate.now().minusDays(2), LocalDate.now().plusDays(28),
                4850.00, 388.00, 5238.00,
                java.util.List.of(
                        new Invoice.LineItem("Cement Mix 25kg", 100, 12.50, 1250.00),
                        new Invoice.LineItem("Plywood Sheet 8x4", 20, 34.00, 680.00),
                        new Invoice.LineItem("Brick Paver Red", 200, 1.20, 240.00),
                        new Invoice.LineItem("Roofing materials (assorted)", 1, 2680.00, 2680.00)
                )));

        // --- Pre-seed generated files so Reports page isn't empty on launch ---
        generatedFiles.addAll(FileGenerationService.seedSampleFiles(transactions, invoices));
    }

    // --- seq helpers ---
    private void addCustomer(String name, String phone, String email, String address,
                             LocalDate since, double balance) {
        customers.add(new Customer(++customerIdSeq, name, phone, email, address, since, balance));
    }

    private void addItem(String name, String sku, String category, int qty,
                         int reorder, double price) {
        inventory.add(new InventoryItem(++itemIdSeq, name, sku, category, qty, reorder, price));
    }

    private void addCredit(int customerId, String customerName, double amount,
                           String desc, LocalDate issued, LocalDate due, boolean settled) {
        credits.add(new Credit(++creditIdSeq, customerId, customerName, amount,
                desc, issued, due, settled));
    }

    private void addTxn(int customerId, String customerName, Transaction.Type type,
                        double amount, String desc, LocalDate date, boolean ongoing) {
        transactions.add(new Transaction(++txnIdSeq, customerId, customerName, type,
                amount, desc, date, ongoing));
    }

    // --- accessors ---
    public List<User> getUsers() { return users; }
    public List<Customer> getCustomers() { return customers; }
    public List<InventoryItem> getInventory() { return inventory; }
    public List<Transaction> getTransactions() { return transactions; }
    public List<Credit> getCredits() { return credits; }
    public List<Invoice> getInvoices() { return invoices; }
    public List<GeneratedFile> getGeneratedFiles() { return generatedFiles; }

    public void addGeneratedFile(GeneratedFile f) { generatedFiles.add(f); }

    public int nextCustomerId() { return ++customerIdSeq; }
    public int nextCreditId() { return ++creditIdSeq; }
    public int nextTxnId() { return ++txnIdSeq; }
    public int nextItemId() { return ++itemIdSeq; }
    public int nextInvoiceId() { return ++invoiceIdSeq; }

    public Customer findCustomer(int id) {
        return customers.stream().filter(c -> c.getId() == id).findFirst().orElse(null);
    }

    public List<Transaction> transactionsFor(int customerId) {
        return transactions.stream().filter(t -> t.getCustomerId() == customerId).toList();
    }
}
