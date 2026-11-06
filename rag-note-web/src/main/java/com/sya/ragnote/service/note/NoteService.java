package com.sya.ragnote.service.note;

import com.sya.ragnote.param.note.*;
import com.sya.ragnote.vo.common.PageVo;
import com.sya.ragnote.vo.note.*;

import java.io.InputStream;
import java.util.UUID;

public interface NoteService {
    NoteVo upload(long ownerId, UploadNoteParam param, InputStream content, String filename);

    PageVo<NoteVo> list(long ownerId, QueryNoteParam param);

    NoteDetailVo detail(long ownerId, UUID noteId);

    void delete(long ownerId, UUID noteId);
}
