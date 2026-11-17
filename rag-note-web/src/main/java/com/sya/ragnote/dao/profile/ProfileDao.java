package com.sya.ragnote.dao.profile;

import com.sya.ragnote.entity.profile.Profile;

public interface ProfileDao {
    Profile find(long ownerId);
    void save(long ownerId, Profile profile);
    void delete(long ownerId);
}
