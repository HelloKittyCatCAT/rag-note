package com.sya.ragnote.security;

import com.sya.ragnote.enums.user.UserRole;

import java.io.Serializable;
import java.security.Principal;

public record LoginUser(long id, String username, UserRole role)
        implements Serializable, Principal {
    public String getName() {
        return Long.toString(id);
    }
}
