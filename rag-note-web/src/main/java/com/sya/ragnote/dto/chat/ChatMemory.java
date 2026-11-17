package com.sya.ragnote.dto.chat;

import java.util.List;

/** 对话历史只帮助理解指代，画像只用于调整表达。 */
public record ChatMemory(List<PreviousTurn> history, String recap, String preference) {
    public static final ChatMemory EMPTY = new ChatMemory(List.of(), "", "");

    public ChatMemory {
        history = List.copyOf(history);
    }
}
