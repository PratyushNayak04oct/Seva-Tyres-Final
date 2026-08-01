package com.sevatyres.repository.impl;

import com.sevatyres.db.DatabaseConfig;
import com.sevatyres.model.PurchaseInfo;
import com.sevatyres.repository.PurchaseInfoRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcPurchaseInfoRepository implements PurchaseInfoRepository {

    private static final String COLS =
            "purchase_id,inventory_id,item_name,brand,rim_size,tyre_size,pattern,tyre_kind,"
                    + "product_code,buying_price,rcp,mrp,notes";

    @Override
    public List<PurchaseInfo> findAll() {
        List<PurchaseInfo> list = new ArrayList<>();
        try (Connection con = DatabaseConfig.get();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT " + COLS + " FROM Purchase_Info ORDER BY item_name");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            return findAllLegacy();
        }
        return list;
    }

    private List<PurchaseInfo> findAllLegacy() {
        List<PurchaseInfo> list = new ArrayList<>();
        try (Connection con = DatabaseConfig.get();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT * FROM Purchase_Info ORDER BY item_name");
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
                     "SELECT " + COLS + " FROM Purchase_Info WHERE purchase_id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            return findAll().stream().filter(p -> p.getId() == id).findFirst();
        }
    }

    @Override
    public Optional<PurchaseInfo> findByInventoryId(int inventoryId) {
        try (Connection con = DatabaseConfig.get();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT " + COLS + " FROM Purchase_Info WHERE inventory_id=? ORDER BY purchase_id DESC")) {
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
                     "SELECT " + COLS + " FROM Purchase_Info WHERE LOWER(item_name)=LOWER(?) ORDER BY purchase_id DESC")) {
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
            String sql = "UPDATE Purchase_Info SET inventory_id=?, item_name=?, brand=?, rim_size=?, tyre_size=?,"
                    + " pattern=?, tyre_kind=?, product_code=?, buying_price=?, rcp=?, mrp=?, notes=? "
                    + "WHERE purchase_id=?";
            try (Connection con = DatabaseConfig.get();
                 PreparedStatement ps = con.prepareStatement(sql)) {
                bind(ps, info);
                ps.setInt(13, info.getId());
                ps.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            return info;
        }
        String sql = "INSERT INTO Purchase_Info(inventory_id,item_name,brand,rim_size,tyre_size,pattern,"
                + "tyre_kind,product_code,buying_price,rcp,mrp,notes) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection con = DatabaseConfig.get();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, info);
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

    private void bind(PreparedStatement ps, PurchaseInfo info) throws SQLException {
        if (info.getInventoryId() != null) ps.setInt(1, info.getInventoryId());
        else ps.setNull(1, Types.INTEGER);
        ps.setString(2, info.getItemName());
        ps.setString(3, blankToNull(info.getBrand()));
        ps.setString(4, blankToNull(info.getRimSize()));
        ps.setString(5, blankToNull(info.getTyreSize()));
        ps.setString(6, blankToNull(info.getPattern()));
        ps.setString(7, blankToNull(info.getTyreKind()));
        ps.setString(8, blankToNull(info.getProductCode()));
        ps.setDouble(9, info.getBuyingPrice());
        ps.setDouble(10, info.getRcp());
        ps.setDouble(11, info.getMrp());
        ps.setString(12, blankToNull(info.getNotes()));
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private PurchaseInfo map(ResultSet rs) throws SQLException {
        PurchaseInfo p = new PurchaseInfo();
        p.setId(rs.getInt("purchase_id"));
        int inv = rs.getInt("inventory_id");
        if (!rs.wasNull()) p.setInventoryId(inv);
        p.setItemName(rs.getString("item_name"));
        p.setBuyingPrice(rs.getDouble("buying_price"));
        try { p.setBrand(rs.getString("brand")); } catch (SQLException ignored) {}
        try { p.setRimSize(rs.getString("rim_size")); } catch (SQLException ignored) {}
        try { p.setTyreSize(rs.getString("tyre_size")); } catch (SQLException ignored) {}
        try { p.setPattern(rs.getString("pattern")); } catch (SQLException ignored) {}
        try { p.setTyreKind(rs.getString("tyre_kind")); } catch (SQLException ignored) {}
        try { p.setProductCode(rs.getString("product_code")); } catch (SQLException ignored) {}
        try { p.setRcp(rs.getDouble("rcp")); } catch (SQLException ignored) {}
        try { p.setMrp(rs.getDouble("mrp")); } catch (SQLException ignored) {}
        try { p.setNotes(rs.getString("notes")); } catch (SQLException ignored) {}
        return p;
    }
}
