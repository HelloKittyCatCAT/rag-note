package com.sya.ragnote.dao.auth;

import com.sya.ragnote.entity.auth.Invitation;

import java.util.UUID;

public interface InvitationDao {
    Invitation findByHashForUpdate(String hash);

    void insert(Invitation invitation);

    int markUsed(UUID id, long userId);
}
