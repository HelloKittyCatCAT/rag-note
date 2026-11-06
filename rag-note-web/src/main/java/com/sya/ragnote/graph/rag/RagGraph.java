package com.sya.ragnote.graph.rag;

import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

import com.sya.ragnote.dto.chat.*;
import com.sya.ragnote.enums.result.ChatResultEnum;
import com.sya.ragnote.exception.ServiceSystemException;
import com.sya.ragnote.graph.rag.node.*;

import org.bsc.langgraph4j.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Supplier;

/** 单轮问答流程。校验范围 → 检索 → 生成 → 校验引用，缺少证据时提前结束。 */
@Component
public class RagGraph {
    private static final Logger log = LoggerFactory.getLogger(RagGraph.class);
    private final ValidateScopeNode scope;
    private final RetrieveNotesNode retrieve;
    private final GenerateAnswerNode generate;
    private final ValidateAnswerNode validate;

    public RagGraph(
            ValidateScopeNode scope,
            RetrieveNotesNode retrieve,
            GenerateAnswerNode generate,
            ValidateAnswerNode validate) {
        this.scope = scope;
        this.retrieve = retrieve;
        this.generate = generate;
        this.validate = validate;
    }

    private Map<String, Object> runNode(
            String name, ExecutionContext context, Supplier<Map<String, Object>> action) {
        long started = System.nanoTime();
        try {
            var output = action.get();
            log.info(
                    "requestId={} node={} durationMs={} result=SUCCESS",
                    context.requestId(),
                    name,
                    (System.nanoTime() - started) / 1_000_000);
            return output;
        } catch (RuntimeException e) {
            log.warn(
                    "requestId={} node={} durationMs={} result=FAILED",
                    context.requestId(),
                    name,
                    (System.nanoTime() - started) / 1_000_000);
            throw e;
        }
    }

    public ValidatedAnswer run(ExecutionContext context, String question, List<UUID> ids) {
        try {
            // 每次请求独立建图，用户身份由不可变的 context 传给节点。
            var graph =
                    new StateGraph<RagState>(RagState::new)
                            .addNode(
                                    "ValidateScope",
                                    node_async(
                                            s ->
                                                    runNode(
                                                            "ValidateScope",
                                                            context,
                                                            () -> scope.apply(s, context))))
                            .addNode(
                                    "RetrieveNotes",
                                    node_async(
                                            s ->
                                                    runNode(
                                                            "RetrieveNotes",
                                                            context,
                                                            () -> retrieve.apply(s, context))))
                            .addNode(
                                    "GenerateAnswer",
                                    node_async(
                                            s ->
                                                    runNode(
                                                            "GenerateAnswer",
                                                            context,
                                                            () -> generate.apply(s, context))))
                            .addNode(
                                    "ValidateAnswer",
                                    node_async(
                                            s ->
                                                    runNode(
                                                            "ValidateAnswer",
                                                            context,
                                                            () -> validate.apply(s, context))))
                            // 节点写入 result 后，条件边直接跳到 END，省去后续模型调用。
                            .addEdge(StateGraph.START, "ValidateScope")
                            .addConditionalEdges(
                                    "ValidateScope",
                                    edge_async(s -> s.finished() ? "end" : "retrieve"),
                                    Map.of("end", StateGraph.END, "retrieve", "RetrieveNotes"))
                            .addConditionalEdges(
                                    "RetrieveNotes",
                                    edge_async(s -> s.finished() ? "end" : "generate"),
                                    Map.of("end", StateGraph.END, "generate", "GenerateAnswer"))
                            .addEdge("GenerateAnswer", "ValidateAnswer")
                            .addEdge("ValidateAnswer", StateGraph.END)
                            .compile();
            // 从问题和笔记范围开始执行，最终结果由最后完成的节点写入状态。
            var state =
                    graph.invoke(
                                    GraphInput.args(RagState.initial(question, ids)),
                                    RunnableConfig.builder().disableCloneState().build())
                            .orElseThrow();
            context.verifyDeadline();
            return state.result();
        } catch (ServiceSystemException e) {
            throw e;
        } catch (Exception e) {
            Throwable cause = e;
            while (cause.getCause() != null && cause.getCause() != cause) {
                if (cause instanceof ServiceSystemException business) {
                    throw business;
                }
                cause = cause.getCause();
            }
            if (cause instanceof ServiceSystemException business) {
                throw business;
            }
            throw new ServiceSystemException(ChatResultEnum.MODEL_UNAVAILABLE, e);
        }
    }
}
