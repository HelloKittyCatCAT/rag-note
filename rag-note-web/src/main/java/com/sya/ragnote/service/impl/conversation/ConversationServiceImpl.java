package com.sya.ragnote.service.impl.conversation;

import com.sya.ragnote.dao.conversation.ConversationDao;
import com.sya.ragnote.entity.conversation.Conversation;
import com.sya.ragnote.entity.conversation.ConversationTurn;
import com.sya.ragnote.enums.result.ConversationResultEnum;
import com.sya.ragnote.exception.ServiceSystemException;
import com.sya.ragnote.param.chat.AskQuestionParam;
import com.sya.ragnote.param.common.PageParam;
import com.sya.ragnote.param.conversation.ConversationAskParam;
import com.sya.ragnote.param.conversation.CreateConversationParam;
import com.sya.ragnote.service.chat.ChatService;
import com.sya.ragnote.service.conversation.ConversationService;
import com.sya.ragnote.service.conversation.ConversationContextService;
import com.sya.ragnote.service.conversation.ConversationTurnService;
import com.sya.ragnote.vo.chat.AskQuestionVo;
import com.sya.ragnote.vo.common.PageVo;
import com.sya.ragnote.vo.conversation.ConversationTurnVo;
import com.sya.ragnote.vo.conversation.ConversationVo;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

@Service
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {
    private final ConversationDao dao;
    private final ConversationTurnService turns;
    private final ChatService chat;
    private final ConversationContextService context;
    private final JsonMapper json = JsonMapper.builder().build();

    @Override
    public ConversationVo create(long ownerId, CreateConversationParam param) {
        UUID id = UUID.randomUUID();
        dao.insert(ownerId, id, param.title().strip());
        return toVo(dao.find(ownerId, id));
    }

    @Override
    public PageVo<ConversationVo> list(long ownerId, PageParam page) {
        return new PageVo<>(dao.list(ownerId, offset(page), page.getSize()).stream()
                .map(ConversationServiceImpl::toVo).toList(),
                dao.count(ownerId), page.getPage(), page.getSize());
    }

    @Override
    public PageVo<ConversationTurnVo> history(long ownerId, UUID id, PageParam page) {
        if (dao.find(ownerId, id) == null) {
            throw new ServiceSystemException(ConversationResultEnum.NOT_FOUND);
        }
        return new PageVo<>(dao.history(ownerId, id, offset(page), page.getSize()).stream()
                .map(this::toVo).toList(), dao.countHistory(ownerId, id), page.getPage(), page.getSize());
    }

    @Override
    public ConversationTurnVo ask(long ownerId, ConversationAskParam param) {
        UUID id = param.conversationId();
        var message = param.message();
        var claim = turns.claim(ownerId, id, param.clientMessageId(), message.getQuestion(), hash(message));
        if (claim.completed() != null) {
            return toVo(claim.completed());
        }
        try {
            // 历史、摘要和画像在调用模型前统一组装，研究模式复用同一个入口。
            var memory = context.load(ownerId, id, message.getNoteIds());
            var answer = chat.ask(ownerId, message, memory);
            turns.finish(ownerId, id, param.clientMessageId(), claim.token(), json.writeValueAsString(answer));
            return new ConversationTurnVo(param.clientMessageId(), message.getQuestion(), answer);
        } catch (RuntimeException failure) {
            try {
                turns.release(ownerId, id, claim.token());
            } catch (RuntimeException releaseFailure) {
                failure.addSuppressed(releaseFailure);
            }
            throw failure;
        }
    }

    @Override
    public void delete(long ownerId, UUID id) {
        if (dao.delete(ownerId, id) == 0) {
            throw new ServiceSystemException(ConversationResultEnum.NOT_FOUND);
        }
    }

    private ConversationTurnVo toVo(ConversationTurn turn) {
        return new ConversationTurnVo(turn.clientMessageId(), turn.question(),
                json.readValue(turn.answerJson(), AskQuestionVo.class));
    }

    private static ConversationVo toVo(Conversation conversation) {
        return new ConversationVo(conversation.id(), conversation.title(), conversation.updatedAt());
    }

    private static long offset(PageParam page) {
        return (page.getPage() - 1L) * page.getSize();
    }

    private String hash(AskQuestionParam message) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(json.writeValueAsBytes(message)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
