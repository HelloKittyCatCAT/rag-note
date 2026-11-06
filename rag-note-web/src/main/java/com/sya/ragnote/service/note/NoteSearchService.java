package com.sya.ragnote.service.note;

import com.sya.ragnote.dto.note.*;

import java.util.List;
import java.util.UUID;

public interface NoteSearchService {
    NoteScope validateScope(long ownerId, List<UUID> noteIds);

    List<NoteEvidence> search(long ownerId, NoteSearchQuery query);

    void validateEvidence(long ownerId, List<NoteEvidence> evidence);
}
