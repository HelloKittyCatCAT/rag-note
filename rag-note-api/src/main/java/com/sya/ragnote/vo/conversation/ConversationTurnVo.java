package com.sya.ragnote.vo.conversation;
import com.sya.ragnote.vo.chat.AskQuestionVo;
import java.util.UUID;

public record ConversationTurnVo(UUID clientMessageId, String question, AskQuestionVo result) {}
