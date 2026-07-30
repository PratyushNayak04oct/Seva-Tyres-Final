package com.sevatyres.repository.impl;

import com.sevatyres.db.DatabaseConfig;
import com.sevatyres.model.CompanyInfo;
import com.sevatyres.model.CompanyMember;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcCompanyRepository {

    public CompanyInfo getCompany() {
        try (Connection con = DatabaseConfig.get();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM Company_Info WHERE company_id=1")) {
            if (rs.next()) return mapCompany(rs);
        } catch (SQLException e) { throw new RuntimeException(e); }
        CompanyInfo defaults = new CompanyInfo();
        saveCompany(defaults);
        return defaults;
    }

    public void saveCompany(CompanyInfo c) {
        // Prefer UPDATE; insert if no row
        String upd = "UPDATE Company_Info SET company_name=?,owner_name=?,email=?,phone=?,dbt_phone=?,"
                + "address=?,city=?,state=?,pincode=?,gstin=?,bank_name=?,bank_account=?,"
                + "bank_ifsc=?,upi_id=?,about_text=?,support_email=?,support_phone=?,alert_email=? "
                + "WHERE company_id=1";
        String ins = "INSERT INTO Company_Info(company_id,company_name,owner_name,email,phone,dbt_phone,"
                + "address,city,state,pincode,gstin,bank_name,bank_account,bank_ifsc,upi_id,"
                + "about_text,support_email,support_phone,alert_email) VALUES(1,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection con = DatabaseConfig.get();
             PreparedStatement ps = con.prepareStatement(upd)) {
            bindCompany(ps, c);
            int n = ps.executeUpdate();
            if (n == 0) {
                try (PreparedStatement insert = con.prepareStatement(ins)) {
                    bindCompany(insert, c);
                    insert.executeUpdate();
                }
            }
        } catch (SQLException e) {
            // Older DB without alert_email — save without that column
            saveCompanyLegacy(c);
        }
    }

    private void saveCompanyLegacy(CompanyInfo c) {
        String upd = "UPDATE Company_Info SET company_name=?,owner_name=?,email=?,phone=?,dbt_phone=?,"
                + "address=?,city=?,state=?,pincode=?,gstin=?,bank_name=?,bank_account=?,"
                + "bank_ifsc=?,upi_id=?,about_text=?,support_email=?,support_phone=? WHERE company_id=1";
        String ins = "INSERT INTO Company_Info(company_id,company_name,owner_name,email,phone,dbt_phone,"
                + "address,city,state,pincode,gstin,bank_name,bank_account,bank_ifsc,upi_id,"
                + "about_text,support_email,support_phone) VALUES(1,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        try (Connection con = DatabaseConfig.get();
             PreparedStatement ps = con.prepareStatement(upd)) {
            ps.setString(1, nullToEmpty(c.getCompanyName(), "Seva Tyres"));
            ps.setString(2, c.getOwnerName());
            ps.setString(3, c.getEmail());
            ps.setString(4, c.getPhone());
            ps.setString(5, c.getDbtPhone());
            ps.setString(6, c.getAddress());
            ps.setString(7, c.getCity());
            ps.setString(8, c.getState());
            ps.setString(9, c.getPincode());
            ps.setString(10, c.getGstin());
            ps.setString(11, c.getBankName());
            ps.setString(12, c.getBankAccount());
            ps.setString(13, c.getBankIfsc());
            ps.setString(14, c.getUpiId());
            ps.setString(15, c.getAboutText());
            ps.setString(16, c.getSupportEmail());
            ps.setString(17, c.getSupportPhone());
            if (ps.executeUpdate() == 0) {
                try (PreparedStatement insert = con.prepareStatement(ins)) {
                    insert.setString(1, nullToEmpty(c.getCompanyName(), "Seva Tyres"));
                    insert.setString(2, c.getOwnerName());
                    insert.setString(3, c.getEmail());
                    insert.setString(4, c.getPhone());
                    insert.setString(5, c.getDbtPhone());
                    insert.setString(6, c.getAddress());
                    insert.setString(7, c.getCity());
                    insert.setString(8, c.getState());
                    insert.setString(9, c.getPincode());
                    insert.setString(10, c.getGstin());
                    insert.setString(11, c.getBankName());
                    insert.setString(12, c.getBankAccount());
                    insert.setString(13, c.getBankIfsc());
                    insert.setString(14, c.getUpiId());
                    insert.setString(15, c.getAboutText());
                    insert.setString(16, c.getSupportEmail());
                    insert.setString(17, c.getSupportPhone());
                    insert.executeUpdate();
                }
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    private void bindCompany(PreparedStatement ps, CompanyInfo c) throws SQLException {
        ps.setString(1, nullToEmpty(c.getCompanyName(), "Seva Tyres"));
        ps.setString(2, c.getOwnerName());
        ps.setString(3, c.getEmail());
        ps.setString(4, c.getPhone());
        ps.setString(5, c.getDbtPhone());
        ps.setString(6, c.getAddress());
        ps.setString(7, c.getCity());
        ps.setString(8, c.getState());
        ps.setString(9, c.getPincode());
        ps.setString(10, c.getGstin());
        ps.setString(11, c.getBankName());
        ps.setString(12, c.getBankAccount());
        ps.setString(13, c.getBankIfsc());
        ps.setString(14, c.getUpiId());
        ps.setString(15, c.getAboutText());
        ps.setString(16, c.getSupportEmail());
        ps.setString(17, c.getSupportPhone());
        ps.setString(18, c.getAlertEmail());
    }

    public List<CompanyMember> findMembers() {
        List<CompanyMember> list = new ArrayList<>();
        try (Connection con = DatabaseConfig.get();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM Company_Member ORDER BY member_id")) {
            while (rs.next()) list.add(mapMember(rs));
        } catch (SQLException e) { throw new RuntimeException(e); }
        return list;
    }

    public CompanyMember saveMember(CompanyMember m) {
        String sql = "INSERT INTO Company_Member(member_name,role_title,email,phone,notes) VALUES(?,?,?,?,?)";
        try (Connection con = DatabaseConfig.get();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, m.getName());
            ps.setString(2, m.getRoleTitle());
            ps.setString(3, m.getEmail());
            ps.setString(4, m.getPhone());
            ps.setString(5, m.getNotes());
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) {
                if (k.next()) m.setId(k.getInt(1));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return m;
    }

    public void updateMember(CompanyMember m) {
        String sql = "UPDATE Company_Member SET member_name=?,role_title=?,email=?,phone=?,notes=? WHERE member_id=?";
        try (Connection con = DatabaseConfig.get();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, m.getName());
            ps.setString(2, m.getRoleTitle());
            ps.setString(3, m.getEmail());
            ps.setString(4, m.getPhone());
            ps.setString(5, m.getNotes());
            ps.setInt(6, m.getId());
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public void deleteMember(int id) {
        try (Connection con = DatabaseConfig.get();
             PreparedStatement ps = con.prepareStatement("DELETE FROM Company_Member WHERE member_id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public Optional<String> getSetting(String key) {
        try (Connection con = DatabaseConfig.get();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT setting_value FROM App_Setting WHERE setting_key=?")) {
            ps.setString(1, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.ofNullable(rs.getString(1));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return Optional.empty();
    }

    public void putSetting(String key, String value) {
        String pg = "INSERT INTO App_Setting(setting_key,setting_value) VALUES(?,?) "
                + "ON CONFLICT (setting_key) DO UPDATE SET setting_value=EXCLUDED.setting_value";
        try (Connection con = DatabaseConfig.get()) {
            try (PreparedStatement ps = con.prepareStatement(pg)) {
                ps.setString(1, key);
                ps.setString(2, value);
                ps.executeUpdate();
            } catch (SQLException e) {
                try (PreparedStatement del = con.prepareStatement("DELETE FROM App_Setting WHERE setting_key=?");
                     PreparedStatement ins = con.prepareStatement(
                             "INSERT INTO App_Setting(setting_key,setting_value) VALUES(?,?)")) {
                    del.setString(1, key);
                    del.executeUpdate();
                    ins.setString(1, key);
                    ins.setString(2, value);
                    ins.executeUpdate();
                } catch (SQLException e2) { throw new RuntimeException(e2); }
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    private CompanyInfo mapCompany(ResultSet rs) throws SQLException {
        CompanyInfo c = new CompanyInfo();
        c.setId(rs.getInt("company_id"));
        c.setCompanyName(rs.getString("company_name"));
        c.setOwnerName(rs.getString("owner_name"));
        c.setEmail(rs.getString("email"));
        c.setPhone(rs.getString("phone"));
        c.setDbtPhone(rs.getString("dbt_phone"));
        c.setAddress(rs.getString("address"));
        c.setCity(rs.getString("city"));
        c.setState(rs.getString("state"));
        c.setPincode(rs.getString("pincode"));
        c.setGstin(rs.getString("gstin"));
        c.setBankName(rs.getString("bank_name"));
        c.setBankAccount(rs.getString("bank_account"));
        c.setBankIfsc(rs.getString("bank_ifsc"));
        c.setUpiId(rs.getString("upi_id"));
        c.setAboutText(rs.getString("about_text"));
        c.setSupportEmail(rs.getString("support_email"));
        c.setSupportPhone(rs.getString("support_phone"));
        try { c.setAlertEmail(rs.getString("alert_email")); } catch (SQLException ignored) {}
        return c;
    }

    private CompanyMember mapMember(ResultSet rs) throws SQLException {
        CompanyMember m = new CompanyMember();
        m.setId(rs.getInt("member_id"));
        m.setName(rs.getString("member_name"));
        m.setRoleTitle(rs.getString("role_title"));
        m.setEmail(rs.getString("email"));
        m.setPhone(rs.getString("phone"));
        m.setNotes(rs.getString("notes"));
        return m;
    }

    private static String nullToEmpty(String v, String def) {
        return v == null || v.isBlank() ? def : v;
    }
}
