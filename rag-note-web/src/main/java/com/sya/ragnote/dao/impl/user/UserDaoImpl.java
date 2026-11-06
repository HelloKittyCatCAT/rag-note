package com.sya.ragnote.dao.impl.user;

import com.sya.ragnote.dao.user.UserDao;
import com.sya.ragnote.entity.user.User;
import com.sya.ragnote.mapper.user.UserMapper;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class UserDaoImpl implements UserDao {
    private final UserMapper mapper;

    public User findById(long id) {
        return mapper.findById(id);
    }

    public User lockById(long id) {
        return mapper.lockById(id);
    }

    public User findByUsername(String username) {
        return mapper.findByUsername(username);
    }

    public void insert(User user) {
        mapper.insert(user);
    }

    public long countAdmins() {
        return mapper.countAdmins();
    }

    public long countEnabledAdmins() {
        return mapper.countEnabledAdmins();
    }

    public void lockAdminChanges() {
        mapper.lockAdminChanges();
    }

    public int updateEnabled(long id, boolean enabled) {
        return mapper.updateEnabled(id, enabled);
    }

    public long countUsers() {
        return mapper.countUsers();
    }

    public List<User> listUsers(long offset, int size) {
        return mapper.listUsers(offset, size);
    }

    public void revokeSessions(long userId) {
        mapper.revokeSessions(Long.toString(userId));
    }
}
