package com.sya.ragnote.service.note;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.sya.ragnote.config.RagProperties;
import com.sya.ragnote.dao.note.*;
import com.sya.ragnote.dao.user.UserDao;
import com.sya.ragnote.entity.note.Note;
import com.sya.ragnote.entity.user.User;
import com.sya.ragnote.service.impl.note.NoteIndexServiceImpl;

import org.junit.jupiter.api.Test;

import java.util.*;

class NoteIndexServiceTest {
    @Test
    void duplicateWinsEvenWhenQuotaIsFull() {
        var notes = mock(NoteDao.class);
        var chunks = mock(NoteChunkDao.class);
        var users = mock(UserDao.class);
        var properties = mock(RagProperties.class, RETURNS_DEEP_STUBS);
        var user = new User();
        user.setId(1L);
        user.setEnabled(true);
        when(users.lockById(1)).thenReturn(user);
        Note incoming = new Note();
        incoming.setOwnerId(1);
        incoming.setContentSha256("same");
        Note existing = new Note();
        existing.setId(UUID.randomUUID());
        when(notes.findOwnedByHash(1, "same")).thenReturn(existing);
        var service = new NoteIndexServiceImpl(notes, chunks, users, properties);
        assertSame(existing, service.saveIndexedNote(incoming, List.of(), Long.MAX_VALUE));
        verify(notes, never()).countOwned(anyLong());
        verify(notes, never()).insert(any());
        verifyNoInteractions(chunks);
    }
}
