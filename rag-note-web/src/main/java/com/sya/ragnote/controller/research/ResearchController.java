package com.sya.ragnote.controller.research;

import com.sya.ragnote.param.research.ResearchQuestionParam;
import com.sya.ragnote.security.LoginContext;
import com.sya.ragnote.service.research.ResearchService;
import com.sya.ragnote.vo.common.ResultVo;
import com.sya.ragnote.vo.research.ResearchAnswerVo;
import com.sya.ragnote.web.Responses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/research")
@RequiredArgsConstructor
public class ResearchController {
    private final ResearchService service;
    private final Responses responses;

    @PostMapping("/ask")
    public ResultVo<ResearchAnswerVo> ask(@Valid @RequestBody ResearchQuestionParam param) {
        return responses.ok(service.ask(LoginContext.userId(), param));
    }
}
