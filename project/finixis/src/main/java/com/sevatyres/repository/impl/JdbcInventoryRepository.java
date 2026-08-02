package com.sevatyres.repository.impl;

import com.sevatyres.db.DatabaseConfig;
import com.sevatyres.model.InventoryItem;
import com.sevatyres.repository.InventoryRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcInventoryRepository implements InventoryRepository {

    private static final int REORDER_LEVEL = 10;
    private static final String SELECT_ALL =
            "SELECT item_id,item_name,brand,available_quantity,unit_price,barcode,hsn_sac,item_type,"
                    + "rim_size,tyre_size,pattern,tyre_kind,product_code,mrp,billing_amount,purchase_id FROM Inventory";

    @Override
    public List<InventoryItem> findAll() {
        try (Connection con = DatabaseConfig.get();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(SELECT_ALL + " ORDER BY item_name")) {
            List<InventoryItem> list = new ArrayList<>();
            while (rs.next()) list.add(map(rs));
            return list;
        } catch (SQLException e) {
            return findAllLegacy();
        }
    }

    private List<InventoryItem> findAllLegacy() {
        List<InventoryItem> list = new ArrayList<>();
        String sql = "SELECT item_id,item_name,brand,available_quantity,unit_price,barcode,hsn_sac,item_type,rim_size "
                + "FROM Inventory ORDER BY item_name";
        try (Connection con = DatabaseConfig.get();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            String sql2 = "SELECT item_id,item_name,brand,available_quantity,unit_price,barcode FROM Inventory ORDER BY item_name";
            try (Connection con = DatabaseConfig.get();
                 Statement st = con.createStatement();
                 ResultSet rs = st.executeQuery(sql2)) {
                while (rs.next()) list.add(mapLegacy(rs));
            } catch (SQLException e2) { throw new RuntimeException(e2); }
        }
        return list;
    }

    @Override
    public Optional<InventoryItem> findById(int id) {
        try (Connection con = DatabaseConfig.get();
             PreparedStatement ps = con.prepareStatement(SELECT_ALL + " WHERE item_id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            return findAll().stream().filter(i -> i.getId() == id).findFirst();
        }
    }

    @Override
    public Optional<InventoryItem> findByBarcode(String barcode) {
        if (barcode == null || barcode.isBlank()) return Optional.empty();
        try (Connection con = DatabaseConfig.get();
             PreparedStatement ps = con.prepareStatement(SELECT_ALL + " WHERE barcode=?")) {
            ps.setString(1, barcode.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public Optional<InventoryItem> findByName(String name) {
        if (name == null || name.isBlank()) return Optional.empty();
        try (Connection con = DatabaseConfig.get();
             PreparedStatement ps = con.prepareStatement(SELECT_ALL + " WHERE LOWER(item_name)=LOWER(?)")) {
            ps.setString(1, name.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public InventoryItem save(InventoryItem item) {
        if (item.getId() == 0) return insert(item);
        update(item);
        return item;
    }

    private InventoryItem insert(InventoryItem item) {
        String sql = "INSERT INTO Inventory(item_name,brand,available_quantity,unit_price,barcode,hsn_sac,item_type,"
                + "rim_size,tyre_size,pattern,tyre_kind,product_code,mrp,billing_amount,purchase_id) "
                + "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection con = DatabaseConfig.get();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, item);
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { k.next(); item.setId(k.getInt(1)); }
        } catch (SQLException e) {
            throw wrapUniqueName(e, item.getName());
        }
        return item;
    }

    private void update(InventoryItem item) {
        String sql = "UPDATE Inventory SET item_name=?,brand=?,available_quantity=?,unit_price=?,barcode=?,hsn_sac=?,"
                + "item_type=?,rim_size=?,tyre_size=?,pattern=?,tyre_kind=?,product_code=?,mrp=?,billing_amount=?,purchase_id=? "
                + "WHERE item_id=?";
        try (Connection con = DatabaseConfig.get();
             PreparedStatement ps = con.prepareStatement(sql)) {
            bind(ps, item);
            ps.setInt(16, item.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw wrapUniqueName(e, item.getName());
        }
    }

    private void bind(PreparedStatement ps, InventoryItem item) throws SQLException {
        ps.setString(1, item.getName());
        ps.setString(2, blankToNull(item.getBrand()));
        ps.setInt(3, item.getQuantity());
        ps.setDouble(4, item.getUnitPrice());
        ps.setString(5, item.getBarcode());
        ps.setString(6, blankToNull(item.getHsnSac()));
        ps.setString(7, item.getItemType() != null ? item.getItemType() : "PRODUCT");
        ps.setString(8, blankToNull(item.getRimSize()));
        ps.setString(9, blankToNull(item.getTyreSize()));
        ps.setString(10, blankToNull(item.getPattern()));
        ps.setString(11, blankToNull(item.getTyreKind()));
        ps.setString(12, blankToNull(item.getProductCode()));
        ps.setDouble(13, item.getMrp());
        ps.setDouble(14, item.getBillingAmount());
        if (item.getPurchaseId() != null) ps.setInt(15, item.getPurchaseId());
        else ps.setNull(15, Types.INTEGER);
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private static RuntimeException wrapUniqueName(SQLException e, String name) {
        String msg = e.getMessage() != null ? e.getMessage() : "";
        if (msg.contains("uq_inventory_name") || "23505".equals(e.getSQLState())) {
            return new IllegalArgumentException(
                    "An item named \"" + name + "\" already exists. Use a different name or edit the existing item.");
        }
        return new RuntimeException(e);
    }

    @Override
    public void delete(int id) {
        try (Connection con = DatabaseConfig.get();
             PreparedStatement ps = con.prepareStatement("DELETE FROM Inventory WHERE item_id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public void adjustStock(int itemId, int delta) {
        String sql = "UPDATE Inventory SET available_quantity = available_quantity + ? WHERE item_id=?";
        try (Connection con = DatabaseConfig.get();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, delta);
            ps.setInt(2, itemId);
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    private InventoryItem map(ResultSet rs) throws SQLException {
        InventoryItem i = mapLegacy(rs);
        try { i.setHsnSac(rs.getString("hsn_sac")); } catch (SQLException ignored) {}
        try { i.setItemType(rs.getString("item_type")); } catch (SQLException ignored) {}
        try { i.setRimSize(rs.getString("rim_size")); } catch (SQLException ignored) {}
        try { i.setTyreSize(rs.getString("tyre_size")); } catch (SQLException ignored) {}
        try { i.setPattern(rs.getString("pattern")); } catch (SQLException ignored) {}
        try { i.setTyreKind(rs.getString("tyre_kind")); } catch (SQLException ignored) {}
        try { i.setProductCode(rs.getString("product_code")); } catch (SQLException ignored) {}
        try { i.setMrp(rs.getDouble("mrp")); } catch (SQLException ignored) {}
        try { i.setBillingAmount(rs.getDouble("billing_amount")); } catch (SQLException ignored) {}
        try {
            int pid = rs.getInt("purchase_id");
            if (!rs.wasNull()) i.setPurchaseId(pid);
        } catch (SQLException ignored) {}
        if (i.getItemType() == null || i.getItemType().isBlank()) i.setItemType("PRODUCT");
        return i;
    }

    private InventoryItem mapLegacy(ResultSet rs) throws SQLException {
        InventoryItem i = new InventoryItem();
        i.setId(rs.getInt("item_id"));
        i.setName(rs.getString("item_name"));
        try { i.setBrand(rs.getString("brand")); } catch (SQLException ignored) {}
        i.setQuantity(rs.getInt("available_quantity"));
        i.setUnitPrice(rs.getDouble("unit_price"));
        i.setReorderLevel(REORDER_LEVEL);
        try { i.setBarcode(rs.getString("barcode")); } catch (SQLException ignored) {}
        return i;
    }
}
