package com.sya.ragnote.service.impl.chat;

import com.sya.ragnote.config.RagProperties;
import com.sya.ragnote.converter.chat.ChatConverter;
import com.sya.ragnote.dto.chat.ExecutionContext;
import com.sya.ragnote.dto.chat.ValidatedAnswer;
import com.sya.ragnote.enums.result.*;
import com.sya.ragnote.exception.ServiceSystemException;
import com.sya.ragnote.graph.rag.RagGraph;
import com.sya.ragnote.param.chat.AskQuestionParam;
import com.sya.ragnote.service.chat.ChatService;
import com.sya.ragnote.tokenizer.TokenCounter;
import com.sya.ragnote.vo.chat.AskQuestionVo;
import com.sya.ragnote.web.Responses;

import jakarta.annotation.PreDestroy;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ChatServiceImpl implements ChatService {
    private final RagGraph graph;
    private final ChatConverter converter;
    private final TokenCounter tokens;
    private final Responses responses;
    private final Semaphore permits;
    private final ExecutorService executor;
    private final int timeout;

    public ChatServiceImpl(
            RagGraph graph,
            ChatConverter converter,
            TokenCounter tokens,
            RagProperties properties,
            Responses responses) {
        this.responses = responses;
        this.graph = graph;
        this.converter = converter;
        this.tokens = tokens;
        timeout = Math.min(90, properties.getChat().getTimeoutSeconds());
        int capacity = Math.min(4, properties.getChat().getMaxConcurrent());
        permits = new Semaphore(capacity);
        var sequence = new AtomicInteger();
        executor =
                new ThreadPoolExecutor(
                        capacity,
                        capacity,
                        0,
                        TimeUnit.SECONDS,
                        new SynchronousQueue<>(),
                        r -> {
                            var thread = new Thread(r, "rag-ask-" + sequence.incrementAndGet());
                            thread.setDaemon(true);
                            return thread;
                        },
                        new ThreadPoolExecutor.AbortPolicy());
    }

    @Override
    public AskQuestionVo ask(long ownerId, AskQuestionParam param) {
        if (param == null
                || param.getQuestion() == null
                || param.getQuestion().isBlank()
                || param.getQuestion().length() > 2000
                || tokens.count(param.getQuestion()) > 1600) {
            throw new ServiceSystemException(CommonResultEnum.INVALID_PARAMETER);
        }
        if (param.getNoteIds() != null
                && (param.getNoteIds().isEmpty()
                || param.getNoteIds().size() > 20
                || param.getNoteIds().stream().anyMatch(Objects::isNull))) {
            throw new ServiceSystemException(CommonResultEnum.INVALID_PARAMETER);
        }
        String question = param.getQuestion();
        List<UUID> ids = param.getNoteIds() == null ? null : List.copyOf(param.getNoteIds());
        // 先占一个并发名额，已满时直接返回限流。
        if (!permits.tryAcquire()) {
            throw new ServiceSystemException(ChatResultEnum.REQUEST_LIMIT_EXCEEDED);
        }
        var context =
                new ExecutionContext(
                        ownerId, responses.requestId(), Instant.now().plusSeconds(timeout));
        var task = new FutureTask<ValidatedAnswer>(() -> graph.run(context, question, ids));
        try {
            executor.execute(
                    () -> {
                        try {
                            task.run();
                        } finally {
                            // 等工作线程真正退出才释放名额，超时响应不会提前腾出容量。
                            permits.release();
                        }
                    });
        } catch (RejectedExecutionException e) {
            permits.release();
            throw new ServiceSystemException(ChatResultEnum.REQUEST_LIMIT_EXCEEDED, e);
        }
        try {
            return converter.toVo(task.get(timeout, TimeUnit.SECONDS));
        } catch (TimeoutException e) {
            task.cancel(true);
            throw new ServiceSystemException(ChatResultEnum.MODEL_TIMEOUT, e);
        } catch (InterruptedException e) {
            task.cancel(true);
            Thread.currentThread().interrupt();
            throw new ServiceSystemException(ChatResultEnum.MODEL_TIMEOUT, e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof ServiceSystemException business) {
                throw business;
            }
            throw new ServiceSystemException(ChatResultEnum.MODEL_UNAVAILABLE, e);
        }
    }

    @PreDestroy
    public void close() {
        executor.shutdownNow();
    }
}
