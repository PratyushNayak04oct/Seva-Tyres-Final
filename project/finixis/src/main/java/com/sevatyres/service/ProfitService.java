package com.sevatyres.service;

import com.sevatyres.model.PurchaseInfo;
import com.sevatyres.model.SaleTransaction;
import com.sevatyres.model.SaleTransactionItem;
import com.sevatyres.repository.impl.JdbcPurchaseInfoRepository;
import com.sevatyres.util.GstUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Profit = total billing − buying cost − taxes − discount (+ round-off)
 *
 * <ul>
 *   <li>total billing = Σ (unit sell price incl. tax × qty) before discount</li>
 *   <li>buying cost   = Σ (purchase buying price × qty)</li>
 *   <li>taxes         = CGST + SGST</li>
 *   <li>discount      = ₹ amount, or % of total billing if amount is empty</li>
 *   <li>round-off     = invoice nearest-rupee adjustment</li>
 * </ul>
 */
public class ProfitService {

    private final JdbcPurchaseInfoRepository purchases = new JdbcPurchaseInfoRepository();

    public double calculateNetProfit(SaleTransaction tx, List<SaleTransactionItem> items) {
        if (tx == null) return 0;

        Map<Integer, Double> buyByInv = new HashMap<>();
        Map<String, Double> buyByName = new HashMap<>();
        for (PurchaseInfo p : purchases.findAll()) {
            if (p.getInventoryId() != null) buyByInv.put(p.getInventoryId(), p.getBuyingPrice());
            if (p.getItemName() != null) {
                buyByName.put(p.getItemName().trim().toLowerCase(Locale.ROOT), p.getBuyingPrice());
            }
        }

        double totalBilling = 0;
        double buyingCost = 0;
        double taxes = 0;

        if (items != null && !items.isEmpty()) {
            for (SaleTransactionItem it : items) {
                int qty = Math.max(0, it.getQuantity());
                if (qty <= 0) continue;
                GstUtil.LineGst split = GstUtil.splitLine(it.getUnitPrice(), qty);
                totalBilling += split.inclusiveTotal();
                taxes += split.cgst() + split.sgst();
                buyingCost += resolveBuy(it.getInventoryId(), it.getItemName(), buyByInv, buyByName) * qty;
            }
        } else if (tx.getQuantity() > 0 && tx.getUnitPrice() > 0) {
            int qty = tx.getQuantity();
            GstUtil.LineGst split = GstUtil.splitLine(tx.getUnitPrice(), qty);
            totalBilling = split.inclusiveTotal();
            taxes = split.cgst() + split.sgst();
            buyingCost = resolveBuy(tx.getInventoryItemId(), tx.getParticulars(), buyByInv, buyByName) * qty;
        } else {
            return 0;
        }

        totalBilling = GstUtil.round2(totalBilling);
        buyingCost = GstUtil.round2(buyingCost);

        // Prefer taxes stored on the invoice header when present
        double headerTax = GstUtil.round2(
                Math.max(0, tx.getCgstTotal()) + Math.max(0, tx.getSgstTotal()));
        if (headerTax > 0.009) {
            taxes = headerTax;
        } else if (tx.getTaxAmount() > 0.009) {
            taxes = GstUtil.round2(tx.getTaxAmount());
        } else {
            taxes = GstUtil.round2(taxes);
        }

        double discount = Math.max(0, tx.getDiscountAmount());
        if (discount <= 0.009 && tx.getDiscountPercent() > 0) {
            discount = GstUtil.round2(totalBilling * tx.getDiscountPercent() / 100.0);
        }
        discount = Math.min(discount, totalBilling);

        double roundOff = tx.getRoundOff();

        // Profit = total billing − buying cost − taxes − discount (+ round-off)
        return GstUtil.round2(totalBilling - buyingCost - taxes - discount + roundOff);
    }

    private double resolveBuy(Integer inventoryId, String name,
                              Map<Integer, Double> buyByInv, Map<String, Double> buyByName) {
        if (inventoryId != null && buyByInv.containsKey(inventoryId)) {
            return buyByInv.get(inventoryId);
        }
        if (name != null && !name.isBlank()) {
            Double v = buyByName.get(name.trim().toLowerCase(Locale.ROOT));
            if (v != null) return v;
            Optional<PurchaseInfo> byName = purchases.findByItemName(name);
            if (byName.isPresent()) return byName.get().getBuyingPrice();
        }
        if (inventoryId != null) {
            Optional<PurchaseInfo> byInv = purchases.findByInventoryId(inventoryId);
            if (byInv.isPresent()) return byInv.get().getBuyingPrice();
        }
        return 0;
    }
}
