package com.sya.ragnote.enums.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 业务错误项；提示文案由枚举集中管理。 */
@Getter
@AllArgsConstructor
public enum ChatResultEnum implements ResultCode {
    MODEL_UNAVAILABLE(30001, "模型服务不可用"),
    MODEL_TIMEOUT(30002, "模型响应超时"),
    REQUEST_LIMIT_EXCEEDED(30003, "请求超过并发或模型限额"),
    SOURCE_CHANGED(30004, "回答期间笔记来源已变更");

    private final int code;
    private final String msg;

    public static String getMsg(int code) {
        for (ChatResultEnum item : values()) {
            if (item.code == code) {
                return item.msg;
            }
        }
        return "";
    }
}
