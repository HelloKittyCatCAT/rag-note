package com.sya.ragnote.controller.profile;

import com.sya.ragnote.param.profile.SaveProfileParam;
import com.sya.ragnote.param.profile.ProfileSuggestionParam;
import com.sya.ragnote.vo.profile.ProfileSuggestionVo;
import com.sya.ragnote.security.LoginContext;
import com.sya.ragnote.service.profile.ProfileService;
import com.sya.ragnote.vo.common.ResultVo;
import com.sya.ragnote.vo.profile.ProfileVo;
import com.sya.ragnote.web.Responses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {
    private final ProfileService service;
    private final Responses responses;

    @GetMapping
    public ResultVo<ProfileVo> get() { return responses.ok(service.get(LoginContext.userId())); }

    @PostMapping
    public ResultVo<ProfileVo> save(@Valid @RequestBody SaveProfileParam param) {
        return responses.ok(service.save(LoginContext.userId(), param));
    }

    @PostMapping("/suggestions")
    public ResultVo<ProfileSuggestionVo> suggest(@Valid @RequestBody ProfileSuggestionParam param) {
        return responses.ok(service.suggest(LoginContext.userId(), param.conversationId()));
    }

    @DeleteMapping
    public ResultVo<Void> delete() {
        service.delete(LoginContext.userId());
        return responses.ok(null);
    }
}
