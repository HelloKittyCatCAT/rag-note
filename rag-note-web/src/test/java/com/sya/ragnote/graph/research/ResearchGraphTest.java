package com.sya.ragnote.graph.research;

import com.sya.ragnote.client.research.SafePageClient;
import com.sya.ragnote.config.RagProperties;
import com.sya.ragnote.dto.note.NoteScope;
import com.sya.ragnote.dto.chat.ChatMemory;
import com.sya.ragnote.service.note.NoteSearchService;
import com.sya.ragnote.tokenizer.TokenCounter;
import com.sya.ragnote.vo.research.ResearchAnswerVo.Status;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ResearchGraphTest {
    private final ChatModel model = mock(ChatModel.class);
    private final NoteSearchService notes = mock(NoteSearchService.class);
    private final SafePageClient pages = mock(SafePageClient.class);
    private final ResearchGraph graph = new ResearchGraph(model, notes, pages,
            new RagProperties(), mock(TokenCounter.class));
    private final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);

    @Test
    void readsCandidateBeforeAcceptingWebCitation() {
        when(notes.validateScope(7, null)).thenReturn(new NoteScope(List.of(), true, true));
        when(pages.read("https://redis.io/", deadline)).thenReturn("实际网页正文");
        when(model.chat(any(ChatRequest.class))).thenReturn(
                call("1", "readWeb", "{\"pageId\":\"P1\"}"),
                answer("{\"answer\":\"结论[W1]\",\"citedSourceIds\":[\"W1\"],\"findings\":[],\"limitations\":[]}"));

        var result = graph.run(7, "查一下 Redis", null, List.of("https://redis.io/"), deadline, ChatMemory.EMPTY);
        assertEquals(Status.COMPLETED, result.status());
        assertEquals("实际网页正文", result.sources().get(0).snippet());
        assertEquals(List.of("readWeb"), result.trace().stream()
                .map(trace -> trace.tool()).toList());
        verify(model, times(2)).chat(any(ChatRequest.class));
    }

    @Test
    void unauthorizedWebToolAndFabricatedCitationDoNotReachNetwork() {
        when(notes.validateScope(7, null)).thenReturn(new NoteScope(List.of(), true, true));
        when(model.chat(any(ChatRequest.class))).thenReturn(
                call("1", "readWeb", "{\"pageId\":\"P1\"}"),
                answer("{\"answer\":\"结论[W1]\",\"citedSourceIds\":[\"W1\"]}"));

        var result = graph.run(7, "Redis 怎么用", null, List.of(), deadline, ChatMemory.EMPTY);
        assertEquals(Status.INSUFFICIENT, result.status());
        assertTrue(result.sources().isEmpty());
        verifyNoInteractions(pages);
    }

    @Test
    void modelCannotReadAnUnlistedPage() {
        when(notes.validateScope(7, null)).thenReturn(new NoteScope(List.of(), true, true));
        when(model.chat(any(ChatRequest.class))).thenReturn(
                call("1", "readWeb", "{\"pageId\":\"P99\"}"), answer("{}"));

        assertEquals(Status.INSUFFICIENT, graph.run(7, "查一下 Redis", null, List.of("https://redis.io/"), deadline, ChatMemory.EMPTY).status());
        verifyNoInteractions(pages);
    }

    private static ChatResponse call(String id, String name, String arguments) {
        return ChatResponse.builder().aiMessage(AiMessage.from(ToolExecutionRequest.builder()
                .id(id).name(name).arguments(arguments).build())).build();
    }

    private static ChatResponse answer(String value) {
        return ChatResponse.builder().aiMessage(AiMessage.from(value)).build();
    }
}
