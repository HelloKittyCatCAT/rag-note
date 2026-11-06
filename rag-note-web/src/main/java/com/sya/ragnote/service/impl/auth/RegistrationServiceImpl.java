package com.sya.ragnote.service.impl.auth;

import com.sya.ragnote.dao.auth.InvitationDao;
import com.sya.ragnote.dao.user.UserDao;
import com.sya.ragnote.entity.user.User;
import com.sya.ragnote.enums.result.AuthResultEnum;
import com.sya.ragnote.enums.user.UserRole;
import com.sya.ragnote.exception.ServiceSystemException;
import com.sya.ragnote.service.auth.RegistrationService;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class RegistrationServiceImpl implements RegistrationService {
    private final InvitationDao invitations;
    private final UserDao users;

    @Transactional(rollbackFor = Exception.class)
    public User createUser(String username, String passwordHash, String inviteHash) {
        var invitation = invitations.findByHashForUpdate(inviteHash);
        if (invitation == null
                || invitation.getUsedBy() != null
                || !invitation.getExpiresAt().isAfter(Instant.now())) {
            throw new ServiceSystemException(AuthResultEnum.REGISTRATION_FAILED);
        }
        if (users.findByUsername(username) != null) {
            throw new ServiceSystemException(AuthResultEnum.REGISTRATION_FAILED);
        }
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordHash);
        user.setRole(UserRole.USER);
        user.setEnabled(true);
        users.insert(user);
        if (invitations.markUsed(invitation.getId(), user.getId()) != 1) {
            throw new ServiceSystemException(AuthResultEnum.REGISTRATION_FAILED);
        }
        return user;
    }
}
