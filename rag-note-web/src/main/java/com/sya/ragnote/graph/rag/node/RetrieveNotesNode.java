package com.sya.ragnote.graph.rag.node;

import com.sya.ragnote.config.RagProperties;
import com.sya.ragnote.dto.chat.ExecutionContext;
import com.sya.ragnote.dto.note.NoteSearchQuery;
import com.sya.ragnote.graph.rag.RagState;
import com.sya.ragnote.service.note.NoteSearchService;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class RetrieveNotesNode {
    private final NoteSearchService search;
    private final RagProperties properties;

    public RetrieveNotesNode(NoteSearchService search, RagProperties properties) {
        this.search = search;
        this.properties = properties;
    }

    public Map<String, Object> apply(RagState state, ExecutionContext context) {
        context.verifyDeadline();
        var evidence =
                search.search(
                        context.ownerId(),
                        new NoteSearchQuery(
                                state.question(),
                                state.scope(),
                                properties.getRetrieval().getTopK(),
                                properties.getRetrieval().getMinScore()));
        if (evidence.isEmpty()) {
            search.validateEvidence(context.ownerId(), evidence);
        }
        return RagState.withEvidence(evidence);
    }
}
