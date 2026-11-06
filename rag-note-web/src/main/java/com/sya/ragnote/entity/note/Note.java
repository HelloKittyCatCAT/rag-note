package com.sya.ragnote.entity.note;

import lombok.Data;

import java.time.Instant;
import java.util.UUID;

@Data
public class Note {
    private UUID id;
    private long ownerId;
    private String title;
    private String originalFilename;
    private int byteSize;
    private String contentSha256;
    private String rawMarkdown;
    private String plainText;
    private String status;
    private int chunkCount;
    private Instant createdAt;
}
