package com.sya.ragnote.service.impl.profile;

import com.sya.ragnote.client.chat.ChatModelClient;
import com.sya.ragnote.dao.conversation.ConversationDao;
import com.sya.ragnote.dao.profile.ProfileDao;
import com.sya.ragnote.entity.conversation.Conversation;
import com.sya.ragnote.entity.conversation.ConversationTurn;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class ProfileServiceImplTest {
    @Test
    void suggestionRequiresUserQuoteAndDoesNotSaveProfile() {
        var profiles = mock(ProfileDao.class);
        var conversations = mock(ConversationDao.class);
        var model = mock(ChatModelClient.class);
        var service = new ProfileServiceImpl(profiles, conversations, model);
        UUID id = UUID.randomUUID();
        UUID message = UUID.randomUUID();
        when(conversations.find(7, id)).thenReturn(new Conversation(id, "学习", Instant.now()));
        when(conversations.history(7, id, 0, 4)).thenReturn(
                List.of(new ConversationTurn(message, "我希望多举例", "hash", "{}", true)));
        when(model.generate(anyString(), anyString())).thenReturn("""
            {"items":[
              {"kind":"EXPLANATION_PREFERENCE","value":"多举例","clientMessageId":"%s","quote":"多举例"},
              {"kind":"LEARNING_GOAL","value":"精通 Redis","clientMessageId":"%s","quote":"精通 Redis"}
            ]}
            """.formatted(message, message));

        var suggestions = service.suggest(7, id);
        assertEquals(List.of("多举例"), suggestions.items().stream().map(item -> item.value()).toList());
        verifyNoInteractions(profiles);
    }
}
