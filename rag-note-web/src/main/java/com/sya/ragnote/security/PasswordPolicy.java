package com.sya.ragnote.security;

import java.nio.charset.StandardCharsets;

public final class PasswordPolicy {
    private PasswordPolicy() {}

    public static boolean valid(String value) {
        return value != null
                && value.length() >= 12
                && value.length() <= 64
                && value.getBytes(StandardCharsets.UTF_8).length <= 72;
    }
}
