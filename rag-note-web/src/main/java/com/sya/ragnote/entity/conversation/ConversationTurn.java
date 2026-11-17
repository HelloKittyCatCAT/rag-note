package com.sya.ragnote.entity.conversation;
import java.util.UUID;

public record ConversationTurn(
        UUID clientMessageId, String question, String requestHash, String answerJson,
        boolean sourcesAvailable) {}
