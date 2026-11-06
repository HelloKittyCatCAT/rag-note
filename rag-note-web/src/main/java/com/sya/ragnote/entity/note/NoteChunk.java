package com.sya.ragnote.entity.note;

import lombok.Data;

import java.util.UUID;

@Data
public class NoteChunk {
    private UUID id;
    private UUID noteId;
    private long ownerId;
    private int chunkIndex;
    private String headingPath;
    private int startLine;
    private int endLine;
    private String content;
    private int tokenCount;
    private String embeddingModel;
    private String embedding;
}
