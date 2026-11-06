package com.sya.ragnote.client.chat;

import com.sya.ragnote.client.ModelFailure;
import com.sya.ragnote.enums.result.ChatResultEnum;
import com.sya.ragnote.exception.ServiceSystemException;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LangChainChatModelClient implements ChatModelClient {
    private final ChatModel model;

    public LangChainChatModelClient(ChatModel model) {
        this.model = model;
    }

    @Override
    public String generate(String system, String user) {
        try {
            var result = model.chat(List.of(SystemMessage.from(system), UserMessage.from(user)));
            if (result == null || result.aiMessage() == null || result.aiMessage().text() == null) {
                throw new ServiceSystemException(ChatResultEnum.MODEL_UNAVAILABLE);
            }
            return result.aiMessage().text();
        } catch (ServiceSystemException e) {
            throw e;
        } catch (RuntimeException e) {
            throw ModelFailure.translate(e);
        }
    }
}
