package com.sya.ragnote.service.impl.conversation;

import com.sya.ragnote.dao.conversation.ConversationDao;
import com.sya.ragnote.entity.conversation.ConversationTurn;
import com.sya.ragnote.enums.result.ChatResultEnum;
import com.sya.ragnote.enums.result.ConversationResultEnum;
import com.sya.ragnote.exception.ServiceSystemException;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConversationTurnServiceImplTest {
    private final ConversationDao dao = mock(ConversationDao.class);
    private final ConversationTurnServiceImpl turns = new ConversationTurnServiceImpl(dao);
    private final UUID id = UUID.randomUUID();
    private final UUID messageId = UUID.randomUUID();

    @Test
    void reusesCompletedTurnWithSameRequest() {
        when(dao.lock(7, id)).thenReturn(id);
        var completed = new ConversationTurn(messageId, "问题", "hash", "{}", true);
        when(dao.findTurn(7, id, messageId)).thenReturn(completed);

        assertSame(completed, turns.claim(7, id, messageId, "问题", "hash").completed());
        verify(dao, never()).expire(7, id);
    }

    @Test
    void rejectsMessageIdReuseAndDeletedSources() {
        when(dao.lock(7, id)).thenReturn(id);
        when(dao.findTurn(7, id, messageId))
                .thenReturn(new ConversationTurn(messageId, "问题", "hash", "{}", false));

        assertEquals(ConversationResultEnum.MESSAGE_ID_CONFLICT, assertThrows(
                ServiceSystemException.class,
                () -> turns.claim(7, id, messageId, "新问题", "different")).getResultCode());
        assertEquals(ChatResultEnum.SOURCE_CHANGED, assertThrows(ServiceSystemException.class,
                () -> turns.claim(7, id, messageId, "问题", "hash")).getResultCode());
    }

    @Test
    void anotherUsersConversationStopsBeforeReadingTurns() {
        assertEquals(ConversationResultEnum.NOT_FOUND, assertThrows(ServiceSystemException.class,
                () -> turns.claim(8, id, messageId, "问题", "hash")).getResultCode());
        verify(dao).lock(8, id);
        verifyNoMoreInteractions(dao);
    }
}
