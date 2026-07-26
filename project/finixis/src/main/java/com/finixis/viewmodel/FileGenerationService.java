package com.finixis.viewmodel;

import com.finixis.model.GeneratedFile;
import com.finixis.model.InventoryItem;
import com.finixis.model.Invoice;
import com.finixis.model.Transaction;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Generates real, openable PDF and Excel (.xlsx) files using only Java standard library.
 * Content is mock/demo data — no business logic. Files are written to ~/Downloads/Finixis/.
 */
public final class FileGenerationService {
    private FileGenerationService() {}

    private static int seq = 700;
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final DateTimeFormatter DISP = DateTimeFormatter.ofPattern("MMM d, yyyy");
    private static final DateTimeFormatter FULL = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a");

    // ─── Public API ────────────────────────────────────────────────────────────

    public static List<GeneratedFile> generateReport(List<Transaction> transactions) throws IOException {
        String ts = LocalDateTime.now().format(TS);
        String name = "Financial Report – " + LocalDateTime.now().format(DISP);
        File dir = outputDir();

        String[] headers = {"Date", "Customer", "Type", "Amount", "Status"};
        List<String[]> rows = new ArrayList<>();
        for (Transaction t : transactions) {
            rows.add(new String[]{
                    t.getDate().format(DateTimeFormatter.ofPattern("MMM d, yyyy")),
                    t.getCustomerName(),
                    t.getType().name(),
                    String.format("₹%,.2f", t.getAmount()),
                    t.isOngoing() ? "Pending" : "Cleared"
            });
        }

        File pdf  = writePdf(name, "Generated: " + LocalDateTime.now().format(FULL), headers, rows,
                new File(dir, "Report_" + ts + ".pdf"));
        File xlsx = writeExcel("Financial Report", headers, rows,
                new File(dir, "Report_" + ts + ".xlsx"));

        LocalDateTime now = LocalDateTime.now();
        return List.of(
                new GeneratedFile(++seq, name, "Report", "PDF",  now, pdf),
                new GeneratedFile(++seq, name, "Report", "Excel", now, xlsx)
        );
    }

    public static List<GeneratedFile> createInvoice(List<Invoice> invoices) throws IOException {
        String ts = LocalDateTime.now().format(TS);
        String name = "Invoice – " + LocalDateTime.now().format(DISP);
        File dir = outputDir();

        String[] headers = {"Invoice #", "Customer", "Issued", "Due", "Total"};
        List<String[]> rows = new ArrayList<>();
        for (Invoice inv : invoices) {
            rows.add(new String[]{
                    inv.getNumber(),
                    inv.getCustomerName(),
                    inv.getIssueDate().format(DateTimeFormatter.ofPattern("MMM d, yyyy")),
                    inv.getDueDate().format(DateTimeFormatter.ofPattern("MMM d, yyyy")),
                    String.format("₹%,.2f", inv.getTotal())
            });
        }

        File pdf  = writePdf(name, "Generated: " + LocalDateTime.now().format(FULL), headers, rows,
                new File(dir, "Invoice_" + ts + ".pdf"));
        File xlsx = writeExcel("Invoice Summary", headers, rows,
                new File(dir, "Invoice_" + ts + ".xlsx"));

        LocalDateTime now = LocalDateTime.now();
        return List.of(
                new GeneratedFile(++seq, name, "Invoice", "PDF",  now, pdf),
                new GeneratedFile(++seq, name, "Invoice", "Excel", now, xlsx)
        );
    }

    /**
     * Generate a per-transaction PDF invoice and return a GeneratedFile.
     * Overload that accepts only a Transaction (customer name from transaction).
     */
    public static GeneratedFile generateInvoiceForTransaction(Transaction t) throws IOException {
        return generateInvoiceForTransaction(t, null);
    }

    /**
     * Generate a per-transaction PDF invoice for the given transaction and customer.
     * Saves to ~/Downloads/Finixis/Invoice-{transactionId}.pdf
     */
    public static GeneratedFile generateInvoiceForTransaction(Transaction t,
            com.finixis.model.Customer customer) throws IOException {
        File dir = outputDir();
        String invoiceNum = "INV-" + t.getId();
        String customerName = (customer != null && customer.getName() != null)
                ? customer.getName()
                : (t.getCustomerName() != null ? t.getCustomerName() : "Unknown");
        String dateStr = t.getDate().format(DateTimeFormatter.ofPattern("MMM d, yyyy"));
        String generatedStr = LocalDateTime.now().format(FULL);

        File outFile = new File(dir, "Invoice-" + t.getId() + ".pdf");
        writeInvoicePdf(invoiceNum, customerName, dateStr, generatedStr, t, outFile);

        String name = "Invoice " + invoiceNum + " – " + customerName;
        return new GeneratedFile(++seq, name, "Invoice", "PDF", LocalDateTime.now(), outFile);
    }

