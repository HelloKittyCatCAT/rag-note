package com.sya.ragnote.mapper.note;

import com.sya.ragnote.entity.note.Note;

import org.apache.ibatis.annotations.Param;

import java.util.*;

public interface NoteMapper {
    Note findOwned(@Param("ownerId") long ownerId, @Param("noteId") UUID noteId);

    Note findOwnedByHash(@Param("ownerId") long ownerId, @Param("hash") String hash);

    long countOwned(@Param("ownerId") long ownerId);

    List<UUID> findOwnedIds(@Param("ownerId") long ownerId, @Param("ids") List<UUID> ids);

    List<Note> listOwned(
            @Param("ownerId") long ownerId,
            @Param("offset") long offset,
            @Param("limit") int limit);

    int insert(Note note);

    int deleteOwned(@Param("ownerId") long ownerId, @Param("noteId") UUID noteId);
}
