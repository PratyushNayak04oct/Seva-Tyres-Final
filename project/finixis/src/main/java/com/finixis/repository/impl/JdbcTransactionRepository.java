package com.finixis.repository.impl;

import com.finixis.db.DatabaseConfig;
import com.finixis.model.Transaction;
import com.finixis.model.TransactionLineItem;
import com.finixis.repository.TransactionRepository;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class JdbcTransactionRepository implements TransactionRepository {

    // ---------------------------------------------------------------
    // CREDIT transactions
    // ---------------------------------------------------------------

    @Override
    public List<Transaction> findAllCredits() {
        return queryCreditTxns("SELECT " + CREDIT_COLS + " FROM Transaction_Credit tc"
                + " JOIN Customer c ON c.customer_id=tc.customer_id ORDER BY tc.transaction_date DESC");
    }

    @Override
    public List<Transaction> findCreditsByCustomer(int customerId) {
        return queryCreditTxns("SELECT " + CREDIT_COLS + " FROM Transaction_Credit tc"
                + " JOIN Customer c ON c.customer_id=tc.customer_id"
                + " WHERE tc.customer_id=" + customerId + " ORDER BY tc.transaction_date DESC");
    }

    @Override
    public List<Transaction> findCreditsByDateRange(LocalDate from, LocalDate to) {
        String sql = "SELECT " + CREDIT_COLS + " FROM Transaction_Credit tc"
                + " JOIN Customer c ON c.customer_id=tc.customer_id"
                + " WHERE tc.transaction_date BETWEEN ? AND ? ORDER BY tc.transaction_date DESC";
        List<Transaction> list = new ArrayList<>();
        try (Connection con = DatabaseConfig.get(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(from)); ps.setDate(2, Date.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(mapCredit(rs)); }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return list;
    }

    @Override
    public Transaction saveCredit(Transaction tx, List<TransactionLineItem> items) {
        String sql = "INSERT INTO Transaction_Credit(customer_id,total_amount,paid_amount,balance,"
                + "transaction_date,is_settled,notes) VALUES(?,?,?,?,?,?,?)";
        try (Connection con = DatabaseConfig.get()) {
            con.setAutoCommit(false);
            try {
                int txnId;
                try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                    double balance = tx.getAmount() - tx.getPaidAmount();
                    ps.setInt(1, tx.getCustomerId());
                    ps.setDouble(2, tx.getAmount());
                    ps.setDouble(3, tx.getPaidAmount());
                    ps.setDouble(4, balance);
                    ps.setDate(5, Date.valueOf(tx.getDate() != null ? tx.getDate() : LocalDate.now()));
                    ps.setBoolean(6, balance <= 0);
                    ps.setString(7, tx.getDescription());
                    ps.executeUpdate();
                    try (ResultSet k = ps.getGeneratedKeys()) { k.next(); txnId = k.getInt(1); }
                }
                tx.setId(txnId);
                tx.setBalance(tx.getAmount() - tx.getPaidAmount());
                tx.setOngoing(tx.getBalance() > 0);
                insertLineItems(con, txnId, items);
                con.commit();
            } catch (Exception ex) {
                con.rollback();
                throw ex;
            } finally {
                con.setAutoCommit(true);
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return tx;
    }

    @Override
    public void markSettled(int transactionId) {
        String sql = "UPDATE Transaction_Credit SET paid_amount=total_amount,balance=0,is_settled=TRUE WHERE transaction_id=?";
        try (Connection con = DatabaseConfig.get(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, transactionId); ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    // ---------------------------------------------------------------
    // DEBIT transactions
    // ---------------------------------------------------------------

    @Override
    public List<Transaction> findAllDebits() {
        return queryDebitTxns("SELECT " + DEBIT_COLS + " FROM Transaction_Debit td"
                + " JOIN Customer c ON c.customer_id=td.customer_id ORDER BY td.debit_date DESC");
    }

    @Override
    public List<Transaction> findDebitsByCustomer(int customerId) {
        return queryDebitTxns("SELECT " + DEBIT_COLS + " FROM Transaction_Debit td"
                + " JOIN Customer c ON c.customer_id=td.customer_id"
                + " WHERE td.customer_id=" + customerId + " ORDER BY td.debit_date DESC");
    }

    @Override
    public Transaction saveDebit(Transaction tx) {
        String sql = "INSERT INTO Transaction_Debit(customer_id,amount,debit_date,notes) VALUES(?,?,?,?)";
        try (Connection con = DatabaseConfig.get();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, tx.getCustomerId());
            ps.setDouble(2, tx.getAmount());
            ps.setDate(3, Date.valueOf(tx.getDate() != null ? tx.getDate() : LocalDate.now()));
            ps.setString(4, tx.getDescription());
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { k.next(); tx.setId(k.getInt(1)); }
        } catch (SQLException e) { throw new RuntimeException(e); }
        tx.setOngoing(true);
        return tx;
    }

    // ---------------------------------------------------------------
    // Combined view
    // ---------------------------------------------------------------

    @Override
    public List<Transaction> findAll() {
        List<Transaction> all = new ArrayList<>(findAllCredits());
        all.addAll(findAllDebits());
        all.sort((a, b) -> b.getDate().compareTo(a.getDate()));
        return all;
    }

    @Override
    public List<Transaction> findByCustomer(int customerId) {
        List<Transaction> all = new ArrayList<>(findCreditsByCustomer(customerId));
        all.addAll(findDebitsByCustomer(customerId));
        all.sort((a, b) -> b.getDate().compareTo(a.getDate()));
        return all;
    }

    @Override
    public List<Transaction> findByDateRange(LocalDate from, LocalDate to) {
        List<Transaction> all = new ArrayList<>(findCreditsByDateRange(from, to));
        // For debits by date range:
        String sql = "SELECT " + DEBIT_COLS + " FROM Transaction_Debit td"
                + " JOIN Customer c ON c.customer_id=td.customer_id"
                + " WHERE td.debit_date BETWEEN ? AND ? ORDER BY td.debit_date DESC";
        try (Connection con = DatabaseConfig.get(); PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(from)); ps.setDate(2, Date.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) all.add(mapDebit(rs)); }
        } catch (SQLException e) { throw new RuntimeException(e); }
        all.sort((a, b) -> b.getDate().compareTo(a.getDate()));
        return all;
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private static final String CREDIT_COLS =
            "tc.transaction_id, c.customer_id, c.customer_name, "
            + "tc.total_amount, tc.paid_amount, tc.balance, "
            + "tc.transaction_date, tc.is_settled, tc.notes";

    private static final String DEBIT_COLS =
            "td.debit_id, c.customer_id, c.customer_name, "
            + "td.amount, td.debit_date, td.notes";

    private List<Transaction> queryCreditTxns(String sql) {
        List<Transaction> list = new ArrayList<>();
        try (Connection con = DatabaseConfig.get();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapCredit(rs));
        } catch (SQLException e) { throw new RuntimeException(e); }
        return list;
    }

    private List<Transaction> queryDebitTxns(String sql) {
        List<Transaction> list = new ArrayList<>();
        try (Connection con = DatabaseConfig.get();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapDebit(rs));
        } catch (SQLException e) { throw new RuntimeException(e); }
        return list;
    }

    private Transaction mapCredit(ResultSet rs) throws SQLException {
        Transaction t = new Transaction();
        t.setId(rs.getInt("transaction_id"));
        t.setCustomerId(rs.getInt("customer_id"));
        t.setCustomerName(rs.getString("customer_name"));
        t.setType(Transaction.Type.CREDIT);
        t.setAmount(rs.getDouble("total_amount"));
        t.setPaidAmount(rs.getDouble("paid_amount"));
        t.setBalance(rs.getDouble("balance"));
        t.setDate(rs.getDate("transaction_date").toLocalDate());
        boolean settled = rs.getBoolean("is_settled");
        t.setOngoing(!settled);
        t.setDescription(rs.getString("notes"));
        return t;
    }

    private Transaction mapDebit(ResultSet rs) throws SQLException {
        Transaction t = new Transaction();
        t.setId(rs.getInt("debit_id"));
        t.setCustomerId(rs.getInt("customer_id"));
        t.setCustomerName(rs.getString("customer_name"));
        t.setType(Transaction.Type.DEBIT);
        t.setAmount(rs.getDouble("amount"));
        t.setPaidAmount(0);
        t.setBalance(rs.getDouble("amount"));
        t.setDate(rs.getDate("debit_date").toLocalDate());
        t.setOngoing(true);
        t.setDescription(rs.getString("notes"));
        return t;
    }

    @Override
    public void partialPayment(int id, double amt, LocalDate date) {
        String updateSql = "UPDATE Transaction_Credit "
                + "SET paid_amount = paid_amount + ?, "
                + "balance = CASE WHEN balance - ? < 0 THEN 0 ELSE balance - ? END "
                + "WHERE transaction_id = ?";
        try (Connection con = DatabaseConfig.get();
             PreparedStatement ps = con.prepareStatement(updateSql)) {
            ps.setDouble(1, amt);
            ps.setDouble(2, amt);
            ps.setDouble(3, amt);
            ps.setInt(4, id);
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
        // mark settled if balance reached zero
        String settleSql = "UPDATE Transaction_Credit SET is_settled = TRUE "
                + "WHERE transaction_id = ? AND balance <= 0";
        try (Connection con = DatabaseConfig.get();
             PreparedStatement ps = con.prepareStatement(settleSql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    private void insertLineItems(Connection con, int txnId, List<TransactionLineItem> items) throws SQLException {
        if (items == null || items.isEmpty()) return;
        String sql = "INSERT INTO Transaction_Credit_Item(transaction_id,item_id,quantity,unit_price_snapshot,line_total) VALUES(?,?,?,?,?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            for (TransactionLineItem li : items) {
                ps.setInt(1, txnId);
                ps.setInt(2, li.getItemId());
                ps.setInt(3, li.getQuantity());
                ps.setDouble(4, li.getUnitPriceSnapshot());
                ps.setDouble(5, li.getLineTotal());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }
}
