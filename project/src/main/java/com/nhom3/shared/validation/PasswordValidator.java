package com.nhom3.shared.validation;

import java.util.regex.Pattern;

public final class PasswordValidator {
    public static final String STRONG_PASSWORD_MESSAGE =
            "Mật khẩu phải có ít nhất 8 ký tự, bao gồm chữ cái, số và ký tự đặc biệt.";

    private static final Pattern STRONG_PASSWORD_PATTERN =
            Pattern.compile("^(?=.*\\p{L})(?=.*\\d)(?=.*[^\\p{L}\\d\\s]).{8,}$");

    private PasswordValidator() {
    }

    public static boolean isStrong(String password) {
        return password != null && STRONG_PASSWORD_PATTERN.matcher(password).matches();
    }
}
