package com.sya.ragnote.service.note;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.sya.ragnote.client.embedding.EmbeddingClient;
import com.sya.ragnote.config.RagProperties;
import com.sya.ragnote.converter.note.NoteConverter;
import com.sya.ragnote.dao.note.NoteDao;
import com.sya.ragnote.dao.user.UserDao;
import com.sya.ragnote.entity.note.Note;
import com.sya.ragnote.entity.user.User;
import com.sya.ragnote.enums.result.*;
import com.sya.ragnote.exception.ServiceSystemException;
import com.sya.ragnote.param.note.UploadNoteParam;
import com.sya.ragnote.parser.MarkdownParser;
import com.sya.ragnote.service.impl.note.NoteServiceImpl;
import com.sya.ragnote.splitter.MarkdownSplitter;
import com.sya.ragnote.tokenizer.TokenCounter;

import org.junit.jupiter.api.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

class NoteUploadTest {
    private NoteDao notes;
    private UserDao users;
    private EmbeddingClient embedding;
    private NoteIndexService index;
    private NoteServiceImpl service;

    @BeforeEach
    void setup() {
        notes = mock(NoteDao.class);
        users = mock(UserDao.class);
        embedding = mock(EmbeddingClient.class);
        index = mock(NoteIndexService.class);
        var properties = new RagProperties();
        var counter = mock(TokenCounter.class);
        when(counter.count(anyString()))
                .thenAnswer(
                        i ->
                                ((String) i.getArgument(0))
                                        .codePointCount(0, ((String) i.getArgument(0)).length()));
        var user = new User();
        user.setId(1L);
        user.setEnabled(true);
        when(users.findById(1)).thenReturn(user);
        service =
                new NoteServiceImpl(
                        notes,
                        users,
                        index,
                        new MarkdownParser(),
                        new MarkdownSplitter(counter, properties),
                        embedding,
                        new NoteConverter(),
                        properties);
    }

    @AfterEach
    void close() {
        service.shutdown();
    }

    @Test
    void originalBytesDuplicateAvoidsModelAndQuota() {
        var existing = new Note();
        existing.setId(UUID.randomUUID());
        when(notes.findOwnedByHash(eq(1L), anyString())).thenReturn(existing);
        var vo = service.upload(1, new UploadNoteParam(), input("# Same"), "same.md");
        assertEquals(existing.getId(), vo.getNoteId());
        assertTrue(vo.isDuplicated());
        verifyNoInteractions(embedding, index);
    }

    @Test
    void invalidUtf8IsRejectedBeforeModel() {
        var error =
                assertThrows(
                        ServiceSystemException.class,
                        () ->
                                service.upload(
                                        1,
                                        new UploadNoteParam(),
                                        new ByteArrayInputStream(new byte[] {(byte) 0xc3, 0x28}),
                                        "bad.md"));
        assertSame(NoteResultEnum.INVALID_FILE_ENCODING, error.getResultCode());
        verifyNoInteractions(embedding, index);
    }

    @Test
    void nulIsParameterError() {
        var error =
                assertThrows(
                        ServiceSystemException.class,
                        () ->
                                service.upload(
                                        1, new UploadNoteParam(), input("text\0tail"), "bad.md"));
        assertSame(CommonResultEnum.INVALID_PARAMETER, error.getResultCode());
        verifyNoInteractions(embedding, index);
    }

    @Test
    void emptyParsedHtmlIsRejected() {
        var error =
                assertThrows(
                        ServiceSystemException.class,
                        () ->
                                service.upload(
                                        1,
                                        new UploadNoteParam(),
                                        input("<script>alert('x')</script>"),
                                        "empty.md"));
        assertSame(NoteResultEnum.EMPTY_NOTE_CONTENT, error.getResultCode());
        verifyNoInteractions(embedding, index);
    }

    @Test
    void bomIsAllowedAndFilenameNeverBecomesPath() {
        when(embedding.embedAll(anyList())).thenReturn(List.of(new float[1024]));
        when(embedding.modelId()).thenReturn("fixture");
        when(index.saveIndexedNote(any(), anyList(), anyLong()))
                .thenAnswer(
                        i -> {
                            Note saved = i.getArgument(0);
                            assertTrue(saved.getRawMarkdown().startsWith("\uFEFF"));
                            return saved;
                        });
        var vo =
                service.upload(
                        1, new UploadNoteParam(), input("\uFEFF# title\nbody"), "../../note.md");
        assertEquals("title", vo.getTitle());
        assertEquals("note.md", vo.getOriginalFilename());
        assertFalse(vo.isDuplicated());
    }

    private InputStream input(String s) {
        return new ByteArrayInputStream(s.getBytes(StandardCharsets.UTF_8));
    }
}
