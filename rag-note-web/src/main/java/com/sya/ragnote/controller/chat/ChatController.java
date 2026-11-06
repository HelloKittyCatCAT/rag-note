package com.sya.ragnote.controller.chat;

import com.sya.ragnote.param.chat.AskQuestionParam;
import com.sya.ragnote.security.LoginContext;
import com.sya.ragnote.service.chat.ChatService;
import com.sya.ragnote.vo.chat.AskQuestionVo;
import com.sya.ragnote.vo.common.ResultVo;
import com.sya.ragnote.web.Responses;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
@Tag(name = "笔记问答")
public class ChatController {
    private final ChatService service;
    private final Responses responses;

    public ChatController(ChatService service, Responses responses) {
        this.service = service;
        this.responses = responses;
    }

    @PostMapping("/ask")
    @Operation(summary = "仅依据本人笔记进行单轮问答")
    public ResultVo<AskQuestionVo> ask(@Valid @RequestBody AskQuestionParam param) {
        return responses.ok(service.ask(LoginContext.userId(), param));
    }
}
