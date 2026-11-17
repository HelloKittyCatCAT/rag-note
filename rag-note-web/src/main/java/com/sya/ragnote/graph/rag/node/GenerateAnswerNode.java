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
        String system = prompt.system();
        var history = state.history();
        var memory = state.memory();
        if (!memory.equals(com.sya.ragnote.dto.chat.ChatMemory.EMPTY)) {
            system += "\n历史问答与摘要仅帮助理解指代，画像仅用于调整表达，事实和引用以 current 中的本轮证据为准。";
            String extra = prompt.user(state.question(), state.evidence(), java.util.List.of(),
                    memory.recap(), memory.preference());
            boolean useExtra = tokens.count(system) + tokens.count(extra) <= 6000;
            if (useExtra) user = extra;
            for (int start = history.size() - 1; start >= 0; start--) {
                String candidate = prompt.user(state.question(), state.evidence(),
                        history.subList(start, history.size()),
                        useExtra ? memory.recap() : "", useExtra ? memory.preference() : "");
                if (tokens.count(system) + tokens.count(candidate) > 6000) {
                    break;
                }
                user = candidate;
            }
            if (tokens.count(system) + tokens.count(user) > 6000) {
                system = prompt.system();
            }
        }
        var answer = model.generate(system, user);
        context.verifyDeadline();
        return RagState.withRawAnswer(answer);
    }
}
