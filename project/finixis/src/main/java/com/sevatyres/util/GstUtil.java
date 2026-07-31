package com.sevatyres.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * GST helpers: inventory prices are tax-inclusive (CGST 9% + SGST 9% = 18%).
 */
public final class GstUtil {
    private GstUtil() {}

    public static final double GST_TOTAL_RATE = 0.18;
    public static final double CGST_RATE = 0.09;
    public static final double SGST_RATE = 0.09;

    public static double round2(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    /** Taxable rate (exclusive) from inclusive unit price. */
    public static double taxableFromInclusive(double inclusive) {
        if (inclusive <= 0) return 0;
        return round2(inclusive / (1.0 + GST_TOTAL_RATE));
    }

    public static double cgstFromInclusive(double inclusive) {
        if (inclusive <= 0) return 0;
        return round2(inclusive * CGST_RATE / (1.0 + GST_TOTAL_RATE));
    }

    public static double sgstFromInclusive(double inclusive) {
        if (inclusive <= 0) return 0;
        return round2(inclusive * SGST_RATE / (1.0 + GST_TOTAL_RATE));
    }

    /** Line amounts for qty units at inclusive unit price. */
    public static LineGst splitLine(double inclusiveUnit, int qty) {
        double inclusiveTotal = round2(inclusiveUnit * qty);
        double taxable = taxableFromInclusive(inclusiveTotal);
        // Derive CGST/SGST so taxable + cgst + sgst ≈ inclusive (fix for paise)
        double cgst = cgstFromInclusive(inclusiveTotal);
        double sgst = round2(inclusiveTotal - taxable - cgst);
        return new LineGst(taxable, cgst, sgst, inclusiveTotal);
    }

    public record LineGst(double taxable, double cgst, double sgst, double inclusiveTotal) {}

    /** Indian FY label for invoice numbers, e.g. 26/27 for Jul 2026. */
    public static String financialYearLabel(LocalDate date) {
        LocalDate d = date != null ? date : LocalDate.now();
        int startYear = d.getMonthValue() >= 4 ? d.getYear() : d.getYear() - 1;
        int y1 = startYear % 100;
        int y2 = (startYear + 1) % 100;
        return String.format("%02d/%02d", y1, y2);
    }

    /** Round-off to nearest rupee for grand total (Tally-style). */
    public static double roundOffToRupee(double amount) {
        double rounded = Math.round(amount);
        return round2(rounded - amount);
    }

    public static double roundToRupee(double amount) {
        return Math.round(amount);
    }
}
