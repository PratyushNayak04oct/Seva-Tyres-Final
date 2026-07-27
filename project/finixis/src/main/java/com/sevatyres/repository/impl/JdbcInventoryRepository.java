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
            "SELECT item_id,item_name,brand,available_quantity,unit_price,barcode FROM Inventory";

    @Override
    public List<InventoryItem> findAll() {
        List<InventoryItem> list = new ArrayList<>();
        try (Connection con = DatabaseConfig.get();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(SELECT_ALL + " ORDER BY item_name")) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) { throw new RuntimeException(e); }
        return list;
    }

    @Override
    public Optional<InventoryItem> findById(int id) {
        String sql = SELECT_ALL + " WHERE item_id=?";
        try (Connection con = DatabaseConfig.get();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public Optional<InventoryItem> findByBarcode(String barcode) {
        if (barcode == null || barcode.isBlank()) return Optional.empty();
        String sql = SELECT_ALL + " WHERE barcode=?";
        try (Connection con = DatabaseConfig.get();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, barcode.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public Optional<InventoryItem> findByName(String name) {
        if (name == null || name.isBlank()) return Optional.empty();
        String sql = SELECT_ALL + " WHERE LOWER(item_name)=LOWER(?)";
        try (Connection con = DatabaseConfig.get();
             PreparedStatement ps = con.prepareStatement(sql)) {
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
        String sql = "INSERT INTO Inventory(item_name,brand,available_quantity,unit_price,barcode) VALUES(?,?,?,?,?)";
        try (Connection con = DatabaseConfig.get();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, item.getName());
            ps.setString(2, blankToNull(item.getBrand()));
            ps.setInt(3, item.getQuantity());
            ps.setDouble(4, item.getUnitPrice());
            ps.setString(5, item.getBarcode());
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { k.next(); item.setId(k.getInt(1)); }
        } catch (SQLException e) {
            throw wrapUniqueName(e, item.getName());
        }
        return item;
    }

    private void update(InventoryItem item) {
        String sql = "UPDATE Inventory SET item_name=?,brand=?,available_quantity=?,unit_price=?,barcode=? WHERE item_id=?";
        try (Connection con = DatabaseConfig.get();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, item.getName());
            ps.setString(2, blankToNull(item.getBrand()));
            ps.setInt(3, item.getQuantity());
            ps.setDouble(4, item.getUnitPrice());
            ps.setString(5, item.getBarcode());
            ps.setInt(6, item.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw wrapUniqueName(e, item.getName());
        }
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
