package com.sya.ragnote.service.impl.chat;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.sya.ragnote.dto.note.NoteEvidence;
import com.sya.ragnote.enums.chat.AnswerStatus;
import com.sya.ragnote.service.note.NoteSearchService;

import org.junit.jupiter.api.Test;

import java.util.*;

class AnswerValidationServiceImplTest {
    private final NoteSearchService search = mock(NoteSearchService.class);
    private final AnswerValidationServiceImpl service = new AnswerValidationServiceImpl(search);
    private final List<NoteEvidence> evidence =
            List.of(
                    new NoteEvidence(
                            UUID.randomUUID(), UUID.randomUUID(), "t", "", 1, 1, "abc", .9));

    @Test
    void inventedSourceNeverLeaksRawAnswer() {
        var result =
                service.validate(
                        1,
                        evidence,
                        "{\"answer\":\"secret"
                            + " [S99]\",\"citedSourceIds\":[\"S99\"],\"insufficientEvidence\":false}");
        assertEquals(AnswerStatus.EVIDENCE_ONLY, result.status());
        assertFalse(result.answer().contains("secret"));
        verify(search).validateEvidence(1, evidence);
    }

    @Test
    void validFencedJsonAccepted() {
        var result =
                service.validate(
                        1,
                        evidence,
                        "```json\n"
                            + "{\"answer\":\"abc"
                            + " [S1]\",\"citedSourceIds\":[\"S1\"],\"insufficientEvidence\":false}\n"
                            + "```");
        assertEquals(AnswerStatus.ANSWERED, result.status());
        assertEquals("abc [S1]", result.answer());
    }

    @Test
    void insufficientRetainsEvidence() {
        var result =
                service.validate(
                        1,
                        evidence,
                        "{\"answer\":\"ignore\",\"citedSourceIds\":[],\"insufficientEvidence\":true}");
        assertEquals(AnswerStatus.NO_EVIDENCE, result.status());
        assertEquals(evidence, result.evidence());
    }

    @Test
    void trailingJsonRejected() {
        var result =
                service.validate(
                        1,
                        evidence,
                        "{\"answer\":\"abc"
                            + " [S1]\",\"citedSourceIds\":[\"S1\"],\"insufficientEvidence\":false}"
                            + " {}");
        assertEquals(AnswerStatus.EVIDENCE_ONLY, result.status());
    }
}
