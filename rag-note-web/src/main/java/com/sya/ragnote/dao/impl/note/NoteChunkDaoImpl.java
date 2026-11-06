package com.sya.ragnote.dao.impl.note;

import com.sya.ragnote.dao.note.NoteChunkDao;
import com.sya.ragnote.dto.note.NoteEvidence;
import com.sya.ragnote.entity.note.NoteChunk;
import com.sya.ragnote.enums.result.NoteResultEnum;
import com.sya.ragnote.exception.ServiceSystemException;
import com.sya.ragnote.mapper.note.NoteChunkMapper;

import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class NoteChunkDaoImpl implements NoteChunkDao {
    private final NoteChunkMapper mapper;

    public NoteChunkDaoImpl(NoteChunkMapper mapper) {
        this.mapper = mapper;
    }

    public List<NoteEvidence> search(
            long ownerId, List<UUID> ids, float[] vector, String model, int limit, double min) {
        if (ownerId <= 0)
            throw new com.sya.ragnote.exception.ServiceSystemException(
                    com.sya.ragnote.enums.result.AuthResultEnum.NOT_AUTHENTICATED);
        if (ids != null && ids.isEmpty()) return List.of();
        if (vector == null || vector.length != 1024)
            throw new ServiceSystemException(NoteResultEnum.EMBEDDING_CONFIGURATION_MISMATCH);
        StringJoiner value = new StringJoiner(",", "[", "]");
        for (float v : vector) {
            if (!Float.isFinite(v))
                throw new ServiceSystemException(NoteResultEnum.EMBEDDING_CONFIGURATION_MISMATCH);
            value.add(Float.toString(v));
        }
        return mapper.search(
                ownerId, ids, value.toString(), model, Math.min(20, Math.max(1, limit)), min);
    }

    public List<UUID> findOwnedChunkIds(long ownerId, List<UUID> ids) {
        if (ownerId <= 0)
            throw new com.sya.ragnote.exception.ServiceSystemException(
                    com.sya.ragnote.enums.result.AuthResultEnum.NOT_AUTHENTICATED);
        if (ids == null || ids.isEmpty()) return List.of();
        return mapper.findOwnedChunkIds(ownerId, ids);
    }

    public boolean hasIncompatibleModel(long ownerId, List<UUID> ids, String modelId) {
        if (ownerId <= 0)
            throw new com.sya.ragnote.exception.ServiceSystemException(
                    com.sya.ragnote.enums.result.AuthResultEnum.NOT_AUTHENTICATED);
        if (ids != null && ids.isEmpty()) return false;
        return mapper.hasIncompatibleModel(ownerId, ids, modelId);
    }

    public void insertAll(List<NoteChunk> chunks) {
        if (chunks.isEmpty()) return;
        for (NoteChunk chunk : chunks) {
            if (chunk.getOwnerId() <= 0)
                throw new com.sya.ragnote.exception.ServiceSystemException(
                        com.sya.ragnote.enums.result.AuthResultEnum.NOT_AUTHENTICATED);
            String encoded = chunk.getEmbedding();
            if (encoded == null || !encoded.startsWith("[") || !encoded.endsWith("]"))
                throw new ServiceSystemException(NoteResultEnum.EMBEDDING_CONFIGURATION_MISMATCH);
            String[] values = encoded.substring(1, encoded.length() - 1).split(",", -1);
            if (values.length != 1024)
                throw new ServiceSystemException(NoteResultEnum.EMBEDDING_CONFIGURATION_MISMATCH);
            for (String value : values) {
                try {
                    if (!Float.isFinite(Float.parseFloat(value)))
                        throw new ServiceSystemException(
                                NoteResultEnum.EMBEDDING_CONFIGURATION_MISMATCH);
                } catch (NumberFormatException e) {
                    throw new ServiceSystemException(
                            NoteResultEnum.EMBEDDING_CONFIGURATION_MISMATCH, e);
                }
            }
        }
        for (int offset = 0; offset < chunks.size(); offset += 32) {
            var batch = chunks.subList(offset, Math.min(offset + 32, chunks.size()));
            if (mapper.insertAll(batch) != batch.size())
                throw new ServiceSystemException(
                        com.sya.ragnote.enums.result.CommonResultEnum.INTERNAL_ERROR);
        }
    }
}
