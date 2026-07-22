package com.hirecheck.util;

import java.security.SecureRandom;

public final class NanoidUtil {
    private static final char[] ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();
    private NanoidUtil() {}
    public static String generate(int size) {
        char[] id = new char[size];
        for (int i = 0; i < size; i++) id[i] = ALPHABET[RANDOM.nextInt(ALPHABET.length)];
        return new String(id);
    }
}
