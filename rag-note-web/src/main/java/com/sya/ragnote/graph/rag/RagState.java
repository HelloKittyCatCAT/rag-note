package com.sya.ragnote.graph.rag;

import com.sya.ragnote.dto.chat.ValidatedAnswer;
import com.sya.ragnote.dto.note.*;

import org.bsc.langgraph4j.state.AgentState;

import java.util.*;

/** 一次问答的数据载体。节点读取已有状态，再返回本步骤要写入的字段。 */
public final class RagState extends AgentState {
    public RagState(Map<String, Object> data) {
        super(data);
    }

    public String question() {
        return this.<String>value("question").orElseThrow();
    }

    public List<UUID> noteIds() {
        return this.<List<UUID>>value("noteIds").orElse(null);
    }

    public NoteScope scope() {
        return this.<NoteScope>value("scope").orElseThrow();
    }

    public List<NoteEvidence> evidence() {
        return this.<List<NoteEvidence>>value("evidence").orElse(List.of());
    }

    public String rawAnswer() {
        return this.<String>value("rawAnswer").orElse("");
    }

    public ValidatedAnswer result() {
        return this.<ValidatedAnswer>value("result").orElseThrow();
    }

    public boolean finished() {
        return value("result").isPresent();
    }

    public static Map<String, Object> initial(String question, List<UUID> ids) {
        var data = new HashMap<String, Object>();
        data.put("question", question);
        if (ids != null) {
            data.put("noteIds", List.copyOf(ids));
        }
        return data;
    }

    public static Map<String, Object> withScope(NoteScope scope) {
        return scope.empty()
                ? Map.of("scope", scope, "result", ValidatedAnswer.noEvidence())
                : Map.of("scope", scope);
    }

    public static Map<String, Object> withEvidence(List<NoteEvidence> evidence) {
        return evidence.isEmpty()
                ? Map.of("evidence", List.of(), "result", ValidatedAnswer.noEvidence())
                : Map.of("evidence", List.copyOf(evidence));
    }

    public static Map<String, Object> withRawAnswer(String raw) {
        return Map.of("rawAnswer", raw == null ? "" : raw);
    }

    public static Map<String, Object> withResult(ValidatedAnswer result) {
        return Map.of("result", result);
    }
}
