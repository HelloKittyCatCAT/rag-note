package com.sya.ragnote.controller.auth;

import com.sya.ragnote.param.auth.*;
import com.sya.ragnote.security.*;
import com.sya.ragnote.service.auth.AuthService;
import com.sya.ragnote.vo.auth.CsrfVo;
import com.sya.ragnote.vo.common.ResultVo;
import com.sya.ragnote.vo.user.UserVo;
import com.sya.ragnote.web.Responses;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.servlet.http.*;
import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "认证")
public class AuthController {
    private final AuthService auth;
    private final Responses responses;
    private final SessionAuthenticationStrategy sessions;
    private final SecurityContextRepository contexts;

    @GetMapping("/csrf")
    @Operation(summary = "获取CSRF令牌")
    public ResultVo<CsrfVo> csrf(CsrfToken token) {
        CsrfVo vo = new CsrfVo();
        vo.setToken(token.getToken());
        vo.setHeaderName(token.getHeaderName());
        return responses.ok(vo);
    }

    @PostMapping("/register")
    @Operation(summary = "邀请码注册")
    public ResultVo<UserVo> register(@Valid @RequestBody RegisterParam param) {
        return responses.ok(auth.register(param));
    }

    @PostMapping("/login")
    @Operation(summary = "登录")
    public ResultVo<UserVo> login(
            @Valid @RequestBody LoginParam param,
            HttpServletRequest request,
            HttpServletResponse response) {
        LoginUser user = auth.authenticate(param);
        var authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        user,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + user.role().name())));
        sessions.onAuthentication(authentication, request, response);
        request.getSession(true).setMaxInactiveInterval(1800);
        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        contexts.saveContext(context, request, response);
        return responses.ok(auth.me(user.id()));
    }

    @GetMapping("/me")
    @Operation(summary = "当前用户")
    public ResultVo<UserVo> me() {
        return responses.ok(auth.me(LoginContext.userId()));
    }
}
