package com.sya.ragnote.security;

import com.sya.ragnote.enums.result.AuthResultEnum;
import com.sya.ragnote.exception.ServiceSystemException;

import org.springframework.security.core.context.SecurityContextHolder;

public final class LoginContext {
    private LoginContext() {}

    public static long userId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null
                || !auth.isAuthenticated()
                || !(auth.getPrincipal() instanceof LoginUser user)) {
            throw new ServiceSystemException(AuthResultEnum.NOT_AUTHENTICATED);
        }
        return user.id();
    }
}
