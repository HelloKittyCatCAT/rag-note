package com.sya.ragnote.dto.chat;

import com.sya.ragnote.enums.result.ChatResultEnum;
import com.sya.ragnote.exception.ServiceSystemException;

import java.time.Instant;

public record ExecutionContext(long ownerId, String requestId, Instant deadline) {
    public void verifyDeadline() {
        if (Thread.currentThread().isInterrupted() || !Instant.now().isBefore(deadline)) {
            throw new ServiceSystemException(ChatResultEnum.MODEL_TIMEOUT);
        }
    }
}
