package com.sevatyres.viewmodel;

import com.sevatyres.model.CompanyInfo;
import com.sevatyres.model.GeneratedFile;
import com.sevatyres.model.InventoryItem;
import com.sevatyres.model.Invoice;
import com.sevatyres.model.SaleTransaction;
import com.sevatyres.model.SaleTransactionItem;
import com.sevatyres.model.Transaction;
import com.sevatyres.repository.impl.JdbcSaleTransactionRepository;
import com.sevatyres.service.AppServices;
import com.sevatyres.service.CompanyService;
import com.sevatyres.util.AmountInWords;
import com.sevatyres.util.GstUtil;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Generates real, openable PDF and Excel (.xlsx) files using only Java standard library.
 * Files are written to ~/Downloads/SevaTypres/.
 */
public final class FileGenerationService {
    private FileGenerationService() {}

    private static int seq = 700;

    /** Path to the user-uploaded invoice template (Task 8). */
    private static String invoiceTemplatePath = null;

    public static String getInvoiceTemplatePath() { return invoiceTemplatePath; }
    public static void setInvoiceTemplatePath(String path) { invoiceTemplatePath = path; }
    private static final DateTimeFormatter TS   = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final DateTimeFormatter DISP = DateTimeFormatter.ofPattern("MMM d, yyyy");
    private static final DateTimeFormatter FULL = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a");

    // ─── Public API ────────────────────────────────────────────────────────────

    /** Generate a sale transaction report in PDF or Excel format (Task 2 / Generate Report). */
    public static GeneratedFile generateSaleReport(List<SaleTransaction> txns, String format) throws IOException {
        String ts   = LocalDateTime.now().format(TS);
        String name = "Seva Tyres Transaction Report – " + LocalDateTime.now().format(DISP);
        File dir    = outputDir();

        String[] headers = {"Date", "Bill No", "Particulars", "Brand", "Qty",
                "PhonePe", "Cash", "Credit", "Total", "Customer"};
        List<String[]> rows = new ArrayList<>();
        for (SaleTransaction t : txns) {
            rows.add(new String[]{
                    t.getSaleDate() != null ? t.getSaleDate().format(DateTimeFormatter.ofPattern("MMM d, yyyy")) : "",
                    t.getBillNo() != null ? t.getBillNo() : "",
                    t.getParticulars() != null ? t.getParticulars() : "",
                    t.getBrand() != null ? t.getBrand() : "",
                    String.valueOf(t.getQuantity()),
                    String.format("%.2f", t.getPhonePe()),
                    String.format("%.2f", t.getCash()),
                    String.format("%.2f", t.getCreditAmount()),
                    String.format("%.2f", t.getTotal()),
                    t.getCustomerName() != null ? t.getCustomerName() : ""
            });
        }

        LocalDateTime now = LocalDateTime.now();
        if ("PDF".equalsIgnoreCase(format)) {
            File pdf = writePdf(name, "Generated: " + now.format(FULL), headers, rows,
                    new File(dir, "SaleReport_" + ts + ".pdf"));
            return new GeneratedFile(++seq, name, "Report", "PDF", now, pdf);
        } else {
            File xlsx = writeExcel("Sale Transactions", headers, rows,
                    new File(dir, "SaleReport_" + ts + ".xlsx"));
            return new GeneratedFile(++seq, name, "Report", "Excel", now, xlsx);
        }
    }

    public static List<GeneratedFile> generateReport(List<Transaction> transactions) throws IOException {
        String ts   = LocalDateTime.now().format(TS);
        String name = "Financial Report – " + LocalDateTime.now().format(DISP);
        File dir    = outputDir();

        String[] headers = {"Date", "Customer", "Type", "Amount", "Status"};
        List<String[]> rows = new ArrayList<>();
        for (Transaction t : transactions) {
            rows.add(new String[]{
                    t.getDate().format(DateTimeFormatter.ofPattern("MMM d, yyyy")),
                    t.getCustomerName(),
                    t.getType().name(),
                    String.format("\u20b9%,.2f", t.getAmount()),
                    t.isOngoing() ? "Pending" : "Cleared"
            });
        }

        File pdf  = writePdf(name, "Generated: " + LocalDateTime.now().format(FULL), headers, rows,
                new File(dir, "Report_" + ts + ".pdf"));
        File xlsx = writeExcel("Financial Report", headers, rows,
                new File(dir, "Report_" + ts + ".xlsx"));

        LocalDateTime now = LocalDateTime.now();
        return List.of(
                new GeneratedFile(++seq, name, "Report", "PDF",   now, pdf),
                new GeneratedFile(++seq, name, "Report", "Excel", now, xlsx)
        );
    }

