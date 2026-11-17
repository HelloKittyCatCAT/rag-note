package com.sya.ragnote.enums.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ResearchResultEnum implements ResultCode {
    DISABLED(42001, "研究功能未开启"),
    WEB_UNAVAILABLE(42002, "网页搜索暂不可用"),
    PAGE_UNREADABLE(42004, "网页不可读取"),
    INVALID_TOOL_ARGUMENT(42006, "工具参数无效");

    private final int code;
    private final String msg;

    public static String getMsg(int code) {
        for (var item : values()) {
            if (item.code == code) return item.msg;
        }
        return "";
    }
}
