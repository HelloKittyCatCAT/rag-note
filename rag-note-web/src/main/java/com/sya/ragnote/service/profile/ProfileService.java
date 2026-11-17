package com.sya.ragnote.service.profile;

import com.sya.ragnote.param.profile.SaveProfileParam;
import com.sya.ragnote.vo.profile.ProfileVo;
import com.sya.ragnote.vo.profile.ProfileSuggestionVo;
import java.util.UUID;

public interface ProfileService {
    ProfileVo get(long ownerId);
    ProfileVo save(long ownerId, SaveProfileParam param);
    ProfileSuggestionVo suggest(long ownerId, UUID conversationId);
    void delete(long ownerId);
}
