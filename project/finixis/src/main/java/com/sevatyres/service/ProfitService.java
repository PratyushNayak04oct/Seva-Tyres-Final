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
 * Net profit for a sale, always after discount and round-off:
 * <pre>
 *   net sell (excl. tax) = (inclusive sales − discount) ÷ 1.18
 *   buy cost             = Σ (buying price × qty)
 *   net profit           = net sell − buy cost + round-off
 * </pre>
 */
public class ProfitService {

    private final JdbcPurchaseInfoRepository purchases = new JdbcPurchaseInfoRepository();

    public double calculateNetProfit(SaleTransaction tx, List<SaleTransactionItem> items) {
        Map<Integer, Double> buyByInv = new HashMap<>();
        Map<String, Double> buyByName = new HashMap<>();
        for (PurchaseInfo p : purchases.findAll()) {
            if (p.getInventoryId() != null) buyByInv.put(p.getInventoryId(), p.getBuyingPrice());
            if (p.getItemName() != null) {
                buyByName.put(p.getItemName().trim().toLowerCase(Locale.ROOT), p.getBuyingPrice());
            }
        }

        double inclusiveSales = 0;
        double buyCost = 0;

        if (items != null && !items.isEmpty()) {
            for (SaleTransactionItem it : items) {
                int qty = Math.max(0, it.getQuantity());
                double sellIncl = it.getLineTotal() > 0
                        ? it.getLineTotal()
                        : GstUtil.splitLine(it.getUnitPrice(), qty).inclusiveTotal();
                inclusiveSales += sellIncl;
                buyCost += resolveBuy(it.getInventoryId(), it.getItemName(), buyByInv, buyByName) * qty;
            }
        } else if (tx.getQuantity() > 0 && tx.getUnitPrice() > 0) {
            inclusiveSales = GstUtil.round2(tx.getUnitPrice() * tx.getQuantity());
            buyCost = resolveBuy(tx.getInventoryItemId(), tx.getParticulars(), buyByInv, buyByName)
                    * tx.getQuantity();
        } else {
            return 0;
        }

        inclusiveSales = GstUtil.round2(inclusiveSales);
        buyCost = GstUtil.round2(buyCost);

        // Discount is tax-inclusive — always reduce revenue before stripping GST
        double discountIncl = Math.max(0, tx.getDiscountAmount());
        if (discountIncl <= 0.009 && tx.getDiscountPercent() > 0) {
            discountIncl = GstUtil.round2(inclusiveSales * tx.getDiscountPercent() / 100.0);
        }
        discountIncl = Math.min(discountIncl, inclusiveSales);

        double netInclusive = GstUtil.round2(inclusiveSales - discountIncl);
        double netSellExclTax = GstUtil.taxableFromInclusive(netInclusive);
        double roundOff = tx.getRoundOff();

        return GstUtil.round2(netSellExclTax - buyCost + roundOff);
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
