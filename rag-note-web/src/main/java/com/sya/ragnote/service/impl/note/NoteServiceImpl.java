package com.sya.ragnote.service.impl.note;

import com.sya.ragnote.client.embedding.EmbeddingClient;
import com.sya.ragnote.config.RagProperties;
import com.sya.ragnote.converter.note.NoteConverter;
import com.sya.ragnote.dao.note.NoteDao;
import com.sya.ragnote.dao.user.UserDao;
import com.sya.ragnote.entity.note.*;
import com.sya.ragnote.enums.result.*;
import com.sya.ragnote.exception.ServiceSystemException;
import com.sya.ragnote.param.note.*;
import com.sya.ragnote.parser.MarkdownParser;
import com.sya.ragnote.service.note.*;
import com.sya.ragnote.splitter.MarkdownSplitter;
import com.sya.ragnote.vo.common.PageVo;
import com.sya.ragnote.vo.note.*;

import jakarta.annotation.PreDestroy;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.*;
import java.nio.charset.*;
import java.security.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

@lombok.extern.slf4j.Slf4j
@Service
public class NoteServiceImpl implements NoteService {
    private final NoteDao notes;
    private final UserDao users;
    private final NoteIndexService index;
    private final MarkdownParser parser;
    private final MarkdownSplitter splitter;
    private final EmbeddingClient embedding;
    private final NoteConverter converter;
    private final RagProperties properties;
    private final Semaphore permits;
    private final ExecutorService workers;

    public NoteServiceImpl(
            NoteDao notes,
            UserDao users,
            NoteIndexService index,
            MarkdownParser parser,
            MarkdownSplitter splitter,
            EmbeddingClient embedding,
            NoteConverter converter,
            RagProperties properties) {
        this.notes = notes;
        this.users = users;
        this.index = index;
        this.parser = parser;
        this.splitter = splitter;
        this.embedding = embedding;
        this.converter = converter;
        this.properties = properties;
        this.permits = new Semaphore(properties.getIngestion().getMaxConcurrent());
        this.workers =
                Executors.newFixedThreadPool(
                        properties.getIngestion().getMaxConcurrent(),
                        r -> {
                            Thread t = new Thread(r, "note-ingestion");
                            t.setDaemon(true);
                            return t;
                        });
    }

