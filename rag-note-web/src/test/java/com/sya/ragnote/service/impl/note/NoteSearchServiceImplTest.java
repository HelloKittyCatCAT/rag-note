package com.sya.ragnote.service.impl.note;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.sya.ragnote.client.embedding.EmbeddingClient;
import com.sya.ragnote.config.RagProperties;
import com.sya.ragnote.dao.note.*;
import com.sya.ragnote.dao.user.UserDao;
import com.sya.ragnote.dto.note.*;
import com.sya.ragnote.entity.user.User;
import com.sya.ragnote.tokenizer.TokenCounter;

import org.junit.jupiter.api.Test;

import java.util.*;

class NoteSearchServiceImplTest {
    @Test
    void emptyLibraryNeverEmbeds() {
        var notes = mock(NoteDao.class);
        var chunks = mock(NoteChunkDao.class);
        var users = mock(UserDao.class);
        var embed = mock(EmbeddingClient.class);
        var tokens = mock(TokenCounter.class);
        var user = mock(User.class);
        when(user.isEnabled()).thenReturn(true);
        when(users.findById(1)).thenReturn(user);
        var service =
                new NoteSearchServiceImpl(notes, chunks, users, embed, tokens, new RagProperties());
        var scope = service.validateScope(1, null);
        assertTrue(scope.empty());
        assertEquals(List.of(), service.search(1, new NoteSearchQuery("x", scope, 6, .35)));
        verifyNoInteractions(embed);
    }

    @Test
    void foreignScopeIsRejectedBeforeEmbedding() {
        var notes = mock(NoteDao.class);
        var chunks = mock(NoteChunkDao.class);
        var users = mock(UserDao.class);
        var embed = mock(EmbeddingClient.class);
        var tokens = mock(TokenCounter.class);
        var user = mock(User.class);
        when(user.isEnabled()).thenReturn(true);
        when(users.findById(1)).thenReturn(user);
        var service =
                new NoteSearchServiceImpl(notes, chunks, users, embed, tokens, new RagProperties());
        assertThrows(
                com.sya.ragnote.exception.ServiceSystemException.class,
                () -> service.validateScope(1, List.of(UUID.randomUUID())));
        verifyNoInteractions(embed, chunks);
    }

    @Test
    void disabledUserRejectsEvenEmptyEvidence() {
        var notes = mock(NoteDao.class);
        var chunks = mock(NoteChunkDao.class);
        var users = mock(UserDao.class);
        var embed = mock(EmbeddingClient.class);
        var tokens = mock(TokenCounter.class);
        when(users.findById(1)).thenReturn(mock(User.class));
        var service =
                new NoteSearchServiceImpl(notes, chunks, users, embed, tokens, new RagProperties());
        assertThrows(
                com.sya.ragnote.exception.ServiceSystemException.class,
                () -> service.validateEvidence(1, List.of()));
    }

    @Test
    void missingChunkRejectsAllReturnedEvidence() {
        var notes = mock(NoteDao.class);
        var chunks = mock(NoteChunkDao.class);
        var users = mock(UserDao.class);
        var embed = mock(EmbeddingClient.class);
        var tokens = mock(TokenCounter.class);
        var user = mock(User.class);
        when(user.isEnabled()).thenReturn(true);
        when(users.findById(1)).thenReturn(user);
        var id = UUID.randomUUID();
        when(notes.findOwnedIds(1, List.of(id))).thenReturn(List.of(id));
        var service =
                new NoteSearchServiceImpl(notes, chunks, users, embed, tokens, new RagProperties());
        var error =
                assertThrows(
                        com.sya.ragnote.exception.ServiceSystemException.class,
                        () ->
                                service.validateEvidence(
                                        1,
                                        List.of(
                                                new NoteEvidence(
                                                        id,
                                                        UUID.randomUUID(),
                                                        "t",
                                                        "",
                                                        1,
                                                        1,
                                                        "x",
                                                        .8))));
        assertEquals(
                com.sya.ragnote.enums.result.ChatResultEnum.SOURCE_CHANGED, error.getResultCode());
    }

    @Test
    void incompatibleEmbeddingModelFailsBeforeEmbedding() {
        var notes = mock(NoteDao.class);
        var chunks = mock(NoteChunkDao.class);
        var users = mock(UserDao.class);
        var embed = mock(EmbeddingClient.class);
        var tokens = mock(TokenCounter.class);
        when(embed.modelId()).thenReturn("new-model");
        when(chunks.hasIncompatibleModel(1, null, "new-model")).thenReturn(true);
        var service =
                new NoteSearchServiceImpl(notes, chunks, users, embed, tokens, new RagProperties());
        var error =
                assertThrows(
                        com.sya.ragnote.exception.ServiceSystemException.class,
                        () ->
                                service.search(
                                        1,
                                        new NoteSearchQuery(
                                                "x",
                                                new NoteScope(List.of(), true, false),
                                                6,
                                                .35)));
        assertEquals(
                com.sya.ragnote.enums.result.NoteResultEnum.EMBEDDING_CONFIGURATION_MISMATCH,
                error.getResultCode());
        verify(embed, never()).embed(anyString());
    }

    @Test
    void duplicateBodiesCollapseButDifferentSameLineBodiesRemain() {
        var notes = mock(NoteDao.class);
        var chunks = mock(NoteChunkDao.class);
        var users = mock(UserDao.class);
        var embed = mock(EmbeddingClient.class);
        var tokens = mock(TokenCounter.class);
        var note = UUID.randomUUID();
        var a = new NoteEvidence(note, UUID.randomUUID(), "t", "", 1, 1, "abcdef", .9);
        var b = new NoteEvidence(note, UUID.randomUUID(), "t", "", 1, 1, "defghi", .8);
        var c = new NoteEvidence(note, UUID.randomUUID(), "t", "", 1, 1, "xyz", .7);
        when(embed.embed("x")).thenReturn(new float[] {1});
        when(embed.modelId()).thenReturn("m");
        when(chunks.search(eq(1L), isNull(), any(), eq("m"), eq(18), eq(.35)))
                .thenReturn(List.of(a, b, c));
        var service =
                new NoteSearchServiceImpl(notes, chunks, users, embed, tokens, new RagProperties());
        assertEquals(
                List.of(a, c),
                service.search(
                        1,
                        new NoteSearchQuery("x", new NoteScope(List.of(), true, false), 6, .35)));
    }
}
