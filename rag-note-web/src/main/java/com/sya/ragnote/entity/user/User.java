package com.sya.ragnote.entity.user;

import com.sya.ragnote.enums.user.UserRole;

import lombok.Data;

import java.time.Instant;

@Data
public class User {
    private Long id;
    private String username;
    @lombok.ToString.Exclude private String passwordHash;
    private UserRole role;
    private boolean enabled;
    private Instant createdAt;
}
