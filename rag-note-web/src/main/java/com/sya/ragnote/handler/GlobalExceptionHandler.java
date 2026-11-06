package com.sya.ragnote.handler;

import com.sya.ragnote.enums.result.CommonResultEnum;
import com.sya.ragnote.enums.result.NoteResultEnum;
import com.sya.ragnote.exception.ServiceSystemException;
import com.sya.ragnote.web.Responses;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolationException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
@RestControllerAdvice
public class GlobalExceptionHandler {
    private final Responses responses;

    @ExceptionHandler(ServiceSystemException.class)
    public void business(ServiceSystemException error, HttpServletResponse response)
            throws IOException {
        // 不记录供应商异常正文；原始原因保留在异常链中供受控调试使用。
        log.warn(
                "Business request failed code={} causeType={}",
                error.getResultCode().getCode(),
                error.getCause() == null ? "none" : error.getCause().getClass().getName());
        responses.error(response, error.getResultCode());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public void uploadTooLarge(MaxUploadSizeExceededException error, HttpServletResponse response)
            throws IOException {
        responses.error(response, NoteResultEnum.FILE_TOO_LARGE);
    }

    @ExceptionHandler({
        MethodArgumentNotValidException.class,
        HandlerMethodValidationException.class,
        ConstraintViolationException.class,
        HttpMessageNotReadableException.class,
        MethodArgumentTypeMismatchException.class,
        ServletRequestBindingException.class,
        HttpMediaTypeNotSupportedException.class,
        MultipartException.class
    })
    public void invalid(Exception error, HttpServletResponse response) throws IOException {
        responses.error(response, CommonResultEnum.INVALID_PARAMETER);
    }

    @ExceptionHandler(Exception.class)
    public void unexpected(Exception error, HttpServletResponse response) throws IOException {
        // SQL/模型异常可能包含正文或凭据；只输出类型与堆栈位置，不输出异常消息。
        log.error(
                "Internal request failure type={} stack={}",
                error.getClass().getName(),
                error.getStackTrace());
        responses.error(response, CommonResultEnum.INTERNAL_ERROR);
    }
}
