package com.sya.ragnote.service.note;

import com.sya.ragnote.entity.note.*;

import java.util.List;

public interface NoteIndexService {
    Note saveIndexedNote(Note note, List<NoteChunk> chunks, long deadline);

    void delete(long ownerId, java.util.UUID noteId);
}