    public static List<GeneratedFile> createInvoice(List<Invoice> invoices) throws IOException {
        String ts   = LocalDateTime.now().format(TS);
        String name = "Invoice – " + LocalDateTime.now().format(DISP);
        File dir    = outputDir();

        String[] headers = {"Invoice #", "Customer", "Issued", "Due", "Total"};
        List<String[]> rows = new ArrayList<>();
        for (Invoice inv : invoices) {
            rows.add(new String[]{
                    inv.getNumber(),
                    inv.getCustomerName(),
                    inv.getIssueDate().format(DateTimeFormatter.ofPattern("MMM d, yyyy")),
                    inv.getDueDate().format(DateTimeFormatter.ofPattern("MMM d, yyyy")),
                    String.format("\u20b9%,.2f", inv.getTotal())
            });
        }

        File pdf  = writePdf(name, "Generated: " + LocalDateTime.now().format(FULL), headers, rows,
                new File(dir, "Invoice_" + ts + ".pdf"));
        File xlsx = writeExcel("Invoice Summary", headers, rows,
                new File(dir, "Invoice_" + ts + ".xlsx"));

        LocalDateTime now = LocalDateTime.now();
        return List.of(
                new GeneratedFile(++seq, name, "Invoice", "PDF",   now, pdf),
                new GeneratedFile(++seq, name, "Invoice", "Excel", now, xlsx)
        );
    }

    /** Generate a professional HTML invoice from the uploaded/built-in template + DB data.
     *  Bill number is used as the document reference (not a separate invoice id). */
    public static GeneratedFile generateSaleInvoice(SaleTransaction t) throws IOException {
        File dir = outputDir();
        String billNo = (t.getBillNo() != null && !t.getBillNo().isBlank())
                ? t.getBillNo().trim()
                : ("B" + t.getId());
        String customerName = t.getCustomerName() != null ? t.getCustomerName() : "Walk-in Customer";
        String generatedStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("d MMMM yyyy"));

        List<SaleTransactionItem> items = new JdbcSaleTransactionRepository().findItemsBySaleId(t.getId());
        String safeName = billNo.replaceAll("[^A-Za-z0-9._-]", "_");
        File outFile = new File(dir, "Invoice-" + safeName + ".html");
        writeSaleInvoiceHtml(billNo, customerName, generatedStr, t, items, outFile);

