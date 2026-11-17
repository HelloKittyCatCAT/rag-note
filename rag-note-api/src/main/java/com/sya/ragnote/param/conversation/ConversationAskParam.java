package com.sya.ragnote.param.conversation;
import com.sya.ragnote.param.chat.AskQuestionParam;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record ConversationAskParam(
        @NotNull UUID conversationId,
        @NotNull UUID clientMessageId,
        @NotNull @Valid AskQuestionParam message) {}
