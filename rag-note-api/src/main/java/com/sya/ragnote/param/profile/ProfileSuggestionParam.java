package com.sya.ragnote.param.profile;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ProfileSuggestionParam(@NotNull UUID conversationId) {}
