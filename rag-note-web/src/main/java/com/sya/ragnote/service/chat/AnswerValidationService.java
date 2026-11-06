package com.sya.ragnote.service.chat;

import com.sya.ragnote.dto.chat.ValidatedAnswer;
import com.sya.ragnote.dto.note.NoteEvidence;

import java.util.List;

public interface AnswerValidationService {
    ValidatedAnswer validate(long ownerId, List<NoteEvidence> evidence, String raw);
}
