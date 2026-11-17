package com.sya.ragnote.dao.impl.conversation;
import com.sya.ragnote.dao.conversation.ConversationDao;
import com.sya.ragnote.mapper.conversation.ConversationMapper;
import com.sya.ragnote.entity.conversation.Conversation;
import com.sya.ragnote.entity.conversation.ConversationTurn;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ConversationDaoImpl implements ConversationDao {
    private final ConversationMapper mapper;
    @Override
    public int insert(long ownerId, UUID id, String title) {
        return mapper.insert(ownerId, id, title);
    }

    @Override
    public Conversation find(long ownerId, UUID id) {
        return mapper.find(ownerId, id);
    }

    @Override
    public UUID lock(long ownerId, UUID id) {
        return mapper.lock(ownerId, id);
    }

    @Override
    public List<Conversation> list(long ownerId, long offset, int limit) {
        return mapper.list(ownerId, offset, limit);
    }

    @Override
    public long count(long ownerId) {
        return mapper.count(ownerId);
    }

    @Override
    public int delete(long ownerId, UUID id) {
        return mapper.delete(ownerId, id);
    }

    @Override
    public int expire(long ownerId, UUID id) {
        return mapper.expire(ownerId, id);
    }

    @Override
    public boolean busy(long ownerId, UUID id) {
        return mapper.busy(ownerId, id);
    }

    @Override
    public ConversationTurn findTurn(long ownerId, UUID id, UUID messageId) {
        return mapper.findTurn(ownerId, id, messageId);
    }

    @Override
    public List<ConversationTurn> history(long ownerId, UUID id, long offset, int limit) {
        return mapper.history(ownerId, id, offset, limit);
    }

    @Override
    public long countHistory(long ownerId, UUID id) {
        return mapper.countHistory(ownerId, id);
    }

    @Override
    public int claim(long ownerId, UUID id, UUID messageId, String question, String hash, UUID token) {
        return mapper.claim(ownerId, id, messageId, question, hash, token);
    }

    @Override
    public int finish(long ownerId, UUID id, UUID token, String answer) {
        return mapper.finish(ownerId, id, token, answer);
    }

    @Override
    public int release(long ownerId, UUID id, UUID token) {
        return mapper.release(ownerId, id, token);
    }

    @Override
    public int touch(long ownerId, UUID id) {
        return mapper.touch(ownerId, id);
    }
}
