package com.sya.ragnote.mapper.note;

import com.sya.ragnote.entity.note.Note;
import com.sya.ragnote.entity.note.NoteHeading;
import org.apache.ibatis.annotations.Select;

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

    @Select("""
        <script>
        SELECT note_id, heading_path FROM (
            SELECT note_id, heading_path, row_number() OVER
                (PARTITION BY note_id ORDER BY first_index, heading_path) AS position
            FROM (
                SELECT note_id, heading_path, min(chunk_index) AS first_index
                FROM note_chunk WHERE owner_id=#{ownerId} AND heading_path != ''
                AND note_id IN
                <foreach collection="ids" item="id" open="(" separator="," close=")">#{id}</foreach>
                GROUP BY note_id, heading_path
            ) grouped
        ) ranked
        WHERE position &lt;= 8 ORDER BY note_id,position
        </script>
        """)
    List<NoteHeading> listHeadings(@Param("ownerId") long ownerId, @Param("ids") List<UUID> ids);

    int insert(Note note);

    int deleteOwned(@Param("ownerId") long ownerId, @Param("noteId") UUID noteId);
}
