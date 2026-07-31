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
 * Net profit for a sale:
 * <ol>
 *   <li>Unit profit (tax-inclusive) = inventory sell price − purchase buying price</li>
 *   <li>Line profit = unit profit × qty</li>
 *   <li>Remove 18% tax embedded in sell price → net = sell/1.18 − buy (× qty)</li>
 *   <li>Subtract excl-tax impact of discount; add bill round-off</li>
 * </ol>
 */
public class ProfitService {

    private final JdbcPurchaseInfoRepository purchases = new JdbcPurchaseInfoRepository();

    public double calculateNetProfit(SaleTransaction tx, List<SaleTransactionItem> items) {
        if (items == null || items.isEmpty()) {
            // Legacy single-item bill
            if (tx.getInventoryItemId() != null && tx.getQuantity() > 0 && tx.getUnitPrice() > 0) {
                double buy = resolveBuyPrice(tx.getInventoryItemId(), tx.getParticulars());
                double sell = tx.getUnitPrice();
                int qty = tx.getQuantity();
                double grossIncl = GstUtil.round2((sell - buy) * qty);
                double taxOnSell = GstUtil.round2(sell * qty - GstUtil.taxableFromInclusive(sell * qty));
                double net = GstUtil.round2(grossIncl - taxOnSell);
                net = applyDiscountAndRoundOff(net, tx.getDiscountAmount(), tx.getRoundOff());
                return net;
            }
            return 0;
        }

        Map<Integer, Double> buyByInv = new HashMap<>();
        Map<String, Double> buyByName = new HashMap<>();
        for (PurchaseInfo p : purchases.findAll()) {
            if (p.getInventoryId() != null) buyByInv.put(p.getInventoryId(), p.getBuyingPrice());
            if (p.getItemName() != null) {
                buyByName.put(p.getItemName().trim().toLowerCase(Locale.ROOT), p.getBuyingPrice());
            }
        }

        double net = 0;
        for (SaleTransactionItem it : items) {
            double sell = it.getUnitPrice(); // tax-inclusive unit price
            int qty = Math.max(0, it.getQuantity());
            double buy = 0;
            if (it.getInventoryId() != null && buyByInv.containsKey(it.getInventoryId())) {
                buy = buyByInv.get(it.getInventoryId());
            } else if (it.getItemName() != null) {
                buy = buyByName.getOrDefault(it.getItemName().trim().toLowerCase(Locale.ROOT), 0.0);
            }

            // Prefer stored taxable (matches invoice); else Base = Inclusive ÷ 1.18
            GstUtil.LineGst split = GstUtil.splitLine(sell, qty);
            double sellBase = it.getTaxableAmount() > 0 ? it.getTaxableAmount() : split.taxable();
            double sellInclusive = it.getLineTotal() > 0 ? it.getLineTotal() : split.inclusiveTotal();
            // Step 2–3: (sell − buy)×qty minus 18% tax in sell = sellBase − buy×qty
            double grossIncl = GstUtil.round2(sellInclusive - buy * qty);
            double taxOnSell = GstUtil.round2(sellInclusive - sellBase);
            net += GstUtil.round2(grossIncl - taxOnSell);
        }
        net = GstUtil.round2(net);
        net = applyDiscountAndRoundOff(net, tx.getDiscountAmount(), tx.getRoundOff());
        return net;
    }

    private double applyDiscountAndRoundOff(double netProfit, double discountIncl, double roundOff) {
        // Discount is on tax-inclusive total → remove excl-tax portion from profit
        double discExcl = discountIncl > 0 ? GstUtil.taxableFromInclusive(discountIncl) : 0;
        // Round-off is cash adjustment on the bill (nearest rupee) — include in net profit
        return GstUtil.round2(netProfit - discExcl + roundOff);
    }

    private double resolveBuyPrice(Integer inventoryId, String name) {
        if (inventoryId != null) {
            Optional<PurchaseInfo> byInv = purchases.findByInventoryId(inventoryId);
            if (byInv.isPresent()) return byInv.get().getBuyingPrice();
        }
        if (name != null && !name.isBlank()) {
            Optional<PurchaseInfo> byName = purchases.findByItemName(name);
            if (byName.isPresent()) return byName.get().getBuyingPrice();
        }
        return 0;
    }
}
