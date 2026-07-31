package com.sevatyres.util;

/** Indian-style amount in words for invoices. */
public final class AmountInWords {
    private AmountInWords() {}

    private static final String[] ONES = {
            "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
            "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen",
            "Seventeen", "Eighteen", "Nineteen"
    };
    private static final String[] TENS = {
            "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
    };

    public static String inr(double amount) {
        long rupees = (long) Math.floor(Math.abs(amount) + 1e-9);
        int paise = (int) Math.round((Math.abs(amount) - rupees) * 100);
        if (paise == 100) { rupees++; paise = 0; }
        String words = "INR " + convert(rupees) + " Only";
        if (paise > 0) {
            words = "INR " + convert(rupees) + " and " + convert(paise) + " Paise Only";
        }
        return words;
    }

    private static String convert(long n) {
        if (n == 0) return "Zero";
        StringBuilder sb = new StringBuilder();
        long crore = n / 10000000;
        n %= 10000000;
        long lakh = n / 100000;
        n %= 100000;
        long thousand = n / 1000;
        n %= 1000;
        long hundred = n / 100;
        n %= 100;
        if (crore > 0) sb.append(twoDigits((int) crore)).append(" Crore ");
        if (lakh > 0) sb.append(twoDigits((int) lakh)).append(" Lakh ");
        if (thousand > 0) sb.append(twoDigits((int) thousand)).append(" Thousand ");
        if (hundred > 0) sb.append(ONES[(int) hundred]).append(" Hundred ");
        if (n > 0) {
            if (sb.length() > 0) sb.append("");
            sb.append(twoDigits((int) n));
        }
        return sb.toString().trim().replaceAll("\\s+", " ");
    }

    private static String twoDigits(int n) {
        if (n < 20) return ONES[n];
        return (TENS[n / 10] + (n % 10 != 0 ? " " + ONES[n % 10] : "")).trim();
    }
}
