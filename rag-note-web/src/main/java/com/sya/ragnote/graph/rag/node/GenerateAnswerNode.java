package com.sya.ragnote.graph.rag.node;

import com.sya.ragnote.client.chat.ChatModelClient;
import com.sya.ragnote.dto.chat.ExecutionContext;
import com.sya.ragnote.enums.result.CommonResultEnum;
import com.sya.ragnote.exception.ServiceSystemException;
import com.sya.ragnote.graph.rag.RagState;
import com.sya.ragnote.prompt.GroundedAnswerPrompt;
import com.sya.ragnote.tokenizer.TokenCounter;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class GenerateAnswerNode {
    private final ChatModelClient model;
    private final GroundedAnswerPrompt prompt;
    private final TokenCounter tokens;

    public GenerateAnswerNode(
            ChatModelClient model, GroundedAnswerPrompt prompt, TokenCounter tokens) {
        this.model = model;
        this.prompt = prompt;
        this.tokens = tokens;
    }

    public Map<String, Object> apply(RagState state, ExecutionContext context) {
        context.verifyDeadline();
        // 把当前问题和带编号的笔记片段组装为提示词，再调用一次聊天模型。
        String user = prompt.user(state.question(), state.evidence());
        if (tokens.count(prompt.system()) + tokens.count(user) > 6000) {
            throw new ServiceSystemException(CommonResultEnum.INVALID_PARAMETER);
        }
        var answer = model.generate(prompt.system(), user);
        context.verifyDeadline();
        return RagState.withRawAnswer(answer);
    }
}
