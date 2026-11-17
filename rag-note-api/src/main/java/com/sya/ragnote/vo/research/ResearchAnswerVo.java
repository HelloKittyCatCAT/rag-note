package com.sya.ragnote.vo.research;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ResearchAnswerVo(Status status, String answer, List<SourceVo> sources,
        List<FindingVo> findings, List<String> limitations, List<TraceVo> trace) {
    public enum Status { COMPLETED, PARTIAL, INSUFFICIENT }
    public enum Assessment { SUPPORTED, CONTRADICTED, UNKNOWN }
    public enum EvidenceType { NOTE, WEB, MIXED }

    public record SourceVo(String id, String title, String snippet, UUID noteId, UUID chunkId,
            String headingPath, Integer startLine, Integer endLine, String url, Instant fetchedAt) {}

    public record FindingVo(String statement, EvidenceType evidenceType, Assessment assessment,
            List<String> sourceIds, String explanation) {}

    public record TraceVo(String tool, String status, long elapsedMillis, List<String> sourceIds) {}
}
