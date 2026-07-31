package com.sevatyres.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * GST helpers for tax-inclusive inventory prices (CGST 9% + SGST 9% = 18%).
 *
 * <pre>
 * Base (taxable) = Inclusive ÷ (1 + 0.18)   // half-up to 2 dp
 * GST amount     = Inclusive − Base
 * CGST = SGST    = GST ÷ 2                  // kept equal
 * </pre>
 *
 * Grand total is rounded to the nearest rupee; round-off is the adjustment delta.
 */
public final class GstUtil {
    private GstUtil() {}

    public static final double GST_TOTAL_RATE = 0.18;
    public static final BigDecimal ONE_PLUS_GST = new BigDecimal("1.18");

    public static double round2(double v) {
        return bd(v).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    private static BigDecimal bd(double v) {
        return BigDecimal.valueOf(v);
    }

    /** Base / taxable from inclusive amount: Inclusive ÷ 1.18 (half-up). */
    public static double taxableFromInclusive(double inclusive) {
        if (inclusive <= 0) return 0;
        return bd(inclusive).divide(ONE_PLUS_GST, 2, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * Split an inclusive line (unit × qty) into taxable Amount, equal CGST/SGST.
     * Rate (excl.) = taxable ÷ qty.
     */
    public static LineGst splitLine(double inclusiveUnit, int qty) {
        int q = Math.max(0, qty);
        BigDecimal inclusive = bd(inclusiveUnit).multiply(BigDecimal.valueOf(q))
                .setScale(2, RoundingMode.HALF_UP);
        if (inclusive.signum() <= 0) {
            return new LineGst(0, 0, 0, 0);
        }

        // Base Price = Inclusive ÷ (1 + GST Rate)
        BigDecimal base = inclusive.divide(ONE_PLUS_GST, 2, RoundingMode.HALF_UP);
        // GST Amount = Inclusive − Base Price
        BigDecimal gst = inclusive.subtract(base).setScale(2, RoundingMode.HALF_UP);

        // Keep CGST == SGST: GST paise must be even
        long gstPaise = gst.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValue();
        long basePaise = base.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValue();
        long inclPaise = inclusive.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValue();
        if ((gstPaise & 1L) != 0) {
            // Move 1 paise from GST into base so halves are equal
            basePaise += 1;
            gstPaise -= 1;
        }
        // Safety: base + gst must equal inclusive
        if (basePaise + gstPaise != inclPaise) {
            gstPaise = inclPaise - basePaise;
            if ((gstPaise & 1L) != 0) {
                basePaise += 1;
                gstPaise -= 1;
            }
        }
        long halfPaise = gstPaise / 2;

        double taxable = basePaise / 100.0;
        double half = halfPaise / 100.0;
        double incl = inclPaise / 100.0;
        return new LineGst(taxable, half, half, incl);
    }

    public record LineGst(double taxable, double cgst, double sgst, double inclusiveTotal) {
        public double gstTotal() { return round2(cgst + sgst); }
    }

    /**
     * Apply discount on inclusive total, then round grand total to nearest rupee.
     * {@code roundOff} is the signed delta shown on the invoice (grand − pre-round).
     */
    public static Totals finalizeTotals(double inclusiveSum, double discountAmount) {
        double inclusive = round2(Math.max(0, inclusiveSum));
        double discount = round2(Math.min(Math.max(0, discountAmount), inclusive));
        double afterDiscount = round2(inclusive - discount);

        // Recompute tax on the net inclusive (after discount) so invoice adds up
        LineGst net = splitLine(afterDiscount, 1);
        double preRound = round2(net.taxable() + net.cgst() + net.sgst()); // == afterDiscount
        double grand = roundToRupee(preRound);
        double roundOff = round2(grand - preRound);

        return new Totals(net.taxable(), net.cgst(), net.sgst(), discount, roundOff, grand, afterDiscount);
    }

    /**
     * Build invoice totals from per-line splits + discount.
     * Taxable/CGST/SGST are sums of lines; discount reduces payable;
     * round-off adjusts to nearest rupee.
     */
    public static Totals totalsFromLines(double taxableSum, double cgstSum, double sgstSum,
                                         double inclusiveSum, double discountAmount) {
        double taxable = round2(taxableSum);
        double cgst = round2(cgstSum);
        double sgst = round2(sgstSum);
        // Enforce equal header CGST/SGST from combined GST
        double gst = round2(cgst + sgst);
        long gstPaise = Math.round(gst * 100.0);
        if ((gstPaise & 1L) != 0) {
            gstPaise -= 1;
            taxable = round2(taxable + 0.01);
        }
        double half = (gstPaise / 2) / 100.0;
        cgst = half;
        sgst = half;

        double inclusive = round2(inclusiveSum);
        // Prefer inclusive from components when it matches
        double fromParts = round2(taxable + cgst + sgst);
        if (Math.abs(fromParts - inclusive) > 0.009) {
            inclusive = fromParts;
        }

        double discount = round2(Math.min(Math.max(0, discountAmount), inclusive));
        double preRound = round2(inclusive - discount);
        double grand = roundToRupee(preRound);
        double roundOff = round2(grand - preRound);
        return new Totals(taxable, cgst, sgst, discount, roundOff, grand, preRound);
    }

    public record Totals(double taxable, double cgst, double sgst, double discount,
                         double roundOff, double grandTotal, double preRound) {}

    public static String financialYearLabel(LocalDate date) {
        LocalDate d = date != null ? date : LocalDate.now();
        int startYear = d.getMonthValue() >= 4 ? d.getYear() : d.getYear() - 1;
        int y1 = startYear % 100;
        int y2 = (startYear + 1) % 100;
        return String.format("%02d/%02d", y1, y2);
    }

    /** Signed delta so that amount + roundOff = nearest rupee. */
    public static double roundOffToRupee(double amount) {
        return round2(roundToRupee(amount) - amount);
    }

    /** Nearest whole rupee (half-up: ≥0.50 up, else down). */
    public static double roundToRupee(double amount) {
        return bd(amount).setScale(0, RoundingMode.HALF_UP).doubleValue();
    }
}
