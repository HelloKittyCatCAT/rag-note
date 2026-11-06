package com.sya.ragnote.vo.note;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.Instant;
import java.util.UUID;

/** 笔记元数据 */
@Data
@Schema(description = "笔记元数据")
public class NoteVo {
    @Schema(description = "笔记编号")
    private UUID noteId;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "原文件名")
    private String originalFilename;

    @Schema(description = "文件字节数")
    private int byteSize;

    @Schema(description = "分块数")
    private int chunkCount;

    @Schema(description = "索引状态")
    private com.sya.ragnote.enums.note.NoteStatus status;

    @Schema(description = "上传是否复用同内容记录，列表中为false")
    private boolean duplicated;

    @Schema(description = "创建时间，UTC")
    private Instant createdAt;

}
