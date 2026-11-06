package com.sya.ragnote.service.impl.note;

import com.sya.ragnote.config.RagProperties;
import com.sya.ragnote.dao.note.*;
import com.sya.ragnote.dao.user.UserDao;
import com.sya.ragnote.entity.note.*;
import com.sya.ragnote.enums.result.*;
import com.sya.ragnote.exception.ServiceSystemException;
import com.sya.ragnote.service.note.NoteIndexService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class NoteIndexServiceImpl implements NoteIndexService {
    private final NoteDao notes;
    private final NoteChunkDao chunks;
    private final UserDao users;
    private final RagProperties properties;

    public NoteIndexServiceImpl(
            NoteDao notes, NoteChunkDao chunks, UserDao users, RagProperties properties) {
        this.notes = notes;
        this.chunks = chunks;
        this.users = users;
        this.properties = properties;
    }

    @Transactional(rollbackFor = Exception.class)
    public Note saveIndexedNote(Note note, List<NoteChunk> values, long deadline) {
        // 锁定同一用户的写入，再检查去重和配额，防止两个并发上传都通过检查。
        var user = users.lockById(note.getOwnerId());
        if (user == null) throw new ServiceSystemException(AuthResultEnum.NOT_AUTHENTICATED);
        if (!user.isEnabled()) throw new ServiceSystemException(AuthResultEnum.ACCOUNT_DISABLED);
        Note existing = notes.findOwnedByHash(note.getOwnerId(), note.getContentSha256());
        if (existing != null) return existing;
        if (notes.countOwned(note.getOwnerId()) >= properties.getIngestion().getMaxNotes())
            throw new ServiceSystemException(NoteResultEnum.NOTE_QUOTA_EXCEEDED);
        if (System.nanoTime() > deadline || Thread.currentThread().isInterrupted())
            throw new ServiceSystemException(ChatResultEnum.MODEL_TIMEOUT);
        // 主记录和所有分块在同一事务提交，任一步失败都会一起回滚。
        notes.insert(note);
        chunks.insertAll(values);
        if (System.nanoTime() > deadline || Thread.currentThread().isInterrupted())
            throw new ServiceSystemException(ChatResultEnum.MODEL_TIMEOUT);
        return note;
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(long ownerId, UUID id) {
        var user = users.lockById(ownerId);
        if (user == null) throw new ServiceSystemException(AuthResultEnum.NOT_AUTHENTICATED);
        if (!user.isEnabled()) throw new ServiceSystemException(AuthResultEnum.ACCOUNT_DISABLED);
        if (notes.deleteOwned(ownerId, id) == 0)
            throw new ServiceSystemException(NoteResultEnum.NOTE_NOT_FOUND);
    }
}
