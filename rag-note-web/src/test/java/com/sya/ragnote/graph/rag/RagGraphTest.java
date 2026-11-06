package com.sya.ragnote.graph.rag;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.sya.ragnote.client.chat.ChatModelClient;
import com.sya.ragnote.config.RagProperties;
import com.sya.ragnote.dto.chat.*;
import com.sya.ragnote.dto.note.*;
import com.sya.ragnote.enums.chat.AnswerStatus;
import com.sya.ragnote.graph.rag.node.*;
import com.sya.ragnote.prompt.GroundedAnswerPrompt;
import com.sya.ragnote.service.chat.AnswerValidationService;
import com.sya.ragnote.service.note.NoteSearchService;
import com.sya.ragnote.tokenizer.TokenCounter;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.*;

class RagGraphTest {
    @Test
    void emptyScopeEndsWithoutRetrievalOrChat() throws Exception {
        var search = mock(NoteSearchService.class);
        var model = mock(ChatModelClient.class);
        var validation = mock(AnswerValidationService.class);
        when(search.validateScope(7, null)).thenReturn(new NoteScope(List.of(), true, true));
        var graph = graph(search, model, validation);
        assertEquals(AnswerStatus.NO_EVIDENCE, graph.run(context(7), "hello", null).status());
        verify(search).validateScope(7, null);
        verifyNoMoreInteractions(search);
        verifyNoInteractions(model, validation);
    }

    @Test
    void emptyRetrievalNeverCallsChat() throws Exception {
        var search = mock(NoteSearchService.class);
        var model = mock(ChatModelClient.class);
        var validation = mock(AnswerValidationService.class);
        when(search.validateScope(7, null)).thenReturn(new NoteScope(List.of(), true, false));
        when(search.search(eq(7L), any())).thenReturn(List.of());
        assertEquals(
                AnswerStatus.NO_EVIDENCE,
                graph(search, model, validation).run(context(7), "hello", null).status());
        verifyNoInteractions(model, validation);
    }

    @Test
    void generatedOutputFlowsThroughValidationWithTrustedOwner() throws Exception {
        var search = mock(NoteSearchService.class);
        var model = mock(ChatModelClient.class);
        var validation = mock(AnswerValidationService.class);
        var evidence =
                List.of(
                        new NoteEvidence(
                                UUID.randomUUID(), UUID.randomUUID(), "t", "", 1, 1, "abc", .8));
        when(search.validateScope(9, null)).thenReturn(new NoteScope(List.of(), true, false));
        when(search.search(eq(9L), any())).thenReturn(evidence);
        when(model.generate(anyString(), anyString())).thenReturn("output");
        var expected = new ValidatedAnswer("answer [S1]", AnswerStatus.ANSWERED, evidence);
        when(validation.validate(9, evidence, "output")).thenReturn(expected);
        assertEquals(expected, graph(search, model, validation).run(context(9), "question", null));
        verify(validation).validate(9, evidence, "output");
    }

    private RagGraph graph(
            NoteSearchService search, ChatModelClient model, AnswerValidationService validation)
            throws Exception {
        return new RagGraph(
                new ValidateScopeNode(search),
                new RetrieveNotesNode(search, new RagProperties()),
                new GenerateAnswerNode(model, new GroundedAnswerPrompt(), mock(TokenCounter.class)),
                new ValidateAnswerNode(validation));
    }

    private ExecutionContext context(long owner) {
        return new ExecutionContext(owner, "test", Instant.now().plusSeconds(10));
    }
}
