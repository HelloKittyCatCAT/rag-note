package com.sya.ragnote.service.impl.chat;

import com.sya.ragnote.dto.chat.*;
import com.sya.ragnote.dto.note.NoteEvidence;
import com.sya.ragnote.enums.chat.AnswerStatus;
import com.sya.ragnote.service.chat.AnswerValidationService;
import com.sya.ragnote.service.note.NoteSearchService;

import org.springframework.stereotype.Service;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class AnswerValidationServiceImpl implements AnswerValidationService {
    private static final String FALLBACK = "无法生成通过引用校验的回答，请查看检索到的相关片段。";
    private static final String INSUFFICIENT = "现有笔记片段不足以回答该问题，以下仅为检索到的相关片段。";
    private static final Pattern CITATION = Pattern.compile("\\[S([^\\]\\r\\n]+)\\]");
    private final NoteSearchService search;
    private final JsonMapper mapper =
            JsonMapper.builder().enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).build();

    public AnswerValidationServiceImpl(NoteSearchService search) {
        this.search = search;
    }

    @Override
    public ValidatedAnswer validate(long ownerId, List<NoteEvidence> evidence, String raw) {
        // 正常答案和降级结果都会带来源，先统一复查这些片段的访问权限。
        search.validateEvidence(ownerId, evidence);
        var fallback = new ValidatedAnswer(FALLBACK, AnswerStatus.EVIDENCE_ONLY, evidence);
        if (raw == null
                || raw.length() > 65536
                || raw.getBytes(StandardCharsets.UTF_8).length > 65536) {
            return fallback;
        }
        String json = raw.strip().replace("\r\n", "\n");
        if (json.startsWith("```json\n") && json.endsWith("\n```")) {
            json = json.substring(8, json.length() - 4).strip();
        } else if (json.startsWith("```\n") && json.endsWith("\n```")) {
            json = json.substring(4, json.length() - 4).strip();
        }
        try {
            var tree = mapper.readTree(json);
            if (!tree.isObject()
                    || tree.size() != 3
                    || !tree.has("answer")
                    || !tree.has("citedSourceIds")
                    || !tree.has("insufficientEvidence")) {
                return fallback;
            }
            var answer = tree.get("answer");
            var cited = tree.get("citedSourceIds");
            var insufficient = tree.get("insufficientEvidence");
            if (!answer.isString()
                    || answer.asString().length() > 8000
                    || !cited.isArray()
                    || cited.size() > 6
                    || !insufficient.isBoolean()) {
                return fallback;
            }
            var ids = new LinkedHashSet<String>();
            for (var item : cited) {
                if (!item.isString() || !ids.add(item.asString())) {
                    return fallback;
                }
            }
            if (insufficient.asBoolean()) {
                return new ValidatedAnswer(INSUFFICIENT, AnswerStatus.NO_EVIDENCE, evidence);
            }
            var validIds = new HashSet<String>();
            for (int i = 1; i <= evidence.size(); i++) {
                validIds.add("S" + i);
            }
            if (ids.isEmpty() || !validIds.containsAll(ids) || answer.asString().isBlank()) {
                return fallback;
            }
            // 正文里的 [S1] 等标记必须与模型单独返回的引用列表一致。
            var markers = new LinkedHashSet<String>();
            var matcher = CITATION.matcher(answer.asString());
            while (matcher.find()) {
                markers.add("S" + matcher.group(1));
            }
            if (!ids.equals(markers)) {
                return fallback;
            }
            return new ValidatedAnswer(answer.asString(), AnswerStatus.ANSWERED, evidence);
        } catch (RuntimeException ignored) {
            return fallback;
        }
    }
}
