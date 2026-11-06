package com.sya.ragnote.converter.chat;

import com.sya.ragnote.dto.chat.ValidatedAnswer;
import com.sya.ragnote.vo.chat.*;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ChatConverter {
    public AskQuestionVo toVo(ValidatedAnswer result) {
        var vo = new AskQuestionVo();
        vo.setAnswer(result.answer());
        vo.setStatus(result.status());
        var sources = new ArrayList<SourceVo>();
        int index = 1;
        // 标题、章节和行号取自数据库检索结果，模型只引用服务端分配的编号。
        for (var e : result.evidence()) {
            var s = new SourceVo();
            s.setSourceId("S" + index++);
            s.setNoteId(e.noteId());
            s.setChunkId(e.chunkId());
            s.setTitle(e.title());
            s.setHeadingPath(e.headingPath());
            s.setStartLine(e.startLine());
            s.setEndLine(e.endLine());
            s.setScore(e.score());
            int length = e.content().codePointCount(0, e.content().length());
            s.setSnippet(
                    e.content()
                            .substring(
                                    0, e.content().offsetByCodePoints(0, Math.min(500, length))));
            sources.add(s);
        }
        vo.setSources(List.copyOf(sources));
        return vo;
    }
}
