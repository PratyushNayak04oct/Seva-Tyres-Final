package com.sevatyres.repository.impl;

import com.sevatyres.db.DatabaseConfig;
import com.sevatyres.model.SaleTransaction;
import com.sevatyres.repository.SaleTransactionRepository;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcSaleTransactionRepository implements SaleTransactionRepository {

    @Override
    public List<SaleTransaction> findAll() {
        return query("SELECT * FROM Sale_Transaction ORDER BY sale_date DESC, sale_id DESC");
    }

    @Override
    public Optional<SaleTransaction> findById(int id) {
        try (Connection con = DatabaseConfig.get();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM Sale_Transaction WHERE sale_id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public List<SaleTransaction> findByDateRange(LocalDate from, LocalDate to) {
        List<SaleTransaction> list = new ArrayList<>();
        String sql = "SELECT * FROM Sale_Transaction WHERE sale_date BETWEEN ? AND ? ORDER BY sale_date DESC";
        try (Connection con = DatabaseConfig.get();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(from));
            ps.setDate(2, Date.valueOf(to));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return list;
    }

    @Override
    public List<SaleTransaction> findByCustomerId(int customerId) {
        List<SaleTransaction> list = new ArrayList<>();
        String sql = "SELECT * FROM Sale_Transaction WHERE customer_id=? ORDER BY sale_date DESC";
        try (Connection con = DatabaseConfig.get();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, customerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return list;
    }

    @Override
    public SaleTransaction save(SaleTransaction tx) {
        String sql = "INSERT INTO Sale_Transaction("
                + "bill_no,sale_date,particulars,brand,quantity,unit_price,inventory_item_id,"
                + "phone_pe,account_transfer,card_swipe,bajaj_finance,cash,cheque,credit_amount,total,"
                + "customer_id,customer_name,customer_email,customer_phone,customer_address"
                + ") VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection con = DatabaseConfig.get();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setParams(ps, tx);
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) {
                if (k.next()) tx.setId(k.getInt(1));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return tx;
    }

    @Override
    public void update(SaleTransaction tx) {
        String sql = "UPDATE Sale_Transaction SET "
                + "bill_no=?,sale_date=?,particulars=?,brand=?,quantity=?,unit_price=?,inventory_item_id=?,"
                + "phone_pe=?,account_transfer=?,card_swipe=?,bajaj_finance=?,cash=?,cheque=?,credit_amount=?,total=?,"
                + "customer_id=?,customer_name=?,customer_email=?,customer_phone=?,customer_address=? "
                + "WHERE sale_id=?";
        try (Connection con = DatabaseConfig.get();
             PreparedStatement ps = con.prepareStatement(sql)) {
            setParams(ps, tx);
            ps.setInt(21, tx.getId());
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public void delete(int id) {
        try (Connection con = DatabaseConfig.get();
             PreparedStatement ps = con.prepareStatement("DELETE FROM Sale_Transaction WHERE sale_id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private List<SaleTransaction> query(String sql) {
        List<SaleTransaction> list = new ArrayList<>();
        try (Connection con = DatabaseConfig.get();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) { throw new RuntimeException(e); }
        return list;
    }

    private void setParams(PreparedStatement ps, SaleTransaction tx) throws SQLException {
        ps.setString(1, tx.getBillNo());
        ps.setDate(2, Date.valueOf(tx.getSaleDate() != null ? tx.getSaleDate() : LocalDate.now()));
        ps.setString(3, tx.getParticulars());
        ps.setString(4, tx.getBrand());
        ps.setInt(5, tx.getQuantity());
        ps.setDouble(6, tx.getUnitPrice());
        if (tx.getInventoryItemId() != null) ps.setInt(7, tx.getInventoryItemId());
        else ps.setNull(7, Types.INTEGER);
        ps.setDouble(8, tx.getPhonePe());
        ps.setDouble(9, tx.getAccountTransfer());
        ps.setDouble(10, tx.getCardSwipe());
        ps.setDouble(11, tx.getBajajFinance());
        ps.setDouble(12, tx.getCash());
        ps.setDouble(13, tx.getCheque());
        ps.setDouble(14, tx.getCreditAmount());
        ps.setDouble(15, tx.getTotal());
        if (tx.getCustomerId() != null) ps.setInt(16, tx.getCustomerId());
        else ps.setNull(16, Types.INTEGER);
        ps.setString(17, tx.getCustomerName());
        ps.setString(18, tx.getCustomerEmail());
        ps.setString(19, tx.getCustomerPhone());
        ps.setString(20, tx.getCustomerAddress());
    }

    private SaleTransaction map(ResultSet rs) throws SQLException {
        SaleTransaction tx = new SaleTransaction();
        tx.setId(rs.getInt("sale_id"));
        tx.setBillNo(rs.getString("bill_no"));
        Date d = rs.getDate("sale_date");
        tx.setSaleDate(d != null ? d.toLocalDate() : null);
        tx.setParticulars(rs.getString("particulars"));
        tx.setBrand(rs.getString("brand"));
        tx.setQuantity(rs.getInt("quantity"));
        try { tx.setUnitPrice(rs.getDouble("unit_price")); } catch (SQLException ignored) {}
        try {
            int itemId = rs.getInt("inventory_item_id");
            if (!rs.wasNull()) tx.setInventoryItemId(itemId);
        } catch (SQLException ignored) {}
        tx.setPhonePe(rs.getDouble("phone_pe"));
        tx.setAccountTransfer(rs.getDouble("account_transfer"));
        tx.setCardSwipe(rs.getDouble("card_swipe"));
        tx.setBajajFinance(rs.getDouble("bajaj_finance"));
        tx.setCash(rs.getDouble("cash"));
        tx.setCheque(rs.getDouble("cheque"));
        tx.setCreditAmount(rs.getDouble("credit_amount"));
        tx.setTotal(rs.getDouble("total"));
        int custId = rs.getInt("customer_id");
        if (!rs.wasNull()) tx.setCustomerId(custId);
        tx.setCustomerName(rs.getString("customer_name"));
        tx.setCustomerEmail(rs.getString("customer_email"));
        tx.setCustomerPhone(rs.getString("customer_phone"));
        tx.setCustomerAddress(rs.getString("customer_address"));
        return tx;
    }
}