    private static void writeInvoicePdf(String invoiceNum, String customerName,
            String dateStr, String generatedStr, Transaction t, File outFile) throws IOException {
        int pageW = 612, leftMargin = 50, rightMargin = 50;
        int contentW = pageW - leftMargin - rightMargin;

        StringBuilder cs = new StringBuilder();
        cs.append("BT\n");

        // Header: FINIXIS (top-left, bold large)
        cs.append("/F1 22 Tf 1 0 0 1 ").append(leftMargin).append(" 740 Tm (FINIXIS) Tj\n");
        // INVOICE (top-right, bold large)
        cs.append("/F1 18 Tf 1 0 0 1 460 740 Tm (INVOICE) Tj\n");
        cs.append("ET\n");

        // Horizontal rule under header
        cs.append("q 0.4 0.4 0.7 RG ").append(leftMargin).append(" 728 ").append(contentW).append(" 1.5 re f Q\n");

        cs.append("BT\n");
        // Invoice # and date
        cs.append("/F1 10 Tf 1 0 0 1 ").append(leftMargin).append(" 710 Tm (Invoice #: ")
          .append(pdfEsc(invoiceNum)).append(") Tj\n");
        cs.append("/F2 10 Tf 1 0 0 1 ").append(leftMargin).append(" 694 Tm (Date: ")
          .append(pdfEsc(dateStr)).append(") Tj\n");

        // Bill To section
        cs.append("/F1 11 Tf 1 0 0 1 ").append(leftMargin).append(" 668 Tm (BILL TO:) Tj\n");
        cs.append("/F2 11 Tf 1 0 0 1 ").append(leftMargin).append(" 652 Tm (")
          .append(pdfEsc(customerName)).append(") Tj\n");

        cs.append("ET\n");

        // Line separator
        cs.append("q 0.75 0.75 0.75 RG ").append(leftMargin).append(" 638 ").append(contentW).append(" 0.8 re f Q\n");

        // Table header
        cs.append("BT /F1 10 Tf\n");
        cs.append("1 0 0 1 ").append(leftMargin).append(" 622 Tm (Description) Tj\n");
        cs.append("1 0 0 1 350 622 Tm (Amount) Tj\n");
        cs.append("1 0 0 1 430 622 Tm (Paid) Tj\n");
        cs.append("1 0 0 1 500 622 Tm (Balance) Tj\n");
        cs.append("ET\n");

        cs.append("q 0.85 0.85 0.85 RG ").append(leftMargin).append(" 610 ").append(contentW).append(" 0.5 re f Q\n");

        // Data row
        String descText = t.getDescription() != null && !t.getDescription().isBlank()
                ? t.getDescription() : t.getType().name();
        cs.append("BT /F2 10 Tf\n");
        cs.append("1 0 0 1 ").append(leftMargin).append(" 595 Tm (").append(pdfEsc(truncate(descText, 30))).append(") Tj\n");
        cs.append("1 0 0 1 350 595 Tm (").append(pdfEsc(String.format("%.2f", t.getAmount()))).append(") Tj\n");
        cs.append("1 0 0 1 430 595 Tm (").append(pdfEsc(String.format("%.2f", t.getPaidAmount()))).append(") Tj\n");
        cs.append("1 0 0 1 500 595 Tm (").append(pdfEsc(String.format("%.2f", t.getBalance()))).append(") Tj\n");
        cs.append("ET\n");

        // Summary box
        cs.append("q 0.95 0.95 0.98 rg 350 555 210 55 re f Q\n");
        cs.append("BT /F1 10 Tf\n");
        cs.append("1 0 0 1 360 598 Tm (Subtotal:) Tj\n");
        cs.append("1 0 0 1 460 598 Tm (").append(pdfEsc(String.format("%.2f", t.getAmount()))).append(") Tj\n");
        cs.append("1 0 0 1 360 582 Tm (Amount Paid:) Tj\n");
        cs.append("1 0 0 1 460 582 Tm (").append(pdfEsc(String.format("%.2f", t.getPaidAmount()))).append(") Tj\n");
        cs.append("/F1 11 Tf 1 0 0 1 360 562 Tm (Balance Due:) Tj\n");
        cs.append("1 0 0 1 460 562 Tm (").append(pdfEsc(String.format("%.2f", t.getBalance()))).append(") Tj\n");
        cs.append("ET\n");

        // Status
        String statusText = t.isOngoing() ? "PENDING" : "PAID / ALL CLEARED";
        cs.append("BT /F1 12 Tf 1 0 0 1 ").append(leftMargin).append(" 562 Tm (Status: ")
          .append(pdfEsc(statusText)).append(") Tj ET\n");

        // Footer rule
        cs.append("q 0.4 0.4 0.7 RG ").append(leftMargin).append(" 80 ").append(contentW).append(" 1 re f Q\n");

        // Footer text
        cs.append("BT /F2 9 Tf\n");
        cs.append("1 0 0 1 ").append(leftMargin).append(" 65 Tm (Thank you for your business.) Tj\n");
        cs.append("1 0 0 1 ").append(leftMargin).append(" 50 Tm (Generated by Finixis  |  ")
          .append(pdfEsc(generatedStr)).append(") Tj\n");
        cs.append("ET\n");

        byte[] csBytes = cs.toString().getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);

