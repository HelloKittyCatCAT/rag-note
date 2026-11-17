package com.sya.ragnote.service.impl.conversation;

import com.sya.ragnote.dao.conversation.ConversationDao;
import com.sya.ragnote.enums.result.ChatResultEnum;
import com.sya.ragnote.enums.result.ConversationResultEnum;
import com.sya.ragnote.exception.ServiceSystemException;
import com.sya.ragnote.service.conversation.ConversationTurnService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConversationTurnServiceImpl implements ConversationTurnService {
    private final ConversationDao dao;

    @Override
    @Transactional
    public Claim claim(long ownerId, UUID id, UUID messageId, String question, String hash) {
        if (dao.lock(ownerId, id) == null) {
            throw new ServiceSystemException(ConversationResultEnum.NOT_FOUND);
        }
        var existing = dao.findTurn(ownerId, id, messageId);
        if (existing != null) {
            if (!hash.equals(existing.requestHash())) {
                throw new ServiceSystemException(ConversationResultEnum.MESSAGE_ID_CONFLICT);
            }
            if (existing.answerJson() != null) {
                if (!existing.sourcesAvailable()) {
                    throw new ServiceSystemException(ChatResultEnum.SOURCE_CHANGED);
                }
                return new Claim(null, existing);
            }
        }
        // 锁只覆盖认领轮次的短事务。模型执行期间由带过期时间的令牌保留处理权。
        dao.expire(ownerId, id);
        if (dao.busy(ownerId, id)) {
            throw new ServiceSystemException(ConversationResultEnum.TURN_IN_PROGRESS);
        }
        UUID token = UUID.randomUUID();
        if (dao.claim(ownerId, id, messageId, question, hash, token) != 1) {
            throw new ServiceSystemException(ConversationResultEnum.TURN_EXPIRED);
        }
        return new Claim(token, null);
    }

    @Override
    @Transactional
    public void finish(long ownerId, UUID id, UUID messageId, UUID token, String answer) {
        if (dao.lock(ownerId, id) == null) {
            throw new ServiceSystemException(ConversationResultEnum.NOT_FOUND);
        }
        if (dao.finish(ownerId, id, token, answer) != 1) {
            throw new ServiceSystemException(ConversationResultEnum.TURN_EXPIRED);
        }
        if (!dao.findTurn(ownerId, id, messageId).sourcesAvailable()) {
            throw new ServiceSystemException(ChatResultEnum.SOURCE_CHANGED);
        }
        dao.touch(ownerId, id);
    }

    @Override
    public void release(long ownerId, UUID id, UUID token) {
        dao.release(ownerId, id, token);
    }
}
