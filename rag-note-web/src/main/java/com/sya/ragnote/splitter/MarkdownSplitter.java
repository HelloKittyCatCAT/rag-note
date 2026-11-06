package com.sya.ragnote.splitter;

import com.sya.ragnote.config.RagProperties;
import com.sya.ragnote.enums.result.ChatResultEnum;
import com.sya.ragnote.enums.result.NoteResultEnum;
import com.sya.ragnote.exception.ServiceSystemException;
import com.sya.ragnote.parser.MarkdownParser;
import com.sya.ragnote.tokenizer.TokenCounter;

import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class MarkdownSplitter {
    public record Chunk(
            String headingPath,
            String content,
            String embeddingText,
            int startLine,
            int endLine,
            int tokenCount) {}

    private record Piece(String text, int start, int end) {}

    private final TokenCounter tokens;
    private final RagProperties properties;

    public MarkdownSplitter(TokenCounter tokens, RagProperties properties) {
        this.tokens = tokens;
        this.properties = properties;
    }

    public List<Chunk> split(MarkdownParser.Parsed parsed) {
        int max = properties.getIngestion().getChunkTokens();
        int overlap = properties.getIngestion().getOverlapTokens();
        List<Chunk> result = new ArrayList<>();
        String path = null;
        List<Piece> buffer = new ArrayList<>();
        for (var section : parsed.sections()) {
            if (Thread.currentThread().isInterrupted())
                throw new ServiceSystemException(ChatResultEnum.MODEL_TIMEOUT);
            if (path != null && !path.equals(section.headingPath())) {
                flush(result, path, buffer);
                buffer.clear();
            }
            path = section.headingPath();
            String prefix = prefix(path);
            int budget = max - tokens.count(prefix) - 2;
            int line = section.startLine();
            for (String row : section.text().split("\n", -1)) {
                for (String part :
                        row.isEmpty() ? List.of("") : parts(row, Math.max(1, budget - overlap))) {
                    Piece piece =
                            new Piece(
                                    part,
                                    Math.min(line, section.endLine()),
                                    Math.min(line, section.endLine()));
                    if (!buffer.isEmpty()
                            && tokens.count(prefix + join(buffer) + "\n" + part) > max) {
                        flush(result, path, buffer);
                        List<Piece> tail = new ArrayList<>();
                        for (int i = buffer.size() - 1; i >= 0; i--) {
                            tail.add(0, buffer.get(i));
                            if (tokens.count(join(tail)) > overlap) {
                                Piece removed = tail.remove(0);
                                if (tail.isEmpty()) {
                                    String suffix = removed.text();
                                    while (!suffix.isEmpty() && tokens.count(suffix) > overlap)
                                        suffix = suffix.substring(suffix.offsetByCodePoints(0, 1));
                                    if (!suffix.isBlank())
                                        tail.add(new Piece(suffix, removed.start(), removed.end()));
                                }
                                break;
                            }
                        }
                        buffer.clear();
                        buffer.addAll(tail);
                        if (!buffer.isEmpty()
                                && tokens.count(prefix + join(buffer) + "\n" + part) > max)
                            buffer.clear();
                    }
                    buffer.add(piece);
                }
                line++;
            }
        }
        if (path != null) flush(result, path, buffer);
        return List.copyOf(result);
    }

    private void flush(List<Chunk> result, String path, List<Piece> buffer) {
        if (buffer.isEmpty() || join(buffer).isBlank()) return;
        String content = prefix(path) + join(buffer);
        if (tokens.count(content) > properties.getIngestion().getChunkTokens())
            throw new ServiceSystemException(NoteResultEnum.CHUNK_LIMIT_EXCEEDED);
        result.add(
                new Chunk(
                        path,
                        join(buffer),
                        content,
                        buffer.get(0).start(),
                        buffer.get(buffer.size() - 1).end(),
                        tokens.count(content)));
        if (result.size() > properties.getIngestion().getMaxChunks())
            throw new ServiceSystemException(NoteResultEnum.CHUNK_LIMIT_EXCEEDED);
    }

    private String join(List<Piece> list) {
        return String.join("\n", list.stream().map(Piece::text).toList());
    }

    private String prefix(String path) {
        if (path.isBlank()) return "";
        int low = 0, high = path.codePointCount(0, path.length()), best = 0;
        while (low <= high) {
            int mid = (low + high) >>> 1;
            String value = path.substring(0, path.offsetByCodePoints(0, mid));
            if (tokens.count(value + "\n") <= 80) {
                best = mid;
                low = mid + 1;
            } else high = mid - 1;
        }
        return path.substring(0, path.offsetByCodePoints(0, best)) + "\n";
    }

    private List<String> parts(String value, int budget) {
        List<String> out = new ArrayList<>();
        while (!value.isEmpty()) {
            if (Thread.currentThread().isInterrupted())
                throw new ServiceSystemException(ChatResultEnum.MODEL_TIMEOUT);
            int points = value.codePointCount(0, value.length());
            int low = 1, high = points, best = 0;
            while (low <= high) {
                int mid = (low + high) >>> 1;
                String part = value.substring(0, value.offsetByCodePoints(0, mid));
                if (tokens.count(part) <= budget) {
                    best = mid;
                    low = mid + 1;
                } else high = mid - 1;
            }
            if (best == 0) throw new ServiceSystemException(NoteResultEnum.CHUNK_LIMIT_EXCEEDED);
            int index = value.offsetByCodePoints(0, best);
            out.add(value.substring(0, index));
            value = value.substring(index);
        }
        return out;
    }
}
