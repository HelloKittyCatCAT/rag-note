package com.sya.ragnote.service.impl.user;

import com.sya.ragnote.dao.auth.InvitationDao;
import com.sya.ragnote.dao.user.UserDao;
import com.sya.ragnote.entity.auth.Invitation;
import com.sya.ragnote.entity.user.User;
import com.sya.ragnote.enums.result.*;
import com.sya.ragnote.enums.user.UserRole;
import com.sya.ragnote.exception.ServiceSystemException;
import com.sya.ragnote.param.user.*;
import com.sya.ragnote.security.*;
import com.sya.ragnote.service.impl.auth.AuthServiceImpl;
import com.sya.ragnote.service.user.UserService;
import com.sya.ragnote.vo.auth.InvitationVo;
import com.sya.ragnote.vo.common.PageVo;
import com.sya.ragnote.vo.user.UserVo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {
    private final UserDao users;
    private final InvitationDao invitations;
    private final PasswordEncoder passwords;
    private final SecureRandom random = new SecureRandom();

    @Transactional(rollbackFor = Exception.class)
    public void initializeAdmin(String username, String password) {
        users.lockAdminChanges();
        if (users.countAdmins() > 0) {
            return;
        }
        if (username == null
                || !username.matches("[a-zA-Z0-9_]{3,32}")
                || !PasswordPolicy.valid(password)) {
            throw new IllegalStateException(
                    "Initial administrator credentials are required and must satisfy the account"
                            + " policy");
        }
        User user = new User();
        user.setUsername(username.toLowerCase(Locale.ROOT));
        user.setPasswordHash(passwords.encode(password));
        user.setRole(UserRole.ADMIN);
        user.setEnabled(true);
        users.insert(user);
        log.info("Initial administrator created userId={}", user.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public InvitationVo createInvitation(long operatorId) {
        checkAdmin(operatorId);
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Invitation invitation = new Invitation();
        invitation.setId(UUID.randomUUID());
        invitation.setTokenHash(AuthTokens.hash(token));
        invitation.setCreatedBy(operatorId);
        invitation.setExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));
        invitations.insert(invitation);
        InvitationVo vo = new InvitationVo();
        vo.setToken(token);
        vo.setExpiresAt(invitation.getExpiresAt());
        log.info(
                "Invitation created operatorId={} invitationId={} requestId={}",
                operatorId,
                invitation.getId(),
                org.slf4j.MDC.get("requestId"));
        return vo;
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(long operatorId, UpdateUserStatusParam param) {
        users.lockAdminChanges();
        checkAdmin(operatorId);
        long targetId = Long.parseLong(param.getUserId());
        User target = users.lockById(targetId);
        if (target == null) {
            throw new ServiceSystemException(UserResultEnum.USER_NOT_FOUND);
        }
        if (!param.getEnabled()
                && (targetId == operatorId
                        || (target.getRole() == UserRole.ADMIN
                                && target.isEnabled()
                                && users.countEnabledAdmins() <= 1))) {
            throw new ServiceSystemException(UserResultEnum.ADMIN_DISABLE_NOT_ALLOWED);
        }
        users.updateEnabled(targetId, param.getEnabled());
        if (!param.getEnabled()) {
            users.revokeSessions(targetId);
        }
        log.info(
                "User status updated operatorId={} userId={} enabled={} requestId={}",
                operatorId,
                targetId,
                param.getEnabled(),
                org.slf4j.MDC.get("requestId"));
    }

    public PageVo<UserVo> listUsers(long operatorId, QueryUserParam param) {
        checkAdmin(operatorId);
        return new PageVo<>(
                users
                        .listUsers(((long) param.getPage() - 1) * param.getSize(), param.getSize())
                        .stream()
                        .map(AuthServiceImpl::toVo)
                        .toList(),
                users.countUsers(),
                param.getPage(),
                param.getSize());
    }

    private void checkAdmin(long operatorId) {
        User user = users.findById(operatorId);
        if (user == null) {
            throw new ServiceSystemException(AuthResultEnum.NOT_AUTHENTICATED);
        }
        if (!user.isEnabled()) {
            throw new ServiceSystemException(AuthResultEnum.ACCOUNT_DISABLED);
        }
        if (user.getRole() != UserRole.ADMIN) {
            throw new ServiceSystemException(AuthResultEnum.PERMISSION_DENIED);
        }
    }
}
