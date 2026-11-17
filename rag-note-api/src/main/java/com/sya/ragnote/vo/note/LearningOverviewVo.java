package com.sya.ragnote.vo.note;

import java.util.List;
import java.util.UUID;

public record LearningOverviewVo(long totalNotes, int coveredNotes, boolean partial, List<TopicVo> topics) {
    public record TopicVo(String title, List<UUID> noteIds) {}
}
