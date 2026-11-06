package com.sya.ragnote.service.impl.chat;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.sya.ragnote.config.RagProperties;
import com.sya.ragnote.converter.chat.ChatConverter;
import com.sya.ragnote.dto.chat.ValidatedAnswer;
import com.sya.ragnote.enums.result.ChatResultEnum;
import com.sya.ragnote.exception.ServiceSystemException;
import com.sya.ragnote.graph.rag.RagGraph;
import com.sya.ragnote.param.chat.AskQuestionParam;
import com.sya.ragnote.tokenizer.TokenCounter;
import com.sya.ragnote.web.Responses;

import org.junit.jupiter.api.Test;

import java.util.concurrent.*;

class ChatServiceImplTest {
    @Test
    void fourRunningRequestsRejectFifthWithoutWaiting() throws Exception {
        var graph = mock(RagGraph.class);
        var entered = new CountDownLatch(4);
        var finish = new CountDownLatch(1);
        when(graph.run(any(), anyString(), isNull()))
                .thenAnswer(
                        call -> {
                            entered.countDown();
                            if (!finish.await(5, TimeUnit.SECONDS)) {
                                throw new IllegalStateException("test timeout");
                            }
                            return ValidatedAnswer.noEvidence();
                        });
        var service =
                new ChatServiceImpl(
                        graph,
                        new ChatConverter(),
                        mock(TokenCounter.class),
                        new RagProperties(),
                        new Responses());
        var callers = Executors.newFixedThreadPool(4);
        try {
            var futures = new java.util.ArrayList<Future<?>>();
            for (int i = 0; i < 4; i++) {
                futures.add(callers.submit(() -> service.ask(1, param())));
            }
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            var error = assertThrows(ServiceSystemException.class, () -> service.ask(1, param()));
            assertEquals(ChatResultEnum.REQUEST_LIMIT_EXCEEDED, error.getResultCode());
            finish.countDown();
            for (var future : futures) {
                future.get(5, TimeUnit.SECONDS);
            }
        } finally {
            finish.countDown();
            callers.shutdownNow();
            service.close();
        }
    }

    @Test
    void requestDeadlineMapsToTimeout() {
        var graph = mock(RagGraph.class);
        when(graph.run(any(), anyString(), isNull()))
                .thenAnswer(
                        call -> {
                            Thread.sleep(5000);
                            return ValidatedAnswer.noEvidence();
                        });
        var properties = new RagProperties();
        properties.getChat().setTimeoutSeconds(1);
        var service =
                new ChatServiceImpl(
                        graph,
                        new ChatConverter(),
                        mock(TokenCounter.class),
                        properties,
                        new Responses());
        try {
            var error = assertThrows(ServiceSystemException.class, () -> service.ask(1, param()));
            assertEquals(ChatResultEnum.MODEL_TIMEOUT, error.getResultCode());
        } finally {
            service.close();
        }
    }

    private AskQuestionParam param() {
        var p = new AskQuestionParam();
        p.setQuestion("question");
        return p;
    }
}