        List<byte[]> objs = new ArrayList<>();
        objs.add(pdf("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n"));
        objs.add(pdf("2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n"));
        objs.add(pdf("3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792]"
                + " /Contents 4 0 R /Resources << /Font << /F1 5 0 R /F2 6 0 R >> >> >>\nendobj\n"));
        objs.add(pdfStream(csBytes, 4));
        objs.add(pdf("5 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold"
                + " /Encoding /WinAnsiEncoding >>\nendobj\n"));
        objs.add(pdf("6 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica"
                + " /Encoding /WinAnsiEncoding >>\nendobj\n"));

        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        byte[] hdr = ("%PDF-1.4\n%" + "\u00e2\u00e3\u00cf\u00d3" + "\n")
                .getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);
        out.write(hdr);
        int offset = hdr.length;
        List<Integer> xrefOffsets = new ArrayList<>();
        for (byte[] obj : objs) {
            xrefOffsets.add(offset);
            out.write(obj);
            offset += obj.length;
        }
        int xrefStart = offset;
        StringBuilder xref = new StringBuilder("xref\n0 ").append(objs.size() + 1).append("\n");
        xref.append("0000000000 65535 f \n");
        for (int off : xrefOffsets) xref.append(String.format("%010d 00000 n \n", off));
        byte[] xrefBytes = xref.toString().getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);
        out.write(xrefBytes);
        String trailer = "trailer\n<< /Size " + (objs.size() + 1) + " /Root 1 0 R >>\n"
                + "startxref\n" + xrefStart + "\n%%EOF\n";
        out.write(trailer.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1));

        Files.write(outFile.toPath(), out.toByteArray());
    }

    public static List<GeneratedFile> exportTransactions(List<Transaction> transactions) throws IOException {
        String ts = LocalDateTime.now().format(TS);
        String name = "Transaction Export – " + LocalDateTime.now().format(DISP);
        File dir = outputDir();

        String[] headers = {"Date", "Customer", "Description", "Type", "Amount", "Status"};
        List<String[]> rows = new ArrayList<>();
        for (Transaction t : transactions) {
            rows.add(new String[]{
                    t.getDate().format(DateTimeFormatter.ofPattern("MMM d, yyyy")),
                    t.getCustomerName(),
                    t.getDescription(),
                    t.getType().name(),
                    String.format("₹%,.2f", t.getAmount()),
                    t.isOngoing() ? "Pending" : "Cleared"
            });
        }

        File pdf  = writePdf(name, "Exported: " + LocalDateTime.now().format(FULL), headers, rows,
                new File(dir, "Export_" + ts + ".pdf"));
        File xlsx = writeExcel("Transactions", headers, rows,
                new File(dir, "Export_" + ts + ".xlsx"));

        LocalDateTime now = LocalDateTime.now();
        return List.of(
                new GeneratedFile(++seq, name, "Export", "PDF",  now, pdf),
                new GeneratedFile(++seq, name, "Export", "Excel", now, xlsx)
        );
    }

    /** Generate sample seed files at startup so the Reports page isn't empty. */
    public static List<GeneratedFile> seedSampleFiles(List<Transaction> transactions, List<Invoice> invoices) {
        try {
            List<GeneratedFile> result = new ArrayList<>();
            // Sample report
            String ts = "20260701_090000";
            String dir = outputDir().getPath();

            String[] rHeaders = {"Date", "Customer", "Type", "Amount", "Status"};
            List<String[]> rRows = new ArrayList<>();
            for (int i = 0; i < Math.min(10, transactions.size()); i++) {
                Transaction t = transactions.get(i);
                rRows.add(new String[]{
                        t.getDate().format(DateTimeFormatter.ofPattern("MMM d, yyyy")),
                        t.getCustomerName(), t.getType().name(),
                        String.format("₹%,.2f", t.getAmount()), t.isOngoing() ? "Pending" : "Cleared"
                });
            }
            File rPdf = writePdf("Q2 Financial Summary", "Generated: Jul 1, 2026  9:00 AM", rHeaders, rRows,
                    new File(outputDir(), "Report_" + ts + ".pdf"));
            File rXlsx = writeExcel("Q2 Financial Summary", rHeaders, rRows,
                    new File(outputDir(), "Report_" + ts + ".xlsx"));

            LocalDateTime t1 = LocalDateTime.of(2026, 7, 1, 9, 0);
            result.add(new GeneratedFile(++seq, "Q2 Financial Summary", "Report", "PDF",  t1, rPdf));
            result.add(new GeneratedFile(++seq, "Q2 Financial Summary", "Report", "Excel", t1, rXlsx));

            // Sample invoice
            if (!invoices.isEmpty()) {
                String[] iHeaders = {"Invoice #", "Customer", "Issued", "Due", "Total"};
                List<String[]> iRows = new ArrayList<>();
                for (Invoice inv : invoices) {
                    iRows.add(new String[]{
                            inv.getNumber(), inv.getCustomerName(),
                    inv.getIssueDate().format(DateTimeFormatter.ofPattern("MMM d, yyyy")),
                    inv.getDueDate().format(DateTimeFormatter.ofPattern("MMM d, yyyy")),
                    String.format("₹%,.2f", inv.getTotal())
                    });
                }
                File iPdf = writePdf("Invoice Package – Jun 2026", "Generated: Jun 28, 2026  4:15 PM",
                        iHeaders, iRows, new File(outputDir(), "Invoice_20260628_161500.pdf"));
                LocalDateTime t2 = LocalDateTime.of(2026, 6, 28, 16, 15);
                result.add(new GeneratedFile(++seq, "Invoice Package – Jun 2026", "Invoice", "PDF", t2, iPdf));
            }

            return result;
        } catch (IOException e) {
            System.err.println("[FileGenerationService] Warning: could not seed sample files: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    // ─── Internal helpers ──────────────────────────────────────────────────────

    private static File outputDir() {
        String home = System.getProperty("user.home");
        File dir = new File(home, "Downloads" + File.separator + "Finixis");
        dir.mkdirs();
        return dir;
    }

    // ─── Pure-Java PDF writer ──────────────────────────────────────────────────

    private static File writePdf(String title, String subtitle,
                                  String[] headers, List<String[]> dataRows, File outFile) throws IOException {
        // Column X positions (points from left margin)
        int cols = headers.length;
        int[] colX = new int[cols];
        int pageW = 612, leftMargin = 50, rightMargin = 50;
        int colW = (pageW - leftMargin - rightMargin) / cols;
        for (int i = 0; i < cols; i++) colX[i] = leftMargin + i * colW;

        // Build content stream
        StringBuilder cs = new StringBuilder();

        // Title
        cs.append("1 0 0 1 ").append(leftMargin).append(" 730 Tm /F1 18 Tf (")
          .append(pdfEsc(title)).append(") Tj\n");

        // Subtitle
        cs.append("1 0 0 1 ").append(leftMargin).append(" 705 Tm /F2 10 Tf (")
          .append(pdfEsc(subtitle)).append(") Tj\n");

        // Horizontal rule (thin rectangle)
        cs.append("q 0.6 0.6 0.6 RG ")
          .append(leftMargin).append(" 695 ")
          .append(pageW - leftMargin - rightMargin).append(" 0.8 re S Q\n");

        // Headers (bold, y=678)
        int y = 678;
        cs.append("BT /F1 10 Tf\n");
        for (int i = 0; i < cols; i++) {
            cs.append("1 0 0 1 ").append(colX[i]).append(" ").append(y)
              .append(" Tm (").append(pdfEsc(truncate(headers[i], 18))).append(") Tj\n");
        }
        cs.append("ET\n");

        // Header underline
        cs.append("q 0.8 0.8 0.8 RG ")
          .append(leftMargin).append(" 672 ")
          .append(pageW - leftMargin - rightMargin).append(" 0.5 re S Q\n");

        // Data rows
        int rowY = 657;
        int maxRows = Math.min(dataRows.size(), 36);
        cs.append("BT /F2 9 Tf\n");
        for (int r = 0; r < maxRows; r++) {
            String[] row = dataRows.get(r);
            // Alternating row shade
            if (r % 2 == 1) {
                cs.append("ET q 0.96 0.96 0.98 rg ")
                  .append(leftMargin).append(" ").append(rowY - 3)
                  .append(" ").append(pageW - leftMargin - rightMargin)
                  .append(" 15 re f Q BT /F2 9 Tf\n");
            }
            for (int i = 0; i < Math.min(cols, row.length); i++) {
                cs.append("1 0 0 1 ").append(colX[i]).append(" ").append(rowY)
                  .append(" Tm (").append(pdfEsc(truncate(row[i], 20))).append(") Tj\n");
            }
            rowY -= 15;
            if (rowY < 60) break; // page overflow guard
        }
        cs.append("ET\n");

        // Footer
        cs.append("1 0 0 1 ").append(leftMargin).append(" 40 Tm /F2 9 Tf (Finixis — UI Prototype  |  ")
          .append(pdfEsc(LocalDateTime.now().format(FULL))).append(") Tj\n");

        byte[] csBytes = cs.toString().getBytes(StandardCharsets.ISO_8859_1);

        // Build PDF objects as byte arrays, track offsets
        List<byte[]> objs = new ArrayList<>();
        objs.add(pdf("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n"));
        objs.add(pdf("2 0 obj\n<< /Type /Pages /Kids [3 0 R] /Count 1 >>\nendobj\n"));
        objs.add(pdf("3 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792]"
                + " /Contents 4 0 R /Resources << /Font << /F1 5 0 R /F2 6 0 R >> >> >>\nendobj\n"));
        objs.add(pdfStream(csBytes, 4));
        objs.add(pdf("5 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold"
                + " /Encoding /WinAnsiEncoding >>\nendobj\n"));
        objs.add(pdf("6 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica"
                + " /Encoding /WinAnsiEncoding >>\nendobj\n"));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] hdr = ("%PDF-1.4\n%" + "\u00e2\u00e3\u00cf\u00d3" + "\n")
                .getBytes(StandardCharsets.ISO_8859_1);
        out.write(hdr);
        int offset = hdr.length;
        List<Integer> xrefOffsets = new ArrayList<>();
        for (byte[] obj : objs) {
            xrefOffsets.add(offset);
            out.write(obj);
            offset += obj.length;
        }

        // xref
        int xrefStart = offset;
        StringBuilder xref = new StringBuilder("xref\n0 ").append(objs.size() + 1).append("\n");
        xref.append("0000000000 65535 f \n");
        for (int off : xrefOffsets) xref.append(String.format("%010d 00000 n \n", off));
        byte[] xrefBytes = xref.toString().getBytes(StandardCharsets.ISO_8859_1);
        out.write(xrefBytes);

        String trailer = "trailer\n<< /Size " + (objs.size() + 1) + " /Root 1 0 R >>\n"
                + "startxref\n" + xrefStart + "\n%%EOF\n";
        out.write(trailer.getBytes(StandardCharsets.ISO_8859_1));

        Files.write(outFile.toPath(), out.toByteArray());
        return outFile;
    }

    private static byte[] pdf(String s) { return s.getBytes(StandardCharsets.ISO_8859_1); }

    private static byte[] pdfStream(byte[] content, int objNum) {
        String header = objNum + " 0 obj\n<< /Length " + content.length + " >>\nstream\n";
        String footer = "\nendstream\nendobj\n";
        byte[] h = header.getBytes(StandardCharsets.ISO_8859_1);
        byte[] f = footer.getBytes(StandardCharsets.ISO_8859_1);
        byte[] result = new byte[h.length + content.length + f.length];
        System.arraycopy(h, 0, result, 0, h.length);
        System.arraycopy(content, 0, result, h.length, content.length);
        System.arraycopy(f, 0, result, h.length + content.length, f.length);
        return result;
    }

    private static String pdfEsc(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c == '(' || c == ')' || c == '\\') sb.append('\\');
            // Replace non-Latin1 chars with '?'
            sb.append(c < 256 ? c : '?');
        }
        return sb.toString();
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max - 1) + "…" : s;
    }

    // ─── Pure-Java Excel (.xlsx) writer ────────────────────────────────────────

    private static File writeExcel(String sheetTitle, String[] headers,
                                    List<String[]> dataRows, File outFile) throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(outFile))) {
            zip.setMethod(ZipOutputStream.DEFLATED);

            addZipEntry(zip, "[Content_Types].xml", contentTypesXml());
            addZipEntry(zip, "_rels/.rels", rootRelsXml());
            addZipEntry(zip, "xl/workbook.xml", workbookXml());
            addZipEntry(zip, "xl/_rels/workbook.xml.rels", workbookRelsXml());
            addZipEntry(zip, "xl/styles.xml", stylesXml());
            addZipEntry(zip, "xl/worksheets/sheet1.xml", sheet1Xml(sheetTitle, headers, dataRows));
        }
        return outFile;
    }

    private static void addZipEntry(ZipOutputStream zip, String name, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static String contentTypesXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
             + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
             + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
             + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
             + "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>"
             + "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
             + "<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>"
             + "</Types>";
    }

    private static String rootRelsXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
             + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
             + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>"
             + "</Relationships>";
    }

    private static String workbookXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
             + "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" "
             + "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">"
             + "<sheets><sheet name=\"Sheet1\" sheetId=\"1\" r:id=\"rId1\"/></sheets>"
             + "</workbook>";
    }

    private static String workbookRelsXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
             + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
             + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>"
             + "<Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>"
             + "</Relationships>";
    }

    private static String stylesXml() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
             + "<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">"
             + "<fonts count=\"2\">"
             + "<font><sz val=\"11\"/><name val=\"Calibri\"/></font>"
             + "<font><sz val=\"11\"/><b/><name val=\"Calibri\"/></font>"
             + "</fonts>"
             + "<fills count=\"3\">"
             + "<fill><patternFill patternType=\"none\"/></fill>"
             + "<fill><patternFill patternType=\"gray125\"/></fill>"
             + "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FF4472C4\"/></patternFill></fill>"
             + "</fills>"
             + "<borders count=\"1\"><border><left/><right/><top/><bottom/><diagonal/></border></borders>"
             + "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>"
             + "<cellXfs count=\"3\">"
             + "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>"  // 0 = normal
             + "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"2\" borderId=\"0\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\"><alignment horizontal=\"center\"/></xf>"  // 1 = header
             + "<xf numFmtId=\"4\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyNumberFormat=\"1\"/>"  // 2 = number
             + "</cellXfs>"
             + "</styleSheet>";
    }

    private static String sheet1Xml(String title, String[] headers, List<String[]> dataRows) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n")
          .append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">")
          .append("<sheetData>");

        // Title row
        sb.append("<row r=\"1\">")
          .append(cell("A1", title, 1))
          .append("</row>");

        // Header row (row 3, with bold/colored style)
        sb.append("<row r=\"3\">");
        for (int i = 0; i < headers.length; i++) {
            sb.append(cell(colRef(i) + "3", headers[i], 1));
        }
        sb.append("</row>");

        // Data rows (start at row 4)
        for (int r = 0; r < dataRows.size(); r++) {
            String[] row = dataRows.get(r);
            int excelRow = r + 4;
            sb.append("<row r=\"").append(excelRow).append("\">");
            for (int c = 0; c < row.length; c++) {
                sb.append(cell(colRef(c) + excelRow, row[c] == null ? "" : row[c], 0));
            }
            sb.append("</row>");
        }

        sb.append("</sheetData></worksheet>");
        return sb.toString();
    }

    /** Build a cell element with inline string content. */
    private static String cell(String ref, String value, int style) {
        String escaped = xmlEsc(value);
        return "<c r=\"" + ref + "\" t=\"inlineStr\" s=\"" + style + "\"><is><t>"
                + escaped + "</t></is></c>";
    }

    private static String colRef(int colIndex) {
        // A-Z for 0-25, AA-AZ for 26-51 (enough for typical table widths)
        if (colIndex < 26) return String.valueOf((char) ('A' + colIndex));
        return String.valueOf((char) ('A' + colIndex / 26 - 1))
             + String.valueOf((char) ('A' + colIndex % 26));
    }

    private static String xmlEsc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}
