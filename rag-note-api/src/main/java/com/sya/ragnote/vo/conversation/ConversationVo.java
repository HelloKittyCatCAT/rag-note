package com.sya.ragnote.vo.conversation;
import java.time.Instant;
import java.util.UUID;

public record ConversationVo(UUID id, String title, Instant updatedAt) {}
