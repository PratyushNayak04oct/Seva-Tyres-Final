package com.finixis.db;

import java.sql.*;
import java.time.LocalDate;
import java.util.List;

/**
 * Seeds realistic demo data into an empty database.
 * All INSERT statements are skipped if the relevant table already has rows,
 * so this is safe to run on every startup.
 */
public final class SeedData {

    private SeedData() {}

    public static void seed() {
        try (Connection c = DatabaseConfig.get()) {
            if (hasData(c)) return;
            insertCustomers(c);
            insertInventory(c);
            insertTransactions(c);
        } catch (Exception e) {
            System.err.println("[SeedData] Warning: could not seed demo data — " + e.getMessage());
        }
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
            new Object[]{"Amit Sharma",    "Mumbai",      "+91-9876543210", "amit.sharma@email.com",    LocalDate.of(2022, 3, 15)},
            new Object[]{"Priya Patel",    "Ahmedabad",   "+91-9765432109", "priya.patel@gmail.com",    LocalDate.of(2022, 6, 20)},
            new Object[]{"Rajesh Kumar",   "Delhi",       "+91-9654321098", "rajesh.kumar@yahoo.com",   LocalDate.of(2021, 11, 5)},
            new Object[]{"Sunita Verma",   "Bangalore",   "+91-9543210987", "sunita.verma@hotmail.com", LocalDate.of(2023, 1, 8)},
            new Object[]{"Vikram Singh",   "Jaipur",      "+91-9432109876", "vikram.singh@email.com",   LocalDate.of(2022, 9, 12)},
            new Object[]{"Deepa Nair",     "Kochi",       "+91-9321098765", "deepa.nair@gmail.com",     LocalDate.of(2023, 4, 25)},
            new Object[]{"Arjun Mehta",    "Pune",        "+91-9210987654", "arjun.mehta@email.com",    LocalDate.of(2021, 7, 30)},
            new Object[]{"Kavitha Reddy",  "Hyderabad",   "+91-9109876543", "kavitha.reddy@gmail.com",  LocalDate.of(2022, 12, 3)},
            new Object[]{"Sanjay Gupta",   "Kolkata",     "+91-9098765432", "sanjay.gupta@yahoo.com",   LocalDate.of(2023, 2, 18)},
            new Object[]{"Meena Iyer",     "Chennai",     "+91-8987654321", "meena.iyer@hotmail.com",   LocalDate.of(2021, 5, 22)},
            new Object[]{"Ravi Krishnan",  "Coimbatore",  "+91-8876543210", "ravi.krishnan@email.com",  LocalDate.of(2022, 8, 7)},
            new Object[]{"Anita Joshi",    "Nagpur",      "+91-8765432109", "anita.joshi@gmail.com",    LocalDate.of(2023, 6, 14)}
        );
        batchInsert(c, sql, rows);
    }

    private static void insertInventory(Connection c) throws SQLException {
        String sql = "INSERT INTO Inventory(item_name,available_quantity,unit_price) VALUES(?,?,?)";
        List<Object[]> rows = List.of(
            new Object[]{"Basmati Rice (5 kg)",         120,  450.00},
            new Object[]{"Refined Sunflower Oil (1 L)",   8,  185.50},
            new Object[]{"Toor Dal (1 kg)",               3,  145.00},
            new Object[]{"Sugar (1 kg)",                 95,   55.00},
            new Object[]{"Wheat Flour (10 kg)",          42,  390.00},
            new Object[]{"Chana Dal (1 kg)",              6,  125.00},
            new Object[]{"Mustard Oil (1 L)",            18,  210.00},
            new Object[]{"Salt (1 kg)",                 200,   25.00},
            new Object[]{"Turmeric Powder (100 g)",      50,   75.00},
            new Object[]{"Red Chilli Powder (200 g)",    35,   90.00},
            new Object[]{"Cumin Seeds (200 g)",          22,   85.00},
            new Object[]{"Tea (250 g)",                   4,  135.00}
        );
        batchInsert(c, sql, rows);
    }

    private static void insertTransactions(Connection c) throws SQLException {
        // Get customer IDs
        int[] cids = new int[12];
        try (Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT customer_id FROM Customer ORDER BY customer_id")) {
            int i = 0;
            while (rs.next() && i < cids.length) cids[i++] = rs.getInt(1);
        }
        // Get item IDs
        int[] iids = new int[12];
        double[] prices = new double[12];
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

        // Insert Transaction_Credit records
        String tcSql = "INSERT INTO Transaction_Credit(customer_id,total_amount,paid_amount,balance,transaction_date,is_settled,notes) VALUES(?,?,?,?,?,?,?)";
        String itemSql = "INSERT INTO Transaction_Credit_Item(transaction_id,item_id,quantity,unit_price_snapshot,line_total) VALUES(?,?,?,?,?)";
        String debitSql = "INSERT INTO Transaction_Debit(customer_id,amount,debit_date,notes) VALUES(?,?,?,?)";

        record TxnDef(int custIdx, int[] itemIdxs, int[] qtys, double paid, LocalDate date, boolean settled) {}

        List<TxnDef> txns = List.of(
            new TxnDef(0, new int[]{0,4}, new int[]{2,1},  500, LocalDate.now().minusDays(2),  false),
            new TxnDef(1, new int[]{1,2}, new int[]{3,2},  300, LocalDate.now().minusDays(5),  false),
            new TxnDef(2, new int[]{3,6}, new int[]{5,2},  700, LocalDate.now().minusDays(7),  true),
            new TxnDef(3, new int[]{8,9}, new int[]{4,3},  200, LocalDate.now().minusDays(10), false),
            new TxnDef(4, new int[]{0,3}, new int[]{3,4}, 1000, LocalDate.now().minusDays(15), true),
            new TxnDef(5, new int[]{5,10},new int[]{2,1},  400, LocalDate.now().minusDays(20), false),
            new TxnDef(6, new int[]{1,7}, new int[]{6,5}, 1200, LocalDate.now().minusDays(25), true),
            new TxnDef(7, new int[]{2,4}, new int[]{2,3},  600, LocalDate.now().minusDays(30), false),
            new TxnDef(8, new int[]{0,8}, new int[]{4,2},  800, LocalDate.now().minusDays(45), true),
            new TxnDef(9, new int[]{3,9}, new int[]{3,2},  350, LocalDate.now().minusDays(60), false),
            new TxnDef(10,new int[]{6,11},new int[]{2,4},  550, LocalDate.now().minusDays(90), true),
            new TxnDef(11,new int[]{0,2}, new int[]{1,3},  480, LocalDate.now().minusDays(5),  false)
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
                try (ResultSet k = ps.getGeneratedKeys()) {
                    k.next();
                    txnId = k.getInt(1);
                }
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

        // Debits
        List<Object[]> debits = List.of(
            new Object[]{cids[0], 2500.00, Date.valueOf(LocalDate.now().minusDays(3)),  "Advance payment returned"},
            new Object[]{cids[2], 1800.00, Date.valueOf(LocalDate.now().minusDays(8)),  "Overcharged — credit note"},
            new Object[]{cids[5], 3200.00, Date.valueOf(LocalDate.now().minusDays(14)), "Damaged goods refund"},
            new Object[]{cids[7], 950.00,  Date.valueOf(LocalDate.now().minusDays(22)), "Short-delivery adjustment"},
            new Object[]{cids[9], 4100.00, Date.valueOf(LocalDate.now().minusDays(35)), "Bulk order discount applied"}
        );
        batchInsert(c, debitSql, debits);
    }

    private static void batchInsert(Connection c, String sql, List<Object[]> rows) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (Object[] row : rows) {
                for (int i = 0; i < row.length; i++) {
                    ps.setObject(i + 1, row[i]);
                }
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }
}
