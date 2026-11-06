package com.sya.ragnote.dto.chat;

import java.util.List;

public record GroundedModelAnswer(
        String answer, List<String> citedSourceIds, boolean insufficientEvidence) {
    public GroundedModelAnswer {
        citedSourceIds = List.copyOf(citedSourceIds);
    }
}
