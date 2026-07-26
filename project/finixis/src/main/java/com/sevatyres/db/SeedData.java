package com.sevatyres.db;

import java.sql.*;
import java.time.LocalDate;
import java.util.List;

/**
 * Seeds tyre-shop demo data into an empty database.
 * All INSERT statements are skipped if the relevant table already has rows.
 */
public final class SeedData {

    private SeedData() {}

    public static void seed() {
        // Demo data is disabled — the application starts with a clean database.
        // To manually populate test data, re-enable the insertCustomers / insertInventory
        // / insertTransactions calls below.
    }

    private static boolean hasData(Connection c) throws SQLException {
        try (Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM Customer")) {
            return rs.next() && rs.getInt(1) > 0;
        }
    }

    private static void insertCustomers(Connection c) throws SQLException {
        String sql = "INSERT INTO Customer(customer_name,location,contact,email,creation_date) VALUES(?,?,?,?,?)";
        List<Object[]> rows = List.of(
            new Object[]{"Rajesh Kumar",   "Nagpur",      "+91-9876543210", "rajesh.kumar@email.com",   LocalDate.of(2022, 3, 15)},
            new Object[]{"Priya Sharma",   "Nagpur",      "+91-9765432109", "priya.sharma@gmail.com",   LocalDate.of(2022, 6, 20)},
            new Object[]{"Sunil Patel",    "Wardha",      "+91-9654321098", "sunil.patel@yahoo.com",    LocalDate.of(2021, 11, 5)},
            new Object[]{"Meena Deshmukh", "Nagpur",      "+91-9543210987", "meena.d@hotmail.com",      LocalDate.of(2023, 1, 8)},
            new Object[]{"Vikram Thakur",  "Amravati",    "+91-9432109876", "vikram.t@email.com",       LocalDate.of(2022, 9, 12)},
            new Object[]{"Deepak Wagh",    "Nagpur",      "+91-9321098765", "deepak.wagh@gmail.com",    LocalDate.of(2023, 4, 25)},
            new Object[]{"Arjun Bhosle",   "Yavatmal",    "+91-9210987654", "arjun.bhosle@email.com",   LocalDate.of(2021, 7, 30)},
            new Object[]{"Kavita Shinde",  "Nagpur",      "+91-9109876543", "kavita.shinde@gmail.com",  LocalDate.of(2022, 12, 3)},
            new Object[]{"Santosh Yadav",  "Chandrapur",  "+91-9098765432", "santosh.y@yahoo.com",      LocalDate.of(2023, 2, 18)},
            new Object[]{"Rekha Joshi",    "Nagpur",      "+91-8987654321", "rekha.joshi@hotmail.com",  LocalDate.of(2021, 5, 22)},
            new Object[]{"Nilesh More",    "Bhandara",    "+91-8876543210", "nilesh.more@email.com",    LocalDate.of(2022, 8, 7)},
            new Object[]{"Sunita Gavhane", "Nagpur",      "+91-8765432109", "sunita.g@gmail.com",       LocalDate.of(2023, 6, 14)}
        );
        batchInsert(c, sql, rows);
    }

    private static void insertInventory(Connection c) throws SQLException {
        String sql = "INSERT INTO Inventory(item_name,available_quantity,unit_price) VALUES(?,?,?)";
        List<Object[]> rows = List.of(
            // Tyres - Cars
            new Object[]{"Tyre 155/70 R13 (Car)",        25, 2800.00},
            new Object[]{"Tyre 185/65 R15 (Car)",        18, 4200.00},
            new Object[]{"Tyre 195/65 R15 (Car)",         8, 4800.00},
            new Object[]{"Tyre 205/55 R16 (Car)",         5, 5500.00},
            // Tyres - Bikes
            new Object[]{"Tyre 90/90-10 (Scooter Front)", 30, 850.00},
            new Object[]{"Tyre 90/90-12 (Scooter Rear)",  28, 950.00},
            new Object[]{"Tyre 2.75-17 (Bike)",           20, 1100.00},
            new Object[]{"Tyre 3.00-17 (Bike)",           15, 1250.00},
            // Tyres - Trucks/Commercial
            new Object[]{"Tyre 7.00-16 (LCV)",             3, 8500.00},
            new Object[]{"Tyre 10.00-20 (Truck)",          2, 18000.00},
            // Services & Parts
            new Object[]{"Wheel Alignment (Car)",          0, 450.00},
            new Object[]{"Wheel Balancing (Car)",          0, 200.00},
            new Object[]{"Tyre Rotation",                  0, 300.00},
            new Object[]{"Nitrogen Filling (per tyre)",    0, 80.00},
            new Object[]{"Puncture Repair",                0, 150.00},
            new Object[]{"Tube 155/70 (Car)",             40, 350.00},
            new Object[]{"Valve Replacement",             60, 50.00},
            new Object[]{"Rim Tape",                      50, 120.00},
            new Object[]{"Wheel Bearing (Car Front)",      6, 1200.00},
            new Object[]{"Shock Absorber (Car)",           4, 3500.00}
        );
        batchInsert(c, sql, rows);
    }

