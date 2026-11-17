package com.sya.ragnote.entity.conversation;
import java.time.Instant;
import java.util.UUID;

public record Conversation(UUID id, String title, Instant updatedAt) {}
