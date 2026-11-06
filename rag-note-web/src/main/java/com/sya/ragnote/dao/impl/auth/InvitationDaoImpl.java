package com.sya.ragnote.dao.impl.auth;

import com.sya.ragnote.dao.auth.InvitationDao;
import com.sya.ragnote.entity.auth.Invitation;
import com.sya.ragnote.mapper.auth.InvitationMapper;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class InvitationDaoImpl implements InvitationDao {
    private final InvitationMapper mapper;

    public Invitation findByHashForUpdate(String hash) {
        return mapper.findByHashForUpdate(hash);
    }

    public void insert(Invitation invitation) {
        mapper.insert(invitation);
    }

    public int markUsed(UUID id, long userId) {
        return mapper.markUsed(id, userId);
    }
}
