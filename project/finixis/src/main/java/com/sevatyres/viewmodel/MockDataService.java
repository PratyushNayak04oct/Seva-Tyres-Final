package com.sevatyres.viewmodel;

import com.sevatyres.model.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates realistic in-memory mock data for Seva Tyres at startup.
 * Tyre shop specific: vehicles, tyres, alignment, nitrogen, parts, services.
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
    private int creditIdSeq   = 200;
    private int txnIdSeq      = 300;
    private int itemIdSeq     = 400;
    private int invoiceIdSeq  = 600;

    public MockDataService() {
        // Mock/demo seed data disabled — start with a clean slate for testing.
        // Keep a single default admin so the Users page is not empty.
        users.add(new User(501, "Admin", "admin@sevatyres.com", "", Role.ADMIN, true));
    }

    @SuppressWarnings("unused")
    private void seed() {
        // --- Staff users ---
        users.add(new User(501, "Rajan Mehta",   "rajan@sevatyres.com",    "+91 98765 43210", Role.ADMIN,    true));
        users.add(new User(502, "Priya Sharma",  "priya@sevatyres.com",    "+91 98765 43211", Role.MANAGER,  true));
        users.add(new User(503, "Arjun Patel",   "arjun@sevatyres.com",    "+91 98765 43212", Role.EMPLOYEE, true));

        // --- Customers: vehicle owners & fleet operators ---
        addCustomer("Ramesh Verma",          "+91 94501 11001", "ramesh.v@gmail.com",
                "12, MG Road, Bangalore", LocalDate.of(2021, 4, 10), 4500.00);
        addCustomer("Sunita Transport Co.",  "+91 94501 22002", "billing@sunitatransport.com",
                "Plot 44, Industrial Area, Pune", LocalDate.of(2020, 8, 15), -18000.00);
        addCustomer("Karan Auto Spares",     "+91 94501 33003", "karan.auto@gmail.com",
                "Shop 7, Auto Market, Delhi", LocalDate.of(2022, 1, 20), 2200.00);
        addCustomer("Meena Logistics",       "+91 94501 44004", "accounts@meenalogistics.com",
                "Warehouse 3, Nasik Rd, Mumbai", LocalDate.of(2019, 11, 5), -9500.00);
        addCustomer("Vijay Kumar",           "+91 94501 55005", "vijay.k@outlook.com",
                "45 Anna Salai, Chennai", LocalDate.of(2023, 6, 18), 0.00);
        addCustomer("Nikhil Fleet Services","+91 94501 66006", "nikhil@fleetservices.in",
                "78 Hiranandani, Thane", LocalDate.of(2018, 3, 22), 12000.00);
        addCustomer("Pooja Taxis",           "+91 94501 77007", "pooja.taxis@gmail.com",
                "Near Bus Stand, Nagpur", LocalDate.of(2024, 2, 3), -3400.00);
        addCustomer("Dev Enterprises",       "+91 94501 88008", "dev.ent@gmail.com",
                "Rajiv Gandhi Nagar, Hyderabad", LocalDate.of(2021, 9, 14), 6750.00);
        addCustomer("Ashok Agencies",        "+91 94501 99009", "ashok@ashokagencies.com",
                "GT Road, Ludhiana", LocalDate.of(2020, 5, 30), 0.00);
        addCustomer("Laxmi Carriers",        "+91 94501 10010", "billing@laxmicarriers.com",
                "NH-8, Rajkot", LocalDate.of(2023, 10, 7), -22000.00);

        // --- Inventory: tyre shop items ---
        // Task 7: qty 0 = Out of Stock, qty < 10 = Low Stock, >= 10 = In Stock
        addItem("Tyre 185/65 R15 Tubeless",   "TYR-18565R15", "Tyres",    28, 10,  3200.00);
        addItem("Tyre 195/65 R15 Tubeless",   "TYR-19565R15", "Tyres",    14, 10,  3500.00);
        addItem("Tyre 205/65 R16 Tubeless",   "TYR-20565R16", "Tyres",     6, 10,  4200.00); // Low Stock
        addItem("Tyre 175/70 R13 Tubeless",   "TYR-17570R13", "Tyres",     0, 10,  2800.00); // Out of Stock
        addItem("Tyre 265/70 R17 SUV",         "TYR-26570R17", "Tyres",    11, 8,   6800.00);
        addItem("Tyre 10.00 R20 Truck",        "TYR-10R20TRK", "Truck Tyres", 20, 5, 14500.00);
        addItem("Tyre 7.50 R16 LCV",           "TYR-750R16",  "Truck Tyres",  8, 5,  8200.00); // Low Stock
        addItem("Wheel Alignment Service",     "SVC-WHL-ALN",  "Services", 99, 10,   400.00);
        addItem("Wheel Balancing Service",     "SVC-WHL-BAL",  "Services", 99, 10,   200.00);
        addItem("Nitrogen Tyre Fill (4 wheels)","SVC-N2-4W",  "Services",  99, 10,   150.00);
        addItem("Tubeless Tyre Puncture Repair","SVC-PNCT",   "Services",  99, 10,   120.00);
        addItem("Tyre Rotation Service",        "SVC-ROT",    "Services",  99, 10,   250.00);
        addItem("Valve Stem Replacement",       "PRT-VALVE",  "Parts",    45, 20,    40.00);
        addItem("Tube (16 inch)",               "PRT-TUBE16", "Parts",     4, 10,   350.00); // Low Stock
        addItem("Rim (Alloy 15 inch)",          "PRT-RIM15A", "Parts",     7,  5,  2200.00); // Low Stock
        addItem("Brake Pad Set (Front)",        "PRT-BKPAD-F","Parts",    12, 10,  1200.00);
        addItem("Engine Oil 5W40 1L",           "PRT-OIL5W40","Lubricants",35, 20,   380.00);
        addItem("Multi-Point Inspection",       "SVC-MPI",    "Services",  99, 10,   300.00);

        // --- Credits ---
        addCredit(2, "Sunita Transport Co.",  18000.00, "Truck tyres 10.00 R20 x12 units",
                LocalDate.of(2024, 9, 1), LocalDate.of(2024, 10, 1), false);
        addCredit(4, "Meena Logistics",       9500.00,  "LCV tyres + alignment package",
                LocalDate.of(2024, 9, 10), LocalDate.of(2024, 10, 10), false);
        addCredit(7, "Pooja Taxis",           3400.00,  "Taxi tyre set + balancing",
                LocalDate.of(2024, 10, 5), LocalDate.of(2024, 11, 5), false);
        addCredit(10,"Laxmi Carriers",       22000.00,  "Fleet tyre replacement x20",
                LocalDate.of(2024, 6, 15), LocalDate.of(2024, 7, 15), false);
        addCredit(1, "Ramesh Verma",          4500.00,  "Car tyres 195/65 R15 set of 4",
                LocalDate.of(2024, 5, 20), LocalDate.of(2024, 6, 20), true);

        // --- Transactions ---
        addTxn(1, "Ramesh Verma", Transaction.Type.CREDIT, 4500.00,
                "Car tyres 195/65 R15 x4 + alignment", LocalDate.now().minusDays(1), true);
        addTxn(2, "Sunita Transport Co.", Transaction.Type.CREDIT, 18000.00,
                "Truck tyres 10.00 R20 x12", LocalDate.now().minusDays(3), true);
        addTxn(2, "Sunita Transport Co.", Transaction.Type.PAYMENT, 8000.00,
                "Partial payment — NEFT", LocalDate.now().minusDays(15), false);
        addTxn(3, "Karan Auto Spares", Transaction.Type.CREDIT, 2200.00,
                "Tyre stock purchase — 185/65 R15 x6", LocalDate.now().minusDays(2), true);
        addTxn(4, "Meena Logistics", Transaction.Type.CREDIT, 9500.00,
                "LCV tyre package + alignment x4", LocalDate.now().minusDays(6), true);
        addTxn(4, "Meena Logistics", Transaction.Type.PAYMENT, 4000.00,
                "Cheque deposit", LocalDate.now().minusDays(20), false);
        addTxn(5, "Vijay Kumar", Transaction.Type.PAYMENT, 3200.00,
                "Tyre replacement — full payment", LocalDate.now().minusDays(4), false);
        addTxn(6, "Nikhil Fleet Services", Transaction.Type.CREDIT, 12000.00,
                "Fleet tyre replacement x8 + balancing", LocalDate.now().minusDays(5), true);
        addTxn(6, "Nikhil Fleet Services", Transaction.Type.PAYMENT, 6000.00,
                "Advance payment — PhonePe", LocalDate.now().minusDays(25), false);
        addTxn(7, "Pooja Taxis", Transaction.Type.CREDIT, 3400.00,
                "Taxi tyres x4 + balancing", LocalDate.now().minusDays(9), true);
        addTxn(8, "Dev Enterprises", Transaction.Type.CREDIT, 6750.00,
                "SUV tyres 265/70 R17 x4 + alignment", LocalDate.now().minusDays(2), true);
        addTxn(10,"Laxmi Carriers", Transaction.Type.CREDIT, 22000.00,
                "Fleet tyre replacement + inspection", LocalDate.now().minusDays(3), true);
        addTxn(10,"Laxmi Carriers", Transaction.Type.PAYMENT, 10000.00,
                "Partial payment — bank transfer", LocalDate.now().minusDays(28), false);

        // --- Invoices ---
        invoices.add(new Invoice(601, "ST-00101", 6, "Nikhil Fleet Services",
                LocalDate.now().minusDays(5), LocalDate.now().plusDays(25),
                12000.00, 960.00, 12960.00,
                java.util.List.of(
                        new Invoice.LineItem("Tyre 185/65 R15 Tubeless", 8, 3200.00, 25600.00),
                        new Invoice.LineItem("Wheel Balancing Service", 8, 200.00, 1600.00)
                )));
        invoices.add(new Invoice(602, "ST-00102", 1, "Ramesh Verma",
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(29),
                4500.00, 360.00, 4860.00,
                java.util.List.of(
                        new Invoice.LineItem("Tyre 195/65 R15 Tubeless", 4, 3500.00, 14000.00),
                        new Invoice.LineItem("Wheel Alignment Service", 1, 400.00, 400.00)
                )));

        // --- Pre-seed generated files ---
        generatedFiles.addAll(FileGenerationService.seedSampleFiles(transactions, invoices));
    }

    // --- helpers ---
    private void addCustomer(String name, String phone, String email, String address,
                             LocalDate since, double balance) {
        customers.add(new Customer(++customerIdSeq, name, phone, email, address, since, balance));
    }

    private void addItem(String name, String sku, String category, int qty, int reorder, double price) {
        inventory.add(new InventoryItem(++itemIdSeq, name, sku, category, qty, reorder, price));
    }

    private void addCredit(int customerId, String customerName, double amount,
                           String desc, LocalDate issued, LocalDate due, boolean settled) {
        credits.add(new Credit(++creditIdSeq, customerId, customerName, amount, desc, issued, due, settled));
    }

    private void addTxn(int customerId, String customerName, Transaction.Type type,
                        double amount, String desc, LocalDate date, boolean ongoing) {
        transactions.add(new Transaction(++txnIdSeq, customerId, customerName, type, amount, desc, date, ongoing));
    }

    // --- accessors ---
    public List<User> getUsers()                    { return users; }
    public List<Customer> getCustomers()            { return customers; }
    public List<InventoryItem> getInventory()       { return inventory; }
    public List<Transaction> getTransactions()      { return transactions; }
    public List<Credit> getCredits()                { return credits; }
    public List<Invoice> getInvoices()              { return invoices; }
    public List<GeneratedFile> getGeneratedFiles()  { return generatedFiles; }

    public void addGeneratedFile(GeneratedFile f)   { generatedFiles.add(f); }

    public int nextCustomerId()  { return ++customerIdSeq; }
    public int nextCreditId()    { return ++creditIdSeq; }
    public int nextTxnId()       { return ++txnIdSeq; }
    public int nextItemId()      { return ++itemIdSeq; }
    public int nextInvoiceId()   { return ++invoiceIdSeq; }

    public Customer findCustomer(int id) {
        return customers.stream().filter(c -> c.getId() == id).findFirst().orElse(null);
    }

    public List<Transaction> transactionsFor(int customerId) {
        return transactions.stream().filter(t -> t.getCustomerId() == customerId).toList();
    }
}
