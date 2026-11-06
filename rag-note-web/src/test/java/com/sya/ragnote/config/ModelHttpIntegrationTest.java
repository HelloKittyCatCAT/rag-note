package com.sya.ragnote.config;

import static org.junit.jupiter.api.Assertions.*;

import com.sun.net.httpserver.HttpServer;
import com.sya.ragnote.client.chat.LangChainChatModelClient;
import com.sya.ragnote.client.embedding.LangChainEmbeddingClient;
import com.sya.ragnote.enums.result.ChatResultEnum;
import com.sya.ragnote.exception.ServiceSystemException;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/** 本地 HTTP 替身验证真实 LangChain4j 适配协议，不访问外部供应商。 */
class ModelHttpIntegrationTest {
    @Test
    void configuredModelsUseOpenAiCompatibleProtocolAndDontRetry429() throws Exception {
        JsonMapper json = JsonMapper.builder().build();
        AtomicBoolean limit = new AtomicBoolean();
        AtomicInteger chatCalls = new AtomicInteger();
        AtomicInteger maxTokens = new AtomicInteger();
        HttpServer server =
                HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext(
                "/v1/chat/completions",
                exchange -> {
                    chatCalls.incrementAndGet();
                    var request = json.readTree(exchange.getRequestBody());
                    maxTokens.set(request.path("max_tokens").asInt());
                    String body =
                            limit.get()
                                    ? "{\"error\":{\"message\":\"rate"
                                            + " limited\",\"type\":\"rate_limit\"}}"
                                    : json.writeValueAsString(
                                            Map.of(
                                                    "id",
                                                    "chat-test",
                                                    "object",
                                                    "chat.completion",
                                                    "created",
                                                    0,
                                                    "model",
                                                    "test-model",
                                                    "choices",
                                                    List.of(
                                                            Map.of(
                                                                    "index",
                                                                    0,
                                                                    "message",
                                                                    Map.of(
                                                                            "role",
                                                                            "assistant",
                                                                            "content",
                                                                            "{\"answer\":\"有依据[S1]\",\"citedSourceIds\":[\"S1\"],\"insufficientEvidence\":false}"),
                                                                    "finish_reason",
                                                                    "stop"))));
                    byte[] bytes = body.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(limit.get() ? 429 : 200, bytes.length);
                    exchange.getResponseBody().write(bytes);
                    exchange.close();
                });
        server.createContext(
                "/v1/embeddings",
                exchange -> {
                    var request = json.readTree(exchange.getRequestBody());
                    int count = request.path("input").size();
                    var vectors = new java.util.ArrayList<Map<String, Object>>();
                    for (int i = 0; i < count; i++) {
                        float[] vector = new float[1024];
                        vector[0] = 1;
                        vectors.add(Map.of("object", "embedding", "index", i, "embedding", vector));
                    }
                    byte[] bytes =
                            json.writeValueAsBytes(
                                    Map.of(
                                            "object",
                                            "list",
                                            "model",
                                            "test-embedding",
                                            "data",
                                            vectors));
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, bytes.length);
                    exchange.getResponseBody().write(bytes);
                    exchange.close();
                });
        server.start();
        try {
            RagProperties properties = new RagProperties();
            String endpoint = "http://localhost:" + server.getAddress().getPort() + "/v1";
            properties.getModels().getChat().setBaseUrl(endpoint);
            properties.getModels().getChat().setApiKey("local-test-only");
            properties.getModels().getChat().setModel("test-model");
            properties.getModels().getEmbedding().setBaseUrl(endpoint);
            properties.getModels().getEmbedding().setApiKey("local-test-only");
            properties.getModels().getEmbedding().setModel("test-embedding");
            ModelConfig config = new ModelConfig();
            var chat = new LangChainChatModelClient(config.chatModel(properties));
            assertTrue(chat.generate("仅据证据", "测试笔记").contains("S1"));
            assertEquals(1000, maxTokens.get());
            limit.set(true);
            assertEquals(
                    ChatResultEnum.REQUEST_LIMIT_EXCEEDED,
                    assertThrows(
                                    ServiceSystemException.class,
                                    () -> chat.generate("system", "user"))
                            .getResultCode());
            assertEquals(2, chatCalls.get());
            var embeddings =
                    new LangChainEmbeddingClient(config.embeddingModel(properties), properties);
            try {
                assertEquals(1024, embeddings.embed("笔记").length);
                assertEquals(
                        17, embeddings.embedAll(java.util.Collections.nCopies(17, "text")).size());
            } finally {
                embeddings.close();
            }
        } finally {
            server.stop(0);
        }
    }
}
