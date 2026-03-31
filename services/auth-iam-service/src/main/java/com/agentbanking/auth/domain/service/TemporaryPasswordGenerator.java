package com.agentbanking.auth.domain.service;

import java.security.SecureRandom;

public class TemporaryPasswordGenerator {
    private static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";
    private static final int MIN_LENGTH = 8;
    
    private final SecureRandom random = new SecureRandom();

    public String generate() {
        StringBuilder password = new StringBuilder();
        password.append(UPPERCASE.charAt(random.nextInt(UPPERCASE.length())));
        password.append(LOWERCASE.charAt(random.nextInt(LOWERCASE.length())));
        password.append(DIGITS.charAt(random.nextInt(DIGITS.length())));
        
        String allChars = UPPERCASE + LOWERCASE + DIGITS;
        for (int i = 3; i < MIN_LENGTH; i++) {
            password.append(allChars.charAt(random.nextInt(allChars.length())));
        }
        
        return shuffle(password.toString());
    }
    
    private String shuffle(String s) {
        char[] chars = s.toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = chars[i];
            chars[i] = chars[j];
            chars[j] = temp;
        }
        return new String(chars);
    }
}
