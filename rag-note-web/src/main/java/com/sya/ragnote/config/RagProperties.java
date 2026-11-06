package com.sya.ragnote.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import lombok.Getter;
import lombok.Setter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties("rag")
public class RagProperties {
    @Valid private Models models = new Models();
    @Valid private Ingestion ingestion = new Ingestion();
    @Valid private Retrieval retrieval = new Retrieval();
    @Valid private Chat chat = new Chat();
    @Valid private Bootstrap bootstrap = new Bootstrap();

    @Getter
    @Setter
    public static class Models {
        @Valid private Model chat = new Model();
        @Valid private Embedding embedding = new Embedding();
    }

    @Getter
    @Setter
    public static class Model {
        @NotBlank
        @Pattern(regexp = "https?://[^\\s]+")
        private String baseUrl = "https://api.openai.com/v1";

        @NotBlank private String apiKey;
        @NotBlank private String model;

        @Min(1)
        @Max(60)
        private int timeoutSeconds = 60;
    }

    @Getter
    @Setter
    public static class Embedding extends Model {
        // V1 schema 固定 vector(1024)；改变维度必须迁移并重建索引。
        @Min(1024)
        @Max(1024)
        private int dimensions = 1024;
    }

    @Getter
    @Setter
    public static class Ingestion {
        @Min(1)
        @Max(128)
        private int maxChunks = 128;

        @Min(1)
        @Max(100)
        private int maxNotes = 100;

        @Min(128)
        @Max(600)
        private int chunkTokens = 600;

        @Min(0)
        @Max(80)
        private int overlapTokens = 80;

        @Min(1)
        @Max(2)
        private int maxConcurrent = 2;
    }

    @Getter
    @Setter
    public static class Retrieval {
        @Min(1)
        @Max(6)
        private int topK = 6;

        @DecimalMin("-1")
        @DecimalMax("1")
        private double minScore = .50;

        @Min(600)
        @Max(4000)
        private int maxEvidenceTokens = 4000;
    }

    @Getter
    @Setter
    public static class Chat {
        @Min(1)
        @Max(4)
        private int maxConcurrent = 4;

        @Min(1)
        @Max(90)
        private int timeoutSeconds = 90;
    }

    @Getter
    @Setter
    public static class Bootstrap {
        private String username;
        private String password;
    }
}
