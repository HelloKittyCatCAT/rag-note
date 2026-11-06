package com.sya.ragnote.mapper.note;

import com.sya.ragnote.dto.note.NoteEvidence;
import com.sya.ragnote.entity.note.NoteChunk;

import org.apache.ibatis.annotations.Param;

import java.util.*;

public interface NoteChunkMapper {
    List<NoteEvidence> search(
            @Param("ownerId") long ownerId,
            @Param("noteIds") List<UUID> noteIds,
            @Param("queryVector") String vector,
            @Param("embeddingModel") String modelId,
            @Param("limit") int limit,
            @Param("minScore") double minScore);

    List<UUID> findOwnedChunkIds(@Param("ownerId") long ownerId, @Param("ids") List<UUID> ids);

    boolean hasIncompatibleModel(
            @Param("ownerId") long ownerId,
            @Param("noteIds") List<UUID> noteIds,
            @Param("embeddingModel") String modelId);

    int insertAll(@Param("chunks") List<NoteChunk> chunks);
}
