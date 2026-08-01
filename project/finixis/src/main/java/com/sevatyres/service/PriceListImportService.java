package com.sevatyres.service;

import com.sevatyres.model.InventoryItem;
import com.sevatyres.model.PurchaseInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Bulk import dealer price lists from CSV into Purchase_Info (and optionally Inventory).
 * PDFs from BSID/Firestone are usually image-based — convert to CSV first, then import here.
 */
public class PriceListImportService {

    public static final String[] HEADERS = {
            "item_name", "brand", "rim", "size", "pattern", "type", "product_code",
            "buying_price", "rcp", "mrp", "notes", "qty"
    };

    public record Result(int purchased, int inventoryCreated, int inventoryUpdated, List<String> errors) {}

    /** Write a starter CSV template the user can fill from the PDF. */
    public Path writeTemplate(Path file) throws IOException {
        try (Writer w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            w.write(String.join(",", HEADERS));
            w.write("\n");
            w.write("B250,Bridgestone,14,165/65 R14 79H,B250,TL,XXXX,3200,4499,4999,BSID May 26,4\n");
            w.write("FS001,Firestone,15,185/65 R15 88H,SAMPLE,TL,YYYY,2800,3999,4499,Firestone Jun 26,2\n");
        }
        return file;
    }

    /**
     * @param alsoInventory when true, create/update Inventory rows (TYRE) and link them
     */
    public Result importCsv(Path csv, boolean alsoInventory) throws IOException {
        List<String> errors = new ArrayList<>();
        int purchased = 0, created = 0, updated = 0;

        try (BufferedReader br = Files.newBufferedReader(csv, StandardCharsets.UTF_8)) {
            String headerLine = br.readLine();
            if (headerLine == null) {
                errors.add("File is empty.");
                return new Result(0, 0, 0, errors);
            }
            // strip BOM
            if (!headerLine.isEmpty() && headerLine.charAt(0) == '\uFEFF') {
                headerLine = headerLine.substring(1);
            }
            Map<String, Integer> col = indexHeaders(splitCsv(headerLine));
            if (!col.containsKey("item_name") && !col.containsKey("name") && !col.containsKey("pattern")) {
                errors.add("CSV needs a header row with at least item_name (or name / pattern).");
                return new Result(0, 0, 0, errors);
            }

            String line;
            int rowNum = 1;
            while ((line = br.readLine()) != null) {
                rowNum++;
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] cells = splitCsv(line);
                try {
                    String name = first(cells, col, "item_name", "name", "pattern");
                    if (name == null || name.isBlank()) {
                        errors.add("Row " + rowNum + ": missing item_name.");
                        continue;
                    }
                    PurchaseInfo p = new PurchaseInfo();
                    p.setItemName(name.trim());
                    p.setBrand(nz(first(cells, col, "brand")));
                    p.setRimSize(nz(first(cells, col, "rim", "rim_size")));
                    p.setTyreSize(nz(first(cells, col, "size", "tyre_size")));
                    String pattern = first(cells, col, "pattern");
                    p.setPattern(nz(pattern));
                    // If item_name was taken from pattern column only, keep pattern too
                    if ((p.getPattern() == null || p.getPattern().isBlank())
                            && col.containsKey("pattern") && name.equals(pattern)) {
                        p.setPattern(name);
                    }
                    p.setTyreKind(nz(first(cells, col, "type", "tyre_kind", "kind")));
                    p.setProductCode(nz(first(cells, col, "product_code", "code")));
                    p.setBuyingPrice(parseMoney(first(cells, col, "buying_price", "buy", "dealer_price")));
                    p.setRcp(parseMoney(first(cells, col, "rcp", "consumer_price", "selling_price")));
                    p.setMrp(parseMoney(first(cells, col, "mrp")));
                    p.setNotes(nz(first(cells, col, "notes", "source")));

                    PurchaseInfo saved = AppServices.purchases().save(p);
                    purchased++;

                    if (alsoInventory) {
                        int qty = (int) Math.max(0, parseMoney(first(cells, col, "qty", "quantity", "stock")));
                        var existing = AppServices.inventory().getByName(saved.getItemName());
                        if (existing.isPresent()) {
                            InventoryItem inv = existing.get();
                            applyPurchaseToInventory(inv, saved, qty, false);
                            inv.setPurchaseId(saved.getId());
                            AppServices.inventory().updateItem(inv);
                            saved.setInventoryId(inv.getId());
                            AppServices.purchases().save(saved);
                            updated++;
                        } else {
                            InventoryItem inv = new InventoryItem();
                            inv.setName(saved.getItemName());
                            applyPurchaseToInventory(inv, saved, qty, true);
                            inv.setPurchaseId(saved.getId());
                            InventoryItem createdInv = AppServices.inventory().addItem(inv);
                            saved.setInventoryId(createdInv.getId());
                            AppServices.purchases().save(saved);
                            created++;
                        }
                    }
                } catch (Exception ex) {
                    errors.add("Row " + rowNum + ": " + (ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName()));
                }
            }
        }
        return new Result(purchased, created, updated, errors);
    }

    private static void applyPurchaseToInventory(InventoryItem inv, PurchaseInfo p, int qty, boolean isNew) {
        inv.setItemType("TYRE");
        if (p.getBrand() != null) inv.setBrand(p.getBrand());
        if (p.getRimSize() != null) inv.setRimSize(p.getRimSize());
        if (p.getTyreSize() != null) inv.setTyreSize(p.getTyreSize());
        if (p.getPattern() != null) inv.setPattern(p.getPattern());
        if (p.getTyreKind() != null) inv.setTyreKind(p.getTyreKind());
        if (p.getProductCode() != null) inv.setProductCode(p.getProductCode());
        if (p.getRcp() > 0) inv.setUnitPrice(p.getRcp());
        if (p.getMrp() > 0) inv.setMrp(p.getMrp());
        if (isNew) inv.setQuantity(qty);
        else if (qty > 0) inv.setQuantity(qty);
    }

    private static Map<String, Integer> indexHeaders(String[] headers) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            String h = headers[i].trim().toLowerCase(Locale.ROOT)
                    .replace(' ', '_')
                    .replace("-", "_");
            map.put(h, i);
        }
        return map;
    }

    private static String first(String[] cells, Map<String, Integer> col, String... keys) {
        for (String k : keys) {
            Integer idx = col.get(k);
            if (idx != null && idx < cells.length) {
                String v = cells[idx].trim();
                if (!v.isEmpty()) return v;
            }
        }
        return null;
    }

    private static String nz(String s) { return s == null ? "" : s.trim(); }

    private static double parseMoney(String s) {
        if (s == null || s.isBlank()) return 0;
        String cleaned = s.replace(",", "").replace("₹", "").replace("Rs.", "")
                .replace("Rs", "").replace("INR", "").trim();
        if (cleaned.isEmpty()) return 0;
        return Double.parseDouble(cleaned);
    }

    /** Minimal CSV split supporting quoted fields. */
    static String[] splitCsv(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    cur.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                out.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        out.add(cur.toString());
        return out.toArray(new String[0]);
    }
}
