package com.sya.ragnote.client.embedding;

import com.sya.ragnote.client.ModelFailure;
import com.sya.ragnote.config.RagProperties;
import com.sya.ragnote.enums.result.ChatResultEnum;
import com.sya.ragnote.enums.result.NoteResultEnum;
import com.sya.ragnote.exception.ServiceSystemException;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

@Component
public class LangChainEmbeddingClient implements EmbeddingClient {
    private final java.util.concurrent.ExecutorService executor =
            new java.util.concurrent.ThreadPoolExecutor(
                    0,
                    6,
                    30,
                    java.util.concurrent.TimeUnit.SECONDS,
                    new java.util.concurrent.SynchronousQueue<>(),
                    r -> {
                        Thread thread = new Thread(r, "embedding-call");
                        thread.setDaemon(true);
                        return thread;
                    });
    private final EmbeddingModel model;
    private final int dimensions;
    private final String modelId;

    public LangChainEmbeddingClient(EmbeddingModel model, RagProperties properties) {
        this.model = model;
        var config = properties.getModels().getEmbedding();
        dimensions = config.getDimensions();
        // 用端点、模型名和维度生成标识，检索时据此识别配置是否一致。
        String identity =
                config.getBaseUrl().replaceAll("/+$", "")
                        + "|"
                        + config.getModel()
                        + "|"
                        + dimensions;
        try {
            modelId =
                    "embedding:"
                            + HexFormat.of()
                            .formatHex(
                                    MessageDigest.getInstance("SHA-256")
                                            .digest(
                                                    identity.getBytes(
                                                            StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public String modelId() {
        return modelId;
    }

    @Override
    public float[] embed(String text) {
        return embedAll(List.of(text)).get(0);
    }

    /**
     * 批量把文本转为向量。返回列表与输入顺序一致，第 i 个向量对应第 i 段文本。
     * 每批最多向模型发送 16 段，所有批次共用 60 秒预算。
     * 任一批失败或返回无效向量时抛出异常，调用方不会拿到部分结果。
     */
    @Override
    public List<float[]> embedAll(List<String> texts) {
        // 固定这次调用的输入，避免调用方在后台任务执行期间修改列表。
        List<String> input = List.copyOf(texts);
        java.util.concurrent.Future<List<float[]>> task;
        try {
            // 后台线程负责分批调用模型，Future 用来等待整批结果或取消任务。
            task = executor.submit(() -> embedBatches(input));
        } catch (java.util.concurrent.RejectedExecutionException error) {
            // 线程池无法接收新任务时，直接向上层报告限流。
            throw new ServiceSystemException(ChatResultEnum.REQUEST_LIMIT_EXCEEDED, error);
        }
        try {
            // 这里等待的是整个任务，60 秒包含所有批次。
            return task.get(60, java.util.concurrent.TimeUnit.SECONDS);
        } catch (java.util.concurrent.TimeoutException error) {
            // 等待超时后请求中断后台任务，本次调用以超时结束。
            task.cancel(true);
            throw new ServiceSystemException(ChatResultEnum.MODEL_TIMEOUT, error);
        } catch (InterruptedException error) {
            task.cancel(true);
            // 保留调用线程收到的中断信号，让上层仍能感知取消。
            Thread.currentThread().interrupt();
            throw new ServiceSystemException(ChatResultEnum.MODEL_TIMEOUT, error);
        } catch (java.util.concurrent.ExecutionException error) {
            // Future 会包装后台异常，取出原来的业务错误，例如模型返回的向量维度不符。
            if (error.getCause() instanceof ServiceSystemException business) {
                throw business;
            }
            throw new ServiceSystemException(ChatResultEnum.MODEL_UNAVAILABLE, error.getCause());
        }
    }

    @jakarta.annotation.PreDestroy
    public void close() {
        executor.shutdownNow();
    }

    private List<float[]> embedBatches(List<String> texts) {
        if (texts.isEmpty()) {
            return List.of();
        }
        long deadline = System.nanoTime() + java.time.Duration.ofSeconds(60).toNanos();
        List<float[]> result = new ArrayList<>();
        // 每次最多发送 16 段文本，并按分块顺序收集向量。
        for (int start = 0; start < texts.size(); start += 16) {
            if (System.nanoTime() >= deadline) {
                throw new ServiceSystemException(ChatResultEnum.MODEL_TIMEOUT);
            }
            var batch = texts.subList(start, Math.min(start + 16, texts.size()));
            List<dev.langchain4j.data.embedding.Embedding> embeddings;
            try {
                // 将文本包装为 LangChain4j 的 TextSegment，由 SDK 发出 Embedding 请求。
                embeddings =
                        model.embedAll(batch.stream().map(TextSegment::from).toList()).content();
            } catch (RuntimeException e) {
                throw ModelFailure.translate(e);
            }
            if (System.nanoTime() >= deadline) {
                throw new ServiceSystemException(ChatResultEnum.MODEL_TIMEOUT);
            }
            // 一段文本应有一个结果，数量对不上就不能把向量与分块一一关联。
            if (embeddings == null || embeddings.size() != batch.size()) {
                throw new ServiceSystemException(NoteResultEnum.EMBEDDING_CONFIGURATION_MISMATCH);
            }
            for (var embedding : embeddings) {
                if (embedding == null
                        || embedding.vector() == null
                        || embedding.vector().length != dimensions) {
                    throw new ServiceSystemException(
                            NoteResultEnum.EMBEDDING_CONFIGURATION_MISMATCH);
                }
                float[] vector = embedding.vector().clone();
                // 同时检查非有限值和全零向量，余弦距离需要有效且非零的向量。
                double norm = 0;
                for (float value : vector) {
                    if (!Float.isFinite(value)) {
                        throw new ServiceSystemException(
                                NoteResultEnum.EMBEDDING_CONFIGURATION_MISMATCH);
                    }
                    norm += (double) value * value;
                }
                if (norm == 0) {
                    throw new ServiceSystemException(
                            NoteResultEnum.EMBEDDING_CONFIGURATION_MISMATCH);
                }
                result.add(vector);
            }
        }
        // 全部批次都成功后才返回，列表顺序与输入一致。
        return List.copyOf(result);
    }
}
