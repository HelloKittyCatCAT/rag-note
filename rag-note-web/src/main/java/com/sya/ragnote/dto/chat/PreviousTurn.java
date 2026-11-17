package com.sya.ragnote.dto.chat;

/** 已完成的历史问答，用于理解追问。 */
public record PreviousTurn(String question, String answer) {}
