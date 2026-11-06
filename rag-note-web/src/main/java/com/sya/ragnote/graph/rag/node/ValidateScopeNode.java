package com.sya.ragnote.graph.rag.node;

import com.sya.ragnote.dto.chat.ExecutionContext;
import com.sya.ragnote.graph.rag.RagState;
import com.sya.ragnote.service.note.NoteSearchService;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ValidateScopeNode {
    private final NoteSearchService search;

    public ValidateScopeNode(NoteSearchService search) {
        this.search = search;
    }

    public Map<String, Object> apply(RagState state, ExecutionContext context) {
        context.verifyDeadline();
        return RagState.withScope(search.validateScope(context.ownerId(), state.noteIds()));
    }
}
