package com.sya.ragnote.dao.impl.note;

import com.sya.ragnote.dao.note.NoteDao;
import com.sya.ragnote.entity.note.Note;
import com.sya.ragnote.entity.note.NoteHeading;
import com.sya.ragnote.enums.result.AuthResultEnum;
import com.sya.ragnote.exception.ServiceSystemException;
import com.sya.ragnote.mapper.note.NoteMapper;

import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class NoteDaoImpl implements NoteDao {
    private final NoteMapper mapper;

    public NoteDaoImpl(NoteMapper mapper) {
        this.mapper = mapper;
    }

    public Note findOwned(long ownerId, UUID id) {
        if (ownerId <= 0) {
            throw new com.sya.ragnote.exception.ServiceSystemException(
                    com.sya.ragnote.enums.result.AuthResultEnum.NOT_AUTHENTICATED);
        }
        return mapper.findOwned(ownerId, id);
    }

    public Note findOwnedByHash(long ownerId, String hash) {
        if (ownerId <= 0) {
            throw new com.sya.ragnote.exception.ServiceSystemException(
                    com.sya.ragnote.enums.result.AuthResultEnum.NOT_AUTHENTICATED);
        }
        return mapper.findOwnedByHash(ownerId, hash);
    }

    public long countOwned(long ownerId) {
        if (ownerId <= 0) {
            throw new com.sya.ragnote.exception.ServiceSystemException(
                    com.sya.ragnote.enums.result.AuthResultEnum.NOT_AUTHENTICATED);
        }
        return mapper.countOwned(ownerId);
    }

    public List<UUID> findOwnedIds(long ownerId, List<UUID> ids) {
        if (ownerId <= 0) {
            throw new com.sya.ragnote.exception.ServiceSystemException(
                    com.sya.ragnote.enums.result.AuthResultEnum.NOT_AUTHENTICATED);
        }
        if (ids == null || ids.isEmpty()) return List.of();
        return mapper.findOwnedIds(ownerId, ids);
    }

    public List<Note> listOwned(long ownerId, long offset, int limit) {
        if (ownerId <= 0) {
            throw new com.sya.ragnote.exception.ServiceSystemException(
                    com.sya.ragnote.enums.result.AuthResultEnum.NOT_AUTHENTICATED);
        }
        return mapper.listOwned(ownerId, offset, limit);
    }

    @Override
    public List<NoteHeading> listHeadings(long ownerId, List<UUID> ids) {
        if (ownerId <= 0) throw new ServiceSystemException(AuthResultEnum.NOT_AUTHENTICATED);
        return ids.isEmpty() ? List.of() : mapper.listHeadings(ownerId, ids);
    }

    public void insert(Note note) {
        if (note.getOwnerId() <= 0) {
            throw new com.sya.ragnote.exception.ServiceSystemException(
                    com.sya.ragnote.enums.result.AuthResultEnum.NOT_AUTHENTICATED);
        }
        if (mapper.insert(note) != 1)
            throw new com.sya.ragnote.exception.ServiceSystemException(
                    com.sya.ragnote.enums.result.CommonResultEnum.INTERNAL_ERROR);
    }

    public int deleteOwned(long ownerId, UUID id) {
        if (ownerId <= 0) {
            throw new com.sya.ragnote.exception.ServiceSystemException(
                    com.sya.ragnote.enums.result.AuthResultEnum.NOT_AUTHENTICATED);
        }
        return mapper.deleteOwned(ownerId, id);
    }
}
