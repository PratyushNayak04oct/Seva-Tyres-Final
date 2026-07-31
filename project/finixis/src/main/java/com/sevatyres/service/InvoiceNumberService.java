package com.sevatyres.service;

import com.sevatyres.db.DatabaseConfig;
import com.sevatyres.util.GstUtil;

import java.sql.*;
import java.time.LocalDate;

/**
 * Generates invoice numbers: ST-26/27-070 (FY-wise sequence).
 */
public class InvoiceNumberService {

    public synchronized String nextInvoiceNumber(LocalDate saleDate) {
        String fy = GstUtil.financialYearLabel(saleDate);
        ensureRow(fy);
        int next;
        String sql = "UPDATE Invoice_Sequence SET last_seq = last_seq + 1 WHERE fy_label = ? RETURNING last_seq";
        try (Connection con = DatabaseConfig.get()) {
            // PostgreSQL RETURNING
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, fy);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        next = rs.getInt(1);
                        return format(fy, next);
                    }
                }
            } catch (SQLException ignored) {
                // H2 may not support RETURNING — fallback
            }
            try (PreparedStatement upd = con.prepareStatement(
                    "UPDATE Invoice_Sequence SET last_seq = last_seq + 1 WHERE fy_label = ?");
                 PreparedStatement sel = con.prepareStatement(
                         "SELECT last_seq FROM Invoice_Sequence WHERE fy_label = ?")) {
                upd.setString(1, fy);
                upd.executeUpdate();
                sel.setString(1, fy);
                try (ResultSet rs = sel.executeQuery()) {
                    if (!rs.next()) throw new SQLException("Invoice sequence missing for " + fy);
                    next = rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not allocate invoice number: " + e.getMessage(), e);
        }
        return format(fy, next);
    }

    private void ensureRow(String fy) {
        // Seed 26/27 at 69 so the next number is 070 (per business requirement)
        int seed = "26/27".equals(fy) ? 69 : 0;
        String sql = "INSERT INTO Invoice_Sequence(fy_label, last_seq) VALUES(?, ?) "
                + "ON CONFLICT (fy_label) DO NOTHING";
        try (Connection con = DatabaseConfig.get();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, fy);
            ps.setInt(2, seed);
            ps.executeUpdate();
        } catch (SQLException e) {
            // H2 / older: try merge-style insert ignore
            try (Connection con = DatabaseConfig.get();
                 PreparedStatement check = con.prepareStatement(
                         "SELECT 1 FROM Invoice_Sequence WHERE fy_label=?");
                 PreparedStatement ins = con.prepareStatement(
                         "INSERT INTO Invoice_Sequence(fy_label, last_seq) VALUES(?, ?)")) {
                check.setString(1, fy);
                try (ResultSet rs = check.executeQuery()) {
                    if (!rs.next()) {
                        ins.setString(1, fy);
                        ins.setInt(2, seed);
                        ins.executeUpdate();
                    }
                }
            } catch (SQLException e2) {
                throw new RuntimeException(e2);
            }
        }
    }

    private static String format(String fy, int seq) {
        return String.format("ST-%s-%03d", fy, seq);
    }
}
