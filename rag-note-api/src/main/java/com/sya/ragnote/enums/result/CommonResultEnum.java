package com.sya.ragnote.enums.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 业务错误项；提示文案由枚举集中管理。 */
@Getter
@AllArgsConstructor
public enum CommonResultEnum implements ResultCode {
    INVALID_PARAMETER(90001, "请求参数错误"),
    INTERNAL_ERROR(90002, "服务内部错误"),
    CSRF_VALIDATION_FAILED(90003, "CSRF校验失败");

    private final int code;
    private final String msg;

    public static String getMsg(int code) {
        for (CommonResultEnum item : values()) {
            if (item.code == code) {
                return item.msg;
            }
        }
        return "";
    }
}
