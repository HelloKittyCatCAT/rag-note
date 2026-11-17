package com.sya.ragnote.service.conversation;

import com.sya.ragnote.entity.conversation.ConversationTurn;
import java.util.UUID;

public interface ConversationTurnService {
    record Claim(UUID token, ConversationTurn completed) {}

    Claim claim(long ownerId, UUID id, UUID messageId, String question, String hash);
    void finish(long ownerId, UUID id, UUID messageId, UUID token, String answer);
    void release(long ownerId, UUID id, UUID token);
}
