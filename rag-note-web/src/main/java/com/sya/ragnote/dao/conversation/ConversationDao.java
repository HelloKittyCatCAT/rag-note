package com.sya.ragnote.dao.conversation;
import com.sya.ragnote.entity.conversation.Conversation;
import com.sya.ragnote.entity.conversation.ConversationTurn;
import java.util.List;
import java.util.UUID;

public interface ConversationDao {
    int insert(long ownerId, UUID id, String title);
    Conversation find(long ownerId, UUID id);
    UUID lock(long ownerId, UUID id);
    List<Conversation> list(long ownerId, long offset, int limit);
    long count(long ownerId);
    int delete(long ownerId, UUID id);
    int expire(long ownerId, UUID id);
    boolean busy(long ownerId, UUID id);
    ConversationTurn findTurn(long ownerId, UUID id, UUID messageId);
    List<ConversationTurn> history(long ownerId, UUID id, long offset, int limit);
    long countHistory(long ownerId, UUID id);
    int claim(long ownerId, UUID id, UUID messageId, String question, String hash, UUID token);
    int finish(long ownerId, UUID id, UUID token, String answer);
    int release(long ownerId, UUID id, UUID token);
    int touch(long ownerId, UUID id);
}
