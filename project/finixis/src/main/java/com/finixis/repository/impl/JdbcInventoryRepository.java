package com.finixis.repository.impl;

import com.finixis.db.DatabaseConfig;
import com.finixis.model.InventoryItem;
import com.finixis.repository.InventoryRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcInventoryRepository implements InventoryRepository {

    private static final int REORDER_LEVEL = 10; // items with qty <= this are "low stock"

    @Override
    public List<InventoryItem> findAll() {
        List<InventoryItem> list = new ArrayList<>();
        String sql = "SELECT item_id,item_name,available_quantity,unit_price FROM Inventory ORDER BY item_name";
        try (Connection con = DatabaseConfig.get();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) { throw new RuntimeException(e); }
        return list;
    }

    @Override
    public Optional<InventoryItem> findById(int id) {
        String sql = "SELECT item_id,item_name,available_quantity,unit_price FROM Inventory WHERE item_id=?";
        try (Connection con = DatabaseConfig.get();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public InventoryItem save(InventoryItem item) {
        if (item.getId() == 0) return insert(item);
        update(item); return item;
    }

    private InventoryItem insert(InventoryItem item) {
        String sql = "INSERT INTO Inventory(item_name,available_quantity,unit_price) VALUES(?,?,?)";
        try (Connection con = DatabaseConfig.get();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, item.getName());
            ps.setInt(2, item.getQuantity());
            ps.setDouble(3, item.getUnitPrice());
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { k.next(); item.setId(k.getInt(1)); }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return item;
    }

    private void update(InventoryItem item) {
        String sql = "UPDATE Inventory SET item_name=?,available_quantity=?,unit_price=? WHERE item_id=?";
        try (Connection con = DatabaseConfig.get();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, item.getName());
            ps.setInt(2, item.getQuantity());
            ps.setDouble(3, item.getUnitPrice());
            ps.setInt(4, item.getId());
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public void delete(int id) {
        try (Connection con = DatabaseConfig.get();
             PreparedStatement ps = con.prepareStatement("DELETE FROM Inventory WHERE item_id=?")) {
            ps.setInt(1, id); ps.executeUpdate();
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
        i.setQuantity(rs.getInt("available_quantity"));
        i.setUnitPrice(rs.getDouble("unit_price"));
        i.setReorderLevel(REORDER_LEVEL);
        return i;
    }
}
