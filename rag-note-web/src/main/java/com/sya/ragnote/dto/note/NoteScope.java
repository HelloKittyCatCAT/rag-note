package com.sya.ragnote.dto.note;

import java.util.List;
import java.util.UUID;

public record NoteScope(List<UUID> ids, boolean allOwned, boolean empty) {
    public NoteScope {
        ids = List.copyOf(ids);
    }
}
