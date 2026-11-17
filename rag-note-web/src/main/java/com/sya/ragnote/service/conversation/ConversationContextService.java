package com.sya.ragnote.service.conversation;

import com.sya.ragnote.dto.chat.ChatMemory;
import java.util.List;
import java.util.UUID;

public interface ConversationContextService {
    ChatMemory load(long ownerId, UUID conversationId, List<UUID> noteIds);
}
