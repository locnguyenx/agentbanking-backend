package com.agentbanking.gateway.transform;

import java.math.BigDecimal;
import java.util.UUID;

public class Transformers {

    public static String maskPan(String pan) {
        if (pan == null || pan.isEmpty() || pan.length() < 13) {
            return pan;
        }
        String first6 = pan.substring(0, 6);
        String last4 = pan.substring(pan.length() - 4);
        return first6 + "******" + last4;
    }

    public static UUID toUUID(String value) {
        if (value == null || value.isEmpty()) {
            return UUID.randomUUID();
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return UUID.nameUUIDFromBytes(value.getBytes());
        }
    }

    public static Double toDouble(Object value) {
        if (value == null) return null;
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static BigDecimal toBigDecimal(Object value) {
        if (value == null) return null;
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static String toString(Object value) {
        return value == null ? null : value.toString();
    }

    public static Boolean toBoolean(Object value) {
        if (value == null) return null;
        if (value instanceof Boolean) return (Boolean) value;
        return Boolean.parseBoolean(value.toString());
    }
}
