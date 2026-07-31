package com.sevatyres.service;

import com.sevatyres.db.DatabaseConfig;
import com.sevatyres.util.GstUtil;

import java.sql.*;
import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Invoice numbers: ST-26/27-070 (FY-wise sequence).
 * last_seq is the last issued number for that FY; next sale uses last_seq + 1.
 */
public class InvoiceNumberService {

    private static final Pattern INVOICE_PAT =
            Pattern.compile("^ST-(\\d{2}/\\d{2})-(\\d{1,6})$", Pattern.CASE_INSENSITIVE);

    public synchronized String nextInvoiceNumber(LocalDate saleDate) {
        String fy = GstUtil.financialYearLabel(saleDate);
        ensureRow(fy);
        int next = increment(fy);
        return format(fy, next);
    }

    /** Last issued invoice for the FY (e.g. ST-26/27-069). */
    public String getCurrentInvoiceNumber(LocalDate date) {
        String fy = GstUtil.financialYearLabel(date);
        ensureRow(fy);
        return format(fy, getLastSeq(fy));
    }

    /** Next invoice that would be allocated (does not increment). */
    public String peekNextInvoiceNumber(LocalDate date) {
        String fy = GstUtil.financialYearLabel(date);
        ensureRow(fy);
        return format(fy, getLastSeq(fy) + 1);
    }

    /**
     * Sets the current (last issued) invoice number in the DB.
     * Accepts formats like ST-26/27-069.
     */
    public synchronized void setCurrentInvoiceNumber(String invoiceNo) {
        if (invoiceNo == null || invoiceNo.isBlank()) {
            throw new IllegalArgumentException("Invoice number is required.");
        }
        Matcher m = INVOICE_PAT.matcher(invoiceNo.trim());
        if (!m.matches()) {
            throw new IllegalArgumentException(
                    "Invalid format. Use ST-YY/YY-NNN (e.g. ST-26/27-069).");
        }
        String fy = m.group(1);
        int seq = Integer.parseInt(m.group(2));
        if (seq < 0) throw new IllegalArgumentException("Sequence cannot be negative.");
        ensureRow(fy);
        try (Connection con = DatabaseConfig.get();
             PreparedStatement ps = con.prepareStatement(
                     "UPDATE Invoice_Sequence SET last_seq = ? WHERE fy_label = ?")) {
            ps.setInt(1, seq);
            ps.setString(2, fy);
            if (ps.executeUpdate() == 0) {
                try (PreparedStatement ins = con.prepareStatement(
                        "INSERT INTO Invoice_Sequence(fy_label, last_seq) VALUES(?, ?)")) {
                    ins.setString(1, fy);
                    ins.setInt(2, seq);
                    ins.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not update invoice number: " + e.getMessage(), e);
        }
    }

    /**
     * After manually assigning a bill number on a sale, advance the sequence
     * so the next auto number stays ahead of this one (same FY only).
     */
    public synchronized void noteIssued(String invoiceNo, LocalDate saleDate) {
        if (invoiceNo == null || invoiceNo.isBlank()) return;
        Matcher m = INVOICE_PAT.matcher(invoiceNo.trim());
        if (!m.matches()) return;
        String fy = m.group(1);
        String currentFy = GstUtil.financialYearLabel(saleDate);
        if (!fy.equals(currentFy)) return;
        int seq = Integer.parseInt(m.group(2));
        ensureRow(fy);
        int last = getLastSeq(fy);
        if (seq > last) {
            setCurrentInvoiceNumber(format(fy, seq));
        }
    }

    private int increment(String fy) {
        String sql = "UPDATE Invoice_Sequence SET last_seq = last_seq + 1 WHERE fy_label = ? RETURNING last_seq";
        try (Connection con = DatabaseConfig.get()) {
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, fy);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) return rs.getInt(1);
                }
            } catch (SQLException ignored) { /* H2 fallback */ }
            try (PreparedStatement upd = con.prepareStatement(
                    "UPDATE Invoice_Sequence SET last_seq = last_seq + 1 WHERE fy_label = ?");
                 PreparedStatement sel = con.prepareStatement(
                         "SELECT last_seq FROM Invoice_Sequence WHERE fy_label = ?")) {
                upd.setString(1, fy);
                upd.executeUpdate();
                sel.setString(1, fy);
                try (ResultSet rs = sel.executeQuery()) {
                    if (!rs.next()) throw new SQLException("Invoice sequence missing for " + fy);
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not allocate invoice number: " + e.getMessage(), e);
        }
    }

    private int getLastSeq(String fy) {
        try (Connection con = DatabaseConfig.get();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT last_seq FROM Invoice_Sequence WHERE fy_label = ?")) {
            ps.setString(1, fy);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return 0;
    }

    private void ensureRow(String fy) {
        int seed = "26/27".equals(fy) ? 69 : 0;
        String sql = "INSERT INTO Invoice_Sequence(fy_label, last_seq) VALUES(?, ?) "
                + "ON CONFLICT (fy_label) DO NOTHING";
        try (Connection con = DatabaseConfig.get();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, fy);
            ps.setInt(2, seed);
            ps.executeUpdate();
        } catch (SQLException e) {
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

    public static String format(String fy, int seq) {
        return String.format("ST-%s-%03d", fy, seq);
    }
}
