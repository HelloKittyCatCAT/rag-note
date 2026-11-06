package com.sya.ragnote.config;

import dev.langchain4j.http.client.jdk.JdkHttpClient;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(RagProperties.class)
public class ModelConfig {
    @Bean
    ChatModel chatModel(RagProperties properties) {
        var config = properties.getModels().getChat();
        return OpenAiChatModel.builder()
                .baseUrl(config.getBaseUrl())
                .apiKey(config.getApiKey())
                .modelName(config.getModel())
                .httpClientBuilder(JdkHttpClient.builder().connectTimeout(Duration.ofSeconds(5)))
                .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                .maxRetries(0)
                .maxTokens(1000)
                .logRequests(false)
                .logResponses(false)
                .build();
    }

    @Bean
    EmbeddingModel embeddingModel(RagProperties properties) {
        var config = properties.getModels().getEmbedding();
        return OpenAiEmbeddingModel.builder()
                .baseUrl(config.getBaseUrl())
                .apiKey(config.getApiKey())
                .modelName(config.getModel())
                .dimensions(config.getDimensions())
                .httpClientBuilder(JdkHttpClient.builder().connectTimeout(Duration.ofSeconds(5)))
                .timeout(Duration.ofSeconds(config.getTimeoutSeconds()))
                .maxRetries(0)
                .maxSegmentsPerBatch(16)
                .logRequests(false)
                .logResponses(false)
                .build();
    }
}
