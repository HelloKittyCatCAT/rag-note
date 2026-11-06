package com.sya.ragnote.converter.note;

import com.sya.ragnote.entity.note.Note;
import com.sya.ragnote.enums.note.NoteStatus;
import com.sya.ragnote.vo.note.*;

import org.springframework.stereotype.Component;

@Component
public class NoteConverter {
    public NoteVo metadata(Note note, boolean duplicated) {
        NoteVo vo = new NoteVo();
        copy(note, vo, duplicated);
        return vo;
    }

    public NoteDetailVo detail(Note note) {
        NoteDetailVo vo = new NoteDetailVo();
        copy(note, vo, false);
        vo.setRawMarkdown(note.getRawMarkdown());
        vo.setPlainText(note.getPlainText());
        return vo;
    }

    private void copy(Note note, NoteVo vo, boolean duplicated) {
        vo.setNoteId(note.getId());
        vo.setTitle(note.getTitle());
        vo.setOriginalFilename(note.getOriginalFilename());
        vo.setByteSize(note.getByteSize());
        vo.setChunkCount(note.getChunkCount());
        vo.setStatus(NoteStatus.READY);
        vo.setDuplicated(duplicated);
        vo.setCreatedAt(note.getCreatedAt());
    }
}
