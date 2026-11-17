package com.sya.ragnote.param.conversation;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateConversationParam(@NotBlank @Size(max = 120) String title) {}
