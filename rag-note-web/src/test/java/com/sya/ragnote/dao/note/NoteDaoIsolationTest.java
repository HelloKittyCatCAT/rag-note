package com.sya.ragnote.dao.note;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.sya.ragnote.dao.impl.note.*;
import com.sya.ragnote.mapper.note.*;

import org.junit.jupiter.api.Test;

import java.util.List;

class NoteDaoIsolationTest {
    @Test
    void explicitEmptyIdsNeverReachDatabase() {
        var mapper = mock(NoteMapper.class);
        var dao = new NoteDaoImpl(mapper);
        assertTrue(dao.findOwnedIds(1, List.of()).isEmpty());
        verifyNoInteractions(mapper);
    }

    @Test
    void emptySearchRangeNeverBecomesAllNotes() {
        var mapper = mock(NoteChunkMapper.class);
        var dao = new NoteChunkDaoImpl(mapper);
        assertTrue(dao.search(1, List.of(), null, "model", 6, .1).isEmpty());
        verifyNoInteractions(mapper);
    }

    @Test
    void missingOwnerIsRejectedBeforeDatabase() {
        var mapper = mock(NoteMapper.class);
        var dao = new NoteDaoImpl(mapper);
        assertThrows(
                com.sya.ragnote.exception.ServiceSystemException.class, () -> dao.countOwned(0));
        verifyNoInteractions(mapper);
    }

    @Test
    void invalidVectorIsRejectedBeforeDatabase() {
        var mapper = mock(NoteChunkMapper.class);
        var dao = new NoteChunkDaoImpl(mapper);
        assertThrows(
                RuntimeException.class, () -> dao.search(1, null, new float[3], "model", 6, .1));
        verifyNoInteractions(mapper);
    }
}
