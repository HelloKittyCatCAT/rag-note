package com.sya.ragnote.client;

import com.sya.ragnote.enums.result.ChatResultEnum;
import com.sya.ragnote.exception.ServiceSystemException;

import dev.langchain4j.exception.HttpException;
import dev.langchain4j.exception.RateLimitException;
import dev.langchain4j.exception.TimeoutException;

import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;

/** 仅分类已发生的模型异常，不执行重试，不向客户端透传供应商响应。 */
public final class ModelFailure {
    private ModelFailure() {}

    public static ServiceSystemException translate(RuntimeException cause) {
        for (Throwable current = cause; current != null; current = current.getCause()) {
            if (current instanceof RateLimitException
                    || current instanceof HttpException http && http.statusCode() == 429) {
                return new ServiceSystemException(ChatResultEnum.REQUEST_LIMIT_EXCEEDED, cause);
            }
            if (current instanceof TimeoutException
                    || current instanceof HttpTimeoutException
                    || current instanceof SocketTimeoutException) {
                return new ServiceSystemException(ChatResultEnum.MODEL_TIMEOUT, cause);
            }
        }
        return new ServiceSystemException(ChatResultEnum.MODEL_UNAVAILABLE, cause);
    }
}
