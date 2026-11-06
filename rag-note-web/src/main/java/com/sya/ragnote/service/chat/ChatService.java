package com.sya.ragnote.service.chat;

import com.sya.ragnote.param.chat.AskQuestionParam;
import com.sya.ragnote.vo.chat.AskQuestionVo;

public interface ChatService {
    AskQuestionVo ask(long ownerId, AskQuestionParam param);
}
