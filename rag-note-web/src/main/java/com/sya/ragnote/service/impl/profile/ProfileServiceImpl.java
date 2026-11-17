package com.sya.ragnote.service.impl.profile;

import com.sya.ragnote.dao.profile.ProfileDao;
import com.sya.ragnote.dao.conversation.ConversationDao;
import com.sya.ragnote.client.chat.ChatModelClient;
import com.sya.ragnote.enums.result.ConversationResultEnum;
import com.sya.ragnote.exception.ServiceSystemException;
import com.sya.ragnote.vo.profile.ProfileSuggestionVo;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import tools.jackson.databind.json.JsonMapper;
import com.sya.ragnote.entity.profile.Profile;
import com.sya.ragnote.param.profile.SaveProfileParam;
import com.sya.ragnote.service.profile.ProfileService;
import com.sya.ragnote.vo.profile.ProfileVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {
    private final ProfileDao dao;
    private final ConversationDao conversations;
    private final ChatModelClient model;
    private final JsonMapper json = JsonMapper.builder().build();

    private record UserStatement(UUID clientMessageId, String question) {}

    @Override
    public ProfileVo get(long ownerId) {
        var profile = dao.find(ownerId);
        return profile == null ? new ProfileVo(false, null, null)
                : new ProfileVo(profile.enabled(), profile.learningGoal(), profile.explanationPreference());
    }

    @Override
    public ProfileVo save(long ownerId, SaveProfileParam param) {
        // 只有用户主动提交的内容进入长期画像，笔记主题不能当作掌握程度。
        dao.save(ownerId, new Profile(param.enabled(), param.learningGoal(), param.explanationPreference()));
        return new ProfileVo(param.enabled(), param.learningGoal(), param.explanationPreference());
    }

    @Override
    public ProfileSuggestionVo suggest(long ownerId, UUID conversationId) {
        if (conversations.find(ownerId, conversationId) == null) {
            throw new ServiceSystemException(ConversationResultEnum.NOT_FOUND);
        }
        var statements = conversations.history(ownerId, conversationId, 0, 4).stream()
                .map(turn -> new UserStatement(turn.clientMessageId(), turn.question())).toList();
        if (statements.isEmpty()) return new ProfileSuggestionVo(List.of());
        // 仅在用户主动请求时提取建议，结果先供用户审阅，保存仍走独立接口。
        String raw = model.generate("""
            从用户原话提取明确陈述的学习目标或讲解偏好。普通提问不视为长期偏好。
            输出 JSON 对象 items 数组，每项包含 kind（LEARNING_GOAL 或 EXPLANATION_PREFERENCE）、
            value、clientMessageId 和用户原话中的连续 quote。没有就返回 {"items":[]}。
            """, json.writeValueAsString(statements));
        if (raw.length() > 8000) return new ProfileSuggestionVo(List.of());
        try {
            var proposed = json.readValue(raw, ProfileSuggestionVo.class);
            if (proposed == null || proposed.items() == null) return new ProfileSuggestionVo(List.of());
            var valid = proposed.items().stream().filter(Objects::nonNull)
                    .filter(item -> item.kind() != null && item.value() != null
                            && !item.value().isBlank() && item.value().length() <= 500
                            && item.quote() != null && !item.quote().isBlank() && item.quote().length() <= 500
                            && statements.stream().anyMatch(source ->
                                    source.clientMessageId().equals(item.clientMessageId())
                                            && source.question().contains(item.quote())))
                    .distinct().limit(4).toList();
            return new ProfileSuggestionVo(valid);
        } catch (tools.jackson.core.JacksonException invalid) {
            return new ProfileSuggestionVo(List.of());
        }
    }

    @Override
    public void delete(long ownerId) { dao.delete(ownerId); }
}
