package com.sya.ragnote.service.impl.note;

import com.sya.ragnote.dao.note.NoteDao;
import com.sya.ragnote.service.note.LearningOverviewService;
import com.sya.ragnote.vo.note.LearningOverviewVo;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LearningOverviewServiceImpl implements LearningOverviewService {
    private static final int PAGE_SIZE = 50;
    private static final int MAX_NOTES = 500;
    private final NoteDao notes;

    @Override
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public LearningOverviewVo overview(long ownerId) {
        // 同一快照中分页枚举本人笔记，覆盖数基于实际读到的记录。
        long total = notes.countOwned(ownerId);
        int covered = 0;
        var topics = new LinkedHashMap<String, List<UUID>>();
        while (covered < Math.min(total, MAX_NOTES)) {
            var page = notes.listOwned(ownerId, covered, Math.min(PAGE_SIZE, MAX_NOTES - covered));
            if (page.isEmpty()) break;
            // 按页批量取得章节，一篇笔记最多取八个章节作为主题线索。
            for (var heading : notes.listHeadings(ownerId, page.stream()
                    .map(com.sya.ragnote.entity.note.Note::getId).toList())) {
                topics.computeIfAbsent(heading.headingPath(), title -> new ArrayList<>())
                        .add(heading.noteId());
            }
            for (var note : page) {
                topics.computeIfAbsent(note.getTitle(), title -> new ArrayList<>()).add(note.getId());
            }
            covered += page.size();
        }
        var result = topics.entrySet().stream()
                .map(item -> new LearningOverviewVo.TopicVo(item.getKey(), item.getValue().stream().distinct().toList()))
                .toList();
        return new LearningOverviewVo(total, covered, covered < total, result);
    }
}
