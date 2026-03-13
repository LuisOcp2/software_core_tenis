package raven.clases.principal;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

public class MoneyFormatter {

    private static final NumberFormat currencyFormat;

    static {
        currencyFormat = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));
        currencyFormat.setMaximumFractionDigits(0);
        currencyFormat.setMinimumFractionDigits(0);
    }

    public static String format(BigDecimal amount) {
        if (amount == null) {
            return currencyFormat.format(0);
        }
        return currencyFormat.format(amount);
    }

    public static String format(double amount) {
        return currencyFormat.format(amount);
    }

    public static BigDecimal parse(String amountStr) throws java.text.ParseException {
        if (amountStr == null || amountStr.trim().isEmpty()) {
            return BigDecimal.ZERO;
        }
        // Remove non-numeric characters except for decimal separator (assuming format
        // is known or reliable)
        // Or better, use the NumberFormat to parse
        Number number = currencyFormat.parse(amountStr);
        return new BigDecimal(number.toString());
    }
}
