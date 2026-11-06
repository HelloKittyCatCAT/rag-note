package com.sya.ragnote.dto.note;

public record NoteSearchQuery(String query, NoteScope scope, int topK, double minScore) {}
