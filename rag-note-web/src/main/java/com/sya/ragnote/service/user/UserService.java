package com.sya.ragnote.service.user;

import com.sya.ragnote.param.user.*;
import com.sya.ragnote.vo.auth.InvitationVo;
import com.sya.ragnote.vo.common.PageVo;
import com.sya.ragnote.vo.user.UserVo;

public interface UserService {
    void initializeAdmin(String username, String password);

    InvitationVo createInvitation(long operatorId);

    void updateStatus(long operatorId, UpdateUserStatusParam param);

    PageVo<UserVo> listUsers(long operatorId, QueryUserParam param);
}
