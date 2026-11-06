package com.sya.ragnote.dto.chat;

import com.sya.ragnote.dto.note.NoteEvidence;
import com.sya.ragnote.enums.chat.AnswerStatus;

import java.util.List;

public record ValidatedAnswer(String answer, AnswerStatus status, List<NoteEvidence> evidence) {
    public ValidatedAnswer {
        evidence = List.copyOf(evidence);
    }

    public static ValidatedAnswer noEvidence() {
        return new ValidatedAnswer("未找到足够的笔记依据。", AnswerStatus.NO_EVIDENCE, List.of());
    }
}
