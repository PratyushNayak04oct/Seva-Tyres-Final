package com.sevatyres.repository.impl;

import com.sevatyres.db.DatabaseConfig;
import com.sevatyres.model.Tax;
import com.sevatyres.repository.TaxRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcTaxRepository implements TaxRepository {

    @Override
    public List<Tax> findAll() {
        return query("SELECT * FROM Tax ORDER BY tax_name");
    }

    @Override
    public List<Tax> findActive() {
        return query("SELECT * FROM Tax WHERE is_active = TRUE ORDER BY tax_name");
    }

    @Override
    public Optional<Tax> findById(int id) {
        try (Connection con = DatabaseConfig.get();
             PreparedStatement ps = con.prepareStatement("SELECT * FROM Tax WHERE tax_id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public Tax save(Tax tax) {
        String sql = "INSERT INTO Tax(tax_name,tax_rate,description,is_active) VALUES(?,?,?,?)";
        try (Connection con = DatabaseConfig.get();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, tax.getName());
            ps.setDouble(2, tax.getRate());
            ps.setString(3, tax.getDescription());
            ps.setBoolean(4, tax.isActive());
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) {
                if (k.next()) tax.setId(k.getInt(1));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return tax;
    }

    @Override
    public void update(Tax tax) {
        String sql = "UPDATE Tax SET tax_name=?, tax_rate=?, description=?, is_active=? WHERE tax_id=?";
        try (Connection con = DatabaseConfig.get();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, tax.getName());
            ps.setDouble(2, tax.getRate());
            ps.setString(3, tax.getDescription());
            ps.setBoolean(4, tax.isActive());
            ps.setInt(5, tax.getId());
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public void delete(int id) {
        try (Connection con = DatabaseConfig.get();
             PreparedStatement ps = con.prepareStatement("DELETE FROM Tax WHERE tax_id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    private List<Tax> query(String sql) {
        List<Tax> list = new ArrayList<>();
        try (Connection con = DatabaseConfig.get();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) { throw new RuntimeException(e); }
        return list;
    }

    private Tax map(ResultSet rs) throws SQLException {
        Tax t = new Tax();
        t.setId(rs.getInt("tax_id"));
        t.setName(rs.getString("tax_name"));
        t.setRate(rs.getDouble("tax_rate"));
        t.setDescription(rs.getString("description"));
        t.setActive(rs.getBoolean("is_active"));
        return t;
    }
}
