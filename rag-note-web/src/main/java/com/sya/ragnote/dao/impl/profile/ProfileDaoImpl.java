package com.sya.ragnote.dao.impl.profile;

import com.sya.ragnote.dao.profile.ProfileDao;
import com.sya.ragnote.entity.profile.Profile;
import com.sya.ragnote.mapper.profile.ProfileMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ProfileDaoImpl implements ProfileDao {
    private final ProfileMapper mapper;

    @Override
    public Profile find(long ownerId) { return mapper.find(ownerId); }

    @Override
    public void save(long ownerId, Profile profile) { mapper.save(ownerId, profile); }

    @Override
    public void delete(long ownerId) { mapper.delete(ownerId); }
}
