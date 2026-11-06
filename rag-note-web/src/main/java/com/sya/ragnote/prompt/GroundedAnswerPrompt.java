package com.sya.ragnote.prompt;

import com.sya.ragnote.dto.note.NoteEvidence;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Component
public class GroundedAnswerPrompt {
    private final JsonMapper mapper = JsonMapper.builder().build();
    private final String system;

    public GroundedAnswerPrompt() throws IOException {
        try (var in = new ClassPathResource("prompts/grounded-answer.txt").getInputStream()) {
            system = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    public String system() {
        return system;
    }

    public String user(String question, List<NoteEvidence> evidence) {
        var sources = new ArrayList<PromptSource>();
        int index = 1;
        for (var item : evidence) {
            sources.add(new PromptSource("S" + index++, item.title(), item.content()));
        }
        return mapper.writeValueAsString(new PromptInput(question, List.copyOf(sources)));
    }

    private record PromptSource(String sourceId, String title, String content) {}

    private record PromptInput(String question, List<PromptSource> untrustedNoteEvidence) {}
}
