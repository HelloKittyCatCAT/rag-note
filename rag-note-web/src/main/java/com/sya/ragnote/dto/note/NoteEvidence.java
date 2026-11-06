package com.sya.ragnote.dto.note;

import java.util.UUID;

public record NoteEvidence(
        UUID noteId,
        UUID chunkId,
        String title,
        String headingPath,
        int startLine,
        int endLine,
        String content,
        double score) {}
