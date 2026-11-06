package com.sya.ragnote.enums.result;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 业务错误项；提示文案由枚举集中管理。 */
@Getter
@AllArgsConstructor
public enum NoteResultEnum implements ResultCode {
    UNSUPPORTED_FILE_TYPE(20001, "仅支持Markdown文件"),
    FILE_TOO_LARGE(20002, "文件超过大小限制"),
    INVALID_FILE_ENCODING(20003, "文件必须使用合法UTF-8编码"),
    EMPTY_NOTE_CONTENT(20004, "笔记内容为空"),
    CHUNK_LIMIT_EXCEEDED(20005, "笔记分块数超过限制"),
    NOTE_NOT_FOUND(20006, "笔记不存在"),
    NOTE_QUOTA_EXCEEDED(20007, "笔记数量超过配额"),
    EMBEDDING_CONFIGURATION_MISMATCH(20008, "向量模型配置不一致");

    private final int code;
    private final String msg;

    public static String getMsg(int code) {
        for (NoteResultEnum item : values()) {
            if (item.code == code) {
                return item.msg;
            }
        }
        return "";
    }
}
