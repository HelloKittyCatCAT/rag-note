package com.sya.ragnote.controller.note;

import com.sya.ragnote.security.LoginContext;
import com.sya.ragnote.service.note.LearningOverviewService;
import com.sya.ragnote.vo.common.ResultVo;
import com.sya.ragnote.vo.note.LearningOverviewVo;
import com.sya.ragnote.web.Responses;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notes/overview")
@RequiredArgsConstructor
public class LearningOverviewController {
    private final LearningOverviewService service;
    private final Responses responses;

    @GetMapping
    public ResultVo<LearningOverviewVo> overview() {
        return responses.ok(service.overview(LoginContext.userId()));
    }
}
