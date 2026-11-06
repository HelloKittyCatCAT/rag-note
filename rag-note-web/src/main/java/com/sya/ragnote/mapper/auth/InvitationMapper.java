package com.sya.ragnote.mapper.auth;

import com.sya.ragnote.entity.auth.Invitation;

import org.apache.ibatis.annotations.*;

import java.util.UUID;

public interface InvitationMapper {
    @Select("SELECT * FROM invitation WHERE token_hash=#{hash} FOR UPDATE")
    Invitation findByHashForUpdate(String hash);

    @Insert(
            "INSERT INTO invitation(id,token_hash,created_by,expires_at)"
                    + " VALUES(#{id},#{tokenHash},#{createdBy},#{expiresAt})")
    int insert(Invitation invitation);

    @Update(
            "UPDATE invitation SET used_by=#{userId},used_at=CURRENT_TIMESTAMP WHERE id=#{id} AND"
                    + " used_by IS NULL AND expires_at>CURRENT_TIMESTAMP")
    int markUsed(@Param("id") UUID id, @Param("userId") long userId);
}
