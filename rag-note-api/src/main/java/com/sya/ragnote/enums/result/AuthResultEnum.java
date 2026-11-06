package com.sya.ragnote.enums.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 业务错误项；提示文案由枚举集中管理。 */
@Getter
@AllArgsConstructor
public enum AuthResultEnum implements ResultCode {
    NOT_AUTHENTICATED(10001, "请先登录"),
    ACCOUNT_DISABLED(10002, "账号已禁用"),
    INVALID_CREDENTIALS(10003, "用户名或密码错误"),
    REGISTRATION_FAILED(10004, "注册失败，请检查用户名和邀请码"),
    PERMISSION_DENIED(10005, "无权限执行此操作");

    private final int code;
    private final String msg;

    public static String getMsg(int code) {
        for (AuthResultEnum item : values()) {
            if (item.code == code) {
                return item.msg;
            }
        }
        return "";
    }
}