    private static void insertTransactions(Connection c) throws SQLException {
        int[] cids = new int[12];
        try (Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT customer_id FROM Customer ORDER BY customer_id")) {
            int i = 0;
            while (rs.next() && i < cids.length) cids[i++] = rs.getInt(1);
        }
        int[] iids = new int[20];
        double[] prices = new double[20];
        try (Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT item_id, unit_price FROM Inventory ORDER BY item_id")) {
            int i = 0;
            while (rs.next() && i < iids.length) {
                iids[i] = rs.getInt(1);
                prices[i] = rs.getDouble(2);
                i++;
            }
        }

        if (cids[0] == 0 || iids[0] == 0) return;

        String tcSql = "INSERT INTO Transaction_Credit(customer_id,total_amount,paid_amount,balance,transaction_date,is_settled,notes) VALUES(?,?,?,?,?,?,?)";
        String itemSql = "INSERT INTO Transaction_Credit_Item(transaction_id,item_id,quantity,unit_price_snapshot,line_total) VALUES(?,?,?,?,?)";

        record TxnDef(int custIdx, int[] itemIdxs, int[] qtys, double paid, LocalDate date, boolean settled) {}
        List<TxnDef> txns = List.of(
            new TxnDef(0, new int[]{1,13}, new int[]{4,4}, 18000, LocalDate.now().minusDays(2),  false),
            new TxnDef(1, new int[]{4,5},  new int[]{2,2},  3500, LocalDate.now().minusDays(5),  false),
            new TxnDef(2, new int[]{0,15}, new int[]{4,4},  9000, LocalDate.now().minusDays(7),  true),
            new TxnDef(3, new int[]{10},   new int[]{1},     450, LocalDate.now().minusDays(10), true),
            new TxnDef(4, new int[]{2,11}, new int[]{2,4},  8000, LocalDate.now().minusDays(15), false),
            new TxnDef(5, new int[]{6,7},  new int[]{2,2},  5000, LocalDate.now().minusDays(20), false),
            new TxnDef(6, new int[]{3},    new int[]{4}, 20000,   LocalDate.now().minusDays(25), true),
            new TxnDef(7, new int[]{13,14},new int[]{4,2},  1000, LocalDate.now().minusDays(30), false),
            new TxnDef(8, new int[]{18,19},new int[]{2,2}, 10000, LocalDate.now().minusDays(45), true),
            new TxnDef(9, new int[]{1,16}, new int[]{4,4},  8000, LocalDate.now().minusDays(60), false)
        );

        for (TxnDef t : txns) {
            double total = 0;
            for (int k = 0; k < t.itemIdxs().length; k++) {
                total += t.qtys()[k] * prices[t.itemIdxs()[k]];
            }
            double paid = Math.min(t.paid(), total);
            double balance = total - paid;
            boolean settled = balance <= 0.001;

            int txnId;
            try (PreparedStatement ps = c.prepareStatement(tcSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, cids[t.custIdx()]);
                ps.setDouble(2, total);
                ps.setDouble(3, paid);
                ps.setDouble(4, balance);
                ps.setDate(5, Date.valueOf(t.date()));
                ps.setBoolean(6, settled);
                ps.setString(7, null);
                ps.executeUpdate();
                try (ResultSet k = ps.getGeneratedKeys()) { k.next(); txnId = k.getInt(1); }
            }
            for (int k = 0; k < t.itemIdxs().length; k++) {
                double lp = prices[t.itemIdxs()[k]];
                try (PreparedStatement ps = c.prepareStatement(itemSql)) {
                    ps.setInt(1, txnId);
                    ps.setInt(2, iids[t.itemIdxs()[k]]);
                    ps.setInt(3, t.qtys()[k]);
                    ps.setDouble(4, lp);
                    ps.setDouble(5, t.qtys()[k] * lp);
                    ps.executeUpdate();
                }
            }
        }
    }

    private static void batchInsert(Connection c, String sql, List<Object[]> rows) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (Object[] row : rows) {
                for (int i = 0; i < row.length; i++) ps.setObject(i + 1, row[i]);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }
}