    public NoteVo upload(long ownerId, UploadNoteParam param, InputStream stream, String filename) {
        long deadline = System.nanoTime() + 90_000_000_000L;
        if (!permits.tryAcquire())
            throw new ServiceSystemException(ChatResultEnum.REQUEST_LIMIT_EXCEEDED);
        FutureTask<NoteVo> task =
                new FutureTask<>(
                        () -> uploadWithinDeadline(ownerId, param, stream, filename, deadline)) {
                    @Override
                    public void run() {
                        try {
                            super.run();
                        } finally {
                            permits.release();
                        }
                    }
                };
        try {
            workers.execute(task);
        } catch (RejectedExecutionException e) {
            permits.release();
            throw new ServiceSystemException(ChatResultEnum.REQUEST_LIMIT_EXCEEDED, e);
        }
        try {
            return task.get(Math.max(1, deadline - System.nanoTime()), TimeUnit.NANOSECONDS);
        } catch (TimeoutException e) {
            task.cancel(true);
            throw new ServiceSystemException(ChatResultEnum.MODEL_TIMEOUT, e);
        } catch (InterruptedException e) {
            task.cancel(true);
            Thread.currentThread().interrupt();
            throw new ServiceSystemException(ChatResultEnum.MODEL_TIMEOUT, e);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof RuntimeException cause) throw cause;
            throw new ServiceSystemException(CommonResultEnum.INTERNAL_ERROR, e.getCause());
        }
    }

    @PreDestroy
    public void shutdown() {
        workers.shutdownNow();
    }

    private NoteVo uploadWithinDeadline(
            long ownerId,
            UploadNoteParam param,
            InputStream stream,
            String filename,
            long deadline) {
        active(ownerId);
        if (filename == null || !filename.toLowerCase(Locale.ROOT).endsWith(".md"))
            throw new ServiceSystemException(NoteResultEnum.UNSUPPORTED_FILE_TYPE);
        String original = filename.replace('\\', '/');
        original = original.substring(original.lastIndexOf('/') + 1);
        original =
                truncate(original.substring(0, original.length() - 3), 252)
                        + original.substring(original.length() - 3);
        byte[] bytes = read(stream, deadline);
        String hash;
        try {
            hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
        // 按用户和原始文件的哈希去重，重复上传可直接复用已有向量。
        Note existing = notes.findOwnedByHash(ownerId, hash);
        if (existing != null) return converter.metadata(existing, true);
        String raw;
        try {
            raw =
                    StandardCharsets.UTF_8
                            .newDecoder()
                            .onMalformedInput(CodingErrorAction.REPORT)
                            .onUnmappableCharacter(CodingErrorAction.REPORT)
                            .decode(ByteBuffer.wrap(bytes))
                            .toString();
        } catch (CharacterCodingException e) {
            throw new ServiceSystemException(NoteResultEnum.INVALID_FILE_ENCODING, e);
        }
        // 原文用于详情展示，解析副本去掉开头的 UTF-8 BOM。
        String sourceMarkdown = raw;
        if (raw.startsWith("\uFEFF")) raw = raw.substring(1);
        if (raw.indexOf('\0') >= 0)
            throw new ServiceSystemException(CommonResultEnum.INVALID_PARAMETER);
        if (raw.isBlank()) throw new ServiceSystemException(NoteResultEnum.EMPTY_NOTE_CONTENT);
        var parsed = parser.parse(raw);
        if (parsed.plainText().isBlank())
            throw new ServiceSystemException(NoteResultEnum.EMPTY_NOTE_CONTENT);
        var pieces = splitter.split(parsed);
        if (pieces.isEmpty()) throw new ServiceSystemException(NoteResultEnum.EMPTY_NOTE_CONTENT);
        {
            if (System.nanoTime() > deadline)
                throw new ServiceSystemException(ChatResultEnum.MODEL_TIMEOUT);
            // 向量化在事务外进行。分块正文用于引用，向量化文本还包含章节前缀。
            List<float[]> vectors =
                    embedding.embedAll(
                            pieces.stream().map(MarkdownSplitter.Chunk::embeddingText).toList());
            if (System.nanoTime() > deadline)
                throw new ServiceSystemException(ChatResultEnum.MODEL_TIMEOUT);
            if (vectors == null || vectors.size() != pieces.size())
                throw new ServiceSystemException(NoteResultEnum.EMBEDDING_CONFIGURATION_MISMATCH);
            Note note = new Note();
            note.setId(UUID.randomUUID());
            note.setOwnerId(ownerId);
            note.setOriginalFilename(original);
            note.setByteSize(bytes.length);
            note.setContentSha256(hash);
            note.setRawMarkdown(sourceMarkdown);
            note.setPlainText(parsed.plainText());
            note.setChunkCount(pieces.size());
            note.setStatus("READY");
            note.setCreatedAt(Instant.now());
            String title = param.getTitle();
            if (title == null) title = parsed.firstHeading();
            if (title == null || title.isBlank())
                title = original.substring(0, original.length() - 3);
            if (title.isBlank()) title = "Untitled";
            note.setTitle(truncate(title.strip(), 200));
            List<NoteChunk> chunks = new ArrayList<>();
            for (int i = 0; i < pieces.size(); i++) {
                float[] vector = vectors.get(i);
                if (vector == null
                        || vector.length != properties.getModels().getEmbedding().getDimensions())
                    throw new ServiceSystemException(
                            NoteResultEnum.EMBEDDING_CONFIGURATION_MISMATCH);
                // 绑定到 SQL 的是向量字面量，pgvector 会把它转为 vector(1024)。
                StringJoiner encoded = new StringJoiner(",", "[", "]");
                for (float value : vector) {
                    if (!Float.isFinite(value))
                        throw new ServiceSystemException(
                                NoteResultEnum.EMBEDDING_CONFIGURATION_MISMATCH);
                    encoded.add(Float.toString(value));
                }
                var piece = pieces.get(i);
                NoteChunk c = new NoteChunk();
                c.setId(UUID.randomUUID());
                c.setNoteId(note.getId());
                c.setOwnerId(ownerId);
                c.setChunkIndex(i);
                c.setHeadingPath(piece.headingPath());
                c.setContent(piece.content());
                c.setStartLine(piece.startLine());
                c.setEndLine(piece.endLine());
                c.setTokenCount(piece.tokenCount());
                c.setEmbeddingModel(embedding.modelId());
                c.setEmbedding(encoded.toString());
                chunks.add(c);
            }
            // 全部分块都有向量后，再通过独立的事务服务一起保存。
            Note saved;
            try {
                saved = index.saveIndexedNote(note, chunks, deadline);
            } catch (DuplicateKeyException e) {
                saved = notes.findOwnedByHash(ownerId, hash);
                if (saved == null) throw e;
            }
            log.info(
                    "Note ingestion completed ownerId={} noteId={} bytes={} chunks={} elapsedMs={}",
                    ownerId,
                    saved.getId(),
                    bytes.length,
                    saved.getChunkCount(),
                    (System.nanoTime() - (deadline - 90_000_000_000L)) / 1_000_000);
            return converter.metadata(saved, !saved.getId().equals(note.getId()));
        }
    }

    public PageVo<NoteVo> list(long ownerId, QueryNoteParam param) {
        active(ownerId);
        return new PageVo<>(
                notes
                        .listOwned(
                                ownerId,
                                ((long) param.getPage() - 1) * param.getSize(),
                                param.getSize())
                        .stream()
                        .map(n -> converter.metadata(n, false))
                        .toList(),
                notes.countOwned(ownerId),
                param.getPage(),
                param.getSize());
    }

    public NoteDetailVo detail(long ownerId, UUID id) {
        active(ownerId);
        Note note = notes.findOwned(ownerId, id);
        if (note == null) throw new ServiceSystemException(NoteResultEnum.NOTE_NOT_FOUND);
        return converter.detail(note);
    }

    public void delete(long ownerId, UUID id) {
        active(ownerId);
        index.delete(ownerId, id);
    }

    private void active(long ownerId) {
        if (ownerId <= 0) throw new ServiceSystemException(AuthResultEnum.NOT_AUTHENTICATED);
        var user = users.findById(ownerId);
        if (user == null) throw new ServiceSystemException(AuthResultEnum.NOT_AUTHENTICATED);
        if (!user.isEnabled()) throw new ServiceSystemException(AuthResultEnum.ACCOUNT_DISABLED);
    }

    private byte[] read(InputStream stream, long deadline) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int length;
            while ((length = stream.read(buffer)) != -1) {
                if (System.nanoTime() > deadline)
                    throw new ServiceSystemException(ChatResultEnum.MODEL_TIMEOUT);
                if (out.size() + length > 2 * 1024 * 1024)
                    throw new ServiceSystemException(NoteResultEnum.FILE_TOO_LARGE);
                out.write(buffer, 0, length);
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new ServiceSystemException(NoteResultEnum.INVALID_FILE_ENCODING, e);
        }
    }

    private String truncate(String value, int max) {
        return value.codePointCount(0, value.length()) > max
                ? value.substring(0, value.offsetByCodePoints(0, max))
                : value;
    }
}
