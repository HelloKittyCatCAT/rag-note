package com.sya.ragnote.service.impl.note;

import com.sya.ragnote.dao.note.NoteDao;
import com.sya.ragnote.entity.note.Note;
import com.sya.ragnote.entity.note.NoteHeading;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LearningOverviewServiceImplTest {
    @Test
    void reportsActualCoverageAndUsesChapterSources() {
        var dao = mock(NoteDao.class);
        var note = new Note();
        UUID id = UUID.randomUUID();
        note.setId(id);
        note.setTitle("读书笔记");
        when(dao.countOwned(7)).thenReturn(2L);
        when(dao.listOwned(7, 0, 50)).thenReturn(List.of(note));
        when(dao.listHeadings(7, List.of(id))).thenReturn(List.of(new NoteHeading(id, "事务隔离")));

        var overview = new LearningOverviewServiceImpl(dao).overview(7);

        assertEquals(2, overview.totalNotes());
        assertEquals(1, overview.coveredNotes());
        assertTrue(overview.partial());
        assertTrue(overview.topics().stream().anyMatch(
                topic -> topic.title().equals("事务隔离") && topic.noteIds().equals(List.of(id))));
    }
}
