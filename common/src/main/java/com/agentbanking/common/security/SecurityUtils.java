package com.agentbanking.common.security;

/**
 * Security utilities for data masking and encryption.
 * Ensures PII is never logged in plaintext.
 */
public class SecurityUtils {

    /**
     * Masks a PAN (Primary Account Number) for display and logging.
     * Shows first 6 and last 4 digits.
     * 
     * @param pan Full PAN
     * @return Masked PAN (e.g., "411111******1111")
     */
    public static String maskPan(String pan) {
        if (pan == null || pan.length() < 10) {
            return "****";
        }
        String first6 = pan.substring(0, 6);
        String last4 = pan.substring(pan.length() - 4);
        int middleLength = pan.length() - 10;
        StringBuilder masked = new StringBuilder(first6);
        for (int i = 0; i < middleLength; i++) {
            masked.append('*');
        }
        masked.append(last4);
        return masked.toString();
    }

    /**
     * Masks a MyKad number for display and logging.
     * Shows first 4 and last 4 digits.
     * 
     * @param mykad Full MyKad (12 digits)
     * @return Masked MyKad (e.g., "1234****9012")
     */
    public static String maskMyKad(String mykad) {
        if (mykad == null || mykad.length() != 12) {
            return "****";
        }
        return mykad.substring(0, 4) + "****" + mykad.substring(8);
    }

    /**
     * Masks a phone number for display and logging.
     * Shows last 4 digits.
     * 
     * @param phone Full phone number
     * @return Masked phone (e.g., "****6789")
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) {
            return "****";
        }
        return "****" + phone.substring(phone.length() - 4);
    }

    /**
     * Generates a trace ID for distributed tracing.
     * Format: trace-{timestamp}-{random}
     * 
     * @return Trace ID string
     */
    public static String generateTraceId() {
        return "trace-" + System.currentTimeMillis() + "-" + 
               Integer.toHexString(java.util.concurrent.ThreadLocalRandom.current().nextInt());
    }
}
