package com.sya.ragnote.security;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PasswordPolicyTest {
    @Test
    void rejectsBcryptTruncationAndShortPasswords() throws Exception {
        var method =
                Class.forName("com.sya.ragnote.security.PasswordPolicy")
                        .getMethod("valid", String.class);
        assertEquals(false, method.invoke(null, "短".repeat(25)));
        assertEquals(false, method.invoke(null, "short"));
        assertEquals(true, method.invoke(null, "a".repeat(64)));
        assertEquals(false, method.invoke(null, "a".repeat(65)));
        assertEquals(true, method.invoke(null, "长".repeat(24)));
    }
}
