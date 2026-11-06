package com.sya.ragnote.service.impl.note;

import com.sya.ragnote.client.embedding.EmbeddingClient;
import com.sya.ragnote.config.RagProperties;
import com.sya.ragnote.dao.note.NoteChunkDao;
import com.sya.ragnote.dao.note.NoteDao;
import com.sya.ragnote.dao.user.UserDao;
import com.sya.ragnote.dto.note.*;
import com.sya.ragnote.enums.result.*;
import com.sya.ragnote.exception.ServiceSystemException;
import com.sya.ragnote.service.note.NoteSearchService;
import com.sya.ragnote.tokenizer.TokenCounter;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class NoteSearchServiceImpl implements NoteSearchService {
    private final NoteDao notes;
    private final NoteChunkDao chunks;
    private final UserDao users;
    private final EmbeddingClient embeddings;
    private final TokenCounter tokens;
    private final RagProperties properties;

    public NoteSearchServiceImpl(
            NoteDao notes,
            NoteChunkDao chunks,
            UserDao users,
            EmbeddingClient embeddings,
            TokenCounter tokens,
            RagProperties properties) {
        this.notes = notes;
        this.chunks = chunks;
        this.users = users;
        this.embeddings = embeddings;
        this.tokens = tokens;
        this.properties = properties;
    }

    @Override
    public NoteScope validateScope(long ownerId, List<UUID> noteIds) {
        var user = users.findById(ownerId);
        if (user == null) {
            throw new ServiceSystemException(AuthResultEnum.NOT_AUTHENTICATED);
        }
        if (!user.isEnabled()) {
            throw new ServiceSystemException(AuthResultEnum.ACCOUNT_DISABLED);
        }
        // 省略范围表示本人全库，显式传入 ID 时逐个验证归属。
        if (noteIds == null) {
            return new NoteScope(List.of(), true, notes.countOwned(ownerId) == 0);
        }
        if (noteIds.isEmpty()
                || noteIds.size() > 20
                || noteIds.stream().anyMatch(Objects::isNull)) {
            throw new ServiceSystemException(CommonResultEnum.INVALID_PARAMETER);
        }
        var ids = List.copyOf(new LinkedHashSet<>(noteIds));
        if (!new HashSet<>(notes.findOwnedIds(ownerId, ids)).containsAll(ids)) {
            throw new ServiceSystemException(NoteResultEnum.NOTE_NOT_FOUND);
        }
        return new NoteScope(ids, false, false);
    }

    @Override
    public List<NoteEvidence> search(long ownerId, NoteSearchQuery query) {
        if (query.scope().empty()) {
            return List.of();
        }
        if (!query.scope().allOwned() && query.scope().ids().isEmpty()) {
            throw new ServiceSystemException(CommonResultEnum.INVALID_PARAMETER);
        }
        var scopeIds = query.scope().allOwned() ? null : query.scope().ids();
        // 问题向量和分块向量必须来自同一套模型配置，才能比较距离。
        if (chunks.hasIncompatibleModel(ownerId, scopeIds, embeddings.modelId())) {
            throw new ServiceSystemException(NoteResultEnum.EMBEDDING_CONFIGURATION_MISMATCH);
        }
        int topK = Math.min(6, Math.max(1, query.topK()));
        var candidates =
                new ArrayList<>(
                        chunks.search(
                                ownerId,
                                scopeIds,
                                embeddings.embed(query.query()),
                                embeddings.modelId(),
                                Math.min(18, topK * 3),
                                query.minScore()));
        candidates.sort(
                Comparator.comparingDouble(NoteEvidence::score)
                        .reversed()
                        .thenComparing(e -> e.chunkId().toString()));
        // 先多取一些候选，再去除同一笔记中重复或高度重叠的片段，给其他证据留空间。
        var selected = new ArrayList<NoteEvidence>();
        int budget = 0;
        for (var candidate : candidates) {
            if (!Double.isFinite(candidate.score()) || candidate.score() < query.minScore()) {
                continue;
            }
            if (selected.stream()
                    .anyMatch(
                            e ->
                                    e.noteId().equals(candidate.noteId())
                                            && overlaps(e.content(), candidate.content()))) {
                continue;
            }
            int cost = tokens.count(candidate.content());
            if (budget + cost > Math.min(4000, properties.getRetrieval().getMaxEvidenceTokens())) {
                continue;
            }
            selected.add(candidate);
            budget += cost;
            if (selected.size() == topK) {
                break;
            }
        }
        return List.copyOf(selected);
    }

    private static boolean overlaps(String a, String b) {
        a = a.replace("\r\n", "\n").replace('\r', '\n').strip();
        b = b.replace("\r\n", "\n").replace('\r', '\n').strip();
        if (a.isEmpty() || b.isEmpty()) {
            return a.equals(b);
        }
        if (a.contains(b) || b.contains(a)) {
            return true;
        }
        int shortest = Math.min(a.length(), b.length());
        for (int n = shortest; n * 2 >= shortest; n--) {
            if (a.regionMatches(a.length() - n, b, 0, n)
                    || b.regionMatches(b.length() - n, a, 0, n)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void validateEvidence(long ownerId, List<NoteEvidence> evidence) {
        var user = users.findById(ownerId);
        if (user == null) {
            throw new ServiceSystemException(AuthResultEnum.NOT_AUTHENTICATED);
        }
        if (!user.isEnabled()) {
            throw new ServiceSystemException(AuthResultEnum.ACCOUNT_DISABLED);
        }
        if (evidence.isEmpty()) {
            return;
        }
        // 生成期间笔记可能被删除，返回前重新核对笔记和分块的归属与存续。
        var noteIds = evidence.stream().map(NoteEvidence::noteId).distinct().toList();
        var chunkIds = evidence.stream().map(NoteEvidence::chunkId).distinct().toList();
        if (!new HashSet<>(notes.findOwnedIds(ownerId, noteIds)).containsAll(noteIds)
                || !new HashSet<>(chunks.findOwnedChunkIds(ownerId, chunkIds))
                        .containsAll(chunkIds)) {
            throw new ServiceSystemException(ChatResultEnum.SOURCE_CHANGED);
        }
    }
}
