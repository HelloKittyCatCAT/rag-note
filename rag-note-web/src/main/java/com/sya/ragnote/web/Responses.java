package com.sya.ragnote.web;

import com.sya.ragnote.enums.result.ResultCode;
import com.sya.ragnote.vo.common.ResultVo;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
public class Responses {
    private final JsonMapper json = JsonMapper.builder().build();

    public <T> ResultVo<T> ok(T data) {
        return new ResultVo<>(0, "成功", data, requestId());
    }

    public ResultVo<Void> failure(ResultCode code) {
        return new ResultVo<>(code.getCode(), code.getMsg(), null, requestId());
    }

    public String requestId() {
        if (RequestContextHolder.getRequestAttributes()
                instanceof ServletRequestAttributes attributes) {
            Object id = attributes.getRequest().getAttribute(RequestIdFilter.ATTRIBUTE);
            if (id != null) {
                return id.toString();
            }
        }
        return UUID.randomUUID().toString();
    }

    public <T> void writeOk(HttpServletResponse response, T data) throws IOException {
        response.setStatus(200);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json");
        response.setHeader("Cache-Control", "no-store");
        String id = response.getHeader("X-Request-Id");
        if (id == null) {
            id = requestId();
            response.setHeader("X-Request-Id", id);
        }
        response.getWriter().write(json.writeValueAsString(new ResultVo<>(0, "成功", data, id)));
    }

    public void error(HttpServletResponse response, ResultCode code) throws IOException {
        response.setStatus(status(code));
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json");
        response.setHeader("Cache-Control", "no-store");
        String id = response.getHeader("X-Request-Id");
        if (id == null) {
            id = requestId();
            response.setHeader("X-Request-Id", id);
        }
        response.getWriter()
                .write(
                        json.writeValueAsString(
                                new ResultVo<>(code.getCode(), code.getMsg(), null, id)));
    }

    public static int status(ResultCode result) {
        return switch (result.getCode()) {
            case 10001, 10003 -> 401;
            case 10002, 10005, 90003 -> 403;
            case 20002 -> 413;
            case 20006, 40001, 41001 -> 404;
            case 20007, 30004, 40002, 41002, 41003, 41004 -> 409;
            case 20008, 42001, 42002 -> 503;
            case 30001 -> 502;
            case 30002 -> 504;
            case 30003 -> 429;
            case 90002 -> 500;
            default -> 400;
        };
    }
}
