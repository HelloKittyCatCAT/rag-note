package com.sya.ragnote.vo.profile;

import java.util.List;
import java.util.UUID;

public record ProfileSuggestionVo(List<ItemVo> items) {
    public enum Kind { LEARNING_GOAL, EXPLANATION_PREFERENCE }
    public record ItemVo(Kind kind, String value, UUID clientMessageId, String quote) {}
}
