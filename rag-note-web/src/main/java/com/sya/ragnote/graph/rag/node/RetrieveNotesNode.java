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
                                retrievalQuestion(state),
                                state.scope(),
                                properties.getRetrieval().getTopK(),
                                properties.getRetrieval().getMinScore()));
        if (evidence.isEmpty()) {
            search.validateEvidence(context.ownerId(), evidence);
        }
        return RagState.withEvidence(evidence);
    }

    private static String retrievalQuestion(RagState state) {
        String question = state.question();
        var history = state.history();
        // 常见短追问补上上一轮的问题，保留当前请求的笔记范围。
        if (!history.isEmpty() && question.length() <= 120
                && question.matches("(?s).*(刚才|上[一轮次]|第.{1,5}点|那.{0,4}[个点条]|展开|继续|再说).*")) {
            return history.get(history.size() - 1).question() + "\n" + question;
        }
        return question;
    }
}
