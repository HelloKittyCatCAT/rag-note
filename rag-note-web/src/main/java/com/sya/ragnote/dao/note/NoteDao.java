package com.sya.ragnote.dao.note;

import com.sya.ragnote.entity.note.Note;
import com.sya.ragnote.entity.note.NoteHeading;

import java.util.*;

public interface NoteDao {
    Note findOwned(long ownerId, UUID noteId);

    Note findOwnedByHash(long ownerId, String hash);

    long countOwned(long ownerId);

    List<UUID> findOwnedIds(long ownerId, List<UUID> ids);

    List<NoteHeading> listHeadings(long ownerId, List<UUID> ids);

    List<Note> listOwned(long ownerId, long offset, int limit);

    void insert(Note note);

    int deleteOwned(long ownerId, UUID noteId);
}
