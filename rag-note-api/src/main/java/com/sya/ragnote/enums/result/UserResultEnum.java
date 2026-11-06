package com.sya.ragnote.enums.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 业务错误项；提示文案由枚举集中管理。 */
@Getter
@AllArgsConstructor
public enum UserResultEnum implements ResultCode {
    USER_NOT_FOUND(40001, "用户不存在"),
    ADMIN_DISABLE_NOT_ALLOWED(40002, "不可禁用当前管理员或最后一个启用的管理员");

    private final int code;
    private final String msg;

    public static String getMsg(int code) {
        for (UserResultEnum item : values()) {
            if (item.code == code) {
                return item.msg;
            }
        }
        return "";
    }
}
