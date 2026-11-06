package com.sya.ragnote.client;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

import com.sya.ragnote.client.chat.LangChainChatModelClient;
import com.sya.ragnote.client.embedding.LangChainEmbeddingClient;
import com.sya.ragnote.config.RagProperties;
import com.sya.ragnote.enums.result.ChatResultEnum;
import com.sya.ragnote.enums.result.NoteResultEnum;
import com.sya.ragnote.exception.ServiceSystemException;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.exception.HttpException;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;

import org.junit.jupiter.api.Test;

import java.util.List;

class ModelClientsTest {
    @Test
    void embeddingRejectsWrongDimensionsAndNonFiniteNumbers() {
        EmbeddingModel model = mock(EmbeddingModel.class);
        RagProperties properties = new RagProperties();
        when(model.embedAll(anyList()))
                .thenReturn(Response.from(List.of(Embedding.from(new float[] {1, 2}))));
        var client = new LangChainEmbeddingClient(model, properties);
        assertEquals(
                NoteResultEnum.EMBEDDING_CONFIGURATION_MISMATCH,
                assertThrows(ServiceSystemException.class, () -> client.embed("text"))
                        .getResultCode());
        float[] vector = new float[1024];
        vector[2] = Float.NaN;
        when(model.embedAll(anyList())).thenReturn(Response.from(List.of(Embedding.from(vector))));
        assertEquals(
                NoteResultEnum.EMBEDDING_CONFIGURATION_MISMATCH,
                assertThrows(ServiceSystemException.class, () -> client.embed("text"))
                        .getResultCode());
    }

    @Test
    void rateLimitIsNotReportedAsSuccessOrRetried() {
        ChatModel model = mock(ChatModel.class);
        when(model.chat(anyList())).thenThrow(new HttpException(429, "provider details"));
        var client = new LangChainChatModelClient(model);
        var failure =
                assertThrows(
                        ServiceSystemException.class,
                        () -> client.generate("system", "private note"));
        assertEquals(ChatResultEnum.REQUEST_LIMIT_EXCEEDED, failure.getResultCode());
        assertInstanceOf(HttpException.class, failure.getCause());
        verify(model, times(1)).chat(anyList());
    }

    @Test
    void modelIdentityIncludesEndpointAndDimensions() {
        RagProperties first = new RagProperties();
        RagProperties second = new RagProperties();
        second.getModels().getEmbedding().setBaseUrl("http://localhost:9999/v1");
        EmbeddingModel model = mock(EmbeddingModel.class);
        assertNotEquals(
                new LangChainEmbeddingClient(model, first).modelId(),
                new LangChainEmbeddingClient(model, second).modelId());
    }
}
