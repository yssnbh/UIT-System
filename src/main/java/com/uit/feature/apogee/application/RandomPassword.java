package com.uit.feature.apogee.application;

import java.security.SecureRandom;

public final class RandomPassword {

    private static final String UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String LOWER = "abcdefghijkmnopqrstuvwxyz";
    private static final String DIGITS = "23456789";
    private static final String ALL = UPPER + LOWER + DIGITS;
    private static final int LENGTH = 12;
    private static final SecureRandom RANDOM = new SecureRandom();

    private RandomPassword() {
    }

    public static String generate() {
        char[] password = new char[LENGTH];
        password[0] = UPPER.charAt(RANDOM.nextInt(UPPER.length()));
        password[1] = LOWER.charAt(RANDOM.nextInt(LOWER.length()));
        password[2] = DIGITS.charAt(RANDOM.nextInt(DIGITS.length()));
        for (int index = 3; index < password.length; index++) {
            password[index] = ALL.charAt(RANDOM.nextInt(ALL.length()));
        }
        for (int index = password.length - 1; index > 0; index--) {
            int swap = RANDOM.nextInt(index + 1);
            char current = password[index];
            password[index] = password[swap];
            password[swap] = current;
        }
        return new String(password);
    }
}
