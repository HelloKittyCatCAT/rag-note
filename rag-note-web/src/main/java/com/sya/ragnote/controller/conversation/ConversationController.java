package com.sya.ragnote.controller.conversation;

import com.sya.ragnote.param.common.PageParam;
import com.sya.ragnote.param.conversation.ConversationAskParam;
import com.sya.ragnote.param.conversation.CreateConversationParam;
import com.sya.ragnote.security.LoginContext;
import com.sya.ragnote.service.conversation.ConversationService;
import com.sya.ragnote.vo.common.PageVo;
import com.sya.ragnote.vo.common.ResultVo;
import com.sya.ragnote.vo.conversation.ConversationTurnVo;
import com.sya.ragnote.vo.conversation.ConversationVo;
import com.sya.ragnote.web.Responses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/conversations")
@Tag(name = "笔记会话")
@RequiredArgsConstructor
public class ConversationController {
    private final ConversationService service;
    private final Responses responses;

    @PostMapping
    @Operation(summary = "创建会话")
    public ResultVo<ConversationVo> create(@Valid @RequestBody CreateConversationParam param) {
        return responses.ok(service.create(LoginContext.userId(), param));
    }

    @GetMapping
    @Operation(summary = "分页查询本人会话")
    public ResultVo<PageVo<ConversationVo>> list(@Valid PageParam page) {
        return responses.ok(service.list(LoginContext.userId(), page));
    }

    @GetMapping("/messages")
    @Operation(summary = "按最近轮次优先查询会话历史")
    public ResultVo<PageVo<ConversationTurnVo>> history(
            @RequestParam("conversationId") UUID id, @Valid PageParam page) {
        return responses.ok(service.history(LoginContext.userId(), id, page));
    }

    @PostMapping("/ask")
    @Operation(summary = "在本人会话中提问")
    public ResultVo<ConversationTurnVo> ask(@Valid @RequestBody ConversationAskParam param) {
        return responses.ok(service.ask(LoginContext.userId(), param));
    }

    @DeleteMapping
    @Operation(summary = "删除本人会话及历史")
    public ResultVo<Void> delete(@RequestParam("conversationId") UUID id) {
        service.delete(LoginContext.userId(), id);
        return responses.ok(null);
    }
}
