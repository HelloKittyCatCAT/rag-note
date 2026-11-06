package com.sya.ragnote.graph.rag.node;

import com.sya.ragnote.dto.chat.ExecutionContext;
import com.sya.ragnote.graph.rag.RagState;
import com.sya.ragnote.service.chat.AnswerValidationService;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ValidateAnswerNode {
    private final AnswerValidationService validation;

    public ValidateAnswerNode(AnswerValidationService validation) {
        this.validation = validation;
    }

    public Map<String, Object> apply(RagState state, ExecutionContext context) {
        context.verifyDeadline();
        return RagState.withResult(
                validation.validate(context.ownerId(), state.evidence(), state.rawAnswer()));
    }
}
