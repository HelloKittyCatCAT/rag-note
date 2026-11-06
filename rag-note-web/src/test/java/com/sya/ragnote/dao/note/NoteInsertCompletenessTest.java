package com.sya.ragnote.dao.note;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.sya.ragnote.dao.impl.note.*;
import com.sya.ragnote.entity.note.*;
import com.sya.ragnote.enums.result.CommonResultEnum;
import com.sya.ragnote.exception.ServiceSystemException;
import com.sya.ragnote.mapper.note.*;

import org.junit.jupiter.api.Test;

import java.util.*;

class NoteInsertCompletenessTest {
    @Test
    void missingNoteInsertFailsRatherThanReturningSuccess() {
        var mapper = mock(NoteMapper.class);
        var note = new Note();
        note.setOwnerId(1);
        var error =
                assertThrows(
                        ServiceSystemException.class, () -> new NoteDaoImpl(mapper).insert(note));
        assertSame(CommonResultEnum.INTERNAL_ERROR, error.getResultCode());
    }

    @Test
    void incompleteChunkBatchFails() {
        var mapper = mock(NoteChunkMapper.class);
        when(mapper.insertAll(anyList())).thenReturn(32, 0);
        var error =
                assertThrows(
                        ServiceSystemException.class,
                        () -> new NoteChunkDaoImpl(mapper).insertAll(chunks(33)));
        assertSame(CommonResultEnum.INTERNAL_ERROR, error.getResultCode());
        verify(mapper, times(2)).insertAll(anyList());
    }

    @Test
    void chunksAreWrittenInBatchesOfThirtyTwo() {
        var mapper = mock(NoteChunkMapper.class);
        when(mapper.insertAll(anyList())).thenAnswer(i -> ((List<?>) i.getArgument(0)).size());
        new NoteChunkDaoImpl(mapper).insertAll(chunks(65));
        @SuppressWarnings("unchecked")
        org.mockito.ArgumentCaptor<List<NoteChunk>> captor =
                org.mockito.ArgumentCaptor.forClass(List.class);
        verify(mapper, times(3)).insertAll(captor.capture());
        assertEquals(List.of(32, 32, 1), captor.getAllValues().stream().map(List::size).toList());
    }

    private List<NoteChunk> chunks(int count) {
        var chunks = new ArrayList<NoteChunk>();
        for (int i = 0; i < count; i++) {
            var chunk = new NoteChunk();
            chunk.setOwnerId(1);
            chunk.setEmbedding("[" + String.join(",", Collections.nCopies(1024, "0")) + "]");
            chunks.add(chunk);
        }
        return chunks;
    }
}
