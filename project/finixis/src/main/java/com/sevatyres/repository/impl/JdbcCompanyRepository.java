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
        // Ensure singleton row
        CompanyInfo defaults = new CompanyInfo();
        saveCompany(defaults);
        return defaults;
    }

    public void saveCompany(CompanyInfo c) {
        String upsert = "MERGE INTO Company_Info (company_id, company_name, owner_name, email, phone, dbt_phone, "
                + "address, city, state, pincode, gstin, bank_name, bank_account, bank_ifsc, upi_id, "
                + "about_text, support_email, support_phone) KEY(company_id) VALUES "
                + "(1,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
        // H2 supports MERGE; PostgreSQL uses INSERT ON CONFLICT
        String pg = "INSERT INTO Company_Info(company_id,company_name,owner_name,email,phone,dbt_phone,"
                + "address,city,state,pincode,gstin,bank_name,bank_account,bank_ifsc,upi_id,"
                + "about_text,support_email,support_phone) VALUES(1,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) "
                + "ON CONFLICT (company_id) DO UPDATE SET "
                + "company_name=EXCLUDED.company_name, owner_name=EXCLUDED.owner_name, email=EXCLUDED.email, "
                + "phone=EXCLUDED.phone, dbt_phone=EXCLUDED.dbt_phone, address=EXCLUDED.address, "
                + "city=EXCLUDED.city, state=EXCLUDED.state, pincode=EXCLUDED.pincode, gstin=EXCLUDED.gstin, "
                + "bank_name=EXCLUDED.bank_name, bank_account=EXCLUDED.bank_account, bank_ifsc=EXCLUDED.bank_ifsc, "
                + "upi_id=EXCLUDED.upi_id, about_text=EXCLUDED.about_text, "
                + "support_email=EXCLUDED.support_email, support_phone=EXCLUDED.support_phone";
        try (Connection con = DatabaseConfig.get()) {
            try (PreparedStatement ps = con.prepareStatement(pg)) {
                bindCompany(ps, c);
                ps.executeUpdate();
            } catch (SQLException primary) {
                try (PreparedStatement ps = con.prepareStatement(upsert)) {
                    bindCompany(ps, c);
                    ps.executeUpdate();
                } catch (SQLException secondary) {
                    // Fallback: update then insert
                    try (PreparedStatement upd = con.prepareStatement(
                            "UPDATE Company_Info SET company_name=?,owner_name=?,email=?,phone=?,dbt_phone=?,"
                                    + "address=?,city=?,state=?,pincode=?,gstin=?,bank_name=?,bank_account=?,"
                                    + "bank_ifsc=?,upi_id=?,about_text=?,support_email=?,support_phone=? "
                                    + "WHERE company_id=1")) {
                        bindCompany(upd, c);
                        int n = upd.executeUpdate();
                        if (n == 0) {
                            try (PreparedStatement ins = con.prepareStatement(
                                    "INSERT INTO Company_Info(company_id,company_name,owner_name,email,phone,dbt_phone,"
                                            + "address,city,state,pincode,gstin,bank_name,bank_account,bank_ifsc,upi_id,"
                                            + "about_text,support_email,support_phone) VALUES(1,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)")) {
                                bindCompany(ins, c);
                                ins.executeUpdate();
                            }
                        }
                    }
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
