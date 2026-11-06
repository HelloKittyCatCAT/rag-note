package com.sya.ragnote.service.auth;

import com.sya.ragnote.param.auth.*;
import com.sya.ragnote.security.LoginUser;
import com.sya.ragnote.vo.user.UserVo;

public interface AuthService {
    UserVo register(RegisterParam param);

    LoginUser authenticate(LoginParam param);

    UserVo me(long userId);
}
