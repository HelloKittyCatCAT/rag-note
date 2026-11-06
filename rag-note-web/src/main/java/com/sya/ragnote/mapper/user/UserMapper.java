package com.sya.ragnote.mapper.user;

import com.sya.ragnote.entity.user.User;

import org.apache.ibatis.annotations.*;

import java.util.List;

public interface UserMapper {
    @Select("SELECT * FROM app_user WHERE id=#{id}")
    User findById(long id);

    @Select("SELECT * FROM app_user WHERE id=#{id} FOR UPDATE")
    User lockById(long id);

    @Select("SELECT * FROM app_user WHERE username=#{username}")
    User findByUsername(String username);

    @Insert(
            "INSERT INTO app_user(username,password_hash,role,enabled)"
                    + " VALUES(#{username},#{passwordHash},#{role},#{enabled})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    @Select("SELECT count(*) FROM app_user WHERE role='ADMIN'")
    long countAdmins();

    @Select("SELECT count(*) FROM app_user WHERE role='ADMIN' AND enabled=true")
    long countEnabledAdmins();

    @Select("SELECT true FROM pg_advisory_xact_lock(728461991)")
    boolean lockAdminChanges();

    @Update("UPDATE app_user SET enabled=#{enabled} WHERE id=#{id}")
    int updateEnabled(@Param("id") long id, @Param("enabled") boolean enabled);

    @Select("SELECT count(*) FROM app_user")
    long countUsers();

    @Select("SELECT * FROM app_user ORDER BY id ASC LIMIT #{size} OFFSET #{offset}")
    List<User> listUsers(@Param("offset") long offset, @Param("size") int size);

    @Delete("DELETE FROM spring_session WHERE principal_name=#{principal}")
    int revokeSessions(String principal);
}
