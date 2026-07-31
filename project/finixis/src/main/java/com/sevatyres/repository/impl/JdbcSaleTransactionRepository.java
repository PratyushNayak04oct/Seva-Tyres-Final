package com.sevatyres.repository.impl;

import com.sevatyres.db.DatabaseConfig;
import com.sevatyres.model.SaleTransaction;
import com.sevatyres.model.SaleTransactionItem;
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
                + "phone_pe,account_transfer,card_swipe,bajaj_finance,cash,cheque,credit_amount,"
                + "subtotal,tax_amount,tax_label,cgst_total,sgst_total,discount_percent,discount_amount,round_off,total,net_profit,"
                + "customer_id,customer_name,customer_email,customer_phone,customer_address"
                + ") VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
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
                + "phone_pe=?,account_transfer=?,card_swipe=?,bajaj_finance=?,cash=?,cheque=?,credit_amount=?,"
                + "subtotal=?,tax_amount=?,tax_label=?,cgst_total=?,sgst_total=?,discount_percent=?,discount_amount=?,round_off=?,total=?,net_profit=?,"
                + "customer_id=?,customer_name=?,customer_email=?,customer_phone=?,customer_address=? "
                + "WHERE sale_id=?";
        try (Connection con = DatabaseConfig.get();
             PreparedStatement ps = con.prepareStatement(sql)) {
            setParams(ps, tx);
            ps.setInt(30, tx.getId());
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

    /** Inserts line items for a multi-item bill. Safe to call with empty list. */
    public void saveItems(int saleId, List<SaleTransactionItem> items) {
        if (items == null || items.isEmpty()) return;
        String sql = "INSERT INTO Sale_Transaction_Item(sale_id,inventory_id,item_name,quantity,unit_price,line_total,"
                + "hsn_sac,item_type,rim_size,rate,cgst_amount,sgst_amount,taxable_amount) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection con = DatabaseConfig.get();
             PreparedStatement ps = con.prepareStatement(sql)) {
            for (SaleTransactionItem item : items) {
                ps.setInt(1, saleId);
                if (item.getInventoryId() != null) ps.setInt(2, item.getInventoryId());
                else ps.setNull(2, Types.INTEGER);
                ps.setString(3, item.getItemName());
                ps.setInt(4, item.getQuantity());
                ps.setDouble(5, item.getUnitPrice());
                ps.setDouble(6, item.getLineTotal());
                ps.setString(7, item.getHsnSac());
                ps.setString(8, item.getItemType());
                ps.setString(9, item.getRimSize());
                ps.setDouble(10, item.getRate());
                ps.setDouble(11, item.getCgstAmount());
                ps.setDouble(12, item.getSgstAmount());
                ps.setDouble(13, item.getTaxableAmount());
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public List<SaleTransactionItem> findItemsBySaleId(int saleId) {
        List<SaleTransactionItem> list = new ArrayList<>();
        String sql = "SELECT * FROM Sale_Transaction_Item WHERE sale_id=? ORDER BY sale_item_id";
        try (Connection con = DatabaseConfig.get();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, saleId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    SaleTransactionItem it = new SaleTransactionItem();
                    it.setSaleItemId(rs.getInt("sale_item_id"));
                    it.setSaleId(rs.getInt("sale_id"));
                    int invId = rs.getInt("inventory_id");
                    if (!rs.wasNull()) it.setInventoryId(invId);
                    it.setItemName(rs.getString("item_name"));
                    it.setQuantity(rs.getInt("quantity"));
                    it.setUnitPrice(rs.getDouble("unit_price"));
                    it.setLineTotal(rs.getDouble("line_total"));
                    try { it.setHsnSac(rs.getString("hsn_sac")); } catch (SQLException ignored) {}
                    try { it.setItemType(rs.getString("item_type")); } catch (SQLException ignored) {}
                    try { it.setRimSize(rs.getString("rim_size")); } catch (SQLException ignored) {}
                    try { it.setRate(rs.getDouble("rate")); } catch (SQLException ignored) {}
                    try { it.setCgstAmount(rs.getDouble("cgst_amount")); } catch (SQLException ignored) {}
                    try { it.setSgstAmount(rs.getDouble("sgst_amount")); } catch (SQLException ignored) {}
                    try { it.setTaxableAmount(rs.getDouble("taxable_amount")); } catch (SQLException ignored) {}
                    list.add(it);
                }
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return list;
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
        ps.setDouble(15, tx.getSubtotal());
        ps.setDouble(16, tx.getTaxAmount());
        ps.setString(17, tx.getTaxLabel());
        ps.setDouble(18, tx.getCgstTotal());
        ps.setDouble(19, tx.getSgstTotal());
        ps.setDouble(20, tx.getDiscountPercent());
        ps.setDouble(21, tx.getDiscountAmount());
        ps.setDouble(22, tx.getRoundOff());
        ps.setDouble(23, tx.getTotal());
        ps.setDouble(24, tx.getNetProfit());
        if (tx.getCustomerId() != null) ps.setInt(25, tx.getCustomerId());
        else ps.setNull(25, Types.INTEGER);
        ps.setString(26, tx.getCustomerName());
        ps.setString(27, tx.getCustomerEmail());
        ps.setString(28, tx.getCustomerPhone());
        ps.setString(29, tx.getCustomerAddress());
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
        try { tx.setSubtotal(rs.getDouble("subtotal")); } catch (SQLException ignored) {}
        try { tx.setTaxAmount(rs.getDouble("tax_amount")); } catch (SQLException ignored) {}
        try { tx.setTaxLabel(rs.getString("tax_label")); } catch (SQLException ignored) {}
        try { tx.setCgstTotal(rs.getDouble("cgst_total")); } catch (SQLException ignored) {}
        try { tx.setSgstTotal(rs.getDouble("sgst_total")); } catch (SQLException ignored) {}
        try { tx.setDiscountPercent(rs.getDouble("discount_percent")); } catch (SQLException ignored) {}
        try { tx.setDiscountAmount(rs.getDouble("discount_amount")); } catch (SQLException ignored) {}
        try { tx.setRoundOff(rs.getDouble("round_off")); } catch (SQLException ignored) {}
        tx.setTotal(rs.getDouble("total"));
        try { tx.setNetProfit(rs.getDouble("net_profit")); } catch (SQLException ignored) {}
        if (tx.getSubtotal() <= 0 && tx.getTotal() > 0) {
            tx.setSubtotal(Math.max(0, tx.getTotal() - tx.getTaxAmount()));
        }
        int custId = rs.getInt("customer_id");
        if (!rs.wasNull()) tx.setCustomerId(custId);
        tx.setCustomerName(rs.getString("customer_name"));
        tx.setCustomerEmail(rs.getString("customer_email"));
        tx.setCustomerPhone(rs.getString("customer_phone"));
        tx.setCustomerAddress(rs.getString("customer_address"));
        return tx;
    }

    public void saveTaxes(int saleId, List<com.sevatyres.model.SaleTaxLine> taxes) {
        try (Connection con = DatabaseConfig.get();
             PreparedStatement del = con.prepareStatement(
                     "DELETE FROM Sale_Transaction_Tax WHERE sale_id=?")) {
            del.setInt(1, saleId);
            del.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
        if (taxes == null || taxes.isEmpty()) return;
        String sql = "INSERT INTO Sale_Transaction_Tax(sale_id,tax_id,tax_name,tax_rate,tax_amount) VALUES(?,?,?,?,?)";
        try (Connection con = DatabaseConfig.get();
             PreparedStatement ps = con.prepareStatement(sql)) {
            for (com.sevatyres.model.SaleTaxLine line : taxes) {
                ps.setInt(1, saleId);
                if (line.getTaxId() != null) ps.setInt(2, line.getTaxId());
                else ps.setNull(2, Types.INTEGER);
                ps.setString(3, line.getTaxName());
                ps.setDouble(4, line.getTaxRate());
                ps.setDouble(5, line.getTaxAmount());
                ps.addBatch();
            }
            ps.executeBatch();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public List<com.sevatyres.model.SaleTaxLine> findTaxesBySaleId(int saleId) {
        List<com.sevatyres.model.SaleTaxLine> list = new ArrayList<>();
        String sql = "SELECT * FROM Sale_Transaction_Tax WHERE sale_id=? ORDER BY sale_tax_id";
        try (Connection con = DatabaseConfig.get();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, saleId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    com.sevatyres.model.SaleTaxLine line = new com.sevatyres.model.SaleTaxLine();
                    line.setSaleTaxId(rs.getInt("sale_tax_id"));
                    line.setSaleId(rs.getInt("sale_id"));
                    int tid = rs.getInt("tax_id");
                    if (!rs.wasNull()) line.setTaxId(tid);
                    line.setTaxName(rs.getString("tax_name"));
                    line.setTaxRate(rs.getDouble("tax_rate"));
                    line.setTaxAmount(rs.getDouble("tax_amount"));
                    list.add(line);
                }
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return list;
    }
}
