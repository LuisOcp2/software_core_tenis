package raven.utils;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Utility class for handling monetary parsing and formatting.
 * Supports correct interpretation of Colombian currency formats (dots for
 * thousands, commas for decimals).
 */
public class MoneyHelper {

    private static final NumberFormat FORMATTER = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));

    /**
     * Formats a BigDecimal as Colombian currency.
     *
     * @param amount The amount to format
     * @return Formatted string (e.g., "$1.234.567")
     */
    public static String format(BigDecimal amount) {
        if (amount == null) {
            return "$0";
        }
        return FORMATTER.format(amount);
    }

    /**
     * Parses a monetary string into a BigDecimal, handling various formats
     * correctly.
     *
     * @param text The text to parse (e.g., "$1.234.567", "150.000", "50,000")
     * @return The parsed BigDecimal, or BigDecimal.ZERO if invalid.
     */
    public static BigDecimal parse(String text) {
        if (text == null || text.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }

        try {
            // 1. Basic cleanup: keep only numbers, dots, and commas
            String clean = text.replaceAll("[^0-9.,]", "").trim();

            if (clean.isEmpty()) {
                return BigDecimal.ZERO;
            }

            // 2. Analyze structure to determine format
            int lastDot = clean.lastIndexOf('.');
            int lastComma = clean.lastIndexOf(',');

            // CASE 1: Contains BOTH separators (dot and comma)
            if (lastDot > -1 && lastComma > -1) {
                if (lastDot > lastComma) {
                    // US Format (1,234.56) -> Comma is thousands, dot is decimal
                    clean = clean.replace(",", ""); // remove thousands
                    // dot stays as decimal
                } else {
                    // CO Format (1.234,56) -> Dot is thousands, comma is decimal
                    clean = clean.replace(".", ""); // remove thousands
                    clean = clean.replace(",", "."); // comma to decimal point
                }
            }
            // CASE 2: Only has DOTS
            else if (lastDot > -1) {
                // In Colombia, dot is THOUSANDS (150.000 = 150 thousand)
                clean = clean.replace(".", "");
            }
            // CASE 3: Only has COMMAS
            else if (lastComma > -1) {
                // Could be:
                // A) Decimals (150,50 -> 150.50)
                // B) US-style Thousands (150,000 -> 150 thousand)

                // Heuristic: If exactly 3 digits follow the final comma
                String afterComma = clean.substring(lastComma + 1);
                if (afterComma.length() == 3) {
                    // Strong ambiguity (100,000). Assume 100 thousand.
                    clean = clean.replace(",", "");
                } else {
                    // Normal decimal (100,50)
                    clean = clean.replace(",", ".");
                }
            }

            return new BigDecimal(clean);

        } catch (Exception e) {
            System.err.println("WARN: MoneyHelper parsing error: " + text + " - " + e.getMessage());
            return BigDecimal.ZERO;
        }
    }
}
