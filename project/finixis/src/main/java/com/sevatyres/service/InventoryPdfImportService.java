package com.sevatyres.service;

import com.sevatyres.model.InventoryItem;
import com.sevatyres.model.PurchaseInfo;
import com.sevatyres.util.GstUtil;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts inventory / purchase rows from a supplier tax-invoice PDF.
 */
public class InventoryPdfImportService {

    private static final Pattern TALLY_LINE = Pattern.compile(
            "^(?<name>.+?)\\s+(?<hsn>\\d{6,8})\\s+(?:(?<gst>\\d+(?:\\.\\d+)?)%\\s+)?"
                    + "(?<qty>\\d+)\\s*(?:Pcs|pcs|NOS|Nos|pcs\\.)?\\s+"
                    + "(?<rate>[\\d,]+\\.\\d{2})\\s+(?<amt>[\\d,]+\\.\\d{2})\\s*$");

    private static final Pattern SIMPLE_LINE = Pattern.compile(
            "^(?<name>[A-Za-z0-9][A-Za-z0-9 /\\-\\.\\(\\)]+?)\\s+"
                    + "(?<qty>\\d+)\\s+"
                    + "(?<price>[\\d,]+\\.\\d{2})\\s*$");

    private static final Pattern SKIP = Pattern.compile(
            "(?i)^(sl\\.?\\s*no|description|total|subtotal|cgst|sgst|round|grand|amount|hsn|taxable|output|invoice|gstin|buyer|consignee|prepared|verified|authorised|declaration|page\\s).*$");

    public record ParsedItem(String name, String hsn, int qty, double inclusiveUnit, double buyingPrice) {}

    public record ImportResult(int added, int skipped, List<String> messages) {}

    /** Scan PDF and return parsed line items (no DB writes). */
    public List<ParsedItem> scanPdf(File pdfFile) throws IOException {
        String text = extractText(pdfFile);
        return parse(text);
    }

    /** Commit selected preview rows into Inventory + Purchase_Info. */
    public ImportResult commit(List<ParsedItem> items) {
        int added = 0, skipped = 0;
        List<String> messages = new ArrayList<>();
        if (items == null || items.isEmpty()) {
            messages.add("No items selected to import.");
            return new ImportResult(0, 0, messages);
        }
        InventoryService inventory = AppServices.inventory();
        PurchaseInfoService purchases = AppServices.purchases();

        for (ParsedItem p : items) {
            try {
                InventoryItem item = new InventoryItem();
                item.setName(p.name());
                item.setQuantity(Math.max(0, p.qty()));
                item.setUnitPrice(p.inclusiveUnit());
                item.setHsnSac(p.hsn());
                item.setItemType("PRODUCT");
                item.setReorderLevel(10);
                InventoryItem saved = inventory.addItem(item);

                PurchaseInfo pi = new PurchaseInfo();
                pi.setInventoryId(saved.getId());
                pi.setItemName(saved.getName());
                pi.setBuyingPrice(p.buyingPrice());
                purchases.save(pi);

                added++;
                messages.add("Added: " + saved.getName());
            } catch (IllegalArgumentException ex) {
                skipped++;
                messages.add("Skipped \"" + p.name() + "\": " + ex.getMessage());
            } catch (Exception ex) {
                skipped++;
                messages.add("Failed \"" + p.name() + "\": " + ex.getMessage());
            }
        }
        return new ImportResult(added, skipped, messages);
    }

    private String extractText(File pdfFile) throws IOException {
        try (PDDocument doc = Loader.loadPDF(pdfFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(doc);
        }
    }

    List<ParsedItem> parse(String text) {
        List<ParsedItem> out = new ArrayList<>();
        if (text == null || text.isBlank()) return out;
        for (String raw : text.split("\\R")) {
            String line = raw.replace('\u00A0', ' ').trim();
            if (line.isEmpty() || SKIP.matcher(line).matches()) continue;
            if (line.toLowerCase(Locale.ROOT).contains("description of goods")) continue;

            Matcher m = TALLY_LINE.matcher(line);
            if (m.matches()) {
                String name = cleanName(m.group("name"));
                if (name.length() < 2) continue;
                int qty = Integer.parseInt(m.group("qty"));
                double rateExcl = parseMoney(m.group("rate"));
                double inclusive = GstUtil.round2(rateExcl * (1.0 + GstUtil.GST_TOTAL_RATE));
                out.add(new ParsedItem(name, m.group("hsn"), Math.max(1, qty), inclusive, rateExcl));
                continue;
            }
            Matcher s = SIMPLE_LINE.matcher(line);
            if (s.matches()) {
                String name = cleanName(s.group("name"));
                if (name.length() < 2 || name.matches("(?i).*total.*")) continue;
                int qty = Integer.parseInt(s.group("qty"));
                double price = parseMoney(s.group("price"));
                out.add(new ParsedItem(name, null, Math.max(1, qty), price, GstUtil.taxableFromInclusive(price)));
            }
        }
        return out;
    }

    private static String cleanName(String name) {
        return name.replaceAll("\\s+", " ").replaceAll("^[\\d\\.\\)\\-]+\\s*", "").trim();
    }

    private static double parseMoney(String s) {
        return Double.parseDouble(s.replace(",", ""));
    }
}
