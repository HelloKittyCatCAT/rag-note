package com.sya.ragnote.service.auth;

import com.sya.ragnote.entity.user.User;

public interface RegistrationService {
    User createUser(String username, String passwordHash, String inviteHash);
}
