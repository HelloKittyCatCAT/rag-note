package com.sya.ragnote.service.impl.auth;

import com.sya.ragnote.dao.user.UserDao;
import com.sya.ragnote.entity.user.User;
import com.sya.ragnote.enums.result.*;
import com.sya.ragnote.exception.ServiceSystemException;
import com.sya.ragnote.param.auth.*;
import com.sya.ragnote.security.*;
import com.sya.ragnote.service.auth.*;
import com.sya.ragnote.vo.user.UserVo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {
    private final UserDao users;
    private final RegistrationService registration;
    private final PasswordEncoder passwords;
    private static final String DUMMY_HASH =
            "$2a$12$R9h/cIPz0gi.URNNX3kh2OPST9/PgBkqquzi.Ss7KIUgO2t0jWMUW";

    public UserVo register(RegisterParam param) {
        if (!PasswordPolicy.valid(param.getPassword())) {
            throw new ServiceSystemException(CommonResultEnum.INVALID_PARAMETER);
        }
        String hash = passwords.encode(param.getPassword());
        try {
            User user =
                    registration.createUser(
                            param.getUsername().toLowerCase(Locale.ROOT),
                            hash,
                            AuthTokens.hash(param.getInviteToken()));
            log.info(
                    "Account created userId={} requestId={}",
                    user.getId(),
                    org.slf4j.MDC.get("requestId"));
            return toVo(user);
        } catch (DataIntegrityViolationException e) {
            throw new ServiceSystemException(AuthResultEnum.REGISTRATION_FAILED, e);
        }
    }

    public LoginUser authenticate(LoginParam param) {
        if (!PasswordPolicy.valid(param.getPassword())) {
            throw new ServiceSystemException(AuthResultEnum.INVALID_CREDENTIALS);
        }
        User user = users.findByUsername(param.getUsername().toLowerCase(Locale.ROOT));
        boolean matched =
                passwords.matches(
                        param.getPassword(), user == null ? DUMMY_HASH : user.getPasswordHash());
        if (user == null || !matched) {
            log.info("Login rejected requestId={}", org.slf4j.MDC.get("requestId"));
            throw new ServiceSystemException(AuthResultEnum.INVALID_CREDENTIALS);
        }
        if (!user.isEnabled()) {
            throw new ServiceSystemException(AuthResultEnum.ACCOUNT_DISABLED);
        }
        log.info(
                "Login accepted userId={} requestId={}",
                user.getId(),
                org.slf4j.MDC.get("requestId"));
        return new LoginUser(user.getId(), user.getUsername(), user.getRole());
    }

    public UserVo me(long userId) {
        User user = users.findById(userId);
        if (user == null) {
            throw new ServiceSystemException(AuthResultEnum.NOT_AUTHENTICATED);
        }
        if (!user.isEnabled()) {
            throw new ServiceSystemException(AuthResultEnum.ACCOUNT_DISABLED);
        }
        return toVo(user);
    }

    public static UserVo toVo(User user) {
        UserVo vo = new UserVo();
        vo.setId(Long.toString(user.getId()));
        vo.setUsername(user.getUsername());
        vo.setRole(user.getRole());
        vo.setEnabled(user.isEnabled());
        return vo;
    }
}
