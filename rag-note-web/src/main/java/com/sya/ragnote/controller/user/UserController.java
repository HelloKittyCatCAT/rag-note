package com.sya.ragnote.controller.user;

import com.sya.ragnote.param.user.*;
import com.sya.ragnote.security.LoginContext;
import com.sya.ragnote.service.user.UserService;
import com.sya.ragnote.vo.auth.InvitationVo;
import com.sya.ragnote.vo.common.*;
import com.sya.ragnote.vo.user.UserVo;
import com.sya.ragnote.web.Responses;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "用户管理")
public class UserController {
    private final UserService users;
    private final Responses responses;

    @PostMapping("/invitations")
    @Operation(summary = "创建邀请码")
    public ResultVo<InvitationVo> invitation() {
        return responses.ok(users.createInvitation(LoginContext.userId()));
    }

    @GetMapping("/users")
    @Operation(summary = "用户列表")
    public ResultVo<PageVo<UserVo>> list(@Valid QueryUserParam param) {
        return responses.ok(users.listUsers(LoginContext.userId(), param));
    }

    @PostMapping("/users/status")
    @Operation(summary = "修改用户状态")
    public ResultVo<Void> status(@Valid @RequestBody UpdateUserStatusParam param) {
        users.updateStatus(LoginContext.userId(), param);
        return responses.ok(null);
    }
}
