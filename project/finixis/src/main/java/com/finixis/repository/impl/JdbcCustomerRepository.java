package com.finixis.repository.impl;

import com.finixis.db.DatabaseConfig;
import com.finixis.model.Customer;
import com.finixis.repository.CustomerRepository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcCustomerRepository implements CustomerRepository {

    @Override
    public List<Customer> findAll() {
        String sql = """
            SELECT c.customer_id, c.customer_name, c.location, c.contact, c.email,
                   c.creation_date,
                   COALESCE((SELECT SUM(tc.balance) FROM Transaction_Credit tc
                             WHERE tc.customer_id=c.customer_id AND tc.is_settled=FALSE),0)
                 - COALESCE((SELECT SUM(td.amount)  FROM Transaction_Debit td
                             WHERE td.customer_id=c.customer_id),0) AS balance
            FROM Customer c
            ORDER BY c.customer_name
            """;
        List<Customer> list = new ArrayList<>();
        try (Connection con = DatabaseConfig.get();
             Statement  st  = con.createStatement();
             ResultSet  rs  = st.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) { throw new RuntimeException(e); }
        return list;
    }

    @Override
    public Optional<Customer> findById(int id) {
        String sql = """
            SELECT c.customer_id, c.customer_name, c.location, c.contact, c.email,
                   c.creation_date,
                   COALESCE((SELECT SUM(tc.balance) FROM Transaction_Credit tc
                             WHERE tc.customer_id=c.customer_id AND tc.is_settled=FALSE),0)
                 - COALESCE((SELECT SUM(td.amount)  FROM Transaction_Debit td
                             WHERE td.customer_id=c.customer_id),0) AS balance
            FROM Customer c WHERE c.customer_id=?
            """;
        try (Connection con = DatabaseConfig.get();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public List<Customer> search(String query) {
        String sql = """
            SELECT c.customer_id, c.customer_name, c.location, c.contact, c.email,
                   c.creation_date,
                   COALESCE((SELECT SUM(tc.balance) FROM Transaction_Credit tc
                             WHERE tc.customer_id=c.customer_id AND tc.is_settled=FALSE),0)
                 - COALESCE((SELECT SUM(td.amount)  FROM Transaction_Debit td
                             WHERE td.customer_id=c.customer_id),0) AS balance
            FROM Customer c
            WHERE LOWER(c.customer_name) LIKE ? OR LOWER(c.email) LIKE ? OR c.contact LIKE ?
            ORDER BY c.customer_name
            """;
        String q = "%" + query.toLowerCase() + "%";
        List<Customer> list = new ArrayList<>();
        try (Connection con = DatabaseConfig.get();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, q); ps.setString(2, q); ps.setString(3, q);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return list;
    }

    @Override
    public Customer save(Customer c) {
        if (c.getId() == 0) return insert(c);
        update(c); return c;
    }

    private Customer insert(Customer c) {
        String sql = "INSERT INTO Customer(customer_name,location,contact,email,creation_date) VALUES(?,?,?,?,?)";
        try (Connection con = DatabaseConfig.get();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, c.getName());
            ps.setString(2, c.getAddress());
            ps.setString(3, c.getPhone());
            ps.setString(4, c.getEmail());
            ps.setDate(5, Date.valueOf(c.getCustomerSince() != null ? c.getCustomerSince() : java.time.LocalDate.now()));
            ps.executeUpdate();
            try (ResultSet k = ps.getGeneratedKeys()) { k.next(); c.setId(k.getInt(1)); }
        } catch (SQLException e) { throw new RuntimeException(e); }
        return c;
    }

    private void update(Customer c) {
        String sql = "UPDATE Customer SET customer_name=?,location=?,contact=?,email=? WHERE customer_id=?";
        try (Connection con = DatabaseConfig.get();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, c.getName());
            ps.setString(2, c.getAddress());
            ps.setString(3, c.getPhone());
            ps.setString(4, c.getEmail());
            ps.setInt(5, c.getId());
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public void delete(int id) {
        try (Connection con = DatabaseConfig.get();
             PreparedStatement ps = con.prepareStatement("DELETE FROM Customer WHERE customer_id=?")) {
            ps.setInt(1, id); ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    @Override
    public double getBalance(int customerId) {
        String sql = """
            SELECT
              COALESCE((SELECT SUM(balance) FROM Transaction_Credit
                        WHERE customer_id=? AND is_settled=FALSE),0)
            - COALESCE((SELECT SUM(amount)  FROM Transaction_Debit
                        WHERE customer_id=?),0)
            """;
        try (Connection con = DatabaseConfig.get();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, customerId); ps.setInt(2, customerId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getDouble(1) : 0; }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    private Customer map(ResultSet rs) throws SQLException {
        Customer c = new Customer();
        c.setId(rs.getInt("customer_id"));
        c.setName(rs.getString("customer_name"));
        c.setAddress(rs.getString("location"));
        c.setPhone(rs.getString("contact"));
        c.setEmail(rs.getString("email"));
        Date d = rs.getDate("creation_date");
        c.setCustomerSince(d != null ? d.toLocalDate() : null);
        c.setBalance(rs.getDouble("balance"));
        return c;
    }
}
