package com.treasury.service;

final class MaskingUtils {
    private MaskingUtils() {
    }

    static String accountNo(String value) {
        if (value == null || value.length() <= 8) {
            return "****";
        }
        return value.substring(0, 4) + " **** **** " + value.substring(value.length() - 4);
    }
}
