package com.sya.ragnote.enums.result;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ConversationResultEnum implements ResultCode {
    NOT_FOUND(41001, "会话不存在"),
    TURN_IN_PROGRESS(41002, "该会话已有问答正在处理"),
    MESSAGE_ID_CONFLICT(41003, "消息编号已用于不同的请求"),
    TURN_EXPIRED(41004, "问答轮次已过期，请重试");

    private final int code;
    private final String msg;

    public static String getMsg(int code) {
        for (var item : values()) {
            if (item.code == code) return item.msg;
        }
        return "";
    }
}
