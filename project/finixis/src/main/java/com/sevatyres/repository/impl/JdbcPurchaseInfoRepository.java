package com.sevatyres.repository.impl;

import com.sevatyres.db.DatabaseConfig;
import com.sevatyres.model.PurchaseInfo;
import com.sevatyres.repository.PurchaseInfoRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcPurchaseInfoRepository implements PurchaseInfoRepository {

    @Override
    public List<PurchaseInfo> findAll() {
        List<PurchaseInfo> list = new ArrayList<>();
        String sql = "SELECT * FROM Purchase_Info ORDER BY item_name";
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
    public Optional<PurchaseInfo> findById(int id) {
        try (Connection con = DatabaseConfig.get();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT * FROM Purchase_Info WHERE purchase_id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<PurchaseInfo> findByInventoryId(int inventoryId) {
        try (Connection con = DatabaseConfig.get();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT * FROM Purchase_Info WHERE inventory_id=? ORDER BY purchase_id DESC")) {
            ps.setInt(1, inventoryId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<PurchaseInfo> findByItemName(String itemName) {
        if (itemName == null || itemName.isBlank()) return Optional.empty();
        try (Connection con = DatabaseConfig.get();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT * FROM Purchase_Info WHERE LOWER(item_name)=LOWER(?) ORDER BY purchase_id DESC")) {
            ps.setString(1, itemName.trim());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public PurchaseInfo save(PurchaseInfo info) {
        if (info.getId() > 0) {
            String sql = "UPDATE Purchase_Info SET inventory_id=?, item_name=?, buying_price=?, notes=? WHERE purchase_id=?";
            try (Connection con = DatabaseConfig.get();
                 PreparedStatement ps = con.prepareStatement(sql)) {
                setParams(ps, info);
                ps.setInt(5, info.getId());
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            return info;
        }
        String sql = "INSERT INTO Purchase_Info(inventory_id, item_name, buying_price, notes) VALUES(?,?,?,?)";
        try (Connection con = DatabaseConfig.get();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setParams(ps, info);
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) {
                if (k.next()) info.setId(k.getInt(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return info;
    }

    @Override
    public void delete(int id) {
        try (Connection con = DatabaseConfig.get();
             PreparedStatement ps = con.prepareStatement(
                     "DELETE FROM Purchase_Info WHERE purchase_id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void setParams(PreparedStatement ps, PurchaseInfo info) throws SQLException {
        if (info.getInventoryId() != null) ps.setInt(1, info.getInventoryId());
        else ps.setNull(1, Types.INTEGER);
        ps.setString(2, info.getItemName());
        ps.setDouble(3, info.getBuyingPrice());
        ps.setString(4, info.getNotes());
    }

    private PurchaseInfo map(ResultSet rs) throws SQLException {
        PurchaseInfo p = new PurchaseInfo();
        p.setId(rs.getInt("purchase_id"));
        int inv = rs.getInt("inventory_id");
        if (!rs.wasNull()) p.setInventoryId(inv);
        p.setItemName(rs.getString("item_name"));
        p.setBuyingPrice(rs.getDouble("buying_price"));
        p.setNotes(rs.getString("notes"));
        return p;
    }
}
