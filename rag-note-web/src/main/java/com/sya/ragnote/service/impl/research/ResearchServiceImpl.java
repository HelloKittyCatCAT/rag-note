package com.sya.ragnote.service.impl.research;

import com.sya.ragnote.enums.result.ChatResultEnum;
import com.sya.ragnote.enums.result.ResearchResultEnum;
import com.sya.ragnote.exception.ServiceSystemException;
import com.sya.ragnote.graph.research.ResearchGraph;
import com.sya.ragnote.param.research.ResearchQuestionParam;
import com.sya.ragnote.service.research.ResearchService;
import com.sya.ragnote.service.conversation.ConversationContextService;
import com.sya.ragnote.vo.research.ResearchAnswerVo;
import jakarta.annotation.PreDestroy;
import java.util.List;
import java.util.concurrent.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ResearchServiceImpl implements ResearchService {
    private static final int TIMEOUT_SECONDS = 120;
    private final ResearchGraph graph;
    private final boolean enabled;
    private final ConversationContextService context;
    private final ExecutorService workers = new ThreadPoolExecutor(2, 2, 0, TimeUnit.SECONDS,
            new SynchronousQueue<>(), r -> {
                Thread worker = new Thread(r, "research-ask");
                worker.setDaemon(true);
                return worker;
            });

    public ResearchServiceImpl(ResearchGraph graph, ConversationContextService context,
            @Value("${rag.research.enabled:false}") boolean enabled) {
        this.graph = graph;
        this.context = context;
        this.enabled = enabled;
    }

    @Override
    public ResearchAnswerVo ask(long ownerId, ResearchQuestionParam param) {
        if (!enabled) throw new ServiceSystemException(ResearchResultEnum.DISABLED);
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(TIMEOUT_SECONDS);
        var ids = param.getNoteIds() == null ? null : List.copyOf(param.getNoteIds());
        String question = param.getQuestion();
        boolean allowWeb = param.isAllowWeb();
        var webUrls = !allowWeb || param.getWebUrls() == null
                ? List.<String>of() : List.copyOf(param.getWebUrls());
        var conversationId = param.getConversationId();
        Future<ResearchAnswerVo> task;
        try {
            task = workers.submit(() -> graph.run(ownerId, question, ids, webUrls, deadline,
                    context.load(ownerId, conversationId, ids)));
        } catch (RejectedExecutionException rejected) {
            throw new ServiceSystemException(ChatResultEnum.REQUEST_LIMIT_EXCEEDED, rejected);
        }
        try {
            return task.get(Math.max(1, deadline - System.nanoTime()), TimeUnit.NANOSECONDS);
        } catch (TimeoutException timeout) {
            task.cancel(true);
            throw new ServiceSystemException(ChatResultEnum.MODEL_TIMEOUT, timeout);
        } catch (InterruptedException interrupted) {
            task.cancel(true);
            Thread.currentThread().interrupt();
            throw new ServiceSystemException(ChatResultEnum.MODEL_TIMEOUT, interrupted);
        } catch (ExecutionException failed) {
            if (failed.getCause() instanceof RuntimeException error) throw error;
            throw new IllegalStateException(failed);
        }
    }

    @PreDestroy
    void close() {
        workers.shutdownNow();
    }
}
