package com.sya.ragnote.service.impl.conversation;

import com.sya.ragnote.dao.conversation.ConversationDao;
import com.sya.ragnote.dto.chat.ChatMemory;
import com.sya.ragnote.dto.chat.PreviousTurn;
import com.sya.ragnote.entity.conversation.ConversationTurn;
import com.sya.ragnote.enums.result.ConversationResultEnum;
import com.sya.ragnote.exception.ServiceSystemException;
import com.sya.ragnote.service.conversation.ConversationContextService;
import com.sya.ragnote.service.profile.ProfileService;
import com.sya.ragnote.vo.chat.AskQuestionVo;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

@Service
@RequiredArgsConstructor
public class ConversationContextServiceImpl implements ConversationContextService {
    private static final int RECENT_TURNS = 6;
    private static final int SUMMARY_TURNS = 12;
    private final ConversationDao conversations;
    private final ProfileService profiles;
    private final JsonMapper json = JsonMapper.builder().build();

    @Override
    public ChatMemory load(long ownerId, UUID id, List<UUID> noteIds) {
        var profile = profiles.get(ownerId);
        String preference = profile.enabled()
                ? Stream.of(profile.learningGoal(), profile.explanationPreference())
                        .filter(Objects::nonNull).collect(Collectors.joining("\n"))
                : "";
        if (id == null) return new ChatMemory(List.of(), "", preference);
        if (conversations.find(ownerId, id) == null) {
            throw new ServiceSystemException(ConversationResultEnum.NOT_FOUND);
        }

        // DAO 已排除来源被删除的轮次。指定笔记时，进一步约束历史回答的来源范围。
        var recent = new ArrayList<PreviousTurn>();
        for (var turn : conversations.history(ownerId, id, 0, RECENT_TURNS)) {
            var answer = answer(turn);
            if (noteIds == null || (!answer.getSources().isEmpty()
                    && answer.getSources().stream().allMatch(s -> noteIds.contains(s.getNoteId())))) {
                recent.add(new PreviousTurn(turn.question(), answer.getAnswer()));
            }
        }
        Collections.reverse(recent);

        // 摘要从仍有效的旧轮次即时提取，避免独立缓存保留失效来源。
        String recap = noteIds == null ? conversations.history(ownerId, id, RECENT_TURNS, SUMMARY_TURNS)
                .stream().map(turn -> excerpt(turn.question()) + " → " + excerpt(answer(turn).getAnswer()))
                .collect(Collectors.joining("\n")) : "";
        return new ChatMemory(recent, recap, preference);
    }

    private AskQuestionVo answer(ConversationTurn turn) {
        return json.readValue(turn.answerJson(), AskQuestionVo.class);
    }

    private static String excerpt(String text) {
        int length = Math.min(100, text.codePointCount(0, text.length()));
        return text.substring(0, text.offsetByCodePoints(0, length));
    }
}
