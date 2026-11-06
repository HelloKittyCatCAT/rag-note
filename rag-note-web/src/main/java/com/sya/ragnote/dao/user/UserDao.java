package com.sya.ragnote.dao.user;

import com.sya.ragnote.entity.user.User;

import java.util.List;

public interface UserDao {
    User findById(long id);

    User lockById(long id);

    User findByUsername(String username);

    void insert(User user);

    long countAdmins();

    long countEnabledAdmins();

    void lockAdminChanges();

    int updateEnabled(long id, boolean enabled);

    long countUsers();

    List<User> listUsers(long offset, int size);

    void revokeSessions(long userId);
}
