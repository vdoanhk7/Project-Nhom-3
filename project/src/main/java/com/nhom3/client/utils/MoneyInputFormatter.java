package com.nhom3.client.utils;

import javafx.scene.control.TextField;
import java.math.BigInteger;

public final class MoneyInputFormatter {

    private MoneyInputFormatter() {
    }

    public static void install(TextField textField) {
        if (textField == null) {
            return;
        }

        final boolean[] isFormatting = {false};
        textField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (isFormatting[0]) {
                return;
            }

            String digitsOnly = newValue.replaceAll("[^\\d]", "");
            if (digitsOnly.isEmpty()) {
                if (!newValue.isEmpty()) {
                    isFormatting[0] = true;
                    textField.clear();
                    isFormatting[0] = false;
                }
                return;
            }

            String formattedValue = formatDigits(digitsOnly);
            if (formattedValue.equals(newValue)) {
                return;
            }

            int digitsBeforeCaret = countDigits(newValue.substring(0, Math.min(textField.getCaretPosition(), newValue.length())));

            isFormatting[0] = true;
            textField.setText(formattedValue);
            textField.positionCaret(calculateCaretPosition(formattedValue, digitsBeforeCaret));
            isFormatting[0] = false;
        });
    }

    public static double parseAmount(String value) {
        if (value == null) {
            throw new NumberFormatException("Missing money amount");
        }

        String normalized = value.trim().replaceAll("[\\s,.]", "");
        if (normalized.isEmpty() || !normalized.matches("\\d+")) {
            throw new NumberFormatException("Invalid money amount: " + value);
        }
        return Double.parseDouble(normalized);
    }

    public static String formatAmount(double value) {
        return String.format("%,.0f", value);
    }

    private static String formatDigits(String digitsOnly) {
        return String.format("%,d", new BigInteger(digitsOnly));
    }

    private static int countDigits(String value) {
        int digitCount = 0;
        for (int i = 0; i < value.length(); i++) {
            if (Character.isDigit(value.charAt(i))) {
                digitCount++;
            }
        }
        return digitCount;
    }

    private static int calculateCaretPosition(String formattedValue, int digitCount) {
        if (digitCount <= 0) {
            return 0;
        }

        int seenDigits = 0;
        for (int i = 0; i < formattedValue.length(); i++) {
            if (Character.isDigit(formattedValue.charAt(i))) {
                seenDigits++;
                if (seenDigits == digitCount) {
                    return i + 1;
                }
            }
        }
        return formattedValue.length();
    }
}
