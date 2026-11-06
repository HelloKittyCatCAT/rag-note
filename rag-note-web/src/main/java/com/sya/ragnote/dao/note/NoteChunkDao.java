package com.sya.ragnote.dao.note;

import com.sya.ragnote.dto.note.NoteEvidence;
import com.sya.ragnote.entity.note.NoteChunk;

import java.util.*;

public interface NoteChunkDao {
    List<NoteEvidence> search(
            long ownerId,
            List<UUID> noteIds,
            float[] vector,
            String modelId,
            int limit,
            double minScore);

    List<UUID> findOwnedChunkIds(long ownerId, List<UUID> chunkIds);

    boolean hasIncompatibleModel(long ownerId, List<UUID> noteIds, String modelId);

    void insertAll(List<NoteChunk> chunks);
}
