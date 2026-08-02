package com.sevatyres.repository.impl;

import com.sevatyres.db.DatabaseConfig;
import com.sevatyres.model.PayableTransaction;
import com.sevatyres.repository.PayableTransactionRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcPayableTransactionRepository implements PayableTransactionRepository {

    @Override
    public List<PayableTransaction> findAll() {
        List<PayableTransaction> list = new ArrayList<>();
        String sql = "SELECT * FROM Payable_Transaction ORDER BY txn_date DESC, payable_id DESC";
        try (Connection con = DatabaseConfig.get();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    @Override
    public Optional<PayableTransaction> findById(int id) {
        try (Connection con = DatabaseConfig.get();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT * FROM Payable_Transaction WHERE payable_id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<PayableTransaction> findByNumber(String txnNumber) {
        if (txnNumber == null || txnNumber.isBlank()) return Optional.empty();
        try (Connection con = DatabaseConfig.get();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT * FROM Payable_Transaction WHERE txn_number=?")) {
            ps.setString(1, txnNumber.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public synchronized String nextTxnNumber() {
        try (Connection con = DatabaseConfig.get()) {
            con.setAutoCommit(false);
            try {
                try (Statement st = con.createStatement()) {
                    st.execute("CREATE TABLE IF NOT EXISTS Payable_Sequence ("
                            + "id INTEGER PRIMARY KEY, last_num INTEGER NOT NULL DEFAULT 99999)");
                    st.execute("INSERT INTO Payable_Sequence(id, last_num) "
                            + "SELECT 1, 99999 WHERE NOT EXISTS (SELECT 1 FROM Payable_Sequence WHERE id=1)");
                }
                int next;
                try (PreparedStatement ps = con.prepareStatement(
                        "SELECT last_num FROM Payable_Sequence WHERE id=1")) {
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new SQLException("Payable_Sequence missing row");
                        next = rs.getInt(1) + 1;
                    }
                }
                if (next < 100000) next = 100000;
                if (next > 999999) next = 100000;
                try (PreparedStatement ups = con.prepareStatement(
                        "UPDATE Payable_Sequence SET last_num=? WHERE id=1")) {
                    ups.setInt(1, next);
                    ups.executeUpdate();
                }
                con.commit();
                return String.format("%06d", next);
            } catch (SQLException e) {
                try { con.rollback(); } catch (SQLException ignored) {}
                throw e;
            } finally {
                try { con.setAutoCommit(true); } catch (SQLException ignored) {}
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public PayableTransaction save(PayableTransaction txn) {
        if (txn.getId() > 0) {
            String sql = "UPDATE Payable_Transaction SET txn_number=?, txn_date=?, paid_to=?, amount=?, notes=? "
                    + "WHERE payable_id=?";
            try (Connection con = DatabaseConfig.get();
                 PreparedStatement ps = con.prepareStatement(sql)) {
                bind(ps, txn);
                ps.setInt(6, txn.getId());
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            return txn;
        }
        if (txn.getTxnNumber() == null || txn.getTxnNumber().isBlank()) {
            txn.setTxnNumber(nextTxnNumber());
        }
        String sql = "INSERT INTO Payable_Transaction(txn_number, txn_date, paid_to, amount, notes) VALUES(?,?,?,?,?)";
        try (Connection con = DatabaseConfig.get();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, txn);
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) {
                if (k.next()) txn.setId(k.getInt(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return txn;
    }

    @Override
    public void delete(int id) {
        try (Connection con = DatabaseConfig.get();
             PreparedStatement ps = con.prepareStatement(
                     "DELETE FROM Payable_Transaction WHERE payable_id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void bind(PreparedStatement ps, PayableTransaction t) throws SQLException {
        ps.setString(1, t.getTxnNumber());
        ps.setDate(2, t.getTxnDate() != null ? Date.valueOf(t.getTxnDate()) : Date.valueOf(java.time.LocalDate.now()));
        ps.setString(3, t.getPaidTo());
        ps.setDouble(4, t.getAmount());
        ps.setString(5, blankToNull(t.getNotes()));
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private PayableTransaction map(ResultSet rs) throws SQLException {
        PayableTransaction p = new PayableTransaction();
        p.setId(rs.getInt("payable_id"));
        p.setTxnNumber(rs.getString("txn_number"));
        Date d = rs.getDate("txn_date");
        if (d != null) p.setTxnDate(d.toLocalDate());
        p.setPaidTo(rs.getString("paid_to"));
        p.setAmount(rs.getDouble("amount"));
        p.setNotes(rs.getString("notes"));
        return p;
    }
}