        String name = "Invoice " + billNo + " – " + customerName;
        return new GeneratedFile(++seq, name, "Invoice", "HTML", LocalDateTime.now(), outFile);
    }

    /** Generate a per-transaction PDF invoice for a Transaction (credit/debit). */
    public static GeneratedFile generateInvoiceForTransaction(Transaction t) throws IOException {
        return generateInvoiceForTransaction(t, null);
    }

    public static GeneratedFile generateInvoiceForTransaction(Transaction t,
            com.sevatyres.model.Customer customer) throws IOException {
        File dir = outputDir();
        String invoiceNum = "INV-" + t.getId();
        String customerName = (customer != null && customer.getName() != null)
                ? customer.getName()
                : (t.getCustomerName() != null ? t.getCustomerName() : "Unknown");
        String dateStr      = t.getDate().format(DateTimeFormatter.ofPattern("MMM d, yyyy"));
        String generatedStr = LocalDateTime.now().format(FULL);

        File outFile = new File(dir, "Invoice-" + t.getId() + ".pdf");
        writeInvoicePdf(invoiceNum, customerName, dateStr, generatedStr, t, outFile);

        String name = "Invoice " + invoiceNum + " – " + customerName;
        return new GeneratedFile(++seq, name, "Invoice", "PDF", LocalDateTime.now(), outFile);
    }

    public static List<GeneratedFile> exportTransactions(List<Transaction> transactions) throws IOException {
        String ts   = LocalDateTime.now().format(TS);
        String name = "Transaction Export – " + LocalDateTime.now().format(DISP);
        File dir    = outputDir();

        String[] headers = {"Date", "Customer", "Description", "Type", "Amount", "Status"};
        List<String[]> rows = new ArrayList<>();
        for (Transaction t : transactions) {
            rows.add(new String[]{
                    t.getDate().format(DateTimeFormatter.ofPattern("MMM d, yyyy")),
                    t.getCustomerName(),
                    t.getDescription(),
                    t.getType().name(),
                    String.format("\u20b9%,.2f", t.getAmount()),
                    t.isOngoing() ? "Pending" : "Cleared"
            });
        }

        File pdf  = writePdf(name, "Exported: " + LocalDateTime.now().format(FULL), headers, rows,
                new File(dir, "Export_" + ts + ".pdf"));
        File xlsx = writeExcel("Transactions", headers, rows,
                new File(dir, "Export_" + ts + ".xlsx"));

        LocalDateTime now = LocalDateTime.now();
        return List.of(
                new GeneratedFile(++seq, name, "Export", "PDF",   now, pdf),
                new GeneratedFile(++seq, name, "Export", "Excel", now, xlsx)
        );
    }

    public static List<GeneratedFile> seedSampleFiles(List<Transaction> transactions, List<Invoice> invoices) {
        try {
            List<GeneratedFile> result = new ArrayList<>();
            String ts = "20260701_090000";

            String[] rHeaders = {"Date", "Customer", "Type", "Amount", "Status"};
            List<String[]> rRows = new ArrayList<>();
            for (int i = 0; i < Math.min(10, transactions.size()); i++) {
                Transaction t = transactions.get(i);
                rRows.add(new String[]{
                        t.getDate().format(DateTimeFormatter.ofPattern("MMM d, yyyy")),
                        t.getCustomerName(), t.getType().name(),
                        String.format("\u20b9%,.2f", t.getAmount()), t.isOngoing() ? "Pending" : "Cleared"
                });
            }
            File rPdf = writePdf("Seva Tyres Q2 Financial Summary", "Generated: Jul 1, 2026  9:00 AM",
                    rHeaders, rRows, new File(outputDir(), "Report_" + ts + ".pdf"));
            File rXlsx = writeExcel("Q2 Financial Summary", rHeaders, rRows,
                    new File(outputDir(), "Report_" + ts + ".xlsx"));

            LocalDateTime t1 = LocalDateTime.of(2026, 7, 1, 9, 0);
            result.add(new GeneratedFile(++seq, "Q2 Financial Summary", "Report", "PDF",   t1, rPdf));
            result.add(new GeneratedFile(++seq, "Q2 Financial Summary", "Report", "Excel", t1, rXlsx));

            if (!invoices.isEmpty()) {
                String[] iHeaders = {"Invoice #", "Customer", "Issued", "Due", "Total"};
                List<String[]> iRows = new ArrayList<>();
                for (Invoice inv : invoices) {
                    iRows.add(new String[]{
                            inv.getNumber(), inv.getCustomerName(),
                            inv.getIssueDate().format(DateTimeFormatter.ofPattern("MMM d, yyyy")),
                            inv.getDueDate().format(DateTimeFormatter.ofPattern("MMM d, yyyy")),
                            String.format("\u20b9%,.2f", inv.getTotal())
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
        File dir = new File(home, "Downloads" + File.separator + "SevaTypres");
        dir.mkdirs();
        return dir;
    }

    // â”€â”€â”€ Professional HTML invoice from template file â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    private static void writeSaleInvoiceHtml(String billNo, String customerName,
            String generatedStr, SaleTransaction t, List<SaleTransactionItem> items,
            File outFile) throws IOException {
        CompanyInfo company;
        String template;
        try {
            company = AppServices.company().getCompany();
            template = AppServices.company().getInvoiceHtmlTemplate();
        } catch (Exception e) {
            company = new CompanyInfo();
            template = CompanyService.loadBuiltinInvoiceHtmlTemplate();
        }
        // Prefer built-in Tax Invoice layout when uploaded template lacks GST placeholders
        if (template == null || !template.contains("{cgst_total}") || !template.contains("{hsn_summary_html}")) {
            template = CompanyService.loadBuiltinInvoiceHtmlTemplate();
        }

        if (items == null || items.isEmpty()) {
            items = new ArrayList<>();
            items.add(new SaleTransactionItem(
                    t.getInventoryItemId(),
                    t.getParticulars() != null ? t.getParticulars() : "Sale",
                    Math.max(1, t.getQuantity()),
                    t.getUnitPrice() > 0 ? t.getUnitPrice() : Math.max(0, t.getSubtotal())));
        }

        LocalDate saleDate = t.getSaleDate() != null ? t.getSaleDate() : LocalDate.now();
        String dateStr = saleDate.format(DateTimeFormatter.ofPattern("dd-MMM-yy"));
        String dateLong = saleDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy"));

        // Ensure GST split on each line
        double taxableSum = 0, cgstSum = 0, sgstSum = 0;
        int totalQty = 0;
        Map<String, double[]> hsnMap = new LinkedHashMap<>(); // hsn -> [taxable,cgst,sgst]
        StringBuilder itemsHtml = new StringBuilder();
        int n = 1;
        for (SaleTransactionItem it : items) {
            GstUtil.LineGst split = (it.getTaxableAmount() > 0 || it.getCgstAmount() > 0)
                    ? new GstUtil.LineGst(it.getTaxableAmount(), it.getCgstAmount(),
                    it.getSgstAmount(), it.getLineTotal() > 0 ? it.getLineTotal()
                    : GstUtil.round2(it.getUnitPrice() * it.getQuantity()))
                    : GstUtil.splitLine(it.getUnitPrice(), it.getQuantity());
            double rate = it.getQuantity() > 0
                    ? GstUtil.round2(split.taxable() / it.getQuantity()) : 0;
            if (it.getRate() > 0) rate = it.getRate();
            taxableSum += split.taxable();
            cgstSum += split.cgst();
            sgstSum += split.sgst();
            totalQty += it.getQuantity();
            String hsn = nz(it.getHsnSac(), "");
            hsnMap.computeIfAbsent(hsn.isBlank() ? "-" : hsn, k -> new double[3]);
            double[] acc = hsnMap.get(hsn.isBlank() ? "-" : hsn);
            acc[0] += split.taxable();
            acc[1] += split.cgst();
            acc[2] += split.sgst();

            String desc = htmlEsc(it.getItemName() != null ? it.getItemName() : "");
            if (it.getRimSize() != null && !it.getRimSize().isBlank()) {
                desc += "<br/><span class=\"small muted\">Rim: " + htmlEsc(it.getRimSize()) + "</span>";
            }
            itemsHtml.append("      <tr>\n")
                    .append("        <td class=\"center\">").append(n++).append("</td>\n")
                    .append("        <td>").append(desc).append("</td>\n")
                    .append("        <td class=\"center\">").append(htmlEsc(hsn)).append("</td>\n")
                    .append("        <td class=\"center\">18%</td>\n")
                    .append("        <td class=\"right\">").append(it.getQuantity()).append(" Pcs</td>\n")
                    .append("        <td class=\"right\">").append(htmlEsc(num(rate))).append("</td>\n")
                    .append("        <td class=\"center\">Pcs</td>\n")
                    .append("        <td class=\"right\">").append(htmlEsc(num(split.taxable()))).append("</td>\n")
                    .append("      </tr>\n");
        }

        if (t.getSubtotal() > 0) taxableSum = t.getSubtotal();
        if (t.getCgstTotal() > 0) cgstSum = t.getCgstTotal();
        if (t.getSgstTotal() > 0) sgstSum = t.getSgstTotal();
        // Keep CGST/SGST equal on the invoice footer
        if (Math.abs(cgstSum - sgstSum) > 0.009) {
            double gst = GstUtil.round2(cgstSum + sgstSum);
            long gstPaise = Math.round(gst * 100.0);
            if ((gstPaise & 1L) != 0) {
                gstPaise -= 1;
                taxableSum = GstUtil.round2(taxableSum + 0.01);
            }
            cgstSum = sgstSum = (gstPaise / 2) / 100.0;
        }
        double taxAmt = GstUtil.round2(cgstSum + sgstSum);
        double discount = Math.max(0, t.getDiscountAmount());
        double preRound = GstUtil.round2(taxableSum + cgstSum + sgstSum - discount);
        double total = t.getTotal() > 0 ? t.getTotal() : GstUtil.roundToRupee(preRound);
        // Round Off column = exact delta used to reach the nearest-rupee total
        double roundOff = GstUtil.round2(total - preRound);
        double paid = Math.max(0, total - t.getCreditAmount());
        String status = t.getCreditAmount() <= 0.009 ? "Paid"
                : (paid <= 0.009 ? "Unpaid" : "Partially paid");
        String statusClass = "Paid".equals(status) ? "paid"
                : "Unpaid".equals(status) ? "unpaid" : "";
        String custId = t.getCustomerId() != null
                ? String.format("CUST-%05d", t.getCustomerId()) : "-";
        String taxLabel = (t.getTaxLabel() != null && !t.getTaxLabel().isBlank())
                ? t.getTaxLabel() : "CGST 9% + SGST 9%";

        StringBuilder hsnHtml = new StringBuilder();
        for (Map.Entry<String, double[]> e : hsnMap.entrySet()) {
            double[] v = e.getValue();
            hsnHtml.append("      <tr>\n")
                    .append("        <td class=\"center\">").append(htmlEsc(e.getKey())).append("</td>\n")
                    .append("        <td class=\"right\">").append(htmlEsc(num(v[0]))).append("</td>\n")
                    .append("        <td class=\"center\">9%</td>\n")
                    .append("        <td class=\"right\">").append(htmlEsc(num(v[1]))).append("</td>\n")
                    .append("        <td class=\"center\">9%</td>\n")
                    .append("        <td class=\"right\">").append(htmlEsc(num(v[2]))).append("</td>\n")
                    .append("        <td class=\"right\">").append(htmlEsc(num(v[1] + v[2]))).append("</td>\n")
                    .append("      </tr>\n");
        }

        String addrLine = nz(company.getAddress(), "");
        String cityLine = "";
        if (company.getCity() != null && !company.getCity().isBlank()) cityLine += company.getCity().trim();
        if (company.getState() != null && !company.getState().isBlank()) {
            if (!cityLine.isEmpty()) cityLine += ", ";
            cityLine += company.getState().trim();
        }
        if (company.getPincode() != null && !company.getPincode().isBlank()) {
            if (!cityLine.isEmpty()) cityLine += " — ";
            cityLine += company.getPincode().trim();
        }
        String companyState = nz(company.getState(), "Odisha");
        String companyStateCode = stateCode(company.getGstin(), companyState);
        String supportEm = nz(company.getSupportEmail(), nz(company.getEmail(), ""));
        String supportPh = nz(company.getSupportPhone(), nz(company.getPhone(), ""));
        String paymentMode = paymentModeLabel(t);

        Map<String, String> vars = new LinkedHashMap<>();
        vars.put("company_name", htmlEsc(nz(company.getCompanyName(), "Seva Tyres")));
        vars.put("company_address", htmlEsc(nz(company.getFullAddress(), "")).replace("\n", "<br/>"));
        vars.put("company_address_line1", htmlEsc(addrLine));
        vars.put("company_city_line", htmlEsc(cityLine));
        vars.put("company_gstin", htmlEsc(nz(company.getGstin(), "")));
        vars.put("company_state", htmlEsc(companyState));
        vars.put("company_state_code", htmlEsc(companyStateCode));
        vars.put("company_contact", htmlEsc(nz(company.getContactLine(), "")));
        vars.put("support_email", htmlEsc(supportEm));
        vars.put("support_phone", htmlEsc(supportPh));
        vars.put("bill_no", htmlEsc(billNo));
        vars.put("invoice_number", htmlEsc(billNo));
        vars.put("invoice_date", htmlEsc(dateStr));
        vars.put("reference_line", htmlEsc("dt. " + dateStr));
        vars.put("payment_mode", htmlEsc(paymentMode));
        vars.put("buyer_order_no", "");
        vars.put("buyer_order_date", htmlEsc(dateStr));
        vars.put("due_date", htmlEsc(dateLong));
        vars.put("status", htmlEsc(status));
        vars.put("status_class", statusClass);
        vars.put("customer_name", htmlEsc(nz(customerName, "")));
        vars.put("customer_id", htmlEsc(custId));
        vars.put("customer_address", htmlEsc(nz(t.getCustomerAddress(), "")));
        vars.put("customer_phone", htmlEsc(nz(t.getCustomerPhone(), "")));
        vars.put("customer_email", htmlEsc(nz(t.getCustomerEmail(), "")));
        vars.put("customer_gstin", "");
        vars.put("customer_state", htmlEsc(companyState));
        vars.put("customer_state_code", htmlEsc(companyStateCode));
        vars.put("place_of_supply", htmlEsc(companyState));
        vars.put("items_html", itemsHtml.toString());
        vars.put("items", itemsHtml.toString());
        vars.put("hsn_summary_html", hsnHtml.toString());
        vars.put("subtotal", htmlEsc(num(taxableSum)));
        vars.put("cgst_total", htmlEsc(num(cgstSum)));
        vars.put("sgst_total", htmlEsc(num(sgstSum)));
        vars.put("discount_amount", htmlEsc(num(discount)));
        vars.put("round_off", htmlEsc(num(roundOff)));
        vars.put("total_qty", htmlEsc(totalQty + " Pcs"));
        vars.put("tax_label", htmlEsc(taxLabel));
        vars.put("tax_amount", htmlEsc(num(taxAmt)));
        vars.put("total", htmlEsc("\u20b9 " + num(total)));
        vars.put("amount_in_words", htmlEsc(AmountInWords.inr(total)));
        vars.put("tax_in_words", htmlEsc(AmountInWords.inr(taxAmt)));
        vars.put("paid", htmlEsc(moneyInr(paid)));
        vars.put("remaining", htmlEsc(moneyInr(t.getCreditAmount())));
        vars.put("bank_name", htmlEsc(nz(company.getBankName(), "")));
        vars.put("bank_account", htmlEsc(nz(company.getBankAccount(), "")));
        vars.put("bank_ifsc", htmlEsc(nz(company.getBankIfsc(), "")));
        vars.put("upi_id", htmlEsc(nz(company.getUpiId(), "")));
        vars.put("generated_on", htmlEsc(generatedStr));
        vars.put("year", String.valueOf(saleDate.getYear()));

        String html = template;
        for (Map.Entry<String, String> e : vars.entrySet()) {
            html = html.replace("{" + e.getKey() + "}", e.getValue() != null ? e.getValue() : "");
        }
        Files.writeString(outFile.toPath(), html, StandardCharsets.UTF_8);
    }

    private static String num(double v) {
        return String.format("%,.2f", v);
    }

    private static String moneyInr(double v) {
        return "\u20b9" + String.format("%,.2f", v);
    }

    private static String stateCode(String gstin, String stateName) {
        if (gstin != null && gstin.length() >= 2 && Character.isDigit(gstin.charAt(0))) {
            return gstin.substring(0, 2);
        }
        if (stateName != null && stateName.toLowerCase(Locale.ROOT).contains("odisha")) return "21";
        return "";
    }

    private static String paymentModeLabel(SaleTransaction t) {
        List<String> parts = new ArrayList<>();
        if (t.getCash() > 0.009) parts.add("Cash");
        if (t.getPhonePe() > 0.009) parts.add("PhonePe");
        if (t.getAccountTransfer() > 0.009) parts.add("A/C Transfer");
        if (t.getCardSwipe() > 0.009) parts.add("Card");
        if (t.getBajajFinance() > 0.009) parts.add("Bajaj");
        if (t.getCheque() > 0.009) parts.add("Cheque");
        if (t.getCreditAmount() > 0.009) parts.add("Credit");
        return parts.isEmpty() ? "" : String.join(", ", parts);
    }

    private static String htmlEsc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private static String nz(String v, String def) {
        return v == null || v.isBlank() ? def : v;
    }

    // ─── Legacy Transaction Invoice PDF ──────────────────────────────────────

    private static void writeInvoicePdf(String invoiceNum, String customerName,
            String dateStr, String generatedStr, Transaction t, File outFile) throws IOException {
        int pageW = 612, leftMargin = 50, contentW = pageW - leftMargin - 50;

        StringBuilder cs = new StringBuilder();
        cs.append("BT\n");
        cs.append("/F1 22 Tf 1 0 0 1 ").append(leftMargin).append(" 740 Tm (SEVA TYRES) Tj\n");
        cs.append("/F1 18 Tf 1 0 0 1 450 740 Tm (INVOICE) Tj\n");
        cs.append("ET\n");

        cs.append("q 0.4 0.4 0.7 RG ").append(leftMargin).append(" 728 ").append(contentW).append(" 1.5 re f Q\n");

        cs.append("BT\n");
        cs.append("/F1 10 Tf 1 0 0 1 ").append(leftMargin).append(" 710 Tm (Invoice #: ").append(pdfEsc(invoiceNum)).append(") Tj\n");
        cs.append("/F2 10 Tf 1 0 0 1 ").append(leftMargin).append(" 694 Tm (Date: ").append(pdfEsc(dateStr)).append(") Tj\n");
        cs.append("/F1 11 Tf 1 0 0 1 ").append(leftMargin).append(" 668 Tm (BILL TO:) Tj\n");
        cs.append("/F2 11 Tf 1 0 0 1 ").append(leftMargin).append(" 652 Tm (").append(pdfEsc(customerName)).append(") Tj\n");
        cs.append("ET\n");

        cs.append("q 0.75 0.75 0.75 RG ").append(leftMargin).append(" 638 ").append(contentW).append(" 0.8 re f Q\n");

        cs.append("BT /F1 10 Tf\n");
        cs.append("1 0 0 1 ").append(leftMargin).append(" 622 Tm (Description) Tj\n");
        cs.append("1 0 0 1 350 622 Tm (Amount) Tj\n");
        cs.append("1 0 0 1 430 622 Tm (Paid) Tj\n");
        cs.append("1 0 0 1 500 622 Tm (Balance) Tj\n");
        cs.append("ET\n");

        cs.append("q 0.85 0.85 0.85 RG ").append(leftMargin).append(" 610 ").append(contentW).append(" 0.5 re f Q\n");

        String descText = t.getDescription() != null && !t.getDescription().isBlank()
                ? t.getDescription() : t.getType().name();
        cs.append("BT /F2 10 Tf\n");
        cs.append("1 0 0 1 ").append(leftMargin).append(" 595 Tm (").append(pdfEsc(truncate(descText, 30))).append(") Tj\n");
        cs.append("1 0 0 1 350 595 Tm (").append(String.format("%.2f", t.getAmount())).append(") Tj\n");
        cs.append("1 0 0 1 430 595 Tm (").append(String.format("%.2f", t.getPaidAmount())).append(") Tj\n");
        cs.append("1 0 0 1 500 595 Tm (").append(String.format("%.2f", t.getBalance())).append(") Tj\n");
        cs.append("ET\n");

        cs.append("q 0.95 0.95 0.98 rg 350 555 210 55 re f Q\n");
        cs.append("BT /F1 10 Tf\n");
        cs.append("1 0 0 1 360 598 Tm (Subtotal:) Tj\n");
        cs.append("1 0 0 1 460 598 Tm (").append(String.format("%.2f", t.getAmount())).append(") Tj\n");
        cs.append("1 0 0 1 360 582 Tm (Amount Paid:) Tj\n");
        cs.append("1 0 0 1 460 582 Tm (").append(String.format("%.2f", t.getPaidAmount())).append(") Tj\n");
        cs.append("/F1 11 Tf 1 0 0 1 360 562 Tm (Balance Due:) Tj\n");
        cs.append("1 0 0 1 460 562 Tm (").append(String.format("%.2f", t.getBalance())).append(") Tj\n");
        cs.append("ET\n");

        String statusText = t.isOngoing() ? "PENDING" : "PAID / ALL CLEARED";
        cs.append("BT /F1 12 Tf 1 0 0 1 ").append(leftMargin).append(" 562 Tm (Status: ").append(pdfEsc(statusText)).append(") Tj ET\n");

        cs.append("q 0.4 0.4 0.7 RG ").append(leftMargin).append(" 80 ").append(contentW).append(" 1 re f Q\n");
        cs.append("BT /F2 9 Tf\n");
        cs.append("1 0 0 1 ").append(leftMargin).append(" 65 Tm (Thank you for choosing Seva Tyres.) Tj\n");
        cs.append("1 0 0 1 ").append(leftMargin).append(" 50 Tm (Generated by Seva Tyres  |  ").append(pdfEsc(generatedStr)).append(") Tj\n");
        cs.append("ET\n");

        byte[] csBytes = cs.toString().getBytes(StandardCharsets.ISO_8859_1);
        writePdfToFile(csBytes, outFile);
    }

    // ─── Pure-Java PDF writer ──────────────────────────────────────────────────

    private static File writePdf(String title, String subtitle,
                                  String[] headers, List<String[]> dataRows, File outFile) throws IOException {
        int cols = headers.length;
        int[] colX = new int[cols];
        int pageW = 612, leftMargin = 50, rightMargin = 50;
        int colW = (pageW - leftMargin - rightMargin) / cols;
        for (int i = 0; i < cols; i++) colX[i] = leftMargin + i * colW;

        StringBuilder cs = new StringBuilder();
        cs.append("BT\n");
        cs.append("1 0 0 1 ").append(leftMargin).append(" 730 Tm /F1 18 Tf (").append(pdfEsc(title)).append(") Tj\n");
        cs.append("1 0 0 1 ").append(leftMargin).append(" 705 Tm /F2 10 Tf (").append(pdfEsc(subtitle)).append(") Tj\n");
        cs.append("ET\n");

        cs.append("q 0.6 0.6 0.6 RG ")
          .append(leftMargin).append(" 695 ").append(pageW - leftMargin - rightMargin).append(" 0.8 re S Q\n");

        int y = 678;
        cs.append("BT /F1 10 Tf\n");
        for (int i = 0; i < cols; i++) {
            cs.append("1 0 0 1 ").append(colX[i]).append(" ").append(y)
              .append(" Tm (").append(pdfEsc(truncate(headers[i], 18))).append(") Tj\n");
        }
        cs.append("ET\n");
        cs.append("q 0.8 0.8 0.8 RG ")
          .append(leftMargin).append(" 672 ").append(pageW - leftMargin - rightMargin).append(" 0.5 re S Q\n");

        int rowY = 657;
        int maxRows = Math.min(dataRows.size(), 36);
        cs.append("BT /F2 9 Tf\n");
        for (int r = 0; r < maxRows; r++) {
            String[] row = dataRows.get(r);
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
            if (rowY < 60) break;
        }
        cs.append("ET\n");
        cs.append("BT 1 0 0 1 ").append(leftMargin).append(" 40 Tm /F2 9 Tf (Seva Tyres  |  ")
          .append(pdfEsc(LocalDateTime.now().format(FULL))).append(") Tj ET\n");

        byte[] csBytes = cs.toString().getBytes(StandardCharsets.ISO_8859_1);
        writePdfToFile(csBytes, outFile);
        return outFile;
    }

    private static void writePdfToFile(byte[] csBytes, File outFile) throws IOException {
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
            sb.append(c < 256 ? c : '?');
        }
        return sb.toString();
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max - 1) + "~" : s;
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
             + "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>"
             + "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"2\" borderId=\"0\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\"><alignment horizontal=\"center\"/></xf>"
             + "<xf numFmtId=\"4\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyNumberFormat=\"1\"/>"
             + "</cellXfs>"
             + "</styleSheet>";
    }

    private static String sheet1Xml(String title, String[] headers, List<String[]> dataRows) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n")
          .append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">")
          .append("<sheetData>");

        sb.append("<row r=\"1\">").append(cell("A1", title, 1)).append("</row>");
        sb.append("<row r=\"3\">");
        for (int i = 0; i < headers.length; i++) sb.append(cell(colRef(i) + "3", headers[i], 1));
        sb.append("</row>");

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

    private static String cell(String ref, String value, int style) {
        String escaped = xmlEsc(value);
        return "<c r=\"" + ref + "\" t=\"inlineStr\" s=\"" + style + "\"><is><t>"
                + escaped + "</t></is></c>";
    }

    private static String colRef(int colIndex) {
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
