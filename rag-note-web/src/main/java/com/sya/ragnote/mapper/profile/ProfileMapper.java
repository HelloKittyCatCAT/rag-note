package com.sya.ragnote.mapper.profile;

import com.sya.ragnote.entity.profile.Profile;
import org.apache.ibatis.annotations.*;

public interface ProfileMapper {
    @Select("SELECT enabled,learning_goal,explanation_preference FROM learning_profile WHERE owner_id=#{ownerId}")
    Profile find(@Param("ownerId") long ownerId);

    @Insert("""
        INSERT INTO learning_profile(owner_id,enabled,learning_goal,explanation_preference)
        VALUES(#{ownerId},#{profile.enabled},#{profile.learningGoal},#{profile.explanationPreference})
        ON CONFLICT(owner_id) DO UPDATE SET enabled=excluded.enabled,
            learning_goal=excluded.learning_goal, explanation_preference=excluded.explanation_preference
        """)
    int save(@Param("ownerId") long ownerId, @Param("profile") Profile profile);

    @Delete("DELETE FROM learning_profile WHERE owner_id=#{ownerId}")
    int delete(@Param("ownerId") long ownerId);
}
