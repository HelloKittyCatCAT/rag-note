package com.sya.ragnote.service.conversation;

import com.sya.ragnote.param.conversation.ConversationAskParam;
import com.sya.ragnote.param.conversation.CreateConversationParam;
import com.sya.ragnote.param.common.PageParam;
import com.sya.ragnote.vo.common.PageVo;
import com.sya.ragnote.vo.conversation.ConversationVo;
import com.sya.ragnote.vo.conversation.ConversationTurnVo;
import java.util.UUID;

public interface ConversationService {
    ConversationVo create(long ownerId, CreateConversationParam param);
    PageVo<ConversationVo> list(long ownerId, PageParam page);
    PageVo<ConversationTurnVo> history(long ownerId, UUID id, PageParam page);
    ConversationTurnVo ask(long ownerId, ConversationAskParam param);
    void delete(long ownerId, UUID id);
}
