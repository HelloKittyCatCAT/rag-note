package com.sya.ragnote.entity.auth;

import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class Invitation {
    private UUID id;
    @lombok.ToString.Exclude private String tokenHash;
    private Long createdBy;
    private Instant expiresAt;
    private Long usedBy;
    private Instant usedAt;
}
